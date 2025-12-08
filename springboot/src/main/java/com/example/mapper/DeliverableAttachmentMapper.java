package com.example.mapper;

import com.example.entity.DeliverableAttachment;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface DeliverableAttachmentMapper {
    int insert(DeliverableAttachment attachment);
    void updateById(DeliverableAttachment attachment);
    void deleteById(Integer id);
    
    @Select("select * from deliverable_attachment where id = #{id}")
    DeliverableAttachment selectById(Integer id);
    
    @Select("select * from deliverable_attachment where deliverable_id = #{deliverableId}")
    List<DeliverableAttachment> selectByDeliverableId(Integer deliverableId);
}

