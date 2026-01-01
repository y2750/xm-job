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
import com.example.mapper.UserMapper;
import com.example.entity.User;
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
    @Resource
    private UserMapper userMapper;

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
            // 检查自由职业者是否已认证
            Freelancer freelancer = freelancerMapper.selectByUserId(currentUser.getId());
            if (freelancer == null) {
                throw new CustomException(ResultCodeEnum.PARAM_ERROR, "自由职业者信息不存在");
            }
            if (!freelancer.getVerified()) {
                throw new CustomException(ResultCodeEnum.PARAM_ERROR, "您尚未完成认证，无法发起沟通。请先完成认证后再试。");
            }
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
     * @param projectId 项目ID
     * @param freelancerId 可选，自由职业者的id（freelancer表的id），用于区分不同自由职业者的聊天
     */
    public List<Message> selectByProjectId(Integer projectId, Integer freelancerId) {
        Account currentUser = TokenUtils.getCurrentUser();
        if (currentUser == null) {
            throw new CustomException(ResultCodeEnum.NO_AUTH_ERROR);
        }
        // 如果提供了freelancerId，按freelancer过滤，确保每个自由职业者都有独立的聊天记录
        if (freelancerId != null) {
            Freelancer freelancer = freelancerMapper.selectById(freelancerId);
            if (freelancer != null) {
                Integer freelancerUserId = freelancer.getUserId();
                return messageMapper.selectByProjectIdAndFreelancer(projectId, currentUser.getId(), freelancerUserId);
            }
        }
        // 否则返回所有消息（保持向后兼容）
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
     * 根据稿件ID查询消息（只返回当前用户相关的消息）
     */
    public List<Message> selectBySubmissionId(Integer submissionId) {
        Account currentUser = TokenUtils.getCurrentUser();
        if (currentUser == null) {
            throw new CustomException(ResultCodeEnum.NO_AUTH_ERROR);
        }
        
        // 先获取submission信息
        Submission submission = submissionMapper.selectById(submissionId);
        if (submission == null) {
            // submission不存在，返回空列表
            return new java.util.ArrayList<>();
        }
        
        // 获取项目信息（用于后续验证）
        Project project = projectMapper.selectById(submission.getProjectId());
        if (project == null) {
            // 项目不存在，返回空列表
            return new java.util.ArrayList<>();
        }
        
        // 验证submission是否属于当前用户
        if ("USER".equals(currentUser.getRole())) {
            // 自由职业者：检查submission是否属于该自由职业者
            Freelancer freelancer = freelancerMapper.selectByUserId(currentUser.getId());
            if (freelancer == null) {
                // 当前用户不是自由职业者，返回空列表
                return new java.util.ArrayList<>();
            }
            if (!submission.getFreelancerId().equals(freelancer.getId())) {
                // 该submission不属于当前自由职业者，返回空列表
                return new java.util.ArrayList<>();
            }
        } else if ("EMPLOY".equals(currentUser.getRole())) {
            // 企业：检查submission是否属于该企业的项目
            Enterprise enterprise = enterpriseMapper.selectByEmployId(currentUser.getId());
            if (enterprise == null) {
                // 当前用户不是企业，返回空列表
                return new java.util.ArrayList<>();
            }
            if (!project.getEnterpriseId().equals(enterprise.getId())) {
                // 该submission不属于当前企业的项目，返回空列表
                return new java.util.ArrayList<>();
            }
        } else {
            // 其他角色（如管理员）可以查看，但这里为了安全，也返回空列表
            // 如果需要管理员可以查看，可以在这里添加逻辑
            return new java.util.ArrayList<>();
        }
        
        // 获取submission的freelancer信息
        Freelancer submissionFreelancer = freelancerMapper.selectById(submission.getFreelancerId());
        if (submissionFreelancer == null) {
            return new java.util.ArrayList<>();
        }
        Integer submissionFreelancerUserId = submissionFreelancer.getUserId();
        
        // 获取项目的企业信息
        Enterprise projectEnterprise = enterpriseMapper.selectById(project.getEnterpriseId());
        if (projectEnterprise == null) {
            return new java.util.ArrayList<>();
        }
        Integer enterpriseEmployId = projectEnterprise.getEmployId();
        
        // SQL查询已经做了严格过滤，但为了安全，在Service层再次验证
        List<Message> allMessages = messageMapper.selectBySubmissionId(submissionId, currentUser.getId());
        
        // 严格验证：确保所有返回的消息都严格属于该submission
        List<Message> validMessages = new java.util.ArrayList<>();
        for (Message msg : allMessages) {
            // 验证1: 消息的submission_id必须完全匹配（绝对不能为null，必须等于submissionId）
            if (msg.getSubmissionId() == null) {
                System.err.println("WARNING: Message " + msg.getId() + " has null submission_id, skipping");
                continue;
            }
            if (!msg.getSubmissionId().equals(submissionId)) {
                System.err.println("WARNING: Message " + msg.getId() + " has wrong submission_id: " + msg.getSubmissionId() + " (expected: " + submissionId + "), skipping");
                continue;
            }
            
            // 验证2: 消息的project_id必须与submission的project_id完全一致
            if (msg.getProjectId() == null || !msg.getProjectId().equals(submission.getProjectId())) {
                System.err.println("WARNING: Message " + msg.getId() + " has wrong project_id: " + msg.getProjectId() + " (expected: " + submission.getProjectId() + "), skipping");
                continue;
            }
            
            // 验证3: 发送者必须是submission的freelancer（user_id）或项目的enterprise（employ_id）
            if (msg.getSenderType() == null || msg.getSenderId() == null) {
                System.err.println("WARNING: Message " + msg.getId() + " has incomplete sender info, skipping");
                continue;
            }
            if ("FREELANCER".equals(msg.getSenderType())) {
                if (!msg.getSenderId().equals(submissionFreelancerUserId)) {
                    System.err.println("WARNING: Message " + msg.getId() + " sender is not submission freelancer: " + msg.getSenderId() + " (expected: " + submissionFreelancerUserId + "), skipping");
                    continue;
                }
            } else if ("ENTERPRISE".equals(msg.getSenderType())) {
                if (!msg.getSenderId().equals(enterpriseEmployId)) {
                    System.err.println("WARNING: Message " + msg.getId() + " sender is not project enterprise: " + msg.getSenderId() + " (expected: " + enterpriseEmployId + "), skipping");
                    continue;
                }
            } else {
                System.err.println("WARNING: Message " + msg.getId() + " has unknown sender type: " + msg.getSenderType() + ", skipping");
                continue;
            }
            
            // 验证4: 接收者必须是submission的freelancer（user_id）或项目的enterprise（employ_id）
            if (msg.getRecipientId() == null) {
                System.err.println("WARNING: Message " + msg.getId() + " has null recipient_id, skipping");
                continue;
            }
            if (!msg.getRecipientId().equals(submissionFreelancerUserId) && 
                !msg.getRecipientId().equals(enterpriseEmployId)) {
                System.err.println("WARNING: Message " + msg.getId() + " recipient is not submission freelancer or enterprise: " + msg.getRecipientId() + " (expected: " + submissionFreelancerUserId + " or " + enterpriseEmployId + "), skipping");
                continue;
            }
            
            // 验证5: 确保当前用户是发送者或接收者
            if (!msg.getSenderId().equals(currentUser.getId()) && !msg.getRecipientId().equals(currentUser.getId())) {
                System.err.println("WARNING: Message " + msg.getId() + " current user is not participant: " + currentUser.getId() + ", skipping");
                continue;
            }
            
            // 所有验证都通过，添加到结果列表
            validMessages.add(msg);
        }
        
        System.out.println("DEBUG: selectBySubmissionId - submissionId=" + submissionId + ", currentUser=" + currentUser.getId() + ", role=" + currentUser.getRole() + 
                          ", submissionFreelancerUserId=" + submissionFreelancerUserId + ", enterpriseEmployId=" + enterpriseEmployId +
                          ", allMessages=" + allMessages.size() + ", validMessages=" + validMessages.size());
        
        return validMessages;
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
        // 注意：对于没有submission的聊天，需要按freelancer_id区分，确保每个自由职业者都有独立的聊天记录
        java.util.Map<String, Message> chatMap = new java.util.HashMap<>();
        for (Message chat : chatList) {
            // 有submission的聊天：projectId_submissionId
            String key = chat.getProjectId() + "_" + (chat.getSubmissionId() != null ? chat.getSubmissionId() : "null");
            chatMap.put(key, chat);
        }
        for (Message chat : projectChatList) {
            // 没有submission的聊天：按projectId和freelancerId区分，确保每个自由职业者都有独立的聊天记录
            String key = chat.getProjectId() + "_null_" + (chat.getFreelancerId() != null ? chat.getFreelancerId() : "null");
            if (!chatMap.containsKey(key)) {
                chatMap.put(key, chat);
            }
        }

        // 验证聊天列表：确保每个聊天记录都是当前用户有权限查看的
        List<Message> validChatList = new java.util.ArrayList<>();
        
        // 如果是企业用户，需要获取企业信息
        Enterprise currentEnterprise = null;
        Freelancer currentUserAsFreelancer = null;
        Integer currentUserAsFreelancerUserId = null;
        if ("EMPLOY".equals(currentUser.getRole())) {
            currentEnterprise = enterpriseMapper.selectByEmployId(currentUser.getId());
            // 检查当前企业用户是否也是freelancer（通过username匹配）
            if (currentUser.getUsername() != null) {
                User userWithSameUsername = userMapper.selectByUsername(currentUser.getUsername());
                if (userWithSameUsername != null) {
                    currentUserAsFreelancer = freelancerMapper.selectByUserId(userWithSameUsername.getId());
                    if (currentUserAsFreelancer != null) {
                        currentUserAsFreelancerUserId = currentUserAsFreelancer.getUserId();
                    }
                }
            }
        }
        
        // 如果是自由职业者，需要获取freelancer信息
        Freelancer currentFreelancer = null;
        Integer currentFreelancerUserId = null;
        if ("USER".equals(currentUser.getRole())) {
            currentFreelancer = freelancerMapper.selectByUserId(currentUser.getId());
            if (currentFreelancer != null) {
                currentFreelancerUserId = currentFreelancer.getUserId();
            }
        }
        
        for (Message chat : chatMap.values()) {
            boolean isValid = false;
            boolean shouldSkip = false;
            
            if (chat.getSubmissionId() != null) {
                // 有submission的聊天：验证submission是否属于当前用户
                Submission submission = submissionMapper.selectById(chat.getSubmissionId());
                if (submission != null) {
                    Project project = projectMapper.selectById(submission.getProjectId());
                    if (project != null) {
                        if ("EMPLOY".equals(currentUser.getRole()) && currentEnterprise != null) {
                            // 企业用户：验证submission是否属于该企业的项目
                            if (project.getEnterpriseId().equals(currentEnterprise.getId())) {
                                // 关键验证：不能显示自己作为freelancer的聊天记录
                                Freelancer submissionFreelancer = freelancerMapper.selectById(submission.getFreelancerId());
                                if (submissionFreelancer != null && currentUserAsFreelancer != null) {
                                    // 如果当前企业用户也是freelancer，且submission的freelancer就是当前用户，则排除
                                    if (submissionFreelancer.getId().equals(currentUserAsFreelancer.getId())) {
                                        // 这是自己作为freelancer的submission，企业用户不应该看到
                                        shouldSkip = true;
                                    }
                                }
                                if (!shouldSkip) {
                                    isValid = true;
                                }
                            }
                        } else if ("USER".equals(currentUser.getRole()) && currentFreelancer != null) {
                            // 自由职业者：验证submission是否属于该自由职业者
                            if (submission.getFreelancerId().equals(currentFreelancer.getId())) {
                                isValid = true;
                            }
                        }
                    }
                }
            } else if (chat.getProjectId() != null) {
                // 没有submission的聊天：验证project是否属于当前用户
                Project project = projectMapper.selectById(chat.getProjectId());
                if (project != null) {
                    if ("EMPLOY".equals(currentUser.getRole()) && currentEnterprise != null) {
                        // 企业用户：验证project是否属于该企业
                        if (project.getEnterpriseId().equals(currentEnterprise.getId())) {
                            // 验证消息的发送者或接收者必须是该项目的freelancer或企业的employ_id
                            // 不能显示自己作为freelancer的聊天记录
                            if (currentUserAsFreelancerUserId != null) {
                                // 如果当前用户也是freelancer，检查消息是否涉及自己作为freelancer
                                if (chat.getSenderId().equals(currentUserAsFreelancerUserId) || 
                                    chat.getRecipientId().equals(currentUserAsFreelancerUserId)) {
                                    // 这是自己作为freelancer的消息，企业用户不应该看到
                                    shouldSkip = true;
                                }
                            }
                            // 企业端：确保freelancerId被正确设置，以便前端能够区分不同的聊天记录
                            // 如果freelancerId为null，说明JOIN失败，可能是数据问题
                            // 但为了确保企业端能看到所有自由职业者的聊天记录，仍然允许
                            if (!shouldSkip) {
                                // 如果freelancerId为null，尝试从消息中推断
                                if (chat.getFreelancerId() == null) {
                                    // 如果消息的发送者是FREELANCER，senderId就是freelancer的user_id
                                    // 如果消息的接收者是FREELANCER（企业发送），recipientId就是freelancer的user_id
                                    Integer freelancerUserId = null;
                                    if ("FREELANCER".equals(chat.getSenderType())) {
                                        freelancerUserId = chat.getSenderId();
                                    } else if ("ENTERPRISE".equals(chat.getSenderType())) {
                                        freelancerUserId = chat.getRecipientId();
                                    }
                                    if (freelancerUserId != null) {
                                        Freelancer inferredFreelancer = freelancerMapper.selectByUserId(freelancerUserId);
                                        if (inferredFreelancer != null) {
                                            chat.setFreelancerId(inferredFreelancer.getId());
                                        }
                                    }
                                }
                                isValid = true;
                            }
                        }
                    } else if ("USER".equals(currentUser.getRole()) && currentFreelancer != null) {
                        // 自由职业者：验证消息是否涉及该自由职业者
                        // 关键：必须检查freelancerId是否匹配，确保只显示自己的聊天记录
                        if (currentFreelancerUserId != null) {
                            // 首先检查消息的发送者或接收者是否是该自由职业者
                            if (chat.getSenderId().equals(currentFreelancerUserId) || chat.getRecipientId().equals(currentFreelancerUserId)) {
                                // 然后检查freelancerId是否匹配，确保只显示自己的聊天记录
                                // 如果freelancerId为null，说明JOIN失败，可能是数据问题，但为了安全，仍然允许
                                // 如果freelancerId不为null，必须匹配当前freelancer的id
                                if (chat.getFreelancerId() == null || chat.getFreelancerId().equals(currentFreelancer.getId())) {
                                    isValid = true;
                                }
                            }
                        }
                    }
                }
            }
            
            if (isValid && !shouldSkip) {
                validChatList.add(chat);
            }
        }

        return validChatList;
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
