package com.example.manager;

import com.example.config.CosClientConfig;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.GetObjectRequest;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.model.PutObjectResult;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.InputStream;

@Component
@Slf4j
public class CosManager {

    @Resource
    private CosClientConfig cosClientConfig;

    @Resource
    private COSClient cosClient;

    /**
     * 上传对象
     *
     * @param key  唯一键
     * @param file 文件
     * @return 上传结果
     */
    public PutObjectResult putObject(String key, File file) {
        PutObjectRequest putObjectRequest = new PutObjectRequest(cosClientConfig.getBucket(), key, file);
        return cosClient.putObject(putObjectRequest);
    }

    /**
     * 上传文件到 COS 并返回访问 URL
     *
     * @param key  COS对象键（完整路径）
     * @param file 要上传的文件
     * @return 文件的访问URL，失败返回null
     */
    public String uploadFile(String key, File file) {
        // 上传文件
        PutObjectResult result = putObject(key, file);
        if (result != null) {
            // 构建访问URL，确保 host 与 key 之间有且仅有一个 "/"
            String host = cosClientConfig.getHost() == null ? "" : cosClientConfig.getHost();
            if (host.endsWith("/")) {
                host = host.substring(0, host.length() - 1);
            }
            if (key.startsWith("/")) {
                key = key.substring(1);
            }
            String url = host + "/" + key;
            log.info("文件上传COS成功: {} -> {}", file.getName(), url);
            return url;
        } else {
            log.error("文件上传COS失败，返回结果为空");
            return null;
        }
    }

    /**
     * 从COS下载文件
     *
     * @param key COS对象键（完整路径）
     * @return 文件输入流，失败返回null
     */
    public InputStream downloadFile(String key) {
        try {
            // 确保 key 不以 / 开头
            if (key.startsWith("/")) {
                key = key.substring(1);
            }
            GetObjectRequest getObjectRequest = new GetObjectRequest(cosClientConfig.getBucket(), key);
            return cosClient.getObject(getObjectRequest).getObjectContent();
        } catch (Exception e) {
            log.error("从COS下载文件失败: key={}", key, e);
            return null;
        }
    }

    /**
     * 从COS URL中提取key
     *
     * @param cosUrl COS访问URL
     * @return COS对象键
     */
    public String extractKeyFromUrl(String cosUrl) {
        if (cosUrl == null || cosUrl.isEmpty()) {
            return null;
        }
        String host = cosClientConfig.getHost();
        if (host != null && cosUrl.startsWith(host)) {
            // 提取 host 之后的部分作为 key
            String key = cosUrl.substring(host.length());
            if (key.startsWith("/")) {
                key = key.substring(1);
            }
            return key;
        }
        return null;
    }
}