package com.example.mapper;

import com.example.entity.Project;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface ProjectMapper {

    int insert(Project project);

    void updateById(Project project);

    void deleteById(Integer id);

    @Select("select project.*, " +
            "employ.name as enterpriseName, employ.avatar as enterpriseAvatar, " +
            "(select count(*) from project_order where project_order.project_id = project.id and project_order.status = 'ACCEPTED') as orderCount, " +
            "(select count(*) from submission where submission.project_id = project.id) as submissionCount " +
            "from project " +
            "left join enterprise on project.enterprise_id = enterprise.id " +
            "left join employ on enterprise.employ_id = employ.id " +
            "where project.id = #{id}")
    Project selectById(Integer id);

    List<Project> selectAll(Project project);

    @Select("select * from project where enterprise_id = #{enterpriseId}")
    List<Project> selectByEnterpriseId(Integer enterpriseId);

    @Select("select * from project where status = 'PUBLISHED' and deadline > now()")
    List<Project> selectPublishedProjects();
}

