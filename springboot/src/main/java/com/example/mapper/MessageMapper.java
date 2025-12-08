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

    @Select("select * from message where project_id = #{projectId} order by created_at asc")
    List<Message> selectByProjectId(Integer projectId);

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
}

