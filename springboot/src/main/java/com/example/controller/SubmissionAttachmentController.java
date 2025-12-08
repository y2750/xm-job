package com.example.controller;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.example.common.Result;
import com.example.common.enums.ResultCodeEnum;
import com.example.entity.Account;
import com.example.entity.Enterprise;
import com.example.entity.Freelancer;
import com.example.entity.Project;
import com.example.entity.Submission;
import com.example.entity.SubmissionAttachment;
import com.example.exception.CustomException;
import com.example.mapper.EnterpriseMapper;
import com.example.mapper.FreelancerMapper;
import com.example.mapper.ProjectMapper;
import com.example.mapper.SubmissionAttachmentMapper;
import com.example.mapper.SubmissionMapper;
import com.example.utils.TokenUtils;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 稿件附件相关接口
 */
@RestController
@RequestMapping("/api/attachments")
public class SubmissionAttachmentController {

    private static final Logger log = LoggerFactory.getLogger(SubmissionAttachmentController.class);

    @Value("${fileBaseUrl:}")
    private String fileBaseUrl;

    private static final String filePath = System.getProperty("user.dir") + "/files/project/";

    @Resource
    private SubmissionAttachmentMapper attachmentMapper;
    @Resource
    private SubmissionMapper submissionMapper;
    @Resource
    private ProjectMapper projectMapper;
    @Resource
    private FreelancerMapper freelancerMapper;
    @Resource
    private EnterpriseMapper enterpriseMapper;

    /**
     * 上传稿件附件
     */
    @PostMapping("/submission/{submissionId}")
    public Result upload(@PathVariable Integer submissionId, @RequestParam("file") MultipartFile file) {
        // 验证文件大小（50MB）
        if (file.getSize() > 50 * 1024 * 1024) {
            return Result.error("文件大小不能超过50MB");
        }

        // 验证权限：只有稿件提交者可以上传附件
        Submission submission = submissionMapper.selectById(submissionId);
        if (submission == null) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR, "稿件不存在");
        }

        Account currentUser = TokenUtils.getCurrentUser();
        if (currentUser == null) {
            throw new CustomException(ResultCodeEnum.NO_AUTH_ERROR);
        }

        Freelancer freelancer = freelancerMapper.selectByUserId(currentUser.getId());
        if (freelancer == null || !freelancer.getId().equals(submission.getFreelancerId())) {
            throw new CustomException(ResultCodeEnum.NO_AUTH_ERROR, "只能为自己的稿件上传附件");
        }

        // 检查稿件总附件大小（200MB）
        List<SubmissionAttachment> existingAttachments = attachmentMapper.selectBySubmissionId(submissionId);
        long totalSize = existingAttachments.stream().mapToLong(SubmissionAttachment::getFileSize).sum();
        if (totalSize + file.getSize() > 200 * 1024 * 1024) {
            return Result.error("稿件附件总大小不能超过200MB");
        }

        try {
            String fileName = file.getOriginalFilename();
            String fileExtension = "";
            if (fileName != null && fileName.contains(".")) {
                fileExtension = fileName.substring(fileName.lastIndexOf("."));
            }

            // 创建目录结构：/files/project/{projectId}/submission/{submissionId}/
            String dirPath = filePath + submission.getProjectId() + "/submission/" + submissionId + "/";
            if (!FileUtil.isDirectory(dirPath)) {
                FileUtil.mkdir(dirPath);
            }

            // 生成唯一文件名
            String uniqueFileName = System.currentTimeMillis() + "-" + fileName;
            String realFilePath = dirPath + uniqueFileName;

            // 保存文件
            FileUtil.writeBytes(file.getBytes(), realFilePath);

            // 保存附件信息到数据库
            SubmissionAttachment attachment = new SubmissionAttachment();
            attachment.setSubmissionId(submissionId);
            attachment.setFileName(fileName);
            attachment.setFileSize(file.getSize());
            attachment.setFileType(fileExtension);
            attachment.setUploadTime(LocalDateTime.now());
            // 先设置一个临时URL，插入后更新
            attachment.setFileUrl(fileBaseUrl + "/files/project/" + submission.getProjectId() + "/submission/" + submissionId + "/" + uniqueFileName);
            attachmentMapper.insert(attachment);
            
            // 插入后更新文件URL（使用附件ID）
            attachment.setFileUrl(fileBaseUrl + "/api/attachments/download/" + attachment.getId());
            attachmentMapper.updateById(attachment);

            return Result.success(attachment);
        } catch (Exception e) {
            log.error("文件上传失败", e);
            return Result.error("文件上传失败：" + e.getMessage());
        }
    }

    /**
     * 下载附件（带权限控制）
     */
    @GetMapping("/download/{id}")
    public void download(@PathVariable Integer id, HttpServletResponse response) {
        try {
            SubmissionAttachment attachment = attachmentMapper.selectById(id);
            if (attachment == null) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                response.setContentType("application/json;charset=utf-8");
                response.getWriter().write("{\"code\":\"404\",\"msg\":\"附件不存在\",\"data\":null}");
                return;
            }

            // 权限校验
            Submission submission = submissionMapper.selectById(attachment.getSubmissionId());
            if (submission == null) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                response.setContentType("application/json;charset=utf-8");
                response.getWriter().write("{\"code\":\"404\",\"msg\":\"稿件不存在\",\"data\":null}");
                return;
            }

            Account currentUser = TokenUtils.getCurrentUser();
            if (currentUser == null) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json;charset=utf-8");
                response.getWriter().write("{\"code\":\"401\",\"msg\":\"无效的token\",\"data\":null}");
                return;
            }

            boolean hasPermission = false;

            // 自由职业者：可下载自己提交的附件
            Freelancer freelancer = freelancerMapper.selectByUserId(currentUser.getId());
            if (freelancer != null && freelancer.getId().equals(submission.getFreelancerId())) {
                hasPermission = true;
            }

            // 企业用户：可下载自己项目的所有稿件附件
            Enterprise enterprise = enterpriseMapper.selectByEmployId(currentUser.getId());
            if (enterprise != null) {
                // 检查项目是否属于该企业
                Project project = projectMapper.selectById(submission.getProjectId());
                if (project != null && enterprise.getId().equals(project.getEnterpriseId())) {
                    hasPermission = true;
                }
            }

            // 管理员：可下载所有附件
            if ("ADMIN".equals(currentUser.getRole())) {
                hasPermission = true;
            }

            if (!hasPermission) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType("application/json;charset=utf-8");
                response.getWriter().write("{\"code\":\"403\",\"msg\":\"无权限下载此附件\",\"data\":null}");
                return;
            }

            // 构建文件路径（需要从文件名中提取时间戳部分）
            String fileName = attachment.getFileName();
            // 查找实际存储的文件（文件名格式：时间戳-原文件名）
            String dirPath = System.getProperty("user.dir") + "/files/project/" + 
                            submission.getProjectId() + "/submission/" + 
                            submission.getId() + "/";
            List<String> files = FileUtil.listFileNames(dirPath);
            String actualFileName = null;
            if (files != null && !files.isEmpty()) {
                for (String f : files) {
                    if (f.endsWith(fileName) || f.contains(fileName)) {
                        actualFileName = f;
                        break;
                    }
                }
            }
            if (actualFileName == null) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                response.setContentType("application/json;charset=utf-8");
                response.getWriter().write("{\"code\":\"404\",\"msg\":\"文件不存在\",\"data\":null}");
                return;
            }
            String filePath = dirPath + actualFileName;

            // 下载文件
            if (StrUtil.isNotEmpty(fileName)) {
                response.addHeader("Content-Disposition", "attachment;filename=" + 
                                 URLEncoder.encode(fileName, StandardCharsets.UTF_8));
                response.setContentType("application/octet-stream");
                byte[] bytes = FileUtil.readBytes(filePath);
                OutputStream os = response.getOutputStream();
                os.write(bytes);
                os.flush();
                os.close();
            }
        } catch (Exception e) {
            log.error("文件下载失败", e);
            try {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.setContentType("application/json;charset=utf-8");
                response.getWriter().write("{\"code\":\"500\",\"msg\":\"文件下载失败：" + e.getMessage() + "\",\"data\":null}");
            } catch (Exception ex) {
                log.error("写入错误响应失败", ex);
            }
        }
    }

    /**
     * 删除附件
     */
    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Integer id) {
        SubmissionAttachment attachment = attachmentMapper.selectById(id);
        if (attachment == null) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR, "附件不存在");
        }

        // 权限校验：只有稿件提交者可以删除附件
        Submission submission = submissionMapper.selectById(attachment.getSubmissionId());
        Account currentUser = TokenUtils.getCurrentUser();
        Freelancer freelancer = freelancerMapper.selectByUserId(currentUser.getId());
        if (freelancer == null || !freelancer.getId().equals(submission.getFreelancerId())) {
            throw new CustomException(ResultCodeEnum.NO_AUTH_ERROR);
        }

        attachmentMapper.deleteById(id);
        return Result.success();
    }
}

