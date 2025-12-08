package com.example.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 稿件实体类
 */
public class Submission {
    private Integer id;
    private Integer projectId;
    private Integer freelancerId;
    private String title;
    private String description;
    private BigDecimal quotePrice;
    private String quoteHistory; // 报价修改历史（JSON格式）
    private String status; // SUBMITTED/INTERESTED/REJECTED/CONFIRMED
    private Boolean depositPaid; // 是否已支付接单保证金
    private Boolean enterpriseConfirmed; // 企业是否已确认合作
    private Boolean freelancerConfirmed; // 接单者是否已确认合作
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 关联查询字段
    private String projectTitle;
    private String projectStatus; // 项目状态
    private String freelancerName;
    private String freelancerAvatar;
    private List<SubmissionAttachment> attachments;
    
    // 临时字段：支付方式（不存储到数据库）
    private String paymentMethod;

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

    public BigDecimal getQuotePrice() {
        return quotePrice;
    }

    public void setQuotePrice(BigDecimal quotePrice) {
        this.quotePrice = quotePrice;
    }

    public String getQuoteHistory() {
        return quoteHistory;
    }

    public void setQuoteHistory(String quoteHistory) {
        this.quoteHistory = quoteHistory;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Boolean getDepositPaid() {
        return depositPaid;
    }

    public void setDepositPaid(Boolean depositPaid) {
        this.depositPaid = depositPaid;
    }

    public Boolean getEnterpriseConfirmed() {
        return enterpriseConfirmed;
    }

    public void setEnterpriseConfirmed(Boolean enterpriseConfirmed) {
        this.enterpriseConfirmed = enterpriseConfirmed;
    }

    public Boolean getFreelancerConfirmed() {
        return freelancerConfirmed;
    }

    public void setFreelancerConfirmed(Boolean freelancerConfirmed) {
        this.freelancerConfirmed = freelancerConfirmed;
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

    public String getProjectTitle() {
        return projectTitle;
    }

    public void setProjectTitle(String projectTitle) {
        this.projectTitle = projectTitle;
    }

    public String getProjectStatus() {
        return projectStatus;
    }

    public void setProjectStatus(String projectStatus) {
        this.projectStatus = projectStatus;
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

    public List<SubmissionAttachment> getAttachments() {
        return attachments;
    }

    public void setAttachments(List<SubmissionAttachment> attachments) {
        this.attachments = attachments;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }
}

