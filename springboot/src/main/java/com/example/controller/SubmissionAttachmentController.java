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
import com.example.service.FileUploadService;
import com.example.utils.TokenUtils;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * 稿件附件相关接口
 */
@RestController
@RequestMapping("/api/attachments")
@Slf4j
public class SubmissionAttachmentController {

    @Value("${fileBaseUrl:}")
    private String fileBaseUrl;

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
    @Resource
    private FileUploadService fileUploadService;
    @Resource
    private com.example.manager.CosManager cosManager;

    /**
     * 上传稿件附件
     */
    @PostMapping("/submission/{submissionId}")
    public Result upload(@PathVariable Integer submissionId, @RequestParam("file") MultipartFile file) {
        log.info("开始上传稿件附件: submissionId={}, fileName={}, fileSize={}", 
                submissionId, file.getOriginalFilename(), file.getSize());
        
        try {
            // 验证文件大小（100MB，与Spring配置一致）
            if (file.getSize() > 100 * 1024 * 1024) {
                log.warn("文件大小超限: fileName={}, size={}", file.getOriginalFilename(), file.getSize());
                return Result.error("文件大小不能超过100MB");
            }

            // 验证权限：只有稿件提交者可以上传附件
            Submission submission = submissionMapper.selectById(submissionId);
            if (submission == null) {
                log.error("稿件不存在: submissionId={}", submissionId);
                throw new CustomException(ResultCodeEnum.PARAM_ERROR, "稿件不存在");
            }

            Account currentUser = TokenUtils.getCurrentUser();
            if (currentUser == null) {
                log.error("用户未登录");
                throw new CustomException(ResultCodeEnum.NO_AUTH_ERROR);
            }

            Freelancer freelancer = freelancerMapper.selectByUserId(currentUser.getId());
            if (freelancer == null || !freelancer.getId().equals(submission.getFreelancerId())) {
                log.warn("无权限上传附件: userId={}, submissionId={}", currentUser.getId(), submissionId);
                throw new CustomException(ResultCodeEnum.NO_AUTH_ERROR, "只能为自己的稿件上传附件");
            }

            // 检查稿件总附件大小（200MB）
            List<SubmissionAttachment> existingAttachments = attachmentMapper.selectBySubmissionId(submissionId);
            long totalSize = existingAttachments.stream().mapToLong(SubmissionAttachment::getFileSize).sum();
            if (totalSize + file.getSize() > 200 * 1024 * 1024) {
                log.warn("稿件附件总大小超限: submissionId={}, totalSize={}", submissionId, totalSize + file.getSize());
                return Result.error("稿件附件总大小不能超过200MB");
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
                return Result.error("不支持的文件类型，支持：图片(png/jpg/jpeg/gif/bmp/webp)、文档(pdf/doc/docx/xls/xlsx/ppt/pptx/txt)、设计文件(psd/ai)、压缩包(zip/rar/7z/tar/gz/bz2)");
            }

            // 构建COS目录路径
            String cosDirectory = "project/" + submission.getProjectId() + "/submission/" + submissionId;
            
            // 上传文件到COS
            String fileUrl = fileUploadService.uploadFile(file, cosDirectory);
            log.info("文件上传到COS成功: fileName={}, url={}", originalFilename, fileUrl);

            // 保存附件信息到数据库
            SubmissionAttachment attachment = new SubmissionAttachment();
            attachment.setSubmissionId(submissionId);
            attachment.setFileName(originalFilename);
            attachment.setFileSize(file.getSize());
            attachment.setFileType(fileType);
            attachment.setUploadTime(LocalDateTime.now());
            attachment.setFileUrl(fileUrl);
            attachmentMapper.insert(attachment);

            log.info("稿件附件上传完成: attachmentId={}, submissionId={}", attachment.getId(), submissionId);
            return Result.success(attachment);
        } catch (CustomException e) {
            log.error("稿件附件上传失败（业务异常）: submissionId={}, error={}", submissionId, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("稿件附件上传失败（系统异常）: submissionId={}, fileName={}", 
                    submissionId, file.getOriginalFilename(), e);
            return Result.error("文件上传失败：" + e.getMessage());
        }
    }

    /**
     * 下载附件（带权限控制）
     */
    @GetMapping("/download/{id}")
    public void download(@PathVariable Integer id, HttpServletRequest request, HttpServletResponse response) {
        log.info("开始下载稿件附件: attachmentId={}", id);
        try {
            SubmissionAttachment attachment = attachmentMapper.selectById(id);
            if (attachment == null) {
                log.error("附件不存在: attachmentId={}", id);
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                response.setContentType("application/json;charset=utf-8");
                response.getWriter().write("{\"code\":\"404\",\"msg\":\"附件不存在\",\"data\":null}");
                return;
            }
            log.debug("找到附件: attachmentId={}, fileName={}, fileUrl={}", id, attachment.getFileName(), attachment.getFileUrl());

            // 权限校验
            Submission submission = submissionMapper.selectById(attachment.getSubmissionId());
            if (submission == null) {
                log.error("稿件不存在: attachmentId={}, submissionId={}", id, attachment.getSubmissionId());
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                response.setContentType("application/json;charset=utf-8");
                response.getWriter().write("{\"code\":\"404\",\"msg\":\"稿件不存在\",\"data\":null}");
                return;
            }

            Account currentUser = TokenUtils.getCurrentUser();
            if (currentUser == null) {
                log.error("用户未登录: attachmentId={}", id);
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json;charset=utf-8");
                response.getWriter().write("{\"code\":\"401\",\"msg\":\"无效的token\",\"data\":null}");
                return;
            }
            log.debug("当前用户: userId={}, role={}", currentUser.getId(), currentUser.getRole());

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
                log.warn("无权限下载附件: attachmentId={}, userId={}, role={}", id, currentUser.getId(), currentUser.getRole());
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType("application/json;charset=utf-8");
                response.getWriter().write("{\"code\":\"403\",\"msg\":\"无权限下载此附件\",\"data\":null}");
                return;
            }

            // 文件存储在COS，直接重定向到COS URL
            String fileUrl = attachment.getFileUrl();
            if (StrUtil.isEmpty(fileUrl)) {
                log.error("附件URL为空: attachmentId={}, fileName={}", id, attachment.getFileName());
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                response.setContentType("application/json;charset=utf-8");
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
                    response.setStatus(HttpServletResponse.SC_OK);  // 确保状态码是 200
                    response.setContentType("application/octet-stream");
                    response.setCharacterEncoding("UTF-8");
                    // 使用 RFC 5987 格式支持中文文件名
                    String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
                    response.setHeader("Content-Disposition", 
                            "attachment; filename=\"" + encodedFileName + "\"; filename*=UTF-8''" + encodedFileName);
                    // 禁用缓存
                    response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
                    response.setHeader("Pragma", "no-cache");
                    response.setHeader("Expires", "0");
                    
                    log.debug("响应头设置完成: Content-Type={}, Content-Disposition={}", 
                            response.getContentType(), response.getHeader("Content-Disposition"));
                    
                    // 将文件流写入响应
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    OutputStream os = response.getOutputStream();
                    long totalBytes = 0;
                    try {
                        while ((bytesRead = inputStream.read(buffer)) != -1) {
                            os.write(buffer, 0, bytesRead);
                            totalBytes += bytesRead;
                        }
                        os.flush();
                        log.info("文件流写入完成: attachmentId={}, size={}", id, totalBytes);
                    } catch (Exception e) {
                        log.error("写入响应流失败: attachmentId={}", id, e);
                        throw e;
                    } finally {
                        // 只关闭输入流，不关闭输出流（Spring会自动处理）
                        try {
                            if (inputStream != null) {
                                inputStream.close();
                            }
                        } catch (Exception e) {
                            log.warn("关闭输入流失败", e);
                        }
                    }
                    
                    log.info("COS文件下载成功: attachmentId={}, fileName={}, size={}", id, fileName, totalBytes);
                    return;
                } catch (Exception e) {
                    log.error("从COS下载文件失败: attachmentId={}, url={}", id, fileUrl, e);
                    response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    response.setContentType("application/json;charset=utf-8");
                    response.getWriter().write("{\"code\":\"500\",\"msg\":\"文件下载失败：" + e.getMessage() + "\",\"data\":null}");
                    return;
                }
            }

            // 兼容旧数据：从本地文件系统读取
            log.warn("使用本地文件系统（兼容旧数据）: attachmentId={}, url={}", id, fileUrl);
            String fileName = attachment.getFileName();
            String dirPath = System.getProperty("user.dir") + "/files/project/" + 
                            submission.getProjectId() + "/submission/" + 
                            submission.getId() + "/";
            log.debug("查找本地文件: dirPath={}, fileName={}", dirPath, fileName);
            
            List<String> files = FileUtil.listFileNames(dirPath);
            String actualFileName = null;
            if (files != null && !files.isEmpty()) {
                log.debug("目录下文件列表: {}", files);
                for (String f : files) {
                    if (f.endsWith(fileName) || f.contains(fileName)) {
                        actualFileName = f;
                        break;
                    }
                }
            }
            if (actualFileName == null) {
                log.error("本地文件不存在: attachmentId={}, dirPath={}, fileName={}, files={}", 
                        id, dirPath, fileName, files);
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                response.setContentType("application/json;charset=utf-8");
                response.getWriter().write("{\"code\":\"404\",\"msg\":\"文件不存在\",\"data\":null}");
                return;
            }
            String localFilePath = dirPath + actualFileName;
            log.debug("找到本地文件: path={}", localFilePath);

            // 下载文件
            if (StrUtil.isNotEmpty(fileName)) {
                try {
                    response.setStatus(HttpServletResponse.SC_OK);
                    response.addHeader("Content-Disposition", "attachment;filename=" + 
                                     URLEncoder.encode(fileName, StandardCharsets.UTF_8));
                    response.setContentType("application/octet-stream");
                    // 禁用缓存
                    response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
                    response.setHeader("Pragma", "no-cache");
                    response.setHeader("Expires", "0");
                    
                    byte[] bytes = FileUtil.readBytes(localFilePath);
                    OutputStream os = response.getOutputStream();
                    os.write(bytes);
                    os.flush();
                    // 不关闭 OutputStream，让 Spring 自动处理
                    log.info("本地文件下载成功: attachmentId={}, fileName={}, size={}", id, fileName, bytes.length);
                } catch (Exception e) {
                    log.error("读取本地文件失败: attachmentId={}, path={}", id, localFilePath, e);
                    throw e;
                }
            }
        } catch (Exception e) {
            log.error("文件下载失败: attachmentId={}", id, e);
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

