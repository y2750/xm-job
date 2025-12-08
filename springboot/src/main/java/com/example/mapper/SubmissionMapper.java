package com.example.mapper;

import com.example.entity.Submission;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface SubmissionMapper {

    int insert(Submission submission);

    void updateById(Submission submission);

    void deleteById(Integer id);

    @Select("select submission.*, " +
            "project.title as projectTitle, " +
            "project.status as projectStatus, " +
            "user.name as freelancerName, user.avatar as freelancerAvatar " +
            "from submission " +
            "left join project on submission.project_id = project.id " +
            "left join freelancer on submission.freelancer_id = freelancer.id " +
            "left join user on freelancer.user_id = user.id " +
            "where submission.id = #{id}")
    Submission selectById(Integer id);

    List<Submission> selectAll(Submission submission);

    @Select("select submission.*, " +
            "project.title as projectTitle, " +
            "project.status as projectStatus, " +
            "user.name as freelancerName, user.avatar as freelancerAvatar " +
            "from submission " +
            "left join project on submission.project_id = project.id " +
            "left join freelancer on submission.freelancer_id = freelancer.id " +
            "left join user on freelancer.user_id = user.id " +
            "where submission.project_id = #{projectId} " +
            "order by submission.created_at desc")
    List<Submission> selectByProjectId(Integer projectId);

    @Select("select submission.*, " +
            "project.title as projectTitle, " +
            "project.status as projectStatus, " +
            "user.name as freelancerName, user.avatar as freelancerAvatar " +
            "from submission " +
            "left join project on submission.project_id = project.id " +
            "left join freelancer on submission.freelancer_id = freelancer.id " +
            "left join user on freelancer.user_id = user.id " +
            "where submission.freelancer_id = #{freelancerId} " +
            "order by submission.created_at desc")
    List<Submission> selectByFreelancerId(Integer freelancerId);
}

