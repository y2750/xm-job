package com.example.controller;

import com.example.common.Result;
import com.example.entity.FreelancerCertificate;
import com.example.service.FreelancerCertificateService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 自由职业者证书管理接口
 */
@RestController
@RequestMapping("/api/certificates")
public class FreelancerCertificateController {
    
    @Resource
    private FreelancerCertificateService certificateService;
    
    /**
     * 添加证书
     */
    @PostMapping
    public Result add(@RequestBody FreelancerCertificate certificate) {
        certificateService.add(certificate);
        return Result.success();
    }
    
    /**
     * 更新证书
     */
    @PutMapping("/{id}")
    public Result update(@PathVariable Integer id, @RequestBody FreelancerCertificate certificate) {
        certificate.setId(id);
        certificateService.updateById(certificate);
        return Result.success();
    }
    
    /**
     * 删除证书
     */
    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Integer id) {
        certificateService.deleteById(id);
        return Result.success();
    }
    
    /**
     * 查询我的证书列表
     */
    @GetMapping("/my")
    public Result selectMyCertificates() {
        List<FreelancerCertificate> list = certificateService.selectMyCertificates();
        return Result.success(list);
    }
    
    /**
     * 根据自由职业者ID查询证书列表
     */
    @GetMapping("/freelancer/{freelancerId}")
    public Result selectByFreelancerId(@PathVariable Integer freelancerId) {
        List<FreelancerCertificate> list = certificateService.selectByFreelancerId(freelancerId);
        return Result.success(list);
    }
    
    /**
     * 管理员：查询所有证书（用于证书管理）
     */
    @GetMapping("/admin/all")
    public Result selectAllCertificates() {
        List<FreelancerCertificate> list = certificateService.selectAllCertificates();
        return Result.success(list);
    }
    
    /**
     * 管理员：根据ID查询证书详情
     */
    @GetMapping("/admin/{id}")
    public Result selectById(@PathVariable Integer id) {
        FreelancerCertificate certificate = certificateService.selectById(id);
        return Result.success(certificate);
    }
}





