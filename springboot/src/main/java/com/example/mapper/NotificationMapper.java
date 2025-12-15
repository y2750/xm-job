package com.example.mapper;

import com.example.entity.Notification;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface NotificationMapper {

    int insert(Notification notification);

    void updateById(Notification notification);

    void deleteById(Integer id);

    @Select("SELECT * FROM notification WHERE id = #{id}")
    Notification selectById(Integer id);

    List<Notification> selectAll(Notification notification);

    /**
     * 查询当前用户的通知列表（包括单体通知和全体通知）
     */
    @Select("SELECT n.*, " +
            "CASE " +
            "  WHEN n.scope = 'ALL' THEN " +
            "    CASE WHEN nr.id IS NOT NULL THEN 1 ELSE 0 END " +
            "  ELSE n.is_read " +
            "END as is_read, " +
            "CASE " +
            "  WHEN n.scope = 'ALL' THEN nr.read_at " +
            "  ELSE n.read_at " +
            "END as read_at " +
            "FROM notification n " +
            "LEFT JOIN notification_read nr ON n.id = nr.notification_id " +
            "  AND nr.user_id = #{userId} AND nr.user_type = #{userType} " +
            "WHERE (n.scope = 'INDIVIDUAL' AND n.recipient_id = #{userId} AND n.recipient_type = #{userType}) " +
            "   OR (n.scope = 'ALL' AND (n.recipient_type = 'ALL' OR n.recipient_type = #{userType})) " +
            "ORDER BY n.created_at DESC")
    List<Notification> selectByUser(@Param("userId") Integer userId, @Param("userType") String userType);

    /**
     * 统计当前用户的未读通知数量
     */
    @Select("SELECT COUNT(*) FROM (" +
            "  SELECT n.id " +
            "  FROM notification n " +
            "  LEFT JOIN notification_read nr ON n.id = nr.notification_id " +
            "    AND nr.user_id = #{userId} AND nr.user_type = #{userType} " +
            "  WHERE ((n.scope = 'INDIVIDUAL' AND n.recipient_id = #{userId} AND n.recipient_type = #{userType}) " +
            "     OR (n.scope = 'ALL' AND (n.recipient_type = 'ALL' OR n.recipient_type = #{userType}))) " +
            "    AND (n.scope = 'ALL' AND nr.id IS NULL OR n.scope = 'INDIVIDUAL' AND n.is_read = 0)" +
            ") t")
    Integer countUnreadByUser(@Param("userId") Integer userId, @Param("userType") String userType);
}

