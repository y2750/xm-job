package com.example.service;

import com.example.entity.FreelancerCertificate;
import com.example.entity.Account;
import com.example.exception.CustomException;
import com.example.common.enums.ResultCodeEnum;
import com.example.mapper.FreelancerCertificateMapper;
import com.example.mapper.FreelancerMapper;
import com.example.mapper.AdminMapper;
import com.example.utils.TokenUtils;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 自由职业者证书管理服务
 */
@Service
public class FreelancerCertificateService {
    
    @Resource
    private FreelancerCertificateMapper certificateMapper;
    
    @Resource
    private FreelancerMapper freelancerMapper;
    
    @Resource
    private AdminMapper adminMapper;
    
    @Resource
    private NotificationService notificationService;
    
    /**
     * 添加证书
     */
    @Transactional
    public void add(FreelancerCertificate certificate) {
        Account currentUser = TokenUtils.getCurrentUser();
        if (currentUser == null) {
            throw new CustomException(ResultCodeEnum.NO_AUTH_ERROR);
        }
        
        // 获取自由职业者信息
        com.example.entity.Freelancer freelancer = freelancerMapper.selectByUserId(currentUser.getId());
        if (freelancer == null) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR, "您还不是自由职业者");
        }
        
        certificate.setFreelancerId(freelancer.getId());
        certificate.setVerified(true); // 默认已认证
        certificateMapper.insert(certificate);
        
        // 通知所有管理员有自由职业者上传了证书
        String freelancerName = freelancer.getUserName() != null ? freelancer.getUserName() : "未知用户";
        String certificateName = certificate.getCertificateName() != null ? certificate.getCertificateName() : "证书";
        String notificationContent = String.format("自由职业者《%s》上传了证书《%s》，请查看。", freelancerName, certificateName);
        
        List<com.example.entity.Admin> admins = adminMapper.selectAll(new com.example.entity.Admin());
        for (com.example.entity.Admin admin : admins) {
            notificationService.sendIndividualNotification(
                "CERTIFICATE", admin.getId(), "ADMIN",
                "证书上传通知", notificationContent, null, null, certificate.getId()
            );
        }
    }
    
    /**
     * 更新证书
     */
    @Transactional
    public void updateById(FreelancerCertificate certificate) {
        Account currentUser = TokenUtils.getCurrentUser();
        if (currentUser == null) {
            throw new CustomException(ResultCodeEnum.NO_AUTH_ERROR);
        }
        
        // 权限校验
        FreelancerCertificate dbCertificate = certificateMapper.selectById(certificate.getId());
        if (dbCertificate == null) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR, "证书不存在");
        }
        
        // 管理员可以修改任何证书的认证状态，普通用户只能更新自己的证书
        boolean isAdmin = "ADMIN".equals(currentUser.getRole());
        if (!isAdmin) {
            com.example.entity.Freelancer freelancer = freelancerMapper.selectByUserId(currentUser.getId());
            if (freelancer == null || !freelancer.getId().equals(dbCertificate.getFreelancerId())) {
                throw new CustomException(ResultCodeEnum.NO_AUTH_ERROR);
            }
        }
        
        // 检测证书认证状态变更（管理员修改认证状态时）
        if (isAdmin && certificate.getVerified() != null && 
            !certificate.getVerified().equals(dbCertificate.getVerified())) {
            // 获取自由职业者信息
            com.example.entity.Freelancer freelancer = freelancerMapper.selectById(dbCertificate.getFreelancerId());
            if (freelancer != null) {
                String freelancerName = freelancer.getUserName() != null ? freelancer.getUserName() : "未知用户";
                String certificateName = dbCertificate.getCertificateName() != null ? dbCertificate.getCertificateName() : "证书";
                String statusText = certificate.getVerified() ? "已认证" : "未认证";
                String notificationContent = String.format("您的证书《%s》认证状态已变更为：%s", certificateName, statusText);
                
                // 通知自由职业者证书状态变更
                notificationService.sendIndividualNotification(
                    "CERTIFICATE", freelancer.getUserId(), "FREELANCER",
                    "证书认证状态变更", notificationContent, null, null, certificate.getId()
                );
            }
        }
        
        certificateMapper.updateById(certificate);
    }
    
    /**
     * 删除证书
     */
    public void deleteById(Integer id) {
        Account currentUser = TokenUtils.getCurrentUser();
        if (currentUser == null) {
            throw new CustomException(ResultCodeEnum.NO_AUTH_ERROR);
        }
        
        // 权限校验：只能删除自己的证书
        FreelancerCertificate certificate = certificateMapper.selectById(id);
        if (certificate == null) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR, "证书不存在");
        }
        
        com.example.entity.Freelancer freelancer = freelancerMapper.selectByUserId(currentUser.getId());
        if (freelancer == null || !freelancer.getId().equals(certificate.getFreelancerId())) {
            throw new CustomException(ResultCodeEnum.NO_AUTH_ERROR);
        }
        
        certificateMapper.deleteById(id);
    }
    
    /**
     * 根据自由职业者ID查询证书列表
     */
    public List<FreelancerCertificate> selectByFreelancerId(Integer freelancerId) {
        return certificateMapper.selectByFreelancerId(freelancerId);
    }
    
    /**
     * 查询我的证书列表
     */
    public List<FreelancerCertificate> selectMyCertificates() {
        Account currentUser = TokenUtils.getCurrentUser();
        if (currentUser == null) {
            throw new CustomException(ResultCodeEnum.NO_AUTH_ERROR);
        }
        
        com.example.entity.Freelancer freelancer = freelancerMapper.selectByUserId(currentUser.getId());
        if (freelancer == null) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR, "您还不是自由职业者");
        }
        
        return certificateMapper.selectByFreelancerId(freelancer.getId());
    }
    
    /**
     * 管理员：查询所有证书
     */
    public List<FreelancerCertificate> selectAllCertificates() {
        Account currentUser = TokenUtils.getCurrentUser();
        if (currentUser == null || !"ADMIN".equals(currentUser.getRole())) {
            throw new CustomException(ResultCodeEnum.NO_AUTH_ERROR);
        }
        return certificateMapper.selectAll();
    }
    
    /**
     * 根据ID查询证书
     */
    public FreelancerCertificate selectById(Integer id) {
        return certificateMapper.selectById(id);
    }
}





