package com.example.controller;

import com.example.common.Result;
import com.example.entity.Freelancer;
import com.example.service.FreelancerService;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

/**
 * 自由职业者相关接口
 */
@RestController
@RequestMapping("/api/freelancers")
public class FreelancerController {

    @Resource
    private FreelancerService freelancerService;

    /**
     * 新增自由职业者信息
     */
    @PostMapping
    public Result add(@RequestBody Freelancer freelancer) {
        freelancerService.add(freelancer);
        return Result.success();
    }

    /**
     * 更新自由职业者信息
     */
    @PutMapping
    public Result update(@RequestBody Freelancer freelancer) {
        freelancerService.updateById(freelancer);
        return Result.success();
    }

    /**
     * 根据ID查询
     */
    @GetMapping("/{id}")
    public Result selectById(@PathVariable Integer id) {
        Freelancer freelancer = freelancerService.selectById(id);
        return Result.success(freelancer);
    }

    /**
     * 查询当前用户的信息（包含用户基本信息）
     */
    @GetMapping("/profile")
    public Result selectCurrent() {
        Freelancer freelancer = freelancerService.selectCurrent();
        return Result.success(freelancer);
    }

    /**
     * 更新当前用户的信息（包含用户基本信息和自由职业者信息）
     */
    @PutMapping("/profile")
    public Result updateProfile(@RequestBody Freelancer freelancer) {
        freelancerService.updateProfile(freelancer);
        return Result.success();
    }

    /**
     * 查询所有
     */
    @GetMapping
    public Result selectAll(Freelancer freelancer,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        PageInfo<Freelancer> pageInfo = freelancerService.selectPage(freelancer, pageNum, pageSize);
        return Result.success(pageInfo);
    }
}
