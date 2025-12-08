package com.example.controller;

import com.example.common.Result;
import com.example.entity.Deliverable;
import com.example.service.DeliverableService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 成品提交相关接口
 */
@RestController
@RequestMapping("/api/deliverables")
public class DeliverableController {

    @Resource
    private DeliverableService deliverableService;

    /**
     * 提交成品
     */
    @PostMapping
    public Result submitDeliverable(@RequestBody Deliverable deliverable) {
        Deliverable result = deliverableService.submitDeliverable(deliverable);
        return Result.success(result);
    }

    /**
     * 验收成品
     */
    @PutMapping("/{id}/review")
    public Result reviewDeliverable(@PathVariable Integer id, 
                                    @RequestParam String status,
                                    @RequestParam(required = false) String reviewComment) {
        deliverableService.reviewDeliverable(id, status, reviewComment);
        return Result.success();
    }

    /**
     * 查询成品列表
     */
    @GetMapping
    public Result selectAll(Deliverable deliverable) {
        List<Deliverable> list = deliverableService.selectAll(deliverable);
        return Result.success(list);
    }

    /**
     * 根据ID查询成品
     */
    @GetMapping("/{id}")
    public Result selectById(@PathVariable Integer id) {
        Deliverable deliverable = deliverableService.selectById(id);
        return Result.success(deliverable);
    }
    
    /**
     * 查询我的成品
     */
    @GetMapping("/my")
    public Result selectMy() {
        List<Deliverable> list = deliverableService.selectMyDeliverables();
        return Result.success(list);
    }
}

