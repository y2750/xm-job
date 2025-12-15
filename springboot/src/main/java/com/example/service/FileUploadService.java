package com.example.service;

import com.example.manager.CosManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.Resource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/**
 * 统一文件上传服务（使用腾讯云COS）
 */
@Service
@Slf4j
public class FileUploadService {

    @Resource
    private CosManager cosManager;

    /**
     * 上传文件到COS
     *
     * @param file      文件
     * @param directory COS目录路径（如：project/1/submission/2/）
     * @return 文件访问URL
     */
    public String uploadFile(MultipartFile file, String directory) {
        if (file == null || file.isEmpty()) {
            log.error("文件上传失败：文件为空");
            throw new RuntimeException("文件不能为空");
        }

        try {
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null || originalFilename.isEmpty()) {
                log.error("文件上传失败：文件名为空");
                throw new RuntimeException("文件名不能为空");
            }

            // 生成唯一文件名
            String fileExtension = "";
            if (originalFilename.contains(".")) {
                fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String uniqueFileName = System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8) + fileExtension;

            // 构建COS对象键（完整路径）
            String cosKey;
            if (directory != null && !directory.isEmpty()) {
                // 确保目录路径以/结尾
                if (!directory.endsWith("/")) {
                    directory = directory + "/";
                }
                cosKey = directory + uniqueFileName;
            } else {
                cosKey = uniqueFileName;
            }

            // 创建临时文件
            Path tempFile = Files.createTempFile("upload_", fileExtension);
            try {
                // 将MultipartFile内容写入临时文件
                file.transferTo(tempFile.toFile());

                // 上传到COS
                log.info("开始上传文件到COS: {} -> {}", originalFilename, cosKey);
                String url = cosManager.uploadFile(cosKey, tempFile.toFile());
                
                if (url == null) {
                    log.error("文件上传到COS失败: {}", originalFilename);
                    throw new RuntimeException("文件上传失败");
                }

                log.info("文件上传成功: {} -> {}", originalFilename, url);
                return url;
            } finally {
                // 删除临时文件
                try {
                    Files.deleteIfExists(tempFile);
                } catch (IOException e) {
                    log.warn("删除临时文件失败: {}", tempFile, e);
                }
            }
        } catch (Exception e) {
            log.error("文件上传异常: {}", file.getOriginalFilename(), e);
            throw new RuntimeException("文件上传失败: " + e.getMessage(), e);
        }
    }

    /**
     * 上传文件到COS（使用默认目录）
     *
     * @param file 文件
     * @return 文件访问URL
     */
    public String uploadFile(MultipartFile file) {
        return uploadFile(file, null);
    }
}

