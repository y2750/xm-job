package com.example.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.example.common.config.FlexibleLocalDateTimeDeserializer;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 自由职业者扩展信息实体类
 */
public class Freelancer {
    private Integer id;
    private Integer userId;
    private String skills;
    private String portfolioUrl;
    private Integer portfolioCount;
    private Boolean verified;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @JsonDeserialize(using = FlexibleLocalDateTimeDeserializer.class)
    private LocalDateTime verifiedAt;
    private String verificationInfo;
    private BigDecimal rating;
    private Integer completedProjects;
    private Integer creditScore; // 信誉分（默认100分）
    private String experienceLevel; // 经验等级：NEWBIE（新手，0-2个项目）/JUNIOR（初级，3-5个）/SENIOR（高级，6-10个）/EXPERT（专家，10+个）
    private BigDecimal basePricePerHour; // 基础时薪（用于价格体系）
    private BigDecimal minProjectBudget; // 最低接单预算
    private BigDecimal maxProjectBudget; // 最高接单预算
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 关联查询字段
    private String userName;
    private String userAvatar;
    private String userEmail;
    private String userPhone;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getSkills() {
        return skills;
    }

    public void setSkills(String skills) {
        this.skills = skills;
    }

    public String getPortfolioUrl() {
        return portfolioUrl;
    }

    public void setPortfolioUrl(String portfolioUrl) {
        this.portfolioUrl = portfolioUrl;
    }

    public Integer getPortfolioCount() {
        return portfolioCount;
    }

    public void setPortfolioCount(Integer portfolioCount) {
        this.portfolioCount = portfolioCount;
    }

    public Boolean getVerified() {
        return verified;
    }

    public void setVerified(Boolean verified) {
        this.verified = verified;
    }

    public LocalDateTime getVerifiedAt() {
        return verifiedAt;
    }

    public void setVerifiedAt(LocalDateTime verifiedAt) {
        this.verifiedAt = verifiedAt;
    }

    public String getVerificationInfo() {
        return verificationInfo;
    }

    public void setVerificationInfo(String verificationInfo) {
        this.verificationInfo = verificationInfo;
    }

    public BigDecimal getRating() {
        return rating;
    }

    public void setRating(BigDecimal rating) {
        this.rating = rating;
    }

    public Integer getCompletedProjects() {
        return completedProjects;
    }

    public void setCompletedProjects(Integer completedProjects) {
        this.completedProjects = completedProjects;
    }

    public Integer getCreditScore() {
        return creditScore;
    }

    public void setCreditScore(Integer creditScore) {
        this.creditScore = creditScore;
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

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserAvatar() {
        return userAvatar;
    }

    public void setUserAvatar(String userAvatar) {
        this.userAvatar = userAvatar;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public String getUserPhone() {
        return userPhone;
    }

    public void setUserPhone(String userPhone) {
        this.userPhone = userPhone;
    }

    public String getExperienceLevel() {
        return experienceLevel;
    }

    public void setExperienceLevel(String experienceLevel) {
        this.experienceLevel = experienceLevel;
    }

    public BigDecimal getBasePricePerHour() {
        return basePricePerHour;
    }

    public void setBasePricePerHour(BigDecimal basePricePerHour) {
        this.basePricePerHour = basePricePerHour;
    }

    public BigDecimal getMinProjectBudget() {
        return minProjectBudget;
    }

    public void setMinProjectBudget(BigDecimal minProjectBudget) {
        this.minProjectBudget = minProjectBudget;
    }

    public BigDecimal getMaxProjectBudget() {
        return maxProjectBudget;
    }

    public void setMaxProjectBudget(BigDecimal maxProjectBudget) {
        this.maxProjectBudget = maxProjectBudget;
    }
}

