package com.example.service;

import com.example.entity.Account;
import com.example.entity.Freelancer;
import com.example.entity.Project;
import com.example.entity.ProjectOrder;
import com.example.exception.CustomException;
import com.example.common.enums.ResultCodeEnum;
import com.example.mapper.*;
import com.example.utils.TokenUtils;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 项目接单业务层
 */
@Service
public class ProjectOrderService {

    @Resource
    private ProjectOrderMapper orderMapper;
    @Resource
    private ProjectMapper projectMapper;
    @Resource
    private FreelancerMapper freelancerMapper;
    @Resource
    private SubmissionMapper submissionMapper;
    @Resource
    private EnterpriseMapper enterpriseMapper;
    @Resource
    private AccountBalanceMapper accountBalanceMapper;
    @Resource
    private PaymentService paymentService;
    @Resource
    private com.example.service.MessageService messageService;
    @Resource
    private NotificationService notificationService;

    /**
     * 接单（自由职业者接单项目）
     */
    public void acceptOrder(Integer projectId) {
        Account currentUser = TokenUtils.getCurrentUser();
        if (currentUser == null) {
            throw new CustomException(ResultCodeEnum.NO_AUTH_ERROR);
        }

        // 获取自由职业者信息
        Freelancer freelancer = freelancerMapper.selectByUserId(currentUser.getId());
        if (freelancer == null) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR, "请先完善自由职业者信息");
        }

        // 检查自由职业者是否已认证
        if (freelancer.getVerified() == null || !freelancer.getVerified()) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR, "自由职业者未认证，无法接单");
        }
        
        // 检查信誉分是否低于60分
        Integer creditScore = freelancer.getCreditScore() != null ? freelancer.getCreditScore() : 100;
        if (creditScore < 60) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR, "信誉分低于60分，不允许接单");
        }

        // 检查项目是否存在
        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR, "项目不存在");
        }
        // 如果项目已确认合作，不允许接单
        if ("CONFIRMED".equals(project.getStatus())) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR, "项目已确定合作，不再接受新的接单");
        }
        if (!"PUBLISHED".equals(project.getStatus())) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR, "项目已截止");
        }

        // 检查是否已接单
        ProjectOrder existOrder = orderMapper.selectByProjectAndFreelancer(projectId, freelancer.getId());
        if (existOrder != null) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR, "您已接单此项目");
        }

        // 检查同时接单数是否超过5个
        List<ProjectOrder> activeOrders = orderMapper.selectActiveByFreelancerId(freelancer.getId());
        if (activeOrders != null && activeOrders.size() >= 5) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR, "同时接单数不能超过5个，请先完成或取消部分订单");
        }

        // 创建接单记录
        ProjectOrder order = new ProjectOrder();
        order.setProjectId(projectId);
        order.setFreelancerId(freelancer.getId());
        order.setStatus("ACCEPTED");
        order.setAcceptedAt(LocalDateTime.now());
        orderMapper.insert(order);
        
        // 发送通知给企业：有新的接单者
        com.example.entity.Enterprise enterprise = enterpriseMapper.selectById(project.getEnterpriseId());
        if (enterprise != null) {
            notificationService.sendIndividualNotification("PROJECT_STATUS_CHANGE", enterprise.getEmployId(), "ENTERPRISE",
                    "项目有新接单者", String.format("您的项目《%s》有新的接单者，请及时查看。", project.getTitle()),
                    projectId, null, null);
        }
    }

    /**
     * 取消接单
     */
    public void cancelOrder(Integer orderId) {
        Account currentUser = TokenUtils.getCurrentUser();
        if (currentUser == null) {
            throw new CustomException(ResultCodeEnum.NO_AUTH_ERROR);
        }

        ProjectOrder order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR, "接单记录不存在");
        }

        Freelancer freelancer = freelancerMapper.selectByUserId(currentUser.getId());
        if (freelancer == null || !freelancer.getId().equals(order.getFreelancerId())) {
            throw new CustomException(ResultCodeEnum.NO_AUTH_ERROR, "只能取消自己的接单");
        }

        if (!"ACCEPTED".equals(order.getStatus())) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR, "只能取消已接单状态的订单");
        }

        // 检查项目状态，如果已确认合作，不允许直接取消，需要调用放弃接单
        Project project = projectMapper.selectById(order.getProjectId());
        if (project != null && "CONFIRMED".equals(project.getStatus())) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR, "项目已确认合作，如需取消请使用放弃接单功能，将进行保证金赔付");
        }

        order.setStatus("CANCELLED");
        orderMapper.updateById(order);
        
        // 发送通知给企业：接单者取消了接单
        com.example.entity.Enterprise enterprise = enterpriseMapper.selectById(project.getEnterpriseId());
        if (enterprise != null) {
            notificationService.sendIndividualNotification("PROJECT_STATUS_CHANGE", enterprise.getEmployId(), "ENTERPRISE",
                    "接单者取消接单", String.format("您的项目《%s》有接单者取消了接单。", project.getTitle()),
                    project.getId(), null, null);
        }
    }
    
    /**
     * 放弃接单（在合作过程中放弃，保证金赔付给企业）
     */
    @org.springframework.transaction.annotation.Transactional
    public void abandonOrder(Integer orderId) {
        Account currentUser = TokenUtils.getCurrentUser();
        if (currentUser == null) {
            throw new CustomException(ResultCodeEnum.NO_AUTH_ERROR);
        }

        ProjectOrder order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR, "接单记录不存在");
        }

        Freelancer freelancer = freelancerMapper.selectByUserId(currentUser.getId());
        if (freelancer == null || !freelancer.getId().equals(order.getFreelancerId())) {
            throw new CustomException(ResultCodeEnum.NO_AUTH_ERROR, "只能放弃自己的接单");
        }

        Project project = projectMapper.selectById(order.getProjectId());
        if (project == null) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR, "项目不存在");
        }
        
        // 检查项目是否已确定合作
        if (!"CONFIRMED".equals(project.getStatus())) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR, "只能放弃已确定合作的项目");
        }
        
        // 检查是否已放弃
        if (order.getAbandoned() != null && order.getAbandoned()) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR, "已放弃此接单");
        }
        
        // 查找对应的稿件
        com.example.entity.Submission submission = new com.example.entity.Submission();
        submission.setProjectId(order.getProjectId());
        submission.setFreelancerId(order.getFreelancerId());
        List<com.example.entity.Submission> submissions = submissionMapper.selectAll(submission);
        com.example.entity.Submission confirmedSubmission = submissions.stream()
            .filter(s -> "CONFIRMED".equals(s.getStatus()))
            .findFirst()
            .orElse(null);
        
        if (confirmedSubmission == null) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR, "未找到对应的稿件");
        }
        
        // 计算保证金（报价的5%）
        if (confirmedSubmission.getQuotePrice() != null && confirmedSubmission.getDepositPaid() != null && confirmedSubmission.getDepositPaid()) {
            BigDecimal depositAmount = confirmedSubmission.getQuotePrice()
                .multiply(new BigDecimal("0.05"))
                .setScale(2, RoundingMode.HALF_UP);
            
            // 将保证金赔付给企业
            com.example.entity.Enterprise enterprise = enterpriseMapper.selectByEmployId(project.getEnterpriseId());
            if (enterprise != null) {
                com.example.entity.AccountBalance enterpriseBalance = paymentService.getOrCreateBalance(
                    project.getEnterpriseId(), "ENTERPRISE");
                enterpriseBalance.setBalance(enterpriseBalance.getBalance().add(depositAmount));
                accountBalanceMapper.updateById(enterpriseBalance);
            }
        }
        
        // 根据距离交单日期计算扣分（距离越近扣分越多，最低10分，最高20分）
        if (freelancer != null) {
            Integer currentScore = freelancer.getCreditScore() != null ? freelancer.getCreditScore() : 100;
            int deductionScore = 10; // 默认扣10分
            
            // 如果项目有交单日期，根据距离交单日期计算扣分
            if (project.getDeliveryDeadline() != null) {
                LocalDateTime now = LocalDateTime.now();
                LocalDateTime deadline = project.getDeliveryDeadline();
                
                // 如果当前时间已经超过交单日期，扣最高分20分
                if (now.isAfter(deadline) || now.isEqual(deadline)) {
                    deductionScore = 20;
                } else {
                    // 计算距离交单日期的天数
                    long daysUntilDeadline = java.time.temporal.ChronoUnit.DAYS.between(now, deadline);
                    
                    // 如果距离交单日期在7天内，根据天数计算扣分
                    // 距离越近扣分越多：7天以上扣10分，7天内按比例增加到20分
                    if (daysUntilDeadline <= 7) {
                        // 线性计算：7天扣10分，0天扣20分
                        // deductionScore = 10 + (7 - daysUntilDeadline) * (10 / 7)
                        deductionScore = 10 + (int) Math.round((7 - daysUntilDeadline) * 10.0 / 7.0);
                        // 确保在10-20分之间
                        deductionScore = Math.max(10, Math.min(20, deductionScore));
                    } else {
                        // 超过7天，扣最低分10分
                        deductionScore = 10;
                    }
                }
            }
            
            // 扣除信誉分（最低0分）
            freelancer.setCreditScore(Math.max(0, currentScore - deductionScore));
            freelancerMapper.updateById(freelancer);
        }
        
        // 标记为已放弃
        order.setAbandoned(true);
        order.setAbandonedAt(java.time.LocalDateTime.now());
        orderMapper.updateById(order);
    }

    /**
     * 查询当前用户的接单列表
     */
    public List<ProjectOrder> selectMyOrders() {
        Account currentUser = TokenUtils.getCurrentUser();
        if (currentUser == null) {
            throw new CustomException(ResultCodeEnum.NO_AUTH_ERROR);
        }

        Freelancer freelancer = freelancerMapper.selectByUserId(currentUser.getId());
        if (freelancer == null) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR, "请先完善自由职业者信息");
        }

        ProjectOrder query = new ProjectOrder();
        query.setFreelancerId(freelancer.getId());
        return orderMapper.selectAll(query);
    }

    /**
     * 查询项目的接单人列表（包含freelancer信息）
     */
    public List<ProjectOrder> selectByProjectIdWithFreelancer(Integer projectId) {
        return orderMapper.selectByProjectIdWithFreelancer(projectId);
    }
}

