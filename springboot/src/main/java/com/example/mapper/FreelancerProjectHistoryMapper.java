package com.example.mapper;

import com.example.entity.FreelancerProjectHistory;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface FreelancerProjectHistoryMapper {
    int insert(FreelancerProjectHistory history);
    
    void updateById(FreelancerProjectHistory history);
    
    @Select("select * from freelancer_project_history where freelancer_id = #{freelancerId} order by completion_date desc, created_at desc")
    List<FreelancerProjectHistory> selectByFreelancerId(Integer freelancerId);
    
    @Select("select * from freelancer_project_history where id = #{id}")
    FreelancerProjectHistory selectById(Integer id);
    
    @Select("select * from freelancer_project_history where project_id = #{projectId}")
    FreelancerProjectHistory selectByProjectId(Integer projectId);
}














