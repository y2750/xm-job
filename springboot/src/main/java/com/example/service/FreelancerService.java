package com.example.service;

import com.example.common.enums.ResultCodeEnum;
import com.example.entity.Account;
import com.example.entity.Freelancer;
import com.example.entity.User;
import com.example.exception.CustomException;
import com.example.mapper.FreelancerMapper;
import com.example.mapper.UserMapper;
import com.example.utils.TokenUtils;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 自由职业者业务层
 */
@Service
public class FreelancerService {

    @Resource
    private FreelancerMapper freelancerMapper;
    @Resource
    private UserMapper userMapper;
    @Resource
    private com.example.service.MessageService messageService;

    /**
     * 新增自由职业者信息
     */
    public void add(Freelancer freelancer) {
        Account currentUser = TokenUtils.getCurrentUser();
        if (currentUser == null) {
            throw new CustomException(ResultCodeEnum.NO_AUTH_ERROR);
        }
        
        // 检查是否已存在
        Freelancer exist = freelancerMapper.selectByUserId(currentUser.getId());
        if (exist != null) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR, "自由职业者信息已存在");
        }
        
        freelancer.setUserId(currentUser.getId());
        freelancer.setCreditScore(100); // 默认信誉分100
        freelancer.setCreatedAt(LocalDateTime.now());
        freelancerMapper.insert(freelancer);
    }

    /**
     * 更新自由职业者信息
     */
    public void updateById(Freelancer freelancer) {
        Account currentUser = TokenUtils.getCurrentUser();
        if (currentUser == null) {
            throw new CustomException(ResultCodeEnum.NO_AUTH_ERROR);
        }
        
        // 如果提供了ID，直接使用ID更新（管理员审核场景）
        if (freelancer.getId() != null) {
            Freelancer dbFreelancer = freelancerMapper.selectById(freelancer.getId());
            if (dbFreelancer == null) {
                throw new CustomException(ResultCodeEnum.PARAM_ERROR, "自由职业者信息不存在");
            }
            // 管理员可以更新任何自由职业者信息，普通用户只能更新自己的
            if (!"ADMIN".equals(currentUser.getRole())) {
                // 非管理员需要验证是否是自己信息
                Freelancer currentFreelancer = freelancerMapper.selectByUserId(currentUser.getId());
                if (currentFreelancer == null || !currentFreelancer.getId().equals(freelancer.getId())) {
                    throw new CustomException(ResultCodeEnum.NO_AUTH_ERROR, "只能更新自己的信息");
                }
            }
            
            // 检测管理员认证自由职业者的情况（从false变为true）
            boolean wasVerified = dbFreelancer.getVerified() != null && dbFreelancer.getVerified();
            boolean willBeVerified = freelancer.getVerified() != null && freelancer.getVerified();
            if (!wasVerified && willBeVerified && "ADMIN".equals(currentUser.getRole())) {
                // 管理员认证了自由职业者，发送通知给自由职业者
                String freelancerName = dbFreelancer.getUserName() != null ? dbFreelancer.getUserName() : "未知用户";
                String notificationContent = String.format("恭喜！您的账号《%s》已通过认证审核，现在可以提交稿件了。", freelancerName);
                messageService.sendNotification(null, dbFreelancer.getUserId(), "FREELANCER", notificationContent);
            }
        } else {
            // 如果没有提供ID，使用当前登录用户的ID（自由职业者更新自己的信息）
            Freelancer dbFreelancer = freelancerMapper.selectByUserId(currentUser.getId());
            if (dbFreelancer == null) {
                // 如果 freelancer 表中没有记录，自动创建一条
                freelancer.setUserId(currentUser.getId());
                freelancer.setVerified(false);
                freelancer.setRating(BigDecimal.ZERO);
                freelancer.setCompletedProjects(0);
                freelancer.setCreditScore(100); // 默认信誉分100
                freelancer.setPortfolioCount(0);
                freelancer.setCreatedAt(LocalDateTime.now());
                freelancerMapper.insert(freelancer);
                return;
            }
            freelancer.setId(dbFreelancer.getId());
        }
        
        freelancerMapper.updateById(freelancer);
    }

    /**
     * 根据ID查询
     */
    public Freelancer selectById(Integer id) {
        return freelancerMapper.selectById(id);
    }

    /**
     * 根据用户ID查询
     */
    public Freelancer selectByUserId(Integer userId) {
        return freelancerMapper.selectByUserId(userId);
    }

    /**
     * 查询当前登录用户的自由职业者信息（包含用户基本信息）
     */
    public Freelancer selectCurrent() {
        Account currentUser = TokenUtils.getCurrentUser();
        if (currentUser == null) {
            throw new CustomException(ResultCodeEnum.NO_AUTH_ERROR);
        }
        Freelancer freelancer = freelancerMapper.selectByUserId(currentUser.getId());
        // 如果不存在，返回一个空对象（前端可以正常显示表单）
        if (freelancer == null) {
            freelancer = new Freelancer();
            freelancer.setUserId(currentUser.getId());
            freelancer.setVerified(false);
            freelancer.setRating(BigDecimal.ZERO);
            freelancer.setCompletedProjects(0);
            freelancer.setPortfolioCount(0);
        }
        
        // 加载用户基本信息
        User user = userMapper.selectById(currentUser.getId());
        if (user != null) {
            freelancer.setUserName(user.getName());
            freelancer.setUserAvatar(user.getAvatar());
            freelancer.setUserEmail(user.getEmail());
            freelancer.setUserPhone(user.getPhone());
        }
        
        return freelancer;
    }

    /**
     * 更新个人资料（同时更新用户基本信息和自由职业者信息）
     */
    public void updateProfile(Freelancer freelancer) {
        Account currentUser = TokenUtils.getCurrentUser();
        if (currentUser == null) {
            throw new CustomException(ResultCodeEnum.NO_AUTH_ERROR);
        }
        
        // 更新用户基本信息
        if (freelancer.getUserName() != null || freelancer.getUserAvatar() != null 
            || freelancer.getUserEmail() != null || freelancer.getUserPhone() != null) {
            User user = userMapper.selectById(currentUser.getId());
            if (user != null) {
                if (freelancer.getUserName() != null) {
                    user.setName(freelancer.getUserName());
                }
                if (freelancer.getUserAvatar() != null) {
                    user.setAvatar(freelancer.getUserAvatar());
                }
                if (freelancer.getUserEmail() != null) {
                    user.setEmail(freelancer.getUserEmail());
                }
                if (freelancer.getUserPhone() != null) {
                    user.setPhone(freelancer.getUserPhone());
                }
                userMapper.updateById(user);
            }
        }
        
        // 更新自由职业者信息
        updateById(freelancer);
    }

    /**
     * 查询所有
     */
    public List<Freelancer> selectAll(Freelancer freelancer) {
        return freelancerMapper.selectAll(freelancer);
    }

    /**
     * 分页查询
     */
    public PageInfo<Freelancer> selectPage(Freelancer freelancer, Integer pageNum, Integer pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<Freelancer> list = freelancerMapper.selectAll(freelancer);
        return PageInfo.of(list);
    }
}

