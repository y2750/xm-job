package com.example.mapper;

import com.example.entity.SubmissionAttachment;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface SubmissionAttachmentMapper {

    int insert(SubmissionAttachment attachment);

    void updateById(SubmissionAttachment attachment);

    void deleteById(Integer id);

    @Select("select * from submission_attachment where id = #{id}")
    SubmissionAttachment selectById(Integer id);

    @Select("select * from submission_attachment where submission_id = #{submissionId}")
    List<SubmissionAttachment> selectBySubmissionId(Integer submissionId);
}

