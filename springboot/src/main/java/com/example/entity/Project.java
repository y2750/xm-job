package com.example.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.example.common.config.FlexibleLocalDateTimeDeserializer;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 外包项目实体类
 */
public class Project {
    private Integer id;
    private Integer enterpriseId;
    private String title;
    private String description;
    private String skillsRequired;
    private BigDecimal budgetMin;
    private BigDecimal budgetMax;
    
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @JsonDeserialize(using = FlexibleLocalDateTimeDeserializer.class)
    private LocalDateTime deadline;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @JsonDeserialize(using = FlexibleLocalDateTimeDeserializer.class)
    private LocalDateTime deliveryDeadline; // 成品提交截止时间
    
    private String deliveryRequirement;
    private String difficultyLevel; // 难度等级：EASY（简单）/MEDIUM（中等）/HARD（困难）
    private String projectType; // 项目类型：WEB（网站开发）/MOBILE（移动应用）/DESIGN（设计）/OTHER（其他）
    private String priority; // 优先级：LOW（低）/MEDIUM（中）/HIGH（高）
    private String preferredExperience; // 偏向经验：NEWBIE（新手）/EXPERIENCED（老手）/BOTH（不限）
    private String coverImage; // 封面图片URL
    private String requirementDetails; // 详细需求说明（支持富文本）
    private String rejectReason; // 打回理由（管理员审核打回时填写）
    private String status; // PENDING（待审核）/PUBLISHED（已发布）/REJECTED（已打回）/CLOSED（已截止）/CONFIRMED（已确定合作）/COMPLETED（已完成）
    private Integer confirmedFreelancerId;
    private BigDecimal paidAmount; // 已支付金额
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 关联查询字段
    private String enterpriseName;
    private String enterpriseAvatar;
    private List<String> skillList;
    
    // 统计字段（不存储到数据库）
    private Integer orderCount; // 接单人数
    private Integer submissionCount; // 提交稿件数
    
    // 临时字段：支付方式（不存储到数据库）
    private String paymentMethod;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getEnterpriseId() {
        return enterpriseId;
    }

    public void setEnterpriseId(Integer enterpriseId) {
        this.enterpriseId = enterpriseId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSkillsRequired() {
        return skillsRequired;
    }

    public void setSkillsRequired(String skillsRequired) {
        this.skillsRequired = skillsRequired;
    }

    public BigDecimal getBudgetMin() {
        return budgetMin;
    }

    public void setBudgetMin(BigDecimal budgetMin) {
        this.budgetMin = budgetMin;
    }

    public BigDecimal getBudgetMax() {
        return budgetMax;
    }

    public void setBudgetMax(BigDecimal budgetMax) {
        this.budgetMax = budgetMax;
    }

    public LocalDateTime getDeadline() {
        return deadline;
    }

    public void setDeadline(LocalDateTime deadline) {
        this.deadline = deadline;
    }

    public LocalDateTime getDeliveryDeadline() {
        return deliveryDeadline;
    }

    public void setDeliveryDeadline(LocalDateTime deliveryDeadline) {
        this.deliveryDeadline = deliveryDeadline;
    }

    public String getDeliveryRequirement() {
        return deliveryRequirement;
    }

    public void setDeliveryRequirement(String deliveryRequirement) {
        this.deliveryRequirement = deliveryRequirement;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getConfirmedFreelancerId() {
        return confirmedFreelancerId;
    }

    public void setConfirmedFreelancerId(Integer confirmedFreelancerId) {
        this.confirmedFreelancerId = confirmedFreelancerId;
    }

    public BigDecimal getPaidAmount() {
        return paidAmount;
    }

    public void setPaidAmount(BigDecimal paidAmount) {
        this.paidAmount = paidAmount;
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

    public String getEnterpriseName() {
        return enterpriseName;
    }

    public void setEnterpriseName(String enterpriseName) {
        this.enterpriseName = enterpriseName;
    }

    public String getEnterpriseAvatar() {
        return enterpriseAvatar;
    }

    public void setEnterpriseAvatar(String enterpriseAvatar) {
        this.enterpriseAvatar = enterpriseAvatar;
    }

    public List<String> getSkillList() {
        return skillList;
    }

    public void setSkillList(List<String> skillList) {
        this.skillList = skillList;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public Integer getOrderCount() {
        return orderCount;
    }

    public void setOrderCount(Integer orderCount) {
        this.orderCount = orderCount;
    }

    public Integer getSubmissionCount() {
        return submissionCount;
    }

    public void setSubmissionCount(Integer submissionCount) {
        this.submissionCount = submissionCount;
    }

    public String getDifficultyLevel() {
        return difficultyLevel;
    }

    public void setDifficultyLevel(String difficultyLevel) {
        this.difficultyLevel = difficultyLevel;
    }

    public String getProjectType() {
        return projectType;
    }

    public void setProjectType(String projectType) {
        this.projectType = projectType;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getPreferredExperience() {
        return preferredExperience;
    }

    public void setPreferredExperience(String preferredExperience) {
        this.preferredExperience = preferredExperience;
    }

    public String getCoverImage() {
        return coverImage;
    }

    public void setCoverImage(String coverImage) {
        this.coverImage = coverImage;
    }

    public String getRequirementDetails() {
        return requirementDetails;
    }

    public void setRequirementDetails(String requirementDetails) {
        this.requirementDetails = requirementDetails;
    }

    public String getRejectReason() {
        return rejectReason;
    }

    public void setRejectReason(String rejectReason) {
        this.rejectReason = rejectReason;
    }
}

