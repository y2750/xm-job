package com.example.service;

import com.example.common.enums.ResultCodeEnum;
import com.example.entity.Account;
import com.example.entity.Enterprise;
import com.example.exception.CustomException;
import com.example.mapper.EnterpriseMapper;
import com.example.utils.TokenUtils;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 企业业务层
 */
@Service
public class EnterpriseService {

    @Resource
    private EnterpriseMapper enterpriseMapper;
    @Resource
    private com.example.service.MessageService messageService;
    @Resource
    private com.example.mapper.AdminMapper adminMapper;

    /**
     * 新增企业信息
     */
    public void add(Enterprise enterprise) {
        Account currentUser = TokenUtils.getCurrentUser();
        if (currentUser == null) {
            throw new CustomException(ResultCodeEnum.NO_AUTH_ERROR);
        }
        
        // 检查是否已存在
        Enterprise exist = enterpriseMapper.selectByEmployId(currentUser.getId());
        if (exist != null) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR, "企业信息已存在");
        }
        
        enterprise.setEmployId(currentUser.getId());
        enterprise.setCreatedAt(LocalDateTime.now());
        enterpriseMapper.insert(enterprise);
    }

    /**
     * 更新企业信息
     */
    public void updateById(Enterprise enterprise) {
        Account currentUser = TokenUtils.getCurrentUser();
        if (currentUser == null) {
            throw new CustomException(ResultCodeEnum.NO_AUTH_ERROR);
        }
        
        // 如果提供了ID，直接使用ID更新（管理员审核场景）
        if (enterprise.getId() != null) {
            Enterprise dbEnterprise = enterpriseMapper.selectById(enterprise.getId());
            if (dbEnterprise == null) {
                throw new CustomException(ResultCodeEnum.PARAM_ERROR, "企业信息不存在");
            }
            // 管理员可以更新任何企业信息，普通企业用户只能更新自己的
            if (!"ADMIN".equals(currentUser.getRole())) {
                // 非管理员需要验证是否是自己企业的信息
                Enterprise currentEnterprise = enterpriseMapper.selectByEmployId(currentUser.getId());
                if (currentEnterprise == null || !currentEnterprise.getId().equals(enterprise.getId())) {
                    throw new CustomException(ResultCodeEnum.NO_AUTH_ERROR, "只能更新自己的企业信息");
                }
            }
            
            // 检测管理员认证企业的情况（从false变为true）
            boolean wasVerified = dbEnterprise.getVerified() != null && dbEnterprise.getVerified();
            boolean willBeVerified = enterprise.getVerified() != null && enterprise.getVerified();
            if (!wasVerified && willBeVerified && "ADMIN".equals(currentUser.getRole())) {
                // 管理员认证了企业，发送通知给企业
                String enterpriseName = dbEnterprise.getEmployName() != null ? dbEnterprise.getEmployName() : "未知企业";
                String notificationContent = String.format("恭喜！您的企业《%s》已通过认证审核，现在可以发布项目了。", enterpriseName);
                messageService.sendNotification(null, dbEnterprise.getEmployId(), "ENTERPRISE", notificationContent);
            }
        } else {
            // 如果没有提供ID，使用当前登录用户的企业ID（企业用户更新自己的信息）
            Enterprise dbEnterprise = enterpriseMapper.selectByEmployId(currentUser.getId());
            if (dbEnterprise == null) {
                throw new CustomException(ResultCodeEnum.PARAM_ERROR, "企业信息不存在");
            }
            enterprise.setId(dbEnterprise.getId());
            
            // 如果企业已认证，且修改了资料，取消认证状态并通知管理员
            if (dbEnterprise.getVerified() != null && dbEnterprise.getVerified()) {
                // 检查是否修改了营业执照（处理null和空字符串的情况）
                String newBusinessLicense = (enterprise.getBusinessLicense() != null && !enterprise.getBusinessLicense().trim().isEmpty()) 
                    ? enterprise.getBusinessLicense().trim() : "";
                String oldBusinessLicense = (dbEnterprise.getBusinessLicense() != null && !dbEnterprise.getBusinessLicense().trim().isEmpty()) 
                    ? dbEnterprise.getBusinessLicense().trim() : "";
                boolean businessLicenseChanged = !newBusinessLicense.equals(oldBusinessLicense);
                
                System.out.println("企业ID: " + enterprise.getId());
                System.out.println("原营业执照: " + oldBusinessLicense);
                System.out.println("新营业执照: " + newBusinessLicense);
                System.out.println("营业执照是否改变: " + businessLicenseChanged);
                System.out.println("企业是否已认证: " + dbEnterprise.getVerified());
                
                // 如果修改了营业执照，取消认证状态并通知管理员
                if (businessLicenseChanged) {
                    // 取消认证状态
                    enterprise.setVerified(false);
                    enterprise.setVerifiedAt(null);
                    
                    System.out.println("设置verified为false, verifiedAt为null");
                    
                    // 通知所有管理员
                    List<com.example.entity.Admin> admins = adminMapper.selectAll(new com.example.entity.Admin());
                    String enterpriseName = dbEnterprise.getEmployName() != null ? dbEnterprise.getEmployName() : "未知企业";
                    String notificationContent = String.format("企业《%s》修改了企业资料，认证状态已自动取消，请审核是否重新认证。", enterpriseName);
                    
                    for (com.example.entity.Admin admin : admins) {
                        // 企业资料修改不关联项目，projectId设为null
                        // 管理员通知使用ADMIN作为recipientType
                        messageService.sendNotification(null, admin.getId(), "ADMIN", notificationContent);
                    }
                }
            }
        }
        
        System.out.println("更新前 - businessLicense: " + enterprise.getBusinessLicense());
        System.out.println("更新前 - verified: " + enterprise.getVerified());
        System.out.println("更新前 - verifiedAt: " + enterprise.getVerifiedAt());
        
        enterpriseMapper.updateById(enterprise);
        
        System.out.println("更新完成");
    }

    /**
     * 根据ID查询
     */
    public Enterprise selectById(Integer id) {
        return enterpriseMapper.selectById(id);
    }

    /**
     * 根据企业ID查询
     */
    public Enterprise selectByEmployId(Integer employId) {
        return enterpriseMapper.selectByEmployId(employId);
    }

    /**
     * 查询当前登录用户的企业信息
     */
    public Enterprise selectCurrent() {
        Account currentUser = TokenUtils.getCurrentUser();
        if (currentUser == null) {
            throw new CustomException(ResultCodeEnum.NO_AUTH_ERROR);
        }
        return enterpriseMapper.selectByEmployId(currentUser.getId());
    }

    /**
     * 查询所有
     */
    public List<Enterprise> selectAll(Enterprise enterprise) {
        return enterpriseMapper.selectAll(enterprise);
    }

    /**
     * 分页查询
     */
    public PageInfo<Enterprise> selectPage(Enterprise enterprise, Integer pageNum, Integer pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<Enterprise> list = enterpriseMapper.selectAll(enterprise);
        return PageInfo.of(list);
    }
}

