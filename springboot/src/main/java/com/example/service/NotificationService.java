package com.example.service;

import com.example.exception.CustomException;
import com.example.common.enums.ResultCodeEnum;
import com.example.utils.TokenUtils;
import com.example.entity.Account;
import com.example.entity.Notification;
import com.example.entity.NotificationRead;
import com.example.mapper.NotificationMapper;
import com.example.mapper.NotificationReadMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 通知业务层
 */
@Service
public class NotificationService {

    @Resource
    private NotificationMapper notificationMapper;

    @Resource
    private NotificationReadMapper notificationReadMapper;

    /**
     * 发送单体通知（指定接收者）
     */
    @Transactional
    public void sendIndividualNotification(String type, Integer recipientId, String recipientType,
                                          String title, String content, Integer projectId,
                                          Integer submissionId, Integer deliverableId) {
        Notification notification = new Notification();
        notification.setType(type);
        notification.setScope("INDIVIDUAL");
        notification.setRecipientId(recipientId);
        notification.setRecipientType(recipientType);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setProjectId(projectId);
        notification.setSubmissionId(submissionId);
        notification.setDeliverableId(deliverableId);
        notification.setIsRead(false);
        notification.setCreatedAt(LocalDateTime.now());
        
        notificationMapper.insert(notification);
    }

    /**
     * 发送全体通知（所有用户或指定类型的所有用户）
     */
    @Transactional
    public void sendBroadcastNotification(String type, String recipientType,
                                         String title, String content, Integer projectId) {
        Notification notification = new Notification();
        notification.setType(type);
        notification.setScope("ALL");
        notification.setRecipientId(null); // 全体通知recipientId为NULL
        notification.setRecipientType(recipientType); // ALL表示所有用户，或ENTERPRISE/FREELANCER/ADMIN表示特定类型
        notification.setTitle(title);
        notification.setContent(content);
        notification.setProjectId(projectId);
        notification.setIsRead(false); // 全体通知的isRead字段不使用，通过notification_read表判断
        notification.setCreatedAt(LocalDateTime.now());
        
        notificationMapper.insert(notification);
    }

    /**
     * 查询当前用户的通知列表
     */
    public List<Notification> selectByCurrentUser() {
        Account currentUser = TokenUtils.getCurrentUser();
        if (currentUser == null) {
            throw new CustomException(ResultCodeEnum.NO_AUTH_ERROR);
        }
        
        String userType = convertRoleToUserType(currentUser.getRole());
        return notificationMapper.selectByUser(currentUser.getId(), userType);
    }

    /**
     * 统计当前用户的未读通知数量
     */
    public Integer countUnreadByCurrentUser() {
        Account currentUser = TokenUtils.getCurrentUser();
        if (currentUser == null) {
            throw new CustomException(ResultCodeEnum.NO_AUTH_ERROR);
        }
        
        String userType = convertRoleToUserType(currentUser.getRole());
        Integer count = notificationMapper.countUnreadByUser(currentUser.getId(), userType);
        return count != null ? count : 0;
    }

    /**
     * 标记通知为已读
     */
    @Transactional
    public void markAsRead(Integer notificationId) {
        Account currentUser = TokenUtils.getCurrentUser();
        if (currentUser == null) {
            throw new CustomException(ResultCodeEnum.NO_AUTH_ERROR);
        }
        
        Notification notification = notificationMapper.selectById(notificationId);
        if (notification == null) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR, "通知不存在");
        }
        
        String userType = convertRoleToUserType(currentUser.getRole());
        
        if ("INDIVIDUAL".equals(notification.getScope())) {
            // 单体通知：直接更新is_read字段
            if (!notification.getRecipientId().equals(currentUser.getId()) ||
                !notification.getRecipientType().equals(userType)) {
                throw new CustomException(ResultCodeEnum.NO_AUTH_ERROR, "无权操作此通知");
            }
            notification.setIsRead(true);
            notification.setReadAt(LocalDateTime.now());
            notificationMapper.updateById(notification);
        } else {
            // 全体通知：在notification_read表中插入记录
            NotificationRead existing = notificationReadMapper.selectByNotificationAndUser(
                notificationId, currentUser.getId(), userType);
            if (existing == null) {
                NotificationRead notificationRead = new NotificationRead();
                notificationRead.setNotificationId(notificationId);
                notificationRead.setUserId(currentUser.getId());
                notificationRead.setUserType(userType);
                notificationRead.setReadAt(LocalDateTime.now());
                notificationReadMapper.insert(notificationRead);
            }
        }
    }

    /**
     * 删除通知
     */
    @Transactional
    public void deleteById(Integer id) {
        Notification notification = notificationMapper.selectById(id);
        if (notification == null) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR, "通知不存在");
        }
        
        // 如果是全体通知，先删除读取记录
        if ("ALL".equals(notification.getScope())) {
            notificationReadMapper.deleteByNotificationId(id);
        }
        
        notificationMapper.deleteById(id);
    }

    /**
     * 将用户角色转换为用户类型
     */
    private String convertRoleToUserType(String role) {
        if ("EMPLOY".equals(role)) {
            return "ENTERPRISE";
        } else if ("USER".equals(role)) {
            return "FREELANCER";
        } else if ("ADMIN".equals(role)) {
            return "ADMIN";
        }
        throw new CustomException(ResultCodeEnum.PARAM_ERROR, "不支持的用户角色：" + role);
    }
}

