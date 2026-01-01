package com.example.service;

import com.example.entity.FreelancerProjectHistory;
import com.example.entity.Account;
import com.example.exception.CustomException;
import com.example.common.enums.ResultCodeEnum;
import com.example.mapper.FreelancerProjectHistoryMapper;
import com.example.mapper.FreelancerMapper;
import com.example.mapper.ProjectMapper;
import com.example.entity.Project;
import com.example.utils.TokenUtils;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * 自由职业者项目历史战绩服务
 */
@Service
public class FreelancerProjectHistoryService {
    
    @Resource
    private FreelancerProjectHistoryMapper historyMapper;
    
    @Resource
    private FreelancerMapper freelancerMapper;
    
    @Resource
    private ProjectMapper projectMapper;
    
    /**
     * 根据自由职业者ID查询历史战绩
     */
    public List<FreelancerProjectHistory> selectByFreelancerId(Integer freelancerId) {
        return historyMapper.selectByFreelancerId(freelancerId);
    }
    
    /**
     * 查询我的历史战绩
     */
    public List<FreelancerProjectHistory> selectMyHistory() {
        Account currentUser = TokenUtils.getCurrentUser();
        if (currentUser == null) {
            throw new CustomException(ResultCodeEnum.NO_AUTH_ERROR);
        }
        
        com.example.entity.Freelancer freelancer = freelancerMapper.selectByUserId(currentUser.getId());
        if (freelancer == null) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR, "您还不是自由职业者");
        }
        
        return historyMapper.selectByFreelancerId(freelancer.getId());
    }
    
    /**
     * 添加历史战绩（项目完成时自动调用）
     */
    public void addHistory(Integer freelancerId, Integer projectId) {
        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            return;
        }
        
        // 检查是否已存在
        FreelancerProjectHistory existing = historyMapper.selectByProjectId(projectId);
        if (existing != null) {
            return; // 已存在，不重复添加
        }
        
        FreelancerProjectHistory history = new FreelancerProjectHistory();
        history.setFreelancerId(freelancerId);
        history.setProjectId(projectId);
        history.setProjectTitle(project.getTitle());
        history.setProjectBudget(project.getBudgetMax());
        history.setCompletionDate(LocalDate.now());
        history.setStatus("COMPLETED");
        
        historyMapper.insert(history);
    }
    
    /**
     * 更新历史战绩（添加评分和评价）
     */
    public void updateHistory(Integer projectId, java.math.BigDecimal rating, String comment) {
        FreelancerProjectHistory history = historyMapper.selectByProjectId(projectId);
        if (history != null) {
            history.setEnterpriseRating(rating);
            history.setEnterpriseComment(comment);
            historyMapper.updateById(history);
        }
    }
}














