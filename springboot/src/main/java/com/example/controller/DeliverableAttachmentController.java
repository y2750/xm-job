package com.example.controller;

import com.example.common.Result;
import com.example.entity.Account;
import com.example.entity.Deliverable;
import com.example.entity.DeliverableAttachment;
import com.example.entity.Enterprise;
import com.example.entity.Freelancer;
import com.example.entity.Project;
import com.example.exception.CustomException;
import com.example.common.enums.ResultCodeEnum;
import cn.hutool.core.io.FileUtil;
import com.example.mapper.DeliverableAttachmentMapper;
import com.example.mapper.DeliverableMapper;
import com.example.mapper.EnterpriseMapper;
import com.example.mapper.FreelancerMapper;
import com.example.mapper.ProjectMapper;
import com.example.mapper.SubmissionMapper;
import com.example.utils.TokenUtils;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * 成品附件管理接口
 */
@RestController
@RequestMapping("/api/attachments/deliverable")
public class DeliverableAttachmentController {

    private static final String filePath = System.getProperty("user.dir") + "/files/project/";

    @Resource
    private DeliverableAttachmentMapper attachmentMapper;
    @Resource
    private DeliverableMapper deliverableMapper;
    @Resource
    private SubmissionMapper submissionMapper;
    @Resource
    private ProjectMapper projectMapper;
    @Resource
    private FreelancerMapper freelancerMapper;
    @Resource
    private EnterpriseMapper enterpriseMapper;

    @Value("${fileBaseUrl:}")
    private String fileBaseUrl;

    /**
     * 上传成品附件
     */
    @PostMapping("/{deliverableId}")
    public Result upload(@PathVariable Integer deliverableId, @RequestParam("file") MultipartFile file) {
        Account currentUser = TokenUtils.getCurrentUser();
        if (currentUser == null) {
            throw new CustomException(ResultCodeEnum.NO_AUTH_ERROR);
        }

        // 检查成品是否存在
        Deliverable deliverable = deliverableMapper.selectById(deliverableId);
        if (deliverable == null) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR, "成品不存在");
        }

        // 权限检查：只有成品提交者（自由职业者）可以上传附件
        Freelancer freelancer = freelancerMapper.selectByUserId(currentUser.getId());
        if (freelancer == null || !freelancer.getId().equals(deliverable.getFreelancerId())) {
            throw new CustomException(ResultCodeEnum.NO_AUTH_ERROR, "只能为自己的成品上传附件");
        }

        // 检查文件大小（50MB）
        if (file.getSize() > 50 * 1024 * 1024) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR, "文件大小不能超过50MB");
        }

        // 检查文件类型
        String originalFilename = file.getOriginalFilename();
        String fileType = originalFilename != null && originalFilename.contains(".")
                ? originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase()
                : "";
        List<String> allowedTypes = Arrays.asList("png", "jpg", "jpeg", "pdf", "psd", "doc", "docx", "zip", "rar");
        if (!allowedTypes.contains(fileType)) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR,
                    "不支持的文件类型，支持：png, jpg, jpeg, pdf, psd, doc, docx, zip, rar");
        }

        try {
            // deliverable 已在上面检查过，直接使用
            String fileName = file.getOriginalFilename();

            // 构建文件存储路径：files/project/{projectId}/deliverable/{deliverableId}/
            String dirPath = filePath + deliverable.getProjectId() + "/deliverable/" + deliverableId + "/";
            if (!FileUtil.isDirectory(dirPath)) {
                FileUtil.mkdir(dirPath);
            }

            // 生成唯一文件名
            String uniqueFileName = System.currentTimeMillis() + "-" + fileName;
            String realFilePath = dirPath + uniqueFileName;

            // 保存文件
            FileUtil.writeBytes(file.getBytes(), realFilePath);

            // 保存附件记录
            DeliverableAttachment attachment = new DeliverableAttachment();
            attachment.setDeliverableId(deliverableId);
            attachment.setFileName(fileName);
            attachment.setFileSize(file.getSize());
            attachment.setFileType(fileType);
            attachment.setUploadTime(LocalDateTime.now());
            // 先设置临时URL
            attachment.setFileUrl(fileBaseUrl + "/files/project/" + deliverable.getProjectId() + "/deliverable/"
                    + deliverableId + "/" + uniqueFileName);
            attachmentMapper.insert(attachment);

            // 更新fileUrl（使用附件ID）
            attachment.setFileUrl(fileBaseUrl + "/api/attachments/deliverable/download/" + attachment.getId());
            attachmentMapper.updateById(attachment);

            return Result.success(attachment);
        } catch (IOException e) {
            throw new CustomException(ResultCodeEnum.SYSTEM_ERROR, "文件上传失败：" + e.getMessage());
        }
    }

    /**
     * 下载成品附件
     */
    @GetMapping("/download/{id}")
    public void download(@PathVariable Integer id, jakarta.servlet.http.HttpServletResponse response)
            throws IOException {
        Account currentUser = TokenUtils.getCurrentUser();
        if (currentUser == null) {
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":\"401\",\"msg\":\"无效的token\",\"data\":null}");
            return;
        }

        DeliverableAttachment attachment = attachmentMapper.selectById(id);
        if (attachment == null) {
            response.setStatus(404);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":\"404\",\"msg\":\"附件不存在\",\"data\":null}");
            return;
        }

        // 权限检查：成品提交者或项目发布者可以下载
        Deliverable deliverable = deliverableMapper.selectById(attachment.getDeliverableId());
        if (deliverable == null) {
            response.setStatus(404);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":\"404\",\"msg\":\"成品不存在\",\"data\":null}");
            return;
        }

        boolean hasPermission = false;

        // 检查是否为成品提交者（自由职业者）
        Freelancer freelancer = freelancerMapper.selectByUserId(currentUser.getId());
        if (freelancer != null && freelancer.getId().equals(deliverable.getFreelancerId())) {
            hasPermission = true;
        }

        // 检查是否为项目发布者（企业）
        if (!hasPermission) {
            Project project = projectMapper.selectById(deliverable.getProjectId());
            if (project != null) {
                Enterprise enterprise = enterpriseMapper.selectByEmployId(currentUser.getId());
                if (enterprise != null && enterprise.getId().equals(project.getEnterpriseId())) {
                    hasPermission = true;
                }
            }
        }

        // 管理员有所有权限
        if (!hasPermission && "ADMIN".equals(currentUser.getRole())) {
            hasPermission = true;
        }

        if (!hasPermission) {
            response.setStatus(403);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":\"403\",\"msg\":\"无权限访问此附件\",\"data\":null}");
            return;
        }

        // 读取文件
        String fileUrl = attachment.getFileUrl();
        String realFilePath = null;

        // 从URL中提取文件路径：/files/project/{projectId}/deliverable/{deliverableId}/{fileName}
        if (fileUrl != null && fileUrl.contains("/files/project/")) {
            // 提取相对路径部分
            int index = fileUrl.indexOf("/files/project/");
            String relativePath = fileUrl.substring(index);
            realFilePath = filePath + relativePath.substring("/files/project/".length());
        } else if (fileUrl != null && fileUrl.startsWith("/files/download/")) {
            // 兼容旧格式
            String fileName = fileUrl.substring("/files/download/".length());
            realFilePath = filePath + fileName;
        }

        if (realFilePath == null) {
            response.setStatus(404);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":\"404\",\"msg\":\"文件路径无效\",\"data\":null}");
            return;
        }

        File file = new File(realFilePath);
        if (!file.exists()) {
            response.setStatus(404);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":\"404\",\"msg\":\"文件不存在\",\"data\":null}");
            return;
        }

        response.setContentType("application/octet-stream");
        response.setHeader("Content-Disposition", "attachment; filename=\"" +
                new String(attachment.getFileName().getBytes("UTF-8"), "ISO-8859-1") + "\"");

        try (java.io.FileInputStream fis = new java.io.FileInputStream(file);
                java.io.OutputStream os = response.getOutputStream()) {
            byte[] buffer = new byte[1024];
            int len;
            while ((len = fis.read(buffer)) != -1) {
                os.write(buffer, 0, len);
            }
        }
    }

    /**
     * 删除成品附件
     */
    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Integer id) {
        Account currentUser = TokenUtils.getCurrentUser();
        if (currentUser == null) {
            throw new CustomException(ResultCodeEnum.NO_AUTH_ERROR);
        }

        DeliverableAttachment attachment = attachmentMapper.selectById(id);
        if (attachment == null) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR, "附件不存在");
        }

        // 权限检查：只有成品提交者可以删除附件
        Deliverable deliverable = deliverableMapper.selectById(attachment.getDeliverableId());
        if (deliverable == null) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR, "成品不存在");
        }

        Freelancer freelancer = freelancerMapper.selectByUserId(currentUser.getId());
        if (freelancer == null || !freelancer.getId().equals(deliverable.getFreelancerId())) {
            throw new CustomException(ResultCodeEnum.NO_AUTH_ERROR, "只能删除自己成品的附件");
        }

        // 删除文件
        String fileUrl = attachment.getFileUrl();
        if (fileUrl != null) {
            String realFilePath = null;
            if (fileUrl.contains("/files/project/")) {
                int index = fileUrl.indexOf("/files/project/");
                String relativePath = fileUrl.substring(index);
                realFilePath = filePath + relativePath.substring("/files/project/".length());
            } else if (fileUrl.startsWith("/files/download/")) {
                String fileName = fileUrl.substring("/files/download/".length());
                realFilePath = filePath + fileName;
            }

            if (realFilePath != null) {
                File file = new File(realFilePath);
                if (file.exists()) {
                    file.delete();
                }
            }
        }

        // 删除记录
        attachmentMapper.deleteById(id);
        return Result.success();
    }
}
