package com.example.controller;

import com.example.common.Result;
import com.example.common.enums.RoleEnum;
import com.example.entity.Account;
import com.example.entity.Enterprise;
import com.example.entity.Freelancer;
import com.example.mapper.EmployMapper;
import com.example.mapper.UserMapper;
import com.example.service.AdminService;
import com.example.service.EmployService;
import com.example.service.EnterpriseService;
import com.example.service.FreelancerService;
import com.example.service.UserService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

/**
 * 统一认证接口
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Resource
    private AdminService adminService;
    @Resource
    private EmployService employService;
    @Resource
    private UserService userService;
    @Resource
    private FreelancerService freelancerService;
    @Resource
    private EnterpriseService enterpriseService;
    @Resource
    private UserMapper userMapper;
    @Resource
    private EmployMapper employMapper;

    /**
     * 统一登录
     */
    @PostMapping("/login")
    public Result login(@RequestBody Account account) {
        Account loginAccount = null;
        if (RoleEnum.ADMIN.name().equals(account.getRole())) {
            loginAccount = adminService.login(account);
        }
        if (RoleEnum.EMPLOY.name().equals(account.getRole())) {
            loginAccount = employService.login(account);
        }
        if (RoleEnum.USER.name().equals(account.getRole())) {
            loginAccount = userService.login(account);
        }
        return Result.success(loginAccount);
    }

    /**
     * 自由职业者注册
     */
    @PostMapping("/register/freelancer")
    public Result registerFreelancer(@RequestBody Account account) {
        account.setRole(RoleEnum.USER.name());
        userService.register(account);
        
        // 创建自由职业者扩展信息
        com.example.entity.User user = userMapper.selectByUsername(account.getUsername());
        if (user != null) {
            Freelancer freelancer = new Freelancer();
            freelancer.setUserId(user.getId());
            try {
                freelancerService.add(freelancer);
            } catch (Exception e) {
                // 如果创建失败，不影响注册流程
            }
        }
        
        return Result.success();
    }

    /**
     * 企业用户注册
     */
    @PostMapping("/register/enterprise")
    public Result registerEnterprise(@RequestBody Account account) {
        account.setRole(RoleEnum.EMPLOY.name());
        employService.register(account);
        
        // 创建企业扩展信息
        com.example.entity.Employ employ = employMapper.selectByUsername(account.getUsername());
        if (employ != null) {
            Enterprise enterprise = new Enterprise();
            enterprise.setEmployId(employ.getId());
            try {
                enterpriseService.add(enterprise);
            } catch (Exception e) {
                // 如果创建失败，不影响注册流程
            }
        }
        
        return Result.success();
    }
}

