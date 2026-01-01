package com.example.service;

import com.example.common.enums.ResultCodeEnum;
import com.example.entity.Account;
import com.example.entity.Enterprise;
import com.example.entity.Project;
import com.example.entity.ProjectPayment;
import com.example.entity.AccountBalance;
import com.example.entity.PaymentRecord;
import com.example.exception.CustomException;
import com.example.mapper.EnterpriseMapper;
import com.example.mapper.ProjectMapper;
import com.example.mapper.ProjectPaymentMapper;
import com.example.mapper.AccountBalanceMapper;
import com.example.mapper.PaymentRecordMapper;
import com.example.utils.TokenUtils;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import com.example.entity.ProjectOrder;
import com.example.entity.Submission;

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
    @Resource
    private NotificationService notificationService;
    @Resource
    private com.example.mapper.SubmissionMapper submissionMapper;
    @Resource
    private com.example.mapper.FreelancerMapper freelancerMapper;
    @Resource
    private ProjectPaymentMapper projectPaymentMapper;
    @Resource
    private AccountBalanceMapper accountBalanceMapper;
    @Resource
    private PaymentRecordMapper paymentRecordMapper;
    @Resource
    private com.example.mapper.AdminMapper adminMapper;
    @Resource
    private com.example.mapper.ProjectAttachmentMapper projectAttachmentMapper;

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
        
        // 验证项目描述字数（至少20个字，不包含空格和符号）
        if (project.getDescription() != null) {
            String desc = project.getDescription().replaceAll("[\\s\\p{P}]", "");
            if (desc.length() < 20) {
                throw new CustomException(ResultCodeEnum.PARAM_ERROR, "项目描述至少需要20个字（不包含空格和符号）");
            }
        }
        
        // 验证详细需求说明字数（至少50个字，不包含空格和符号）
        if (project.getRequirementDetails() != null && !project.getRequirementDetails().trim().isEmpty()) {
            String details = project.getRequirementDetails().replaceAll("[\\s\\p{P}]", "");
            if (details.length() < 50) {
                throw new CustomException(ResultCodeEnum.PARAM_ERROR, "详细需求说明至少需要50个字（不包含空格和符号）");
            }
        }
        
        project.setEnterpriseId(enterprise.getId());
        project.setStatus("PENDING"); // 改为待审核状态，等待管理员审核
        project.setCreatedAt(LocalDateTime.now());
        projectMapper.insert(project);
        
        // 通知所有管理员有新项目发布
        List<com.example.entity.Admin> admins = adminMapper.selectAll(new com.example.entity.Admin());
        for (com.example.entity.Admin admin : admins) {
            notificationService.sendIndividualNotification(
                "PROJECT_STATUS_CHANGE",
                admin.getId(),
                "ADMIN",
                "新项目待审核",
                "企业发布了新项目《" + project.getTitle() + "》，请及时审核。",
                project.getId(),
                null,
                null
            );
        }
        
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
     * 更新项目并通知已接单的自由职业者（不处理保证金变化，仅用于兼容旧API）
     * 注意：建议使用 updateByIdWithPayment 方法来处理预算变更时的保证金流程
     */
    @Transactional
    public void updateByIdWithNotification(Project project) {
        // 先执行更新
        updateById(project);
        
        // 查询项目信息
        Project dbProject = projectMapper.selectById(project.getId());
        
        // 查询该项目的所有接单记录
        ProjectOrder orderQuery = new ProjectOrder();
        orderQuery.setProjectId(project.getId());
        List<ProjectOrder> orders = projectOrderMapper.selectAll(orderQuery);
        
        // 查询该项目的所有稿件（包括有意向合作的）
        List<Submission> submissions = submissionMapper.selectByProjectId(project.getId());
        
        // 收集需要通知的自由职业者ID（去重）
        java.util.Set<Integer> freelancerIdsToNotify = new java.util.HashSet<>();
        
        // 添加所有已接单的自由职业者
        for (ProjectOrder order : orders) {
            if ("ACCEPTED".equals(order.getStatus()) && order.getFreelancerId() != null) {
                freelancerIdsToNotify.add(order.getFreelancerId());
            }
        }
        
        // 添加所有有稿件的自由职业者（状态为SUBMITTED或INTERESTED或CONFIRMED的）
        for (Submission submission : submissions) {
            if (submission.getFreelancerId() != null && 
                ("SUBMITTED".equals(submission.getStatus()) || 
                 "INTERESTED".equals(submission.getStatus()) || 
                 "CONFIRMED".equals(submission.getStatus()))) {
                freelancerIdsToNotify.add(submission.getFreelancerId());
            }
        }
        
        // 发送通知给每个自由职业者
        for (Integer freelancerId : freelancerIdsToNotify) {
            // 获取自由职业者的userId
            com.example.entity.Freelancer freelancer = freelancerMapper.selectById(freelancerId);
            if (freelancer != null && freelancer.getUserId() != null) {
                notificationService.sendIndividualNotification(
                    "PROJECT_STATUS_CHANGE",
                    freelancer.getUserId(),
                    "FREELANCER",
                    "项目信息已更新",
                    "您关注的项目「" + dbProject.getTitle() + "」的信息已更新，请查看最新内容。",
                    project.getId(),
                    null,
                    null
                );
            }
        }
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
     * 获取项目的附件列表
     */
    public List<com.example.entity.ProjectAttachment> getProjectAttachments(Integer projectId) {
        return projectAttachmentMapper.selectByProjectId(projectId);
    }
    
    /**
     * 添加项目附件
     */
    public void addProjectAttachment(com.example.entity.ProjectAttachment attachment) {
        projectAttachmentMapper.insert(attachment);
    }
    
    /**
     * 删除项目附件
     */
    public void deleteProjectAttachment(Integer attachmentId) {
        projectAttachmentMapper.deleteById(attachmentId);
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
     * 分页查询（支持智能推荐）
     */
    public PageInfo<Project> selectPage(Project project, Integer pageNum, Integer pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<Project> list = selectAll(project);
        
        // 如果是自由职业者查看项目列表，进行智能推荐排序
        Account currentUser = TokenUtils.getCurrentUser();
        if (currentUser != null && "USER".equals(currentUser.getRole())) {
            try {
                com.example.entity.Freelancer freelancer = freelancerMapper.selectByUserId(currentUser.getId());
                if (freelancer != null && freelancer.getExperienceLevel() != null) {
                    // 根据经验等级和项目偏好进行排序
                    final String experienceLevel = freelancer.getExperienceLevel();
                    list.sort((p1, p2) -> {
                        int score1 = calculateMatchScore(p1, experienceLevel);
                        int score2 = calculateMatchScore(p2, experienceLevel);
                        return Integer.compare(score2, score1); // 降序排列
                    });
                }
            } catch (Exception e) {
                // 如果获取自由职业者信息失败，不影响正常查询
            }
        }
        
        return PageInfo.of(list);
    }
    
    /**
     * 计算项目与自由职业者的匹配分数
     * @param project 项目
     * @param experienceLevel 自由职业者经验等级
     * @return 匹配分数（越高越匹配）
     */
    private int calculateMatchScore(Project project, String experienceLevel) {
        int score = 0;
        
        // 经验匹配度（40分）
        String preferredExperience = project.getPreferredExperience();
        if ("BOTH".equals(preferredExperience)) {
            score += 40;
        } else if ("NEWBIE".equals(preferredExperience) && "NEWBIE".equals(experienceLevel)) {
            score += 40;
        } else if ("EXPERIENCED".equals(preferredExperience) && 
                   ("JUNIOR".equals(experienceLevel) || "SENIOR".equals(experienceLevel) || "EXPERT".equals(experienceLevel))) {
            score += 40;
        } else if ("NEWBIE".equals(preferredExperience) && !"NEWBIE".equals(experienceLevel)) {
            score += 10; // 老手也可以接新手项目，但匹配度较低
        } else if ("EXPERIENCED".equals(preferredExperience) && "NEWBIE".equals(experienceLevel)) {
            score += 5; // 新手接老手项目，匹配度很低
        }
        
        // 难度匹配度（30分）
        String difficultyLevel = project.getDifficultyLevel();
        if ("EASY".equals(difficultyLevel) && "NEWBIE".equals(experienceLevel)) {
            score += 30;
        } else if ("MEDIUM".equals(difficultyLevel) && 
                   ("JUNIOR".equals(experienceLevel) || "SENIOR".equals(experienceLevel))) {
            score += 30;
        } else if ("HARD".equals(difficultyLevel) && 
                   ("SENIOR".equals(experienceLevel) || "EXPERT".equals(experienceLevel))) {
            score += 30;
        } else if (difficultyLevel == null || "MEDIUM".equals(difficultyLevel)) {
            score += 20; // 默认中等难度，给中等分数
        }
        
        // 时间因素（30分）- 新发布的项目优先
        // 这里可以根据created_at计算，但为了简化，给所有项目相同的基础分
        score += 30;
        
        return score;
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
     * 管理员审核通过项目
     */
    @Transactional
    public void approveProject(Integer id) {
        Project project = projectMapper.selectById(id);
        if (project == null) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR, "项目不存在");
        }
        if (!"PENDING".equals(project.getStatus())) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR, "只有待审核状态的项目才能审核");
        }
        
        project.setStatus("PUBLISHED");
        projectMapper.updateById(project);
        
        // 通知企业项目已通过审核
        Enterprise enterprise = enterpriseMapper.selectById(project.getEnterpriseId());
        if (enterprise != null && enterprise.getEmployId() != null) {
            notificationService.sendIndividualNotification(
                "PROJECT_STATUS_CHANGE",
                enterprise.getEmployId(),
                "ENTERPRISE",
                "项目审核通过",
                "您的项目《" + project.getTitle() + "》已通过审核，现已发布。",
                project.getId(),
                null,
                null
            );
        }
    }
    
    /**
     * 管理员打回项目
     */
    @Transactional
    public void rejectProject(Integer id, String rejectReason) {
        Project project = projectMapper.selectById(id);
        if (project == null) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR, "项目不存在");
        }
        if (!"PENDING".equals(project.getStatus())) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR, "只有待审核状态的项目才能打回");
        }
        if (rejectReason == null || rejectReason.trim().isEmpty()) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR, "打回理由不能为空");
        }
        
        project.setStatus("REJECTED");
        project.setRejectReason(rejectReason);
        projectMapper.updateById(project);
        
        // 通知企业项目已被打回
        Enterprise enterprise = enterpriseMapper.selectById(project.getEnterpriseId());
        if (enterprise != null && enterprise.getEmployId() != null) {
            notificationService.sendIndividualNotification(
                "PROJECT_STATUS_CHANGE",
                enterprise.getEmployId(),
                "ENTERPRISE",
                "项目审核未通过",
                "您的项目《" + project.getTitle() + "》审核未通过。打回理由：" + rejectReason + "。请修改后重新提交。",
                project.getId(),
                null,
                null
            );
        }
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
    
    /**
     * 计算预算变更后的保证金变化
     * @param projectId 项目ID
     * @param newBudgetMin 新预算最小值
     * @param newBudgetMax 新预算最大值
     * @return 包含保证金变化信息的Map
     */
    public java.util.Map<String, Object> calculateDepositChange(Integer projectId, BigDecimal newBudgetMin, BigDecimal newBudgetMax) {
        java.util.Map<String, Object> result = new java.util.HashMap<>();
        
        Project oldProject = projectMapper.selectById(projectId);
        if (oldProject == null) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR, "项目不存在");
        }
        
        // 获取已支付的保证金
        ProjectPayment projectPayment = projectPaymentMapper.selectByProjectId(projectId);
        BigDecimal paidDeposit = BigDecimal.ZERO;
        if (projectPayment != null && projectPayment.getDepositAmount() != null) {
            paidDeposit = projectPayment.getDepositAmount();
        }
        
        // 计算新的保证金要求（预算中位数的50%）
        BigDecimal newMedian = newBudgetMin.add(newBudgetMax)
                .divide(new BigDecimal("2"), 2, RoundingMode.HALF_UP);
        BigDecimal newRequiredDeposit = newMedian.multiply(new BigDecimal("0.5"))
                .setScale(2, RoundingMode.HALF_UP);
        
        result.put("oldDeposit", paidDeposit);
        result.put("newRequiredDeposit", newRequiredDeposit);
        result.put("newMedian", newMedian);
        result.put("newBudgetMin", newBudgetMin);
        result.put("newBudgetMax", newBudgetMax);
        
        // 判断保证金变化类型
        if (newRequiredDeposit.compareTo(paidDeposit) > 0) {
            // 需要补足
            BigDecimal supplementAmount = newRequiredDeposit.subtract(paidDeposit);
            result.put("changeType", "SUPPLEMENT");
            result.put("changeAmount", supplementAmount);
            result.put("message", "预算增加，需补缴保证金 ¥" + supplementAmount.setScale(2, RoundingMode.HALF_UP));
        } else if (newBudgetMax.compareTo(paidDeposit) < 0) {
            // 预算最大值小于已缴纳保证金，可以退款
            BigDecimal refundAmount = paidDeposit.subtract(newRequiredDeposit);
            result.put("changeType", "REFUND");
            result.put("changeAmount", refundAmount);
            result.put("message", "预算减少，保证金将退还 ¥" + refundAmount.setScale(2, RoundingMode.HALF_UP) + " 至账户余额");
        } else {
            // 无需变化
            result.put("changeType", "NONE");
            result.put("changeAmount", BigDecimal.ZERO);
            result.put("message", "保证金无需变化");
        }
        
        return result;
    }
    
    /**
     * 更新项目并处理支付（带支付方式参数）
     */
    @Transactional
    public java.util.Map<String, Object> updateByIdWithPayment(Project project, String paymentMethod) {
        java.util.Map<String, Object> result = new java.util.HashMap<>();
        
        // 获取原项目信息
        Project oldProject = projectMapper.selectById(project.getId());
        if (oldProject == null) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR, "项目不存在");
        }
        
        Account currentUser = TokenUtils.getCurrentUser();
        Enterprise enterprise = enterpriseMapper.selectByEmployId(currentUser.getId());
        if (enterprise == null || !enterprise.getId().equals(oldProject.getEnterpriseId())) {
            throw new CustomException(ResultCodeEnum.NO_AUTH_ERROR);
        }
        
        // 已截止或已确定合作的项目不能编辑
        if ("CLOSED".equals(oldProject.getStatus()) || "CONFIRMED".equals(oldProject.getStatus())) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR, "项目已截止或已确定合作，无法编辑");
        }
        
        // 计算保证金变化
        BigDecimal newBudgetMin = project.getBudgetMin();
        BigDecimal newBudgetMax = project.getBudgetMax();
        
        if (newBudgetMin == null || newBudgetMax == null) {
            // 如果没有预算，使用原来的
            newBudgetMin = oldProject.getBudgetMin();
            newBudgetMax = oldProject.getBudgetMax();
            project.setBudgetMin(newBudgetMin);
            project.setBudgetMax(newBudgetMax);
        }
        
        // 获取已支付的保证金
        ProjectPayment projectPayment = projectPaymentMapper.selectByProjectId(project.getId());
        BigDecimal paidDeposit = BigDecimal.ZERO;
        if (projectPayment != null && projectPayment.getDepositAmount() != null) {
            paidDeposit = projectPayment.getDepositAmount();
        }
        
        // 计算新的保证金要求
        BigDecimal newMedian = newBudgetMin.add(newBudgetMax)
                .divide(new BigDecimal("2"), 2, RoundingMode.HALF_UP);
        BigDecimal newRequiredDeposit = newMedian.multiply(new BigDecimal("0.5"))
                .setScale(2, RoundingMode.HALF_UP);
        
        // 处理保证金补缴
        if (newRequiredDeposit.compareTo(paidDeposit) > 0) {
            BigDecimal supplementAmount = newRequiredDeposit.subtract(paidDeposit);
            
            // 如果选择账户余额支付，检查余额并扣除
            if ("BALANCE".equals(paymentMethod)) {
                AccountBalance balance = accountBalanceMapper.selectByUserIdAndType(currentUser.getId(), "ENTERPRISE");
                if (balance == null || balance.getBalance() == null || balance.getBalance().compareTo(supplementAmount) < 0) {
                    throw new CustomException(ResultCodeEnum.PARAM_ERROR, "账户余额不足，无法使用余额支付");
                }
                // 扣除余额
                balance.setBalance(balance.getBalance().subtract(supplementAmount));
                balance.setUpdatedAt(LocalDateTime.now());
                accountBalanceMapper.updateById(balance);
            }
            
            // 创建补缴支付记录
            PaymentRecord paymentRecord = new PaymentRecord();
            paymentRecord.setUserId(currentUser.getId());
            paymentRecord.setUserType("ENTERPRISE");
            paymentRecord.setProjectId(project.getId());
            paymentRecord.setPaymentType("DEPOSIT_SUPPLEMENT");
            paymentRecord.setAmount(supplementAmount);
            paymentRecord.setStatus("SUCCESS");
            paymentRecord.setPaymentMethod("BALANCE".equals(paymentMethod) ? "BALANCE" : "MOCK");
            paymentRecord.setTransactionId(("BALANCE".equals(paymentMethod) ? "BALANCE_" : "MOCK_") 
                    + UUID.randomUUID().toString().replace("-", ""));
            paymentRecord.setRemark("项目预算调整，保证金补缴" + ("BALANCE".equals(paymentMethod) ? "（账户余额支付）" : "（在线支付）"));
            paymentRecord.setCreatedAt(LocalDateTime.now());
            paymentRecord.setUpdatedAt(LocalDateTime.now());
            paymentRecordMapper.insert(paymentRecord);
            
            // 更新项目支付记录
            if (projectPayment != null) {
                projectPayment.setDepositAmount(newRequiredDeposit);
                BigDecimal currentPaid = projectPayment.getPaidAmount() != null ? projectPayment.getPaidAmount() : BigDecimal.ZERO;
                projectPayment.setPaidAmount(currentPaid.add(supplementAmount));
                projectPayment.setUpdatedAt(LocalDateTime.now());
                projectPaymentMapper.updateById(projectPayment);
            }
            
            result.put("depositAction", "SUPPLEMENT");
            result.put("depositAmount", supplementAmount);
            result.put("message", "已" + ("BALANCE".equals(paymentMethod) ? "从账户余额扣除" : "支付") + "保证金补缴 ¥" + supplementAmount.setScale(2, RoundingMode.HALF_UP));
        }
        // 处理保证金退款
        else if (newBudgetMax.compareTo(paidDeposit) < 0) {
            BigDecimal refundAmount = paidDeposit.subtract(newRequiredDeposit);
            
            // 退款到企业余额
            AccountBalance balance = accountBalanceMapper.selectByUserIdAndType(enterprise.getEmployId(), "ENTERPRISE");
            if (balance == null) {
                balance = new AccountBalance();
                balance.setUserId(enterprise.getEmployId());
                balance.setUserType("ENTERPRISE");
                balance.setBalance(BigDecimal.ZERO);
                balance.setFrozenBalance(BigDecimal.ZERO);
                balance.setCreatedAt(LocalDateTime.now());
                balance.setUpdatedAt(LocalDateTime.now());
                accountBalanceMapper.insert(balance);
            }
            BigDecimal currentBalance = balance.getBalance() != null ? balance.getBalance() : BigDecimal.ZERO;
            balance.setBalance(currentBalance.add(refundAmount));
            balance.setUpdatedAt(LocalDateTime.now());
            accountBalanceMapper.updateById(balance);
            
            // 创建退款记录
            PaymentRecord refundRecord = new PaymentRecord();
            refundRecord.setUserId(currentUser.getId());
            refundRecord.setUserType("ENTERPRISE");
            refundRecord.setProjectId(project.getId());
            refundRecord.setPaymentType("DEPOSIT_REFUND");
            refundRecord.setAmount(refundAmount);
            refundRecord.setStatus("SUCCESS");
            refundRecord.setPaymentMethod("BALANCE");
            refundRecord.setTransactionId("REFUND_" + UUID.randomUUID().toString().replace("-", ""));
            refundRecord.setRemark("项目预算调整，保证金退款至余额");
            refundRecord.setCreatedAt(LocalDateTime.now());
            refundRecord.setUpdatedAt(LocalDateTime.now());
            paymentRecordMapper.insert(refundRecord);
            
            // 更新项目支付记录
            if (projectPayment != null) {
                projectPayment.setDepositAmount(newRequiredDeposit);
                BigDecimal currentPaid = projectPayment.getPaidAmount() != null ? projectPayment.getPaidAmount() : BigDecimal.ZERO;
                projectPayment.setPaidAmount(currentPaid.subtract(refundAmount));
                projectPayment.setUpdatedAt(LocalDateTime.now());
                projectPaymentMapper.updateById(projectPayment);
            }
            
            result.put("depositAction", "REFUND");
            result.put("depositAmount", refundAmount);
            result.put("message", "保证金 ¥" + refundAmount.setScale(2, RoundingMode.HALF_UP) + " 已退还至账户余额");
        } else {
            result.put("depositAction", "NONE");
            result.put("depositAmount", BigDecimal.ZERO);
        }
        
        // 更新项目
        projectMapper.updateById(project);
        
        // 通知已接单的自由职业者
        notifyFreelancersAboutProjectUpdate(project.getId());
        
        result.put("success", true);
        return result;
    }
    
    /**
     * 通知自由职业者项目更新
     */
    private void notifyFreelancersAboutProjectUpdate(Integer projectId) {
        Project dbProject = projectMapper.selectById(projectId);
        if (dbProject == null) return;
        
        // 查询该项目的所有接单记录
        ProjectOrder orderQuery = new ProjectOrder();
        orderQuery.setProjectId(projectId);
        List<ProjectOrder> orders = projectOrderMapper.selectAll(orderQuery);
        
        // 查询该项目的所有稿件
        List<Submission> submissions = submissionMapper.selectByProjectId(projectId);
        
        // 收集需要通知的自由职业者ID（去重）
        java.util.Set<Integer> freelancerIdsToNotify = new java.util.HashSet<>();
        
        for (ProjectOrder order : orders) {
            if ("ACCEPTED".equals(order.getStatus()) && order.getFreelancerId() != null) {
                freelancerIdsToNotify.add(order.getFreelancerId());
            }
        }
        
        for (Submission submission : submissions) {
            if (submission.getFreelancerId() != null && 
                ("SUBMITTED".equals(submission.getStatus()) || 
                 "INTERESTED".equals(submission.getStatus()) || 
                 "CONFIRMED".equals(submission.getStatus()))) {
                freelancerIdsToNotify.add(submission.getFreelancerId());
            }
        }
        
        for (Integer freelancerId : freelancerIdsToNotify) {
            com.example.entity.Freelancer freelancer = freelancerMapper.selectById(freelancerId);
            if (freelancer != null && freelancer.getUserId() != null) {
                notificationService.sendIndividualNotification(
                    "PROJECT_STATUS_CHANGE",
                    freelancer.getUserId(),
                    "FREELANCER",
                    "项目信息已更新",
                    "您关注的项目「" + dbProject.getTitle() + "」的信息已更新，请查看最新内容。",
                    projectId,
                    null,
                    null
                );
            }
        }
    }
}

