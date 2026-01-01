package com.example.mapper;

import com.example.entity.NotificationRead;
import org.apache.ibatis.annotations.Param;

public interface NotificationReadMapper {

    int insert(NotificationRead notificationRead);

    /**
     * 检查用户是否已读某条全体通知
     */
    NotificationRead selectByNotificationAndUser(@Param("notificationId") Integer notificationId,
                                                  @Param("userId") Integer userId,
                                                  @Param("userType") String userType);

    void deleteByNotificationId(Integer notificationId);
}
















