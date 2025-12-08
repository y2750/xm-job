package com.example.mapper;

import com.example.entity.Deliverable;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface DeliverableMapper {
    int insert(Deliverable deliverable);
    void updateById(Deliverable deliverable);
    void deleteById(Integer id);
    
    @Select("select deliverable.*, " +
            "project.title as projectTitle, " +
            "submission.title as submissionTitle, " +
            "user.name as freelancerName " +
            "from deliverable " +
            "left join project on deliverable.project_id = project.id " +
            "left join submission on deliverable.submission_id = submission.id " +
            "left join freelancer on deliverable.freelancer_id = freelancer.id " +
            "left join user on freelancer.user_id = user.id " +
            "where deliverable.id = #{id}")
    Deliverable selectById(Integer id);
    
    List<Deliverable> selectAll(Deliverable deliverable);
}

