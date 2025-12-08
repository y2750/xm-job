package com.example.controller;

import com.example.common.Result;
import com.example.entity.AccountBalance;
import com.example.entity.PaymentRecord;
import com.example.entity.WithdrawalRecord;
import com.example.service.PaymentService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

/**
 * 支付相关接口
 */
@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    @Resource
    private PaymentService paymentService;

    /**
     * 获取账户余额
     */
    @GetMapping("/balance")
    public Result getBalance() {
        AccountBalance balance = paymentService.getBalance();
        return Result.success(balance);
    }

    /**
     * 确认合作补款
     */
    @PostMapping("/confirm/{projectId}")
    public Result confirmPayment(@PathVariable Integer projectId, @RequestParam BigDecimal quotePrice) {
        PaymentRecord paymentRecord = paymentService.payConfirmPayment(projectId, quotePrice);
        return Result.success(paymentRecord);
    }

    /**
     * 接单保证金支付
     */
    @PostMapping("/accept-deposit/{projectId}/{submissionId}")
    public Result payAcceptDeposit(@PathVariable Integer projectId, @PathVariable Integer submissionId,
            @RequestParam(required = false, defaultValue = "ONLINE") String paymentMethod) {
        PaymentRecord paymentRecord = paymentService.payAcceptDeposit(projectId, submissionId, paymentMethod);
        return Result.success(paymentRecord);
    }

    /**
     * 提现
     */
    @PostMapping("/withdraw")
    public Result withdraw(@RequestParam BigDecimal amount,
            @RequestParam String bankAccount,
            @RequestParam String bankName) {
        WithdrawalRecord withdrawalRecord = paymentService.withdraw(amount, bankAccount, bankName);
        return Result.success(withdrawalRecord);
    }
}
