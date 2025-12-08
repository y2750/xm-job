package com.example.service;

import com.example.common.enums.ResultCodeEnum;
import com.example.entity.Account;
import com.example.entity.Enterprise;
import com.example.entity.Project;
import com.example.exception.CustomException;
import com.example.mapper.EnterpriseMapper;
import com.example.mapper.ProjectMapper;
import com.example.utils.TokenUtils;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * 项目管理业务层
 */
@Service
public class ProjectService {

    @Resource
    private ProjectMapper projectMapper;
    @Resource
    private EnterpriseMapper enterpriseMapper;
    @Resource
    private PaymentService paymentService;
    @Resource
    private com.example.mapper.ProjectOrderMapper projectOrderMapper;

    /**
     * 发布项目
     */
    @Transactional
    public void add(Project project) {
        Account currentUser = TokenUtils.getCurrentUser();
        if (currentUser == null) {
            throw new CustomException(ResultCodeEnum.NO_AUTH_ERROR);
        }
        
        // 获取企业信息
        Enterprise enterprise = enterpriseMapper.selectByEmployId(currentUser.getId());
        if (enterprise == null) {
            // 如果 enterprise 表中没有记录，自动创建一条
            enterprise = new Enterprise();
            enterprise.setEmployId(currentUser.getId());
            enterprise.setVerified(false);
            enterprise.setCreatedAt(LocalDateTime.now());
            enterpriseMapper.insert(enterprise);
            // 重新查询获取生成的ID
            enterprise = enterpriseMapper.selectByEmployId(currentUser.getId());
        }
        
        // 检查企业是否已认证
        if (enterprise.getVerified() == null || !enterprise.getVerified()) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR, "企业未认证，无法发布项目，请先完善企业信息并等待审核");
        }
        
        project.setEnterpriseId(enterprise.getId());
        project.setStatus("PUBLISHED");
        project.setCreatedAt(LocalDateTime.now());
        projectMapper.insert(project);
        
        // 支付发布保证金（从请求参数中获取支付方式，默认为在线支付）
        String paymentMethod = project.getPaymentMethod();
        if (paymentMethod == null || paymentMethod.isEmpty()) {
            paymentMethod = "ONLINE"; // 默认在线支付
        }
        paymentService.payPublishDeposit(project, paymentMethod);
    }

    /**
     * 更新项目
     */
    public void updateById(Project project) {
        // 权限校验：只能更新自己的项目
        Project dbProject = projectMapper.selectById(project.getId());
        if (dbProject == null) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR, "项目不存在");
        }
        
        Account currentUser = TokenUtils.getCurrentUser();
        Enterprise enterprise = enterpriseMapper.selectByEmployId(currentUser.getId());
        if (enterprise == null || !enterprise.getId().equals(dbProject.getEnterpriseId())) {
            throw new CustomException(ResultCodeEnum.NO_AUTH_ERROR);
        }
        
        // 已截止或已确定合作的项目不能编辑
        if ("CLOSED".equals(dbProject.getStatus()) || "CONFIRMED".equals(dbProject.getStatus())) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR, "项目已截止或已确定合作，无法编辑");
        }
        
        projectMapper.updateById(project);
    }

    /**
     * 删除项目
     */
    public void deleteById(Integer id) {
        Project project = projectMapper.selectById(id);
        if (project == null) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR, "项目不存在");
        }
        
        Account currentUser = TokenUtils.getCurrentUser();
        Enterprise enterprise = enterpriseMapper.selectByEmployId(currentUser.getId());
        if (enterprise == null || !enterprise.getId().equals(project.getEnterpriseId())) {
            throw new CustomException(ResultCodeEnum.NO_AUTH_ERROR);
        }
        
        projectMapper.deleteById(id);
    }

    /**
     * 根据ID查询
     */
    public Project selectById(Integer id) {
        Project project = projectMapper.selectById(id);
        if (project != null && project.getSkillsRequired() != null) {
            // 支持中文逗号和英文逗号：先替换中文逗号为英文逗号，再分割
            String skills = project.getSkillsRequired().replace("，", ",");
            project.setSkillList(Arrays.asList(skills.split(",")));
        }
        return project;
    }

    /**
     * 查询所有（支持筛选）
     */
    public List<Project> selectAll(Project project) {
        List<Project> list = projectMapper.selectAll(project);
        // 处理技能标签列表
        list.forEach(p -> {
            if (p.getSkillsRequired() != null) {
                // 支持中文逗号和英文逗号：先替换中文逗号为英文逗号，再分割
                String skills = p.getSkillsRequired().replace("，", ",");
                p.setSkillList(Arrays.asList(skills.split(",")));
            }
        });
        return list;
    }

    /**
     * 分页查询
     */
    public PageInfo<Project> selectPage(Project project, Integer pageNum, Integer pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<Project> list = selectAll(project);
        return PageInfo.of(list);
    }

    /**
     * 更新项目状态
     */
    public void updateStatus(Integer id, String status) {
        Project project = projectMapper.selectById(id);
        if (project == null) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR, "项目不存在");
        }
        
        Account currentUser = TokenUtils.getCurrentUser();
        Enterprise enterprise = enterpriseMapper.selectByEmployId(currentUser.getId());
        if (enterprise == null || !enterprise.getId().equals(project.getEnterpriseId())) {
            throw new CustomException(ResultCodeEnum.NO_AUTH_ERROR);
        }
        
        project.setStatus(status);
        projectMapper.updateById(project);
    }

    /**
     * 自动关闭已截止的项目
     */
    public void closeExpiredProjects() {
        List<Project> projects = projectMapper.selectPublishedProjects();
        LocalDateTime now = LocalDateTime.now();
        for (Project project : projects) {
            if (project.getDeadline() != null && project.getDeadline().isBefore(now)) {
                project.setStatus("CLOSED");
                projectMapper.updateById(project);
            }
        }
    }
    
    /**
     * 重新上架项目（当确认合作的接单人放弃接单后，企业可以重新上架）
     */
    @Transactional
    public void republishProject(Integer projectId) {
        Account currentUser = TokenUtils.getCurrentUser();
        if (currentUser == null) {
            throw new CustomException(ResultCodeEnum.NO_AUTH_ERROR);
        }
        
        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR, "项目不存在");
        }
        
        // 检查权限：只有项目发布者可以重新上架
        Enterprise enterprise = enterpriseMapper.selectByEmployId(currentUser.getId());
        if (enterprise == null || !enterprise.getId().equals(project.getEnterpriseId())) {
            throw new CustomException(ResultCodeEnum.NO_AUTH_ERROR, "只有项目发布者可以重新上架项目");
        }
        
        // 检查项目状态：必须是已确认合作状态
        if (!"CONFIRMED".equals(project.getStatus())) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR, "只能重新上架已确认合作的项目");
        }
        
        // 检查确认合作的接单人是否已放弃接单
        if (project.getConfirmedFreelancerId() != null) {
            com.example.entity.ProjectOrder confirmedOrder = new com.example.entity.ProjectOrder();
            confirmedOrder.setProjectId(projectId);
            confirmedOrder.setFreelancerId(project.getConfirmedFreelancerId());
            List<com.example.entity.ProjectOrder> orders = projectOrderMapper.selectAll(confirmedOrder);
            com.example.entity.ProjectOrder order = orders.stream()
                .filter(o -> o.getFreelancerId().equals(project.getConfirmedFreelancerId()))
                .findFirst()
                .orElse(null);
            
            // 如果接单人没有放弃接单，不允许重新上架
            if (order == null || order.getAbandoned() == null || !order.getAbandoned()) {
                throw new CustomException(ResultCodeEnum.PARAM_ERROR, "确认合作的接单人尚未放弃接单，无法重新上架");
            }
        }
        
        // 重新上架：将项目状态改为 PUBLISHED，清空确认合作的接单人
        project.setStatus("PUBLISHED");
        project.setConfirmedFreelancerId(null);
        projectMapper.updateById(project);
    }
}

