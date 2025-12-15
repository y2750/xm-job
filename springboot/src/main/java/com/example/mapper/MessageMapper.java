package com.example.mapper;

import com.example.entity.Message;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface MessageMapper {

        int insert(Message message);

        void updateById(Message message);

        void deleteById(Integer id);

        @Select("select message.*, " +
                        "project.title as projectTitle " +
                        "from message " +
                        "left join project on message.project_id = project.id " +
                        "where message.id = #{id}")
        Message selectById(Integer id);

        List<Message> selectAll(Message message);

        @Select("select message.*, " +
                        "project.title as projectTitle, " +
                        "CASE " +
                        "  WHEN message.sender_type = 'ENTERPRISE' THEN employ.name " +
                        "  WHEN message.sender_type = 'FREELANCER' THEN user.name " +
                        "END as senderName, " +
                        "CASE " +
                        "  WHEN message.sender_type = 'ENTERPRISE' THEN employ.avatar " +
                        "  WHEN message.sender_type = 'FREELANCER' THEN user.avatar " +
                        "END as senderAvatar, " +
                        "CASE " +
                        "  WHEN message.sender_type = 'ENTERPRISE' THEN freelancer_user.name " +
                        "  WHEN message.sender_type = 'FREELANCER' THEN enterprise_employ.name " +
                        "END as recipientName, " +
                        "CASE " +
                        "  WHEN message.sender_type = 'ENTERPRISE' THEN freelancer_user.avatar " +
                        "  WHEN message.sender_type = 'FREELANCER' THEN enterprise_employ.avatar " +
                        "END as recipientAvatar " +
                        "from message " +
                        "left join project on message.project_id = project.id " +
                        "left join employ on message.sender_type = 'ENTERPRISE' and message.sender_id = employ.id " +
                        "left join user on message.sender_type = 'FREELANCER' and message.sender_id = user.id " +
                        "left join user freelancer_user on (message.sender_type = 'FREELANCER' and message.sender_id = freelancer_user.id) "
                        +
                        "  or (message.sender_type = 'ENTERPRISE' and message.recipient_id = freelancer_user.id) " +
                        "left join freelancer on freelancer_user.id = freelancer.user_id " +
                        "left join enterprise on project.enterprise_id = enterprise.id " +
                        "left join employ enterprise_employ on enterprise.employ_id = enterprise_employ.id " +
                        "where message.project_id = #{projectId} " +
                        "  and message.submission_id is null " +
                        "  and (message.sender_id = #{userId} or message.recipient_id = #{userId}) " +
                        "order by message.created_at asc")
        List<Message> selectByProjectId(Integer projectId, Integer userId);

        @Select("select * from message where recipient_id = #{recipientId} and is_read = 0")
        List<Message> selectUnreadByRecipientId(Integer recipientId);

        @Select("select count(*) from message where recipient_id = #{recipientId} and is_read = 0 and submission_id IS NULL")
        Integer countUnreadNotifications(Integer recipientId);

        @Select("select message.*, " +
                        "project.title as projectTitle, " +
                        "CASE " +
                        "  WHEN message.sender_type = 'ENTERPRISE' THEN employ.name " +
                        "  WHEN message.sender_type = 'FREELANCER' THEN user.name " +
                        "END as senderName, " +
                        "CASE " +
                        "  WHEN message.sender_type = 'ENTERPRISE' THEN employ.avatar " +
                        "  WHEN message.sender_type = 'FREELANCER' THEN user.avatar " +
                        "END as senderAvatar " +
                        "from message " +
                        "left join project on message.project_id = project.id " +
                        "left join employ on message.sender_type = 'ENTERPRISE' and message.sender_id = employ.id " +
                        "left join user on message.sender_type = 'FREELANCER' and message.sender_id = user.id " +
                        "where message.submission_id = #{submissionId} " +
                        "order by message.created_at asc")
        List<Message> selectBySubmissionId(Integer submissionId);

        /**
         * 获取聊天列表（按submission分组，返回每个对话的最新消息）
         * 返回当前用户参与的所有对话的最新消息
         */
        @Select("SELECT m.*, " +
                        "project.title as projectTitle, " +
                        "submission.title as submissionTitle, " +
                        "CASE " +
                        "  WHEN m.sender_type = 'ENTERPRISE' THEN employ.name " +
                        "  WHEN m.sender_type = 'FREELANCER' THEN user.name " +
                        "END as senderName, " +
                        "CASE " +
                        "  WHEN m.sender_type = 'ENTERPRISE' THEN employ.avatar " +
                        "  WHEN m.sender_type = 'FREELANCER' THEN user.avatar " +
                        "END as senderAvatar, " +
                        "CASE " +
                        "  WHEN m.sender_type = 'ENTERPRISE' THEN freelancer_user.name " +
                        "  WHEN m.sender_type = 'FREELANCER' THEN enterprise_employ.name " +
                        "END as recipientName, " +
                        "CASE " +
                        "  WHEN m.sender_type = 'ENTERPRISE' THEN freelancer_user.avatar " +
                        "  WHEN m.sender_type = 'FREELANCER' THEN enterprise_employ.avatar " +
                        "END as recipientAvatar, " +
                        "submission.freelancer_id as freelancerId, " +
                        "submission.status as submissionStatus, " +
                        "(SELECT COUNT(*) FROM message unread_msg " +
                        " WHERE unread_msg.submission_id = m.submission_id " +
                        "   AND unread_msg.recipient_id = #{userId} " +
                        "   AND unread_msg.is_read = 0) as unreadCount " +
                        "FROM message m " +
                        "INNER JOIN ( " +
                        "  SELECT submission_id, MAX(created_at) as max_created_at " +
                        "  FROM message " +
                        "  WHERE submission_id IS NOT NULL " +
                        "    AND sender_id != 0 " +
                        "    AND sender_id IS NOT NULL " +
                        "    AND (sender_id = #{userId} OR recipient_id = #{userId}) " +
                        "  GROUP BY submission_id " +
                        ") latest ON m.submission_id = latest.submission_id AND m.created_at = latest.max_created_at " +
                        "LEFT JOIN project ON m.project_id = project.id " +
                        "LEFT JOIN submission ON m.submission_id = submission.id " +
                        "LEFT JOIN employ ON m.sender_type = 'ENTERPRISE' AND m.sender_id = employ.id " +
                        "LEFT JOIN user ON m.sender_type = 'FREELANCER' AND m.sender_id = user.id " +
                        "LEFT JOIN freelancer ON submission.freelancer_id = freelancer.id " +
                        "LEFT JOIN user freelancer_user ON freelancer.user_id = freelancer_user.id " +
                        "LEFT JOIN enterprise ON project.enterprise_id = enterprise.id " +
                        "LEFT JOIN employ enterprise_employ ON enterprise.employ_id = enterprise_employ.id " +
                        "WHERE m.submission_id IS NOT NULL " +
                        "  AND m.sender_id != 0 " +
                        "  AND m.sender_id IS NOT NULL " +
                        "  AND (m.sender_id = #{userId} OR m.recipient_id = #{userId}) " +
                        "  AND submission.status IN ('SUBMITTED', 'INTERESTED', 'CONFIRMED') " +
                        "ORDER BY m.created_at DESC")
        List<Message> selectChatList(Integer userId);

        /**
         * 获取聊天列表（按project分组，返回每个对话的最新消息，用于没有submission的聊天）
         */
        @Select("SELECT m.*, " +
                        "project.title as projectTitle, " +
                        "NULL as submissionTitle, " +
                        "CASE " +
                        "  WHEN m.sender_type = 'ENTERPRISE' THEN employ.name " +
                        "  WHEN m.sender_type = 'FREELANCER' THEN user.name " +
                        "END as senderName, " +
                        "CASE " +
                        "  WHEN m.sender_type = 'ENTERPRISE' THEN employ.avatar " +
                        "  WHEN m.sender_type = 'FREELANCER' THEN user.avatar " +
                        "END as senderAvatar, " +
                        "CASE " +
                        "  WHEN m.sender_type = 'ENTERPRISE' THEN freelancer_user.name " +
                        "  WHEN m.sender_type = 'FREELANCER' THEN enterprise_employ.name " +
                        "END as recipientName, " +
                        "CASE " +
                        "  WHEN m.sender_type = 'ENTERPRISE' THEN freelancer_user.avatar " +
                        "  WHEN m.sender_type = 'FREELANCER' THEN enterprise_employ.avatar " +
                        "END as recipientAvatar, " +
                        "freelancer.id as freelancerId, " +
                        "NULL as submissionStatus, " +
                        "(SELECT COUNT(*) FROM message unread_msg " +
                        " WHERE unread_msg.project_id = m.project_id " +
                        "   AND unread_msg.submission_id IS NULL " +
                        "   AND unread_msg.recipient_id = #{userId} " +
                        "   AND unread_msg.sender_id != #{userId} " +
                        "   AND unread_msg.is_read = 0) as unreadCount " +
                        "FROM message m " +
                        "INNER JOIN ( " +
                        "  SELECT project_id, " +
                        "    CASE WHEN sender_type = 'FREELANCER' THEN sender_id ELSE recipient_id END as freelancer_user_id, "
                        +
                        "    CASE WHEN sender_type = 'ENTERPRISE' THEN sender_id ELSE recipient_id END as enterprise_user_id, "
                        +
                        "    MAX(created_at) as max_created_at " +
                        "  FROM message " +
                        "  WHERE submission_id IS NULL " +
                        "    AND project_id IS NOT NULL " +
                        "    AND sender_id != 0 " +
                        "    AND (sender_id = #{userId} OR recipient_id = #{userId}) " +
                        "  GROUP BY project_id, " +
                        "    CASE WHEN sender_type = 'FREELANCER' THEN sender_id ELSE recipient_id END, " +
                        "    CASE WHEN sender_type = 'ENTERPRISE' THEN sender_id ELSE recipient_id END " +
                        ") latest ON m.project_id = latest.project_id " +
                        "  AND m.created_at = latest.max_created_at " +
                        "  AND ((m.sender_type = 'FREELANCER' AND m.sender_id = latest.freelancer_user_id) " +
                        "    OR (m.sender_type = 'ENTERPRISE' AND m.recipient_id = latest.freelancer_user_id)) " +
                        "LEFT JOIN project ON m.project_id = project.id " +
                        "LEFT JOIN employ ON m.sender_type = 'ENTERPRISE' AND m.sender_id = employ.id " +
                        "LEFT JOIN user ON m.sender_type = 'FREELANCER' AND m.sender_id = user.id " +
                        "LEFT JOIN user freelancer_user ON (m.sender_type = 'FREELANCER' AND m.sender_id = freelancer_user.id) "
                        +
                        "  OR (m.sender_type = 'ENTERPRISE' AND m.recipient_id = freelancer_user.id) " +
                        "LEFT JOIN freelancer ON freelancer_user.id = freelancer.user_id " +
                        "LEFT JOIN enterprise ON project.enterprise_id = enterprise.id " +
                        "LEFT JOIN employ enterprise_employ ON enterprise.employ_id = enterprise_employ.id " +
                        "WHERE m.submission_id IS NULL " +
                        "  AND m.project_id IS NOT NULL " +
                        "  AND m.sender_id != 0 " +
                        "  AND m.sender_id IS NOT NULL " +
                        "  AND (m.sender_id = #{userId} OR m.recipient_id = #{userId}) " +
                        "ORDER BY m.created_at DESC")
        List<Message> selectChatListByProject(Integer userId);
}
