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
        
        // 如果提供了submissionId，自动设置recipientId和projectId
        if (message.getSubmissionId() != null) {
            Submission submission = submissionMapper.selectById(message.getSubmissionId());
            if (submission == null) {
                throw new CustomException(ResultCodeEnum.PARAM_ERROR, "稿件不存在");
            }
            
            // 检查稿件状态是否为INTERESTED或CONFIRMED（已确认合作时也可以继续聊天）
            if (!"INTERESTED".equals(submission.getStatus()) && !"CONFIRMED".equals(submission.getStatus())) {
                throw new CustomException(ResultCodeEnum.PARAM_ERROR, "只有在有意向或已确认合作状态时才能发送消息");
            }
            
            // 设置projectId
            if (message.getProjectId() == null) {
                message.setProjectId(submission.getProjectId());
            }
            
            Project project = projectMapper.selectById(submission.getProjectId());
            if (project == null) {
                throw new CustomException(ResultCodeEnum.PARAM_ERROR, "项目不存在");
            }
            
            // 检查项目状态，如果已完成，不允许发送消息
            if ("COMPLETED".equals(project.getStatus())) {
                throw new CustomException(ResultCodeEnum.PARAM_ERROR, "项目已完成，无法继续聊天");
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
        return messageMapper.selectByProjectId(projectId);
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

