package com.example.controller;

import com.example.common.Result;
import com.example.entity.FreelancerProjectHistory;
import com.example.service.FreelancerProjectHistoryService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 自由职业者项目历史战绩接口
 */
@RestController
@RequestMapping("/api/project-history")
public class FreelancerProjectHistoryController {
    
    @Resource
    private FreelancerProjectHistoryService historyService;
    
    /**
     * 查询我的历史战绩
     */
    @GetMapping("/my")
    public Result selectMyHistory() {
        List<FreelancerProjectHistory> list = historyService.selectMyHistory();
        return Result.success(list);
    }
    
    /**
     * 根据自由职业者ID查询历史战绩
     */
    @GetMapping("/freelancer/{freelancerId}")
    public Result selectByFreelancerId(@PathVariable Integer freelancerId) {
        List<FreelancerProjectHistory> list = historyService.selectByFreelancerId(freelancerId);
        return Result.success(list);
    }
}














