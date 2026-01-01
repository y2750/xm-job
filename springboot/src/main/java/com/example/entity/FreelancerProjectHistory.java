package com.example.entity;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 自由职业者项目历史战绩实体类
 */
public class FreelancerProjectHistory {
    private Integer id;
    private Integer freelancerId;
    private Integer projectId;
    private String projectTitle;
    private BigDecimal projectBudget;
    
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private LocalDate completionDate;
    
    private BigDecimal enterpriseRating; // 企业评分（1-5分）
    private String enterpriseComment; // 企业评价
    private String status; // COMPLETED（已完成）/CANCELLED（已取消）
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createdAt;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getFreelancerId() {
        return freelancerId;
    }

    public void setFreelancerId(Integer freelancerId) {
        this.freelancerId = freelancerId;
    }

    public Integer getProjectId() {
        return projectId;
    }

    public void setProjectId(Integer projectId) {
        this.projectId = projectId;
    }

    public String getProjectTitle() {
        return projectTitle;
    }

    public void setProjectTitle(String projectTitle) {
        this.projectTitle = projectTitle;
    }

    public BigDecimal getProjectBudget() {
        return projectBudget;
    }

    public void setProjectBudget(BigDecimal projectBudget) {
        this.projectBudget = projectBudget;
    }

    public LocalDate getCompletionDate() {
        return completionDate;
    }

    public void setCompletionDate(LocalDate completionDate) {
        this.completionDate = completionDate;
    }

    public BigDecimal getEnterpriseRating() {
        return enterpriseRating;
    }

    public void setEnterpriseRating(BigDecimal enterpriseRating) {
        this.enterpriseRating = enterpriseRating;
    }

    public String getEnterpriseComment() {
        return enterpriseComment;
    }

    public void setEnterpriseComment(String enterpriseComment) {
        this.enterpriseComment = enterpriseComment;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}














