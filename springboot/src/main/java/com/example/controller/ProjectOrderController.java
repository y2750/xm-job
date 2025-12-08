package com.example.controller;

import com.example.common.Result;
import com.example.entity.ProjectOrder;
import com.example.service.ProjectOrderService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 项目接单相关接口
 */
@RestController
@RequestMapping("/api/orders")
public class ProjectOrderController {

    @Resource
    private ProjectOrderService orderService;

    /**
     * 接单
     */
    @PostMapping("/accept/{projectId}")
    public Result acceptOrder(@PathVariable Integer projectId) {
        orderService.acceptOrder(projectId);
        return Result.success();
    }

    /**
     * 取消接单
     */
    @PutMapping("/cancel/{orderId}")
    public Result cancelOrder(@PathVariable Integer orderId) {
        orderService.cancelOrder(orderId);
        return Result.success();
    }

    /**
     * 查询我的接单列表
     */
    @GetMapping("/my")
    public Result selectMyOrders() {
        List<ProjectOrder> list = orderService.selectMyOrders();
        return Result.success(list);
    }
    
    /**
     * 放弃接单（在合作过程中放弃，保证金赔付给企业）
     */
    @PutMapping("/abandon/{orderId}")
    public Result abandonOrder(@PathVariable Integer orderId) {
        orderService.abandonOrder(orderId);
        return Result.success();
    }
}

