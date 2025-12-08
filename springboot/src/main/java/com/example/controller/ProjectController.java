package com.example.controller;

import com.example.common.Result;
import com.example.entity.Project;
import com.example.entity.Submission;
import com.example.mapper.SubmissionMapper;
import com.example.service.ProjectService;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 项目相关接口
 */
@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    @Resource
    private ProjectService projectService;
    @Resource
    private SubmissionMapper submissionMapper;

    /**
     * 发布项目
     */
    @PostMapping
    public Result add(@RequestBody Project project) {
        projectService.add(project);
        return Result.success();
    }

    /**
     * 更新项目
     */
    @PutMapping
    public Result update(@RequestBody Project project) {
        projectService.updateById(project);
        return Result.success();
    }

    /**
     * 删除项目
     */
    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Integer id) {
        projectService.deleteById(id);
        return Result.success();
    }

    /**
     * 根据ID查询
     */
    @GetMapping("/{id}")
    public Result selectById(@PathVariable Integer id) {
        Project project = projectService.selectById(id);
        return Result.success(project);
    }

    /**
     * 查询所有（支持筛选）
     */
    @GetMapping
    public Result selectAll(Project project,
                            @RequestParam(defaultValue = "1") Integer pageNum,
                            @RequestParam(defaultValue = "10") Integer pageSize) {
        PageInfo<Project> pageInfo = projectService.selectPage(project, pageNum, pageSize);
        return Result.success(pageInfo);
    }

    /**
     * 更新项目状态
     */
    @PutMapping("/{id}/status")
    public Result updateStatus(@PathVariable Integer id, @RequestParam String status) {
        projectService.updateStatus(id, status);
        return Result.success();
    }

    /**
     * 获取项目的所有稿件
     */
    @GetMapping("/{id}/submissions")
    public Result getProjectSubmissions(@PathVariable Integer id) {
        List<Submission> list = submissionMapper.selectByProjectId(id);
        return Result.success(list);
    }

    /**
     * 获取企业项目统计信息
     */
    @GetMapping("/statistics")
    public Result getStatistics(@RequestParam(required = false) Integer enterpriseId) {
        if (enterpriseId == null) {
            return Result.error("企业ID不能为空");
        }
        
        Project query = new Project();
        query.setEnterpriseId(enterpriseId);
        List<Project> allProjects = projectService.selectAll(query);
        
        System.out.println("统计接口 - 企业ID: " + enterpriseId + ", 查询到项目数量: " + allProjects.size());
        
        // 已发布项目：包括PUBLISHED、CLOSED、CONFIRMED状态（所有曾经发布过的项目）
        int totalPublishedProjects = 0;
        int publishedProjects = 0;
        int confirmedProjects = 0;
        int totalSubmissions = 0;
        
        for (Project project : allProjects) {
            System.out.println("项目ID: " + project.getId() + ", 状态: " + project.getStatus() + ", 稿件数: " + project.getSubmissionCount());
            // 已发布项目：包括所有状态（PUBLISHED、CLOSED、CONFIRMED都是已发布过的项目）
            if ("PUBLISHED".equals(project.getStatus()) || "CLOSED".equals(project.getStatus()) || "CONFIRMED".equals(project.getStatus())) {
                totalPublishedProjects++;
            }
            // 当前已发布状态（未截止、未确定合作）
            if ("PUBLISHED".equals(project.getStatus())) {
                publishedProjects++;
            }
            // 已确定合作
            if ("CONFIRMED".equals(project.getStatus())) {
                confirmedProjects++;
            }
            if (project.getSubmissionCount() != null) {
                totalSubmissions += project.getSubmissionCount();
            }
        }
        
        System.out.println("统计结果 - 已发布项目总数: " + totalPublishedProjects + ", 当前已发布: " + publishedProjects + ", 已确定合作: " + confirmedProjects + ", 总稿件数: " + totalSubmissions);
        
        java.util.Map<String, Object> statistics = new java.util.HashMap<>();
        // 已发布项目总数：包括所有曾经发布过的项目（PUBLISHED、CLOSED、CONFIRMED）
        statistics.put("publishedProjects", totalPublishedProjects);
        statistics.put("confirmedProjects", confirmedProjects);
        statistics.put("totalSubmissions", totalSubmissions);
        
        return Result.success(statistics);
    }
    
    /**
     * 重新上架项目（当确认合作的接单人放弃接单后，企业可以重新上架）
     */
    @PutMapping("/{id}/republish")
    public Result republish(@PathVariable Integer id) {
        projectService.republishProject(id);
        return Result.success();
    }
}

