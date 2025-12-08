package com.example.controller;

import com.example.common.Result;
import com.example.entity.Submission;
import com.example.service.SubmissionService;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 稿件相关接口
 */
@RestController
@RequestMapping("/api/submissions")
public class SubmissionController {

    @Resource
    private SubmissionService submissionService;

    /**
     * 提交稿件
     */
    @PostMapping
    public Result add(@RequestBody Submission submission) {
        submissionService.add(submission);
        // 返回创建的稿件对象，包含生成的ID，方便前端上传附件
        return Result.success(submission);
    }

    /**
     * 更新稿件
     */
    @PutMapping
    public Result update(@RequestBody Submission submission) {
        submissionService.updateById(submission);
        return Result.success();
    }

    /**
     * 撤回稿件
     */
    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Integer id) {
        submissionService.deleteById(id);
        return Result.success();
    }

    /**
     * 根据ID查询
     */
    @GetMapping("/{id}")
    public Result selectById(@PathVariable Integer id) {
        Submission submission = submissionService.selectById(id);
        return Result.success(submission);
    }

    /**
     * 查询所有（支持筛选）
     */
    @GetMapping
    public Result selectAll(Submission submission,
                            @RequestParam(defaultValue = "1") Integer pageNum,
                            @RequestParam(defaultValue = "10") Integer pageSize) {
        PageInfo<Submission> pageInfo = submissionService.selectPage(submission, pageNum, pageSize);
        return Result.success(pageInfo);
    }

    /**
     * 查询我的稿件
     */
    @GetMapping("/my")
    public Result selectMy() {
        List<Submission> list = submissionService.selectMySubmissions();
        return Result.success(list);
    }

    /**
     * 更新稿件状态
     */
    @PutMapping("/{id}/status")
    public Result updateStatus(@PathVariable Integer id, @RequestParam String status) {
        submissionService.updateStatus(id, status);
        return Result.success();
    }

    /**
     * 确定合作（已废弃，使用分步确认）
     */
    @PutMapping("/{id}/confirm")
    public Result confirm(@PathVariable Integer id) {
        submissionService.updateStatus(id, "CONFIRMED");
        return Result.success();
    }
    
    /**
     * 企业确认合作（支付剩余尾款）
     */
    @PutMapping("/{id}/enterprise-confirm")
    public Result enterpriseConfirm(@PathVariable Integer id) {
        submissionService.enterpriseConfirm(id);
        return Result.success();
    }
    
    /**
     * 接单者确认合作（支付5%保证金）
     */
    @PutMapping("/{id}/freelancer-confirm")
    public Result freelancerConfirm(@PathVariable Integer id, @RequestParam(required = false) String paymentMethod) {
        Submission submission = new Submission();
        submission.setId(id);
        submission.setPaymentMethod(paymentMethod);
        submissionService.freelancerConfirm(id, submission);
        return Result.success();
    }

    /**
     * 修改报价（仅INTERESTED状态可修改）
     */
    @PutMapping("/{id}/quote")
    public Result updateQuote(@PathVariable Integer id, @RequestParam java.math.BigDecimal quotePrice) {
        com.example.entity.Submission submission = new com.example.entity.Submission();
        submission.setId(id);
        submission.setQuotePrice(quotePrice);
        submissionService.updateById(submission);
        return Result.success();
    }
}

