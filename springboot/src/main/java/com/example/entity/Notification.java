package com.example.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.example.common.config.FlexibleLocalDateTimeDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.time.LocalDateTime;

/**
 * 业务通知实体类
 * 区别于系统公告（Notice）和聊天消息（Message）
 */
public class Notification {
    private Integer id;
    private String type; // 通知类型：PROJECT_STATUS_CHANGE/SUBMISSION/VIOLATION/SYSTEM/DELIVERABLE/CERTIFICATION
    private String scope; // 通知范围：INDIVIDUAL（单体）/ALL（全体）
    private Integer recipientId; // 接收者ID（单体通知时使用，全体通知时为NULL）
    private String recipientType; // 接收者类型：ENTERPRISE/FREELANCER/ADMIN/ALL
    private Integer projectId; // 关联项目ID
    private Integer submissionId; // 关联稿件ID
    private Integer deliverableId; // 关联成品ID
    private String title; // 通知标题
    private String content; // 通知内容
    private Boolean isRead; // 是否已读（单体通知使用）
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @JsonDeserialize(using = FlexibleLocalDateTimeDeserializer.class)
    private LocalDateTime readAt; // 已读时间
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @JsonDeserialize(using = FlexibleLocalDateTimeDeserializer.class)
    private LocalDateTime createdAt; // 创建时间

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public Integer getRecipientId() {
        return recipientId;
    }

    public void setRecipientId(Integer recipientId) {
        this.recipientId = recipientId;
    }

    public String getRecipientType() {
        return recipientType;
    }

    public void setRecipientType(String recipientType) {
        this.recipientType = recipientType;
    }

    public Integer getProjectId() {
        return projectId;
    }

    public void setProjectId(Integer projectId) {
        this.projectId = projectId;
    }

    public Integer getSubmissionId() {
        return submissionId;
    }

    public void setSubmissionId(Integer submissionId) {
        this.submissionId = submissionId;
    }

    public Integer getDeliverableId() {
        return deliverableId;
    }

    public void setDeliverableId(Integer deliverableId) {
        this.deliverableId = deliverableId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Boolean getIsRead() {
        return isRead;
    }

    public void setIsRead(Boolean isRead) {
        this.isRead = isRead;
    }

    public LocalDateTime getReadAt() {
        return readAt;
    }

    public void setReadAt(LocalDateTime readAt) {
        this.readAt = readAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

