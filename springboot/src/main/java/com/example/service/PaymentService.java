package com.example.service;

import com.example.common.enums.ResultCodeEnum;
import com.example.entity.*;
import com.example.exception.CustomException;
import com.example.mapper.*;
import com.example.utils.TokenUtils;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 支付服务
 */
@Service
public class PaymentService {

    @Resource
    private AccountBalanceMapper accountBalanceMapper;
    @Resource
    private PaymentRecordMapper paymentRecordMapper;
    @Resource
    private WithdrawalRecordMapper withdrawalRecordMapper;
    @Resource
    private ProjectPaymentMapper projectPaymentMapper;
    @Resource
    private ProjectMapper projectMapper;
    @Resource
    private SubmissionMapper submissionMapper;
    @Resource
    private FreelancerMapper freelancerMapper;
    @Resource
    private EnterpriseMapper enterpriseMapper;

    /**
     * 获取或创建账户余额
     */
    public AccountBalance getOrCreateBalance(Integer userId, String userType) {
        AccountBalance balance = accountBalanceMapper.selectByUserIdAndType(userId, userType);
        if (balance == null) {
            balance = new AccountBalance();
            balance.setUserId(userId);
            balance.setUserType(userType);
            balance.setBalance(BigDecimal.ZERO);
            balance.setFrozenBalance(BigDecimal.ZERO);
            balance.setCreatedAt(LocalDateTime.now());
            balance.setUpdatedAt(LocalDateTime.now());
            accountBalanceMapper.insert(balance);
        }
        return balance;
    }

    /**
     * 支付（发布项目保证金）
     * @param project 项目
     * @param paymentMethod 支付方式：BALANCE（账户余额）或 ONLINE（在线支付）
     */
    @Transactional
    public PaymentRecord payPublishDeposit(Project project, String paymentMethod) {
        Account currentUser = TokenUtils.getCurrentUser();
        if (currentUser == null) {
            throw new CustomException(ResultCodeEnum.NO_AUTH_ERROR);
        }

        // 计算保证金：报价中位数的50%
        BigDecimal budgetMin = project.getBudgetMin();
        BigDecimal budgetMax = project.getBudgetMax();
        if (budgetMin == null || budgetMax == null) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR, "请设置项目预算范围");
        }
        BigDecimal median = budgetMin.add(budgetMax).divide(new BigDecimal("2"), 2, RoundingMode.HALF_UP);
        BigDecimal depositAmount = median.multiply(new BigDecimal("0.5")).setScale(2, RoundingMode.HALF_UP);

        // 如果选择账户余额支付，检查余额并扣除
        if ("BALANCE".equals(paymentMethod)) {
            AccountBalance balance = getOrCreateBalance(currentUser.getId(), "ENTERPRISE");
            BigDecimal currentBalance = balance.getBalance();
            if (currentBalance == null) {
                currentBalance = BigDecimal.ZERO;
            }
            if (currentBalance.compareTo(depositAmount) < 0) {
                throw new CustomException(ResultCodeEnum.PARAM_ERROR, "账户余额不足，无法使用余额支付");
            }
            // 从账户余额扣除
            balance.setBalance(currentBalance.subtract(depositAmount));
            accountBalanceMapper.updateById(balance);
        }

        // 创建支付记录
        PaymentRecord paymentRecord = new PaymentRecord();
        paymentRecord.setUserId(currentUser.getId());
        paymentRecord.setUserType("ENTERPRISE");
        paymentRecord.setProjectId(project.getId());
        paymentRecord.setPaymentType("PUBLISH_DEPOSIT");
        paymentRecord.setAmount(depositAmount);
        paymentRecord.setStatus("SUCCESS"); // 支付成功
        paymentRecord.setPaymentMethod("BALANCE".equals(paymentMethod) ? "BALANCE" : "MOCK");
        paymentRecord.setTransactionId("BALANCE".equals(paymentMethod) ? 
            "BALANCE_" + UUID.randomUUID().toString().replace("-", "") : 
            "MOCK_" + UUID.randomUUID().toString().replace("-", ""));
        paymentRecord.setRemark("BALANCE".equals(paymentMethod) ? "项目发布保证金（账户余额支付）" : "项目发布保证金（模拟支付）");
        paymentRecord.setCreatedAt(LocalDateTime.now());
        paymentRecord.setUpdatedAt(LocalDateTime.now());
        paymentRecordMapper.insert(paymentRecord);

        // 创建项目支付记录
        ProjectPayment projectPayment = new ProjectPayment();
        projectPayment.setProjectId(project.getId());
        projectPayment.setEnterpriseId(project.getEnterpriseId());
        projectPayment.setDepositAmount(depositAmount);
        projectPayment.setFullAmount(BigDecimal.ZERO); // 发布时还未确定最终报价，设为0
        projectPayment.setPaidAmount(depositAmount);
        projectPayment.setFreelancerDeposit(BigDecimal.ZERO); // 发布时还未有接单者，设为0
        projectPayment.setStatus("DEPOSIT_PAID");
        projectPayment.setCreatedAt(LocalDateTime.now());
        projectPayment.setUpdatedAt(LocalDateTime.now());
        projectPaymentMapper.insert(projectPayment);

        // 更新项目已支付金额
        project.setPaidAmount(depositAmount);
        projectMapper.updateById(project);

        return paymentRecord;
    }

    /**
     * 模拟支付（确认合作补款）
     */
    @Transactional
    public PaymentRecord payConfirmPayment(Integer projectId, BigDecimal quotePrice) {
        Account currentUser = TokenUtils.getCurrentUser();
        if (currentUser == null) {
            throw new CustomException(ResultCodeEnum.NO_AUTH_ERROR);
        }

        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR, "项目不存在");
        }

        // 获取或创建项目支付记录
        ProjectPayment projectPayment = projectPaymentMapper.selectByProjectId(projectId);
        if (projectPayment == null) {
            // 如果项目支付记录不存在，自动创建
            projectPayment = new ProjectPayment();
            projectPayment.setProjectId(projectId);
            projectPayment.setEnterpriseId(project.getEnterpriseId());
            // 使用项目已支付金额作为初始保证金
            BigDecimal initialDeposit = project.getPaidAmount();
            if (initialDeposit == null) {
                initialDeposit = BigDecimal.ZERO;
            }
            projectPayment.setDepositAmount(initialDeposit);
            projectPayment.setPaidAmount(initialDeposit);
            projectPayment.setFullAmount(BigDecimal.ZERO);
            projectPayment.setFreelancerDeposit(BigDecimal.ZERO);
            projectPayment.setStatus("DEPOSIT_PAID");
            projectPayment.setCreatedAt(LocalDateTime.now());
            projectPayment.setUpdatedAt(LocalDateTime.now());
            projectPaymentMapper.insert(projectPayment);
        }

        // 计算需要补足的金额：报价金额 - 已支付金额
        BigDecimal paidAmount = projectPayment.getPaidAmount();
        if (paidAmount == null) {
            paidAmount = BigDecimal.ZERO;
        }
        BigDecimal needPay = quotePrice.subtract(paidAmount);
        if (needPay.compareTo(BigDecimal.ZERO) < 0) {
            needPay = BigDecimal.ZERO; // 如果已支付金额大于报价，不需要再支付
        }

        // 创建支付记录（模拟支付，直接成功）
        PaymentRecord paymentRecord = new PaymentRecord();
        paymentRecord.setUserId(currentUser.getId());
        paymentRecord.setUserType("ENTERPRISE");
        paymentRecord.setProjectId(projectId);
        paymentRecord.setPaymentType("CONFIRM_PAYMENT");
        paymentRecord.setAmount(needPay);
        paymentRecord.setStatus("SUCCESS");
        paymentRecord.setPaymentMethod("MOCK");
        paymentRecord.setTransactionId("MOCK_" + UUID.randomUUID().toString().replace("-", ""));
        paymentRecord.setRemark("确认合作补款（模拟支付）");
        paymentRecord.setCreatedAt(LocalDateTime.now());
        paymentRecord.setUpdatedAt(LocalDateTime.now());
        paymentRecordMapper.insert(paymentRecord);

        // 更新项目支付记录
        projectPayment.setFullAmount(quotePrice);
        projectPayment.setPaidAmount(quotePrice);
        projectPayment.setStatus("FULL_PAID");
        projectPayment.setUpdatedAt(LocalDateTime.now());
        projectPaymentMapper.updateById(projectPayment);

        // 更新项目已支付金额
        project.setPaidAmount(quotePrice);
        projectMapper.updateById(project);

        return paymentRecord;
    }

    /**
     * 支付（接单保证金）
     * @param projectId 项目ID
     * @param submissionId 稿件ID
     * @param paymentMethod 支付方式：BALANCE（账户余额）或 ONLINE（在线支付）
     */
    @Transactional
    public PaymentRecord payAcceptDeposit(Integer projectId, Integer submissionId, String paymentMethod) {
        Account currentUser = TokenUtils.getCurrentUser();
        if (currentUser == null) {
            throw new CustomException(ResultCodeEnum.NO_AUTH_ERROR);
        }

        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR, "项目不存在");
        }

        Submission submission = submissionMapper.selectById(submissionId);
        if (submission == null) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR, "稿件不存在");
        }

        // 计算保证金：报价的5%
        BigDecimal quotePrice = submission.getQuotePrice();
        if (quotePrice == null) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR, "请先设置报价");
        }
        BigDecimal depositAmount = quotePrice.multiply(new BigDecimal("0.05")).setScale(2, RoundingMode.HALF_UP);

        // 获取或创建账户余额
        Freelancer freelancer = freelancerMapper.selectByUserId(currentUser.getId());
        if (freelancer == null) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR, "自由职业者信息不存在");
        }
        
        // 如果选择账户余额支付，检查余额并扣除
        if ("BALANCE".equals(paymentMethod)) {
            AccountBalance balance = getOrCreateBalance(currentUser.getId(), "FREELANCER");
            BigDecimal currentBalance = balance.getBalance();
            if (currentBalance == null) {
                currentBalance = BigDecimal.ZERO;
            }
            BigDecimal currentFrozenBalance = balance.getFrozenBalance();
            if (currentFrozenBalance == null) {
                currentFrozenBalance = BigDecimal.ZERO;
            }
            if (currentBalance.compareTo(depositAmount) < 0) {
                throw new CustomException(ResultCodeEnum.PARAM_ERROR, "账户余额不足，无法使用余额支付");
            }
            // 扣除余额
            balance.setBalance(currentBalance.subtract(depositAmount));
            balance.setFrozenBalance(currentFrozenBalance.add(depositAmount));
            balance.setUpdatedAt(LocalDateTime.now());
            accountBalanceMapper.updateById(balance);
        }

        // 创建支付记录
        PaymentRecord paymentRecord = new PaymentRecord();
        paymentRecord.setUserId(currentUser.getId());
        paymentRecord.setUserType("FREELANCER");
        paymentRecord.setProjectId(projectId);
        paymentRecord.setSubmissionId(submissionId);
        paymentRecord.setPaymentType("ACCEPT_DEPOSIT");
        paymentRecord.setAmount(depositAmount);
        paymentRecord.setStatus("SUCCESS");
        paymentRecord.setPaymentMethod("BALANCE".equals(paymentMethod) ? "BALANCE" : "MOCK");
        paymentRecord.setTransactionId("BALANCE".equals(paymentMethod) ? 
            "BALANCE_" + UUID.randomUUID().toString().replace("-", "") : 
            "MOCK_" + UUID.randomUUID().toString().replace("-", ""));
        paymentRecord.setRemark("BALANCE".equals(paymentMethod) ? "接单保证金（账户余额支付）" : "接单保证金（模拟支付）");
        paymentRecord.setCreatedAt(LocalDateTime.now());
        paymentRecord.setUpdatedAt(LocalDateTime.now());
        paymentRecordMapper.insert(paymentRecord);

        // 更新项目支付记录
        ProjectPayment projectPayment = projectPaymentMapper.selectByProjectId(projectId);
        if (projectPayment != null) {
            projectPayment.setFreelancerId(freelancer.getId());
            projectPayment.setFreelancerDeposit(depositAmount);
            projectPayment.setUpdatedAt(LocalDateTime.now());
            projectPaymentMapper.updateById(projectPayment);
        }

        // 更新稿件状态
        submission.setDepositPaid(true);
        submissionMapper.updateById(submission);

        return paymentRecord;
    }

    /**
     * 项目完成，付款给接单者
     */
    @Transactional
    public void payCompletionPayment(Integer projectId) {
        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR, "项目不存在");
        }

        ProjectPayment projectPayment = projectPaymentMapper.selectByProjectId(projectId);
        if (projectPayment == null || projectPayment.getFreelancerId() == null) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR, "项目支付记录不存在或未确定接单者");
        }

        // 获取接单者信息
        Freelancer freelancer = freelancerMapper.selectById(projectPayment.getFreelancerId());
        if (freelancer == null) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR, "接单者信息不存在");
        }

        // 计算应付款：项目全额 - 接单者保证金（保证金退回）
        BigDecimal paymentAmount = projectPayment.getFullAmount().subtract(projectPayment.getFreelancerDeposit());

        // 增加接单者余额
        AccountBalance balance = getOrCreateBalance(freelancer.getUserId(), "FREELANCER");
        BigDecimal currentBalance = balance.getBalance();
        if (currentBalance == null) {
            currentBalance = BigDecimal.ZERO;
        }
        BigDecimal currentFrozenBalance = balance.getFrozenBalance();
        if (currentFrozenBalance == null) {
            currentFrozenBalance = BigDecimal.ZERO;
        }
        BigDecimal freelancerDeposit = projectPayment.getFreelancerDeposit();
        if (freelancerDeposit == null) {
            freelancerDeposit = BigDecimal.ZERO;
        }
        
        // 解冻保证金：从冻结余额中减去保证金
        balance.setFrozenBalance(currentFrozenBalance.subtract(freelancerDeposit));
        // 将保证金退回至余额：余额 = 当前余额 + 项目付款 + 退回的保证金
        balance.setBalance(currentBalance.add(paymentAmount).add(freelancerDeposit));
        balance.setUpdatedAt(LocalDateTime.now());
        accountBalanceMapper.updateById(balance);

        // 创建支付记录（项目完成付款）
        PaymentRecord paymentRecord = new PaymentRecord();
        paymentRecord.setUserId(freelancer.getUserId());
        paymentRecord.setUserType("FREELANCER");
        paymentRecord.setProjectId(projectId);
        paymentRecord.setPaymentType("COMPLETION_PAYMENT");
        paymentRecord.setAmount(paymentAmount);
        paymentRecord.setStatus("SUCCESS");
        paymentRecord.setPaymentMethod("MOCK");
        paymentRecord.setTransactionId("MOCK_" + UUID.randomUUID().toString().replace("-", ""));
        paymentRecord.setRemark("项目完成付款（项目全额 - 保证金）");
        paymentRecord.setCreatedAt(LocalDateTime.now());
        paymentRecord.setUpdatedAt(LocalDateTime.now());
        paymentRecordMapper.insert(paymentRecord);
        
        // 如果有保证金，创建保证金退回记录
        if (freelancerDeposit.compareTo(BigDecimal.ZERO) > 0) {
            PaymentRecord depositRefundRecord = new PaymentRecord();
            depositRefundRecord.setUserId(freelancer.getUserId());
            depositRefundRecord.setUserType("FREELANCER");
            depositRefundRecord.setProjectId(projectId);
            depositRefundRecord.setPaymentType("DEPOSIT_REFUND");
            depositRefundRecord.setAmount(freelancerDeposit);
            depositRefundRecord.setStatus("SUCCESS");
            depositRefundRecord.setPaymentMethod("MOCK");
            depositRefundRecord.setTransactionId("MOCK_" + UUID.randomUUID().toString().replace("-", ""));
            depositRefundRecord.setRemark("验收通过，保证金解冻并退回至余额");
            depositRefundRecord.setCreatedAt(LocalDateTime.now());
            depositRefundRecord.setUpdatedAt(LocalDateTime.now());
            paymentRecordMapper.insert(depositRefundRecord);
        }

        // 更新项目支付记录
        projectPayment.setStatus("COMPLETED");
        projectPayment.setUpdatedAt(LocalDateTime.now());
        projectPaymentMapper.updateById(projectPayment);
    }

    /**
     * 项目未完成，退款给企业
     */
    @Transactional
    public void refundToEnterprise(Integer projectId) {
        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR, "项目不存在");
        }

        ProjectPayment projectPayment = projectPaymentMapper.selectByProjectId(projectId);
        if (projectPayment == null) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR, "项目支付记录不存在");
        }

        // 获取企业信息
        Enterprise enterprise = enterpriseMapper.selectById(projectPayment.getEnterpriseId());
        if (enterprise == null) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR, "企业信息不存在");
        }

        // 计算退款金额：已支付金额
        BigDecimal refundAmount = projectPayment.getPaidAmount();

        // 增加企业余额
        AccountBalance balance = getOrCreateBalance(enterprise.getEmployId(), "ENTERPRISE");
        BigDecimal currentBalance = balance.getBalance();
        if (currentBalance == null) {
            currentBalance = BigDecimal.ZERO;
        }
        balance.setBalance(currentBalance.add(refundAmount));
        balance.setUpdatedAt(LocalDateTime.now());
        accountBalanceMapper.updateById(balance);

        // 如果有接单者保证金，也需要退回
        if (projectPayment.getFreelancerDeposit() != null && projectPayment.getFreelancerDeposit().compareTo(BigDecimal.ZERO) > 0) {
            Freelancer freelancer = freelancerMapper.selectById(projectPayment.getFreelancerId());
            if (freelancer != null) {
                AccountBalance freelancerBalance = getOrCreateBalance(freelancer.getUserId(), "FREELANCER");
                BigDecimal freelancerFrozenBalance = freelancerBalance.getFrozenBalance();
                if (freelancerFrozenBalance == null) {
                    freelancerFrozenBalance = BigDecimal.ZERO;
                }
                BigDecimal freelancerDepositForFrozen = projectPayment.getFreelancerDeposit();
                if (freelancerDepositForFrozen == null) {
                    freelancerDepositForFrozen = BigDecimal.ZERO;
                }
                freelancerBalance.setFrozenBalance(freelancerFrozenBalance.subtract(freelancerDepositForFrozen));
                BigDecimal freelancerCurrentBalance = freelancerBalance.getBalance();
                if (freelancerCurrentBalance == null) {
                    freelancerCurrentBalance = BigDecimal.ZERO;
                }
                BigDecimal freelancerDeposit = projectPayment.getFreelancerDeposit();
                if (freelancerDeposit == null) {
                    freelancerDeposit = BigDecimal.ZERO;
                }
                freelancerBalance.setBalance(freelancerCurrentBalance.add(freelancerDeposit));
                freelancerBalance.setUpdatedAt(LocalDateTime.now());
                accountBalanceMapper.updateById(freelancerBalance);
            }
        }

        // 创建退款记录
        PaymentRecord refundRecord = new PaymentRecord();
        refundRecord.setUserId(enterprise.getEmployId());
        refundRecord.setUserType("ENTERPRISE");
        refundRecord.setProjectId(projectId);
        refundRecord.setPaymentType("REFUND");
        refundRecord.setAmount(refundAmount);
        refundRecord.setStatus("SUCCESS");
        refundRecord.setPaymentMethod("MOCK");
        refundRecord.setTransactionId("MOCK_" + UUID.randomUUID().toString().replace("-", ""));
        refundRecord.setRemark("项目未完成退款");
        refundRecord.setCreatedAt(LocalDateTime.now());
        refundRecord.setUpdatedAt(LocalDateTime.now());
        paymentRecordMapper.insert(refundRecord);

        // 更新项目支付记录
        projectPayment.setStatus("REFUNDED");
        projectPayment.setUpdatedAt(LocalDateTime.now());
        projectPaymentMapper.updateById(projectPayment);
    }

    /**
     * 获取账户余额
     */
    public AccountBalance getBalance() {
        Account currentUser = TokenUtils.getCurrentUser();
        if (currentUser == null) {
            throw new CustomException(ResultCodeEnum.NO_AUTH_ERROR);
        }

        String userType = "FREELANCER";
        if ("EMPLOY".equals(currentUser.getRole())) {
            userType = "ENTERPRISE";
        }

        return getOrCreateBalance(currentUser.getId(), userType);
    }

    /**
     * 提现
     */
    @Transactional
    public WithdrawalRecord withdraw(BigDecimal amount, String bankAccount, String bankName) {
        Account currentUser = TokenUtils.getCurrentUser();
        if (currentUser == null) {
            throw new CustomException(ResultCodeEnum.NO_AUTH_ERROR);
        }

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR, "提现金额必须大于0");
        }

        String userType = "FREELANCER";
        if ("EMPLOY".equals(currentUser.getRole())) {
            userType = "ENTERPRISE";
        }

        // 获取账户余额
        AccountBalance balance = getOrCreateBalance(currentUser.getId(), userType);
        if (balance.getBalance().compareTo(amount) < 0) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR, "账户余额不足");
        }

        // 计算手续费（3%）
        BigDecimal fee = amount.multiply(new BigDecimal("0.03")).setScale(2, RoundingMode.HALF_UP);
        BigDecimal actualAmount = amount.subtract(fee);

        // 扣除余额
        balance.setBalance(balance.getBalance().subtract(amount));
        balance.setUpdatedAt(LocalDateTime.now());
        accountBalanceMapper.updateById(balance);

        // 创建提现记录
        WithdrawalRecord withdrawalRecord = new WithdrawalRecord();
        withdrawalRecord.setUserId(currentUser.getId());
        withdrawalRecord.setUserType(userType);
        withdrawalRecord.setAmount(amount);
        withdrawalRecord.setFee(fee);
        withdrawalRecord.setActualAmount(actualAmount);
        withdrawalRecord.setStatus("SUCCESS"); // 模拟提现，直接成功
        withdrawalRecord.setBankAccount(bankAccount);
        withdrawalRecord.setBankName(bankName);
        withdrawalRecord.setRemark("提现");
        withdrawalRecord.setCreatedAt(LocalDateTime.now());
        withdrawalRecord.setUpdatedAt(LocalDateTime.now());
        withdrawalRecordMapper.insert(withdrawalRecord);

        return withdrawalRecord;
    }
}

