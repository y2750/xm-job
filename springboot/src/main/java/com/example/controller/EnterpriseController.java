package com.example.controller;

import com.example.common.Result;
import com.example.entity.Enterprise;
import com.example.service.EnterpriseService;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 企业相关接口
 */
@RestController
@RequestMapping("/api/enterprises")
public class EnterpriseController {

    @Resource
    private EnterpriseService enterpriseService;

    /**
     * 新增企业信息
     */
    @PostMapping
    public Result add(@RequestBody Enterprise enterprise) {
        enterpriseService.add(enterprise);
        return Result.success();
    }

    /**
     * 更新企业信息
     */
    @PutMapping
    public Result update(@RequestBody Enterprise enterprise) {
        enterpriseService.updateById(enterprise);
        return Result.success();
    }

    /**
     * 根据ID查询
     */
    @GetMapping("/{id}")
    public Result selectById(@PathVariable Integer id) {
        Enterprise enterprise = enterpriseService.selectById(id);
        return Result.success(enterprise);
    }

    /**
     * 查询当前用户的企业信息
     */
    @GetMapping("/profile")
    public Result selectCurrent() {
        Enterprise enterprise = enterpriseService.selectCurrent();
        return Result.success(enterprise);
    }

    /**
     * 更新当前用户的企业信息
     */
    @PutMapping("/profile")
    public Result updateProfile(@RequestBody Enterprise enterprise) {
        enterpriseService.updateById(enterprise);
        return Result.success();
    }

    /**
     * 查询所有
     */
    @GetMapping
    public Result selectAll(Enterprise enterprise,
                            @RequestParam(defaultValue = "1") Integer pageNum,
                            @RequestParam(defaultValue = "10") Integer pageSize) {
        PageInfo<Enterprise> pageInfo = enterpriseService.selectPage(enterprise, pageNum, pageSize);
        return Result.success(pageInfo);
    }
}

