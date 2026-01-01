package com.example.mapper;

import com.example.entity.ProjectOrder;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface ProjectOrderMapper {
    int insert(ProjectOrder order);
    void updateById(ProjectOrder order);
    void deleteById(Integer id);
    
    @Select("select * from project_order where id = #{id}")
    ProjectOrder selectById(Integer id);
    
    @Select("select project_order.*, " +
            "project.title as projectTitle, " +
            "project.status as projectStatus, " +
            "user.name as freelancerName, user.avatar as freelancerAvatar " +
            "from project_order " +
            "left join project on project_order.project_id = project.id " +
            "left join freelancer on project_order.freelancer_id = freelancer.id " +
            "left join user on freelancer.user_id = user.id " +
            "where project_order.freelancer_id = #{freelancerId} " +
            "and project_order.status = 'ACCEPTED' " +
            "order by project_order.accepted_at desc")
    List<ProjectOrder> selectActiveByFreelancerId(Integer freelancerId);
    
    @Select("select * from project_order where project_id = #{projectId} and freelancer_id = #{freelancerId}")
    ProjectOrder selectByProjectAndFreelancer(Integer projectId, Integer freelancerId);
    
    @Select("select * from project_order where project_id = #{projectId}")
    List<ProjectOrder> selectByProjectId(Integer projectId);
    
    @Select("select project_order.*, " +
            "user.name as freelancerName, user.avatar as freelancerAvatar, " +
            "freelancer.experience_level as freelancerExperienceLevel, " +
            "freelancer.completed_projects as freelancerCompletedProjects, " +
            "freelancer.rating as freelancerRating, " +
            "freelancer.credit_score as freelancerCreditScore, " +
            "freelancer.skills as freelancerSkills " +
            "from project_order " +
            "left join freelancer on project_order.freelancer_id = freelancer.id " +
            "left join user on freelancer.user_id = user.id " +
            "where project_order.project_id = #{projectId} " +
            "and project_order.status = 'ACCEPTED' " +
            "order by project_order.accepted_at desc")
    List<ProjectOrder> selectByProjectIdWithFreelancer(Integer projectId);
    
    List<ProjectOrder> selectAll(ProjectOrder order);
}

