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
import cn.hutool.core.util.StrUtil;
import com.example.mapper.DeliverableAttachmentMapper;
import com.example.mapper.DeliverableMapper;
import com.example.mapper.EnterpriseMapper;
import com.example.mapper.FreelancerMapper;
import com.example.mapper.ProjectMapper;
import com.example.mapper.SubmissionMapper;
import com.example.service.FileUploadService;
import com.example.utils.TokenUtils;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class DeliverableAttachmentController {

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
    @Resource
    private FileUploadService fileUploadService;
    @Resource
    private com.example.manager.CosManager cosManager;

    @Value("${fileBaseUrl:}")
    private String fileBaseUrl;

    /**
     * 上传成品附件
     */
    @PostMapping("/{deliverableId}")
    public Result upload(@PathVariable Integer deliverableId, @RequestParam("file") MultipartFile file) {
        log.info("开始上传成品附件: deliverableId={}, fileName={}, fileSize={}", 
                deliverableId, file.getOriginalFilename(), file.getSize());
        
        try {
            Account currentUser = TokenUtils.getCurrentUser();
            if (currentUser == null) {
                log.error("用户未登录");
                throw new CustomException(ResultCodeEnum.NO_AUTH_ERROR);
            }

            // 检查成品是否存在
            Deliverable deliverable = deliverableMapper.selectById(deliverableId);
            if (deliverable == null) {
                log.error("成品不存在: deliverableId={}", deliverableId);
                throw new CustomException(ResultCodeEnum.PARAM_ERROR, "成品不存在");
            }

            // 权限检查：只有成品提交者（自由职业者）可以上传附件
            Freelancer freelancer = freelancerMapper.selectByUserId(currentUser.getId());
            if (freelancer == null || !freelancer.getId().equals(deliverable.getFreelancerId())) {
                log.warn("无权限上传附件: userId={}, deliverableId={}", currentUser.getId(), deliverableId);
                throw new CustomException(ResultCodeEnum.NO_AUTH_ERROR, "只能为自己的成品上传附件");
            }

            // 检查文件大小（100MB，与Spring配置一致）
            if (file.getSize() > 100 * 1024 * 1024) {
                log.warn("文件大小超限: fileName={}, size={}", file.getOriginalFilename(), file.getSize());
                throw new CustomException(ResultCodeEnum.PARAM_ERROR, "文件大小不能超过100MB");
            }

            // 检查文件类型
            String originalFilename = file.getOriginalFilename();
            String fileType = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                String lowerFilename = originalFilename.toLowerCase();
                // 优先检查复合扩展名（如.tar.gz, .tar.bz2）
                if (lowerFilename.endsWith(".tar.gz")) {
                    fileType = "tar.gz";
                } else if (lowerFilename.endsWith(".tar.bz2")) {
                    fileType = "tar.bz2";
                } else {
                    // 普通扩展名
                    fileType = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
                }
            }
            // 支持的文件类型：图片、文档、压缩包等
            List<String> allowedTypes = Arrays.asList(
                    "png", "jpg", "jpeg", "gif", "bmp", "webp", // 图片
                    "pdf", "psd", "ai", // 设计文件
                    "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", // 文档
                    "zip", "rar", "7z", "tar", "gz", "tar.gz", "bz2", "tar.bz2" // 压缩包
            );
            if (!allowedTypes.contains(fileType)) {
                log.warn("不支持的文件类型: fileName={}, fileType={}", originalFilename, fileType);
                throw new CustomException(ResultCodeEnum.PARAM_ERROR,
                        "不支持的文件类型，支持：图片(png/jpg/jpeg/gif/bmp/webp)、文档(pdf/doc/docx/xls/xlsx/ppt/pptx/txt)、设计文件(psd/ai)、压缩包(zip/rar/7z/tar/gz/bz2)");
            }

            // 构建COS目录路径
            String cosDirectory = "project/" + deliverable.getProjectId() + "/deliverable/" + deliverableId;
            
            // 上传文件到COS
            String fileUrl = fileUploadService.uploadFile(file, cosDirectory);
            log.info("文件上传到COS成功: fileName={}, url={}", originalFilename, fileUrl);

            // 保存附件记录
            DeliverableAttachment attachment = new DeliverableAttachment();
            attachment.setDeliverableId(deliverableId);
            attachment.setFileName(originalFilename);
            attachment.setFileSize(file.getSize());
            attachment.setFileType(fileType);
            attachment.setUploadTime(LocalDateTime.now());
            attachment.setFileUrl(fileUrl);
            attachmentMapper.insert(attachment);

            log.info("成品附件上传完成: attachmentId={}, deliverableId={}", attachment.getId(), deliverableId);
            return Result.success(attachment);
        } catch (CustomException e) {
            log.error("成品附件上传失败（业务异常）: deliverableId={}, error={}", deliverableId, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("成品附件上传失败（系统异常）: deliverableId={}, fileName={}", 
                    deliverableId, file.getOriginalFilename(), e);
            throw new CustomException(ResultCodeEnum.SYSTEM_ERROR, "文件上传失败：" + e.getMessage());
        }
    }

    /**
     * 下载成品附件
     */
    @GetMapping("/download/{id}")
    public void download(@PathVariable Integer id, HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        log.info("开始下载成品附件: attachmentId={}", id);
        try {
            Account currentUser = TokenUtils.getCurrentUser();
            if (currentUser == null) {
                log.error("用户未登录: attachmentId={}", id);
                response.setStatus(401);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"code\":\"401\",\"msg\":\"无效的token\",\"data\":null}");
                return;
            }
            log.debug("当前用户: userId={}, role={}", currentUser.getId(), currentUser.getRole());

            DeliverableAttachment attachment = attachmentMapper.selectById(id);
            if (attachment == null) {
                log.error("附件不存在: attachmentId={}", id);
                response.setStatus(404);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"code\":\"404\",\"msg\":\"附件不存在\",\"data\":null}");
                return;
            }
            log.debug("找到附件: attachmentId={}, fileName={}, fileUrl={}", id, attachment.getFileName(), attachment.getFileUrl());

            // 权限检查：成品提交者或项目发布者可以下载
            Deliverable deliverable = deliverableMapper.selectById(attachment.getDeliverableId());
            if (deliverable == null) {
                log.error("成品不存在: attachmentId={}, deliverableId={}", id, attachment.getDeliverableId());
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
                log.warn("无权限下载附件: attachmentId={}, userId={}, role={}", id, currentUser.getId(), currentUser.getRole());
                response.setStatus(403);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"code\":\"403\",\"msg\":\"无权限访问此附件\",\"data\":null}");
                return;
            }

            // 文件存储在COS，直接重定向到COS URL
            String fileUrl = attachment.getFileUrl();
            if (StrUtil.isEmpty(fileUrl)) {
                log.error("附件URL为空: attachmentId={}, fileName={}", id, attachment.getFileName());
                response.setStatus(404);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"code\":\"404\",\"msg\":\"文件URL不存在\",\"data\":null}");
                return;
            }

            // 如果是COS URL，从COS下载并返回文件流
            if (fileUrl.startsWith("http://") || fileUrl.startsWith("https://")) {
                log.info("从COS下载文件: attachmentId={}, fileName={}, url={}", id, attachment.getFileName(), fileUrl);
                
                try {
                    // 从URL中提取COS key
                    String key = cosManager.extractKeyFromUrl(fileUrl);
                    if (key == null || key.isEmpty()) {
                        // 如果提取失败，尝试手动解析
                        try {
                            java.net.URL urlObj = new java.net.URL(fileUrl);
                            key = urlObj.getPath();
                            if (key.startsWith("/")) {
                                key = key.substring(1);
                            }
                        } catch (Exception e) {
                            log.error("解析COS URL失败: url={}", fileUrl, e);
                            throw new RuntimeException("无法解析COS URL");
                        }
                    }
                    
                    log.debug("提取的COS key: {}", key);
                    
                    // 从COS下载文件
                    java.io.InputStream inputStream = cosManager.downloadFile(key);
                    if (inputStream == null) {
                        throw new RuntimeException("无法从COS下载文件，key: " + key);
                    }
                    
                    // 设置响应头，强制下载，保持原始文件名
                    String fileName = attachment.getFileName();
                    response.setContentType("application/octet-stream");
                    response.setHeader("Content-Disposition", "attachment; filename=\"" +
                            new String(fileName.getBytes("UTF-8"), "ISO-8859-1") + "\"");
                    
                    // 将文件流写入响应
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    java.io.OutputStream os = response.getOutputStream();
                    long totalBytes = 0;
                    try {
                        while ((bytesRead = inputStream.read(buffer)) != -1) {
                            os.write(buffer, 0, bytesRead);
                            totalBytes += bytesRead;
                        }
                        os.flush();
                    } finally {
                        os.close();
                        inputStream.close();
                    }
                    
                    log.info("COS文件下载成功: attachmentId={}, fileName={}, size={}", id, fileName, totalBytes);
                    return;
                } catch (Exception e) {
                    log.error("从COS下载文件失败: attachmentId={}, url={}", id, fileUrl, e);
                    response.setStatus(500);
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write("{\"code\":\"500\",\"msg\":\"文件下载失败：" + e.getMessage() + "\",\"data\":null}");
                    return;
                }
            }

            // 兼容旧数据：从本地文件系统读取
            log.warn("使用本地文件系统（兼容旧数据）: attachmentId={}, url={}", id, fileUrl);
            String realFilePath = null;
            String filePath = System.getProperty("user.dir") + "/files/project/";
            
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
                log.error("无法解析文件路径: attachmentId={}, url={}", id, fileUrl);
                response.setStatus(404);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"code\":\"404\",\"msg\":\"文件路径无效\",\"data\":null}");
                return;
            }
            log.debug("解析的文件路径: path={}", realFilePath);

            File file = new File(realFilePath);
            if (!file.exists()) {
                log.error("本地文件不存在: attachmentId={}, path={}, exists={}", id, realFilePath, file.exists());
                response.setStatus(404);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"code\":\"404\",\"msg\":\"文件不存在\",\"data\":null}");
                return;
            }

            response.setContentType("application/octet-stream");
            response.setHeader("Content-Disposition", "attachment; filename=\"" +
                    new String(attachment.getFileName().getBytes("UTF-8"), "ISO-8859-1") + "\"");

            java.io.FileInputStream fis = null;
            try {
                fis = new java.io.FileInputStream(file);
                java.io.OutputStream os = response.getOutputStream();
                byte[] buffer = new byte[8192];
                int len;
                long totalBytes = 0;
                while ((len = fis.read(buffer)) != -1) {
                    os.write(buffer, 0, len);
                    totalBytes += len;
                }
                os.flush();
                log.info("本地文件下载成功: attachmentId={}, fileName={}, size={}", id, attachment.getFileName(), totalBytes);
            } catch (Exception e) {
                log.error("读取本地文件失败: attachmentId={}, path={}", id, realFilePath, e);
                throw e;
            } finally {
                // 只关闭输入流，不关闭输出流（Spring会自动处理）
                if (fis != null) {
                    try {
                        fis.close();
                    } catch (Exception e) {
                        log.warn("关闭文件输入流失败", e);
                    }
                }
            }
        } catch (Exception e) {
            log.error("下载成品附件失败: attachmentId={}", id, e);
            try {
                response.setStatus(500);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"code\":\"500\",\"msg\":\"文件下载失败：" + e.getMessage() + "\",\"data\":null}");
            } catch (Exception ex) {
                log.error("写入错误响应失败", ex);
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

        // 删除文件（如果是COS存储，文件在COS中，不需要本地删除；兼容旧数据，尝试删除本地文件）
        String fileUrl = attachment.getFileUrl();
        if (fileUrl != null && !fileUrl.startsWith("http://") && !fileUrl.startsWith("https://")) {
            // 兼容旧数据：尝试删除本地文件
            String filePath = System.getProperty("user.dir") + "/files/project/";
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
                    boolean deleted = file.delete();
                    log.info("删除本地文件: path={}, success={}", realFilePath, deleted);
                }
            }
        } else {
            log.info("文件存储在COS，无需删除本地文件: attachmentId={}, url={}", id, fileUrl);
        }

        // 删除记录
        attachmentMapper.deleteById(id);
        return Result.success();
    }
}
