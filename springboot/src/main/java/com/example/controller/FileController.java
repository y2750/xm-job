package com.example.controller;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Dict;
import cn.hutool.core.util.StrUtil;
import com.example.common.Result;
import com.example.service.FileUploadService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/files")
@Slf4j
public class FileController {

    private static final String filePath = System.getProperty("user.dir") + "/files/";

    @Value("${fileBaseUrl:}")
    private String fileBaseUrl;
    
    @Resource
    private FileUploadService fileUploadService;

    /**
     * 文件上传
     */
    @PostMapping("/upload")
    public Result upload(MultipartFile file) {
        log.info("开始上传文件: fileName={}, fileSize={}, contentType={}", file.getOriginalFilename(), file.getSize(), file.getContentType());
        try {
            // 验证文件类型（如果是证书上传，只允许图片）
            if (file.getContentType() != null && !file.getContentType().startsWith("image/")) {
                log.warn("文件类型不符合要求: fileName={}, contentType={}", file.getOriginalFilename(), file.getContentType());
                return Result.error("只能上传图片文件！");
            }
            
            // 上传到COS
            String url = fileUploadService.uploadFile(file, "files");
            log.info("文件上传COS成功: fileName={}, url={}", file.getOriginalFilename(), url);
            return Result.success(url);
        } catch (Exception e) {
            log.error("文件上传失败: fileName={}", file.getOriginalFilename(), e);
            return Result.error("文件上传失败：" + e.getMessage());
        }
    }

    /**
     * 获取文件
     */
    @GetMapping("/download/{fileName}")
    public void download(@PathVariable String fileName, HttpServletResponse response) {
        OutputStream os;
        try {
            if (StrUtil.isNotEmpty(fileName)) {
                response.addHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode(fileName, StandardCharsets.UTF_8));
                response.setContentType("application/octet-stream");
                byte[] bytes = FileUtil.readBytes(filePath + fileName);
                os = response.getOutputStream();
                os.write(bytes);
                os.flush();
                os.close();
            }
        } catch (Exception e) {
            log.warn("文件下载失败：" + fileName);
        }
    }

    /**
     * wang-editor编辑器文件上传接口
     */
    @PostMapping("/wang/upload")
    public Map<String, Object> wangEditorUpload(MultipartFile file) {
        log.info("开始上传wangEditor文件: fileName={}, fileSize={}", file.getOriginalFilename(), file.getSize());
        try {
            // 上传到COS
            String url = fileUploadService.uploadFile(file, "files/wang");
            log.info("wangEditor文件上传成功: fileName={}, url={}", file.getOriginalFilename(), url);
            
            Map<String, Object> resMap = new HashMap<>();
            // wangEditor上传图片成功后， 需要返回的参数
            resMap.put("errno", 0);
            resMap.put("data", CollUtil.newArrayList(Dict.create().set("url", url)));
            return resMap;
        } catch (Exception e) {
            log.error("wangEditor文件上传失败: fileName={}", file.getOriginalFilename(), e);
            Map<String, Object> resMap = new HashMap<>();
            resMap.put("errno", 1);
            resMap.put("message", "文件上传失败：" + e.getMessage());
            return resMap;
        }
    }
}
