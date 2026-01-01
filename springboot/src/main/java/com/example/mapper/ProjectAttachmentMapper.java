package com.example.mapper;

import com.example.entity.ProjectAttachment;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface ProjectAttachmentMapper {
    int insert(ProjectAttachment attachment);
    
    void deleteById(Integer id);
    
    @Select("select * from project_attachment where project_id = #{projectId} order by upload_time desc")
    List<ProjectAttachment> selectByProjectId(Integer projectId);
    
    @Select("select * from project_attachment where id = #{id}")
    ProjectAttachment selectById(Integer id);
}














