package com.example.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.example.common.config.FlexibleLocalDateTimeDeserializer;

import java.time.LocalDateTime;

/**
 * 项目接单实体类
 */
public class ProjectOrder {
    private Integer id;
    private Integer projectId;
    private Integer freelancerId;
    private String status; // ACCEPTED/COMPLETED/CANCELLED
    private Boolean abandoned; // 是否已放弃接单
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @JsonDeserialize(using = FlexibleLocalDateTimeDeserializer.class)
    private LocalDateTime acceptedAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @JsonDeserialize(using = FlexibleLocalDateTimeDeserializer.class)
    private LocalDateTime abandonedAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @JsonDeserialize(using = FlexibleLocalDateTimeDeserializer.class)
    private LocalDateTime createdAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @JsonDeserialize(using = FlexibleLocalDateTimeDeserializer.class)
    private LocalDateTime updatedAt;

    // 关联查询字段
    private String projectTitle;
    private String projectStatus; // 项目状态
    private String freelancerName;
    private String freelancerAvatar;
    private String freelancerExperienceLevel; // 自由职业者经验等级
    private Integer freelancerCompletedProjects; // 自由职业者完成项目数
    private java.math.BigDecimal freelancerRating; // 自由职业者评分
    private Integer freelancerCreditScore; // 自由职业者信誉分
    private String freelancerSkills; // 自由职业者技能

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getProjectId() {
        return projectId;
    }

    public void setProjectId(Integer projectId) {
        this.projectId = projectId;
    }

    public Integer getFreelancerId() {
        return freelancerId;
    }

    public void setFreelancerId(Integer freelancerId) {
        this.freelancerId = freelancerId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Boolean getAbandoned() {
        return abandoned;
    }

    public void setAbandoned(Boolean abandoned) {
        this.abandoned = abandoned;
    }

    public LocalDateTime getAcceptedAt() {
        return acceptedAt;
    }

    public void setAcceptedAt(LocalDateTime acceptedAt) {
        this.acceptedAt = acceptedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public LocalDateTime getAbandonedAt() {
        return abandonedAt;
    }

    public void setAbandonedAt(LocalDateTime abandonedAt) {
        this.abandonedAt = abandonedAt;
    }

    public String getProjectTitle() {
        return projectTitle;
    }

    public void setProjectTitle(String projectTitle) {
        this.projectTitle = projectTitle;
    }

    public String getFreelancerName() {
        return freelancerName;
    }

    public void setFreelancerName(String freelancerName) {
        this.freelancerName = freelancerName;
    }

    public String getFreelancerAvatar() {
        return freelancerAvatar;
    }

    public void setFreelancerAvatar(String freelancerAvatar) {
        this.freelancerAvatar = freelancerAvatar;
    }

    public String getProjectStatus() {
        return projectStatus;
    }

    public void setProjectStatus(String projectStatus) {
        this.projectStatus = projectStatus;
    }

    public String getFreelancerExperienceLevel() {
        return freelancerExperienceLevel;
    }

    public void setFreelancerExperienceLevel(String freelancerExperienceLevel) {
        this.freelancerExperienceLevel = freelancerExperienceLevel;
    }

    public Integer getFreelancerCompletedProjects() {
        return freelancerCompletedProjects;
    }

    public void setFreelancerCompletedProjects(Integer freelancerCompletedProjects) {
        this.freelancerCompletedProjects = freelancerCompletedProjects;
    }

    public java.math.BigDecimal getFreelancerRating() {
        return freelancerRating;
    }

    public void setFreelancerRating(java.math.BigDecimal freelancerRating) {
        this.freelancerRating = freelancerRating;
    }

    public Integer getFreelancerCreditScore() {
        return freelancerCreditScore;
    }

    public void setFreelancerCreditScore(Integer freelancerCreditScore) {
        this.freelancerCreditScore = freelancerCreditScore;
    }

    public String getFreelancerSkills() {
        return freelancerSkills;
    }

    public void setFreelancerSkills(String freelancerSkills) {
        this.freelancerSkills = freelancerSkills;
    }
}

