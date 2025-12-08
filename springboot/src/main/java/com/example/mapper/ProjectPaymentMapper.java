package com.example.mapper;

import com.example.entity.ProjectPayment;

public interface ProjectPaymentMapper {
    int insert(ProjectPayment projectPayment);
    void updateById(ProjectPayment projectPayment);
    ProjectPayment selectByProjectId(Integer projectId);
}

