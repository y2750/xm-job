package com.example.service;

import com.example.common.enums.ResultCodeEnum;
import com.example.entity.Account;
import com.example.entity.Message;
import com.example.entity.Submission;
import com.example.entity.Project;
import com.example.entity.Enterprise;
import com.example.entity.Freelancer;
import com.example.exception.CustomException;
import com.example.mapper.MessageMapper;
import com.example.mapper.SubmissionMapper;
import com.example.mapper.ProjectMapper;
import com.example.mapper.EnterpriseMapper;
import com.example.mapper.FreelancerMapper;
import com.example.mapper.ProjectOrderMapper;
import com.example.utils.TokenUtils;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 消息业务层
 */
@Service
public class MessageService {

    @Resource
    private MessageMapper messageMapper;
    @Resource
    private SubmissionMapper submissionMapper;
    @Resource
    private ProjectMapper projectMapper;
    @Resource
    private EnterpriseMapper enterpriseMapper;
    @Resource
    private FreelancerMapper freelancerMapper;
    @Resource
    private ProjectOrderMapper orderMapper;

    /**
     * 发送消息
     */
    public void add(Message message) {
        Account currentUser = TokenUtils.getCurrentUser();
        if (currentUser == null) {
            throw new CustomException(ResultCodeEnum.NO_AUTH_ERROR);
        }

        message.setSenderId(currentUser.getId());
        message.setCreatedAt(LocalDateTime.now());
        message.setIsRead(false);

        // 自动设置senderType
        if ("EMPLOY".equals(currentUser.getRole())) {
            message.setSenderType("ENTERPRISE");
        } else if ("USER".equals(currentUser.getRole())) {
            message.setSenderType("FREELANCER");
        } else {
            throw new CustomException(ResultCodeEnum.NO_AUTH_ERROR, "不支持的用户角色：" + currentUser.getRole());
        }

        Project project = null;

        // 如果提供了projectId，先加载项目信息
        if (message.getProjectId() != null) {
            project = projectMapper.selectById(message.getProjectId());
            if (project == null) {
                throw new CustomException(ResultCodeEnum.PARAM_ERROR, "项目不存在");
            }

            // 检查项目状态，如果已完成，不允许发送消息
            if ("COMPLETED".equals(project.getStatus())) {
                throw new CustomException(ResultCodeEnum.PARAM_ERROR, "项目已完成，无法继续聊天");
            }
        }

        // 如果提供了submissionId，自动设置recipientId和projectId
        if (message.getSubmissionId() != null) {
            Submission submission = submissionMapper.selectById(message.getSubmissionId());
            if (submission == null) {
                throw new CustomException(ResultCodeEnum.PARAM_ERROR, "稿件不存在");
            }

            // 设置projectId
            if (message.getProjectId() == null) {
                message.setProjectId(submission.getProjectId());
                project = projectMapper.selectById(submission.getProjectId());
                if (project == null) {
                    throw new CustomException(ResultCodeEnum.PARAM_ERROR, "项目不存在");
                }
            }

            // 检查项目状态，如果已完成，不允许发送消息
            if ("COMPLETED".equals(project.getStatus())) {
                throw new CustomException(ResultCodeEnum.PARAM_ERROR, "项目已完成，无法继续聊天");
            }

            // 如果是自由职业者发送消息，检查稿件是否属于该自由职业者
            if ("FREELANCER".equals(message.getSenderType())) {
                Freelancer freelancer = freelancerMapper.selectByUserId(message.getSenderId());
                if (freelancer == null) {
                    throw new CustomException(ResultCodeEnum.PARAM_ERROR, "自由职业者信息不存在");
                }

                // 检查稿件是否属于该自由职业者
                if (!submission.getFreelancerId().equals(freelancer.getId())) {
                    throw new CustomException(ResultCodeEnum.PARAM_ERROR, "稿件不属于当前用户");
                }

                // 检查稿件状态：SUBMITTED（已提交）、INTERESTED（有意向）、CONFIRMED（已确认合作）都可以聊天
                if (!"SUBMITTED".equals(submission.getStatus())
                        && !"INTERESTED".equals(submission.getStatus())
                        && !"CONFIRMED".equals(submission.getStatus())) {
                    throw new CustomException(ResultCodeEnum.PARAM_ERROR, "只有在已提交、有意向或已确认合作状态时才能发送消息");
                }
            } else if ("ENTERPRISE".equals(message.getSenderType())) {
                // 企业发送消息，检查稿件状态
                if (!"SUBMITTED".equals(submission.getStatus())
                        && !"INTERESTED".equals(submission.getStatus())
                        && !"CONFIRMED".equals(submission.getStatus())) {
                    throw new CustomException(ResultCodeEnum.PARAM_ERROR, "只有在已提交、有意向或已确认合作状态时才能发送消息");
                }
            }

            // 如果未设置recipientId，根据发送者类型自动确定接收者
            if (message.getRecipientId() == null) {
                // 根据发送者类型确定接收者
                if ("ENTERPRISE".equals(message.getSenderType())) {
                    // 企业发送，接收者是自由职业者
                    Freelancer freelancer = freelancerMapper.selectById(submission.getFreelancerId());
                    if (freelancer != null) {
                        message.setRecipientId(freelancer.getUserId());
                    } else {
                        throw new CustomException(ResultCodeEnum.PARAM_ERROR, "自由职业者信息不存在");
                    }
                } else if ("FREELANCER".equals(message.getSenderType())) {
                    // 自由职业者发送，接收者是企业
                    Enterprise enterprise = enterpriseMapper.selectById(project.getEnterpriseId());
                    if (enterprise != null) {
                        message.setRecipientId(enterprise.getEmployId());
                    } else {
                        throw new CustomException(ResultCodeEnum.PARAM_ERROR, "企业信息不存在");
                    }
                }
            }
        } else if (message.getProjectId() != null && project != null) {
            // 如果没有submissionId但有projectId，允许直接通过项目聊天（未接单也可以）
            // 如果未设置recipientId，根据发送者类型自动确定接收者
            if (message.getRecipientId() == null) {
                if ("FREELANCER".equals(message.getSenderType())) {
                    // 自由职业者发送，接收者是企业
                    Enterprise enterprise = enterpriseMapper.selectById(project.getEnterpriseId());
                    if (enterprise != null) {
                        message.setRecipientId(enterprise.getEmployId());
                    } else {
                        throw new CustomException(ResultCodeEnum.PARAM_ERROR, "企业信息不存在");
                    }
                } else if ("ENTERPRISE".equals(message.getSenderType())) {
                    // 企业发送，尝试从消息历史中查找自由职业者
                    // 查找该项目中第一条自由职业者发送的消息，获取其senderId作为recipientId
                    List<Message> existingMessages = messageMapper.selectByProjectId(project.getId(),
                            currentUser.getId());
                    Message freelancerMessage = existingMessages.stream()
                            .filter(m -> "FREELANCER".equals(m.getSenderType()))
                            .findFirst()
                            .orElse(null);

                    if (freelancerMessage != null) {
                        // 找到了自由职业者发送的消息，使用其senderId作为recipientId
                        message.setRecipientId(freelancerMessage.getSenderId());
                    } else {
                        // 如果没有找到历史消息，检查是否有接单记录
                        // 查询该项目的接单记录，获取接单者的freelancerId
                        List<com.example.entity.ProjectOrder> orders = orderMapper.selectByProjectId(project.getId());
                        com.example.entity.ProjectOrder acceptedOrder = orders.stream()
                                .filter(o -> "ACCEPTED".equals(o.getStatus())
                                        && (o.getAbandoned() == null || !o.getAbandoned()))
                                .findFirst()
                                .orElse(null);

                        if (acceptedOrder != null) {
                            // 找到了接单记录，通过freelancerId获取userId
                            Freelancer freelancer = freelancerMapper.selectById(acceptedOrder.getFreelancerId());
                            if (freelancer != null) {
                                message.setRecipientId(freelancer.getUserId());
                            } else {
                                throw new CustomException(ResultCodeEnum.PARAM_ERROR, "自由职业者信息不存在");
                            }
                        } else {
                            // 如果没有接单记录，提示错误
                            throw new CustomException(ResultCodeEnum.PARAM_ERROR,
                                    "企业发送消息时必须指定接收的自由职业者，请先等待自由职业者接单或发送消息");
                        }
                    }
                }
            }
        }

        // 验证必要字段
        if (message.getSenderId() == null) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR, "发送者ID不能为空");
        }
        if (message.getRecipientId() == null) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR, "接收者ID不能为空，请确保稿件状态为有意向且相关用户信息存在");
        }
        if (message.getSenderType() == null) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR, "发送者类型不能为空");
        }
        if (message.getContent() == null || message.getContent().trim().isEmpty()) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR, "消息内容不能为空");
        }

        try {
            messageMapper.insert(message);
        } catch (Exception e) {
            e.printStackTrace();
            throw new CustomException(ResultCodeEnum.SYSTEM_ERROR, "消息发送失败：" + e.getMessage());
        }
    }

    /**
     * 更新消息（标记已读）
     */
    public void updateById(Message message) {
        messageMapper.updateById(message);
    }

    /**
     * 标记消息已读
     */
    public void markAsRead(Integer id) {
        Message message = messageMapper.selectById(id);
        if (message == null) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR, "消息不存在");
        }

        Account currentUser = TokenUtils.getCurrentUser();
        // 管理员可以标记任何消息为已读，普通用户只能标记自己接收的消息为已读
        if (!"ADMIN".equals(currentUser.getRole()) && !currentUser.getId().equals(message.getRecipientId())) {
            throw new CustomException(ResultCodeEnum.NO_AUTH_ERROR);
        }

        message.setIsRead(true);
        messageMapper.updateById(message);
    }

    /**
     * 根据ID查询
     */
    public Message selectById(Integer id) {
        return messageMapper.selectById(id);
    }

    /**
     * 查询所有
     */
    public List<Message> selectAll(Message message) {
        return messageMapper.selectAll(message);
    }

    /**
     * 分页查询
     */
    public PageInfo<Message> selectPage(Message message, Integer pageNum, Integer pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<Message> list = messageMapper.selectAll(message);
        return PageInfo.of(list);
    }

    /**
     * 根据项目ID查询消息
     */
    public List<Message> selectByProjectId(Integer projectId) {
        Account currentUser = TokenUtils.getCurrentUser();
        if (currentUser == null) {
            throw new CustomException(ResultCodeEnum.NO_AUTH_ERROR);
        }
        // 只返回当前用户相关的消息（sender_id或recipient_id是当前用户），且submission_id为null（通过project直接聊天）
        return messageMapper.selectByProjectId(projectId, currentUser.getId());
    }

    /**
     * 查询未读消息
     */
    public List<Message> selectUnread() {
        Account currentUser = TokenUtils.getCurrentUser();
        if (currentUser == null) {
            throw new CustomException(ResultCodeEnum.NO_AUTH_ERROR);
        }
        return messageMapper.selectUnreadByRecipientId(currentUser.getId());
    }

    /**
     * 统计未读通知数量（排除聊天消息）
     */
    public Integer countUnreadNotifications() {
        Account currentUser = TokenUtils.getCurrentUser();
        if (currentUser == null) {
            return 0;
        }
        Integer count = messageMapper.countUnreadNotifications(currentUser.getId());
        return count != null ? count : 0;
    }

    /**
     * 根据稿件ID查询消息
     */
    public List<Message> selectBySubmissionId(Integer submissionId) {
        return messageMapper.selectBySubmissionId(submissionId);
    }

    /**
     * 获取聊天列表（按submission或project分组，返回每个对话的最新消息）
     */
    public List<Message> getChatList() {
        Account currentUser = TokenUtils.getCurrentUser();
        if (currentUser == null) {
            throw new CustomException(ResultCodeEnum.NO_AUTH_ERROR);
        }
        // 获取基于submission的聊天列表
        List<Message> chatList = messageMapper.selectChatList(currentUser.getId());

        // 获取基于project的聊天列表（没有submission的）
        List<Message> projectChatList = messageMapper.selectChatListByProject(currentUser.getId());

        // 合并两个列表，去重（如果同一个project既有submission聊天又有project聊天，优先显示submission聊天）
        java.util.Map<String, Message> chatMap = new java.util.HashMap<>();
        for (Message chat : chatList) {
            String key = chat.getProjectId() + "_" + (chat.getSubmissionId() != null ? chat.getSubmissionId() : "null");
            chatMap.put(key, chat);
        }
        for (Message chat : projectChatList) {
            String key = chat.getProjectId() + "_null";
            if (!chatMap.containsKey(key)) {
                chatMap.put(key, chat);
            }
        }

        return new java.util.ArrayList<>(chatMap.values());
    }

    /**
     * 删除消息
     */
    public void deleteById(Integer id) {
        messageMapper.deleteById(id);
    }

    /**
     * 发送系统通知（系统自动发送，senderId设为0表示系统）
     */
    public void sendNotification(Integer projectId, Integer recipientId, String recipientType, String content) {
        Message notification = new Message();
        notification.setProjectId(projectId);
        notification.setRecipientId(recipientId);
        notification.setContent(content);
        notification.setIsRead(false);
        notification.setCreatedAt(LocalDateTime.now());
        // 系统通知senderId设为0，表示是系统自动发送
        notification.setSenderId(0);
        // 根据接收者类型设置senderType
        // 如果recipientType是ENTERPRISE，senderType设为FREELANCER（表示来自自由职业者的操作）
        // 如果recipientType是FREELANCER，senderType设为ENTERPRISE（表示来自企业的操作）
        // 如果recipientType是ADMIN，senderType设为ENTERPRISE（表示来自企业的操作，通知管理员）
        if ("ENTERPRISE".equals(recipientType)) {
            notification.setSenderType("FREELANCER");
        } else if ("FREELANCER".equals(recipientType)) {
            notification.setSenderType("ENTERPRISE");
        } else if ("ADMIN".equals(recipientType)) {
            notification.setSenderType("ENTERPRISE");
        }
        messageMapper.insert(notification);
    }
}
