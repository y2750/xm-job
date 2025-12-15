package com.example.service;

import com.example.common.enums.ResultCodeEnum;
import com.example.entity.Account;
import com.example.entity.Freelancer;
import com.example.entity.Project;
import com.example.entity.ProjectOrder;
import com.example.entity.Submission;
import com.example.entity.SubmissionAttachment;
import com.example.exception.CustomException;
import com.example.mapper.EnterpriseMapper;
import com.example.mapper.FreelancerMapper;
import com.example.mapper.ProjectMapper;
import com.example.mapper.ProjectOrderMapper;
import com.example.mapper.SubmissionAttachmentMapper;
import com.example.mapper.SubmissionMapper;
import com.example.utils.TokenUtils;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 稿件业务层
 */
@Service
public class SubmissionService {

    @Resource
    private SubmissionMapper submissionMapper;
    @Resource
    private ProjectMapper projectMapper;
    @Resource
    private FreelancerMapper freelancerMapper;
    @Resource
    private SubmissionAttachmentMapper attachmentMapper;
    @Resource
    private EnterpriseMapper enterpriseMapper;
    @Resource
    private ProjectOrderMapper orderMapper;
    @Resource
    private PaymentService paymentService;
    @Resource
    private com.example.service.MessageService messageService;
    @Resource
    private NotificationService notificationService;

    /**
     * 提交稿件
     */
    public void add(Submission submission) {
        Account currentUser = TokenUtils.getCurrentUser();
        if (currentUser == null) {
            throw new CustomException(ResultCodeEnum.NO_AUTH_ERROR);
        }
        
        // 获取自由职业者信息
        Freelancer freelancer = freelancerMapper.selectByUserId(currentUser.getId());
        if (freelancer == null) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR, "请先完善自由职业者信息");
        }
        
        // 检查自由职业者是否已认证
        if (freelancer.getVerified() == null || !freelancer.getVerified()) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR, "自由职业者未认证，无法提交稿件，请先完善个人信息并等待审核");
        }
        
        // 检查项目是否存在且可投稿
        Project project = projectMapper.selectById(submission.getProjectId());
        if (project == null) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR, "项目不存在");
        }
        // 如果项目已确认合作，不允许提交稿件
        if ("CONFIRMED".equals(project.getStatus())) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR, "项目已确定合作，不再接受新的稿件");
        }
        if (!"PUBLISHED".equals(project.getStatus())) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR, "项目已截止");
        }
        
        // 检查是否已接单（必须先接单才能提交稿件）
        ProjectOrder order = orderMapper.selectByProjectAndFreelancer(submission.getProjectId(), freelancer.getId());
        if (order == null || !"ACCEPTED".equals(order.getStatus())) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR, "请先接单此项目");
        }
        
        // 检查是否已提交过稿件（只检查有效状态的稿件：SUBMITTED、INTERESTED、CONFIRMED）
        // 如果稿件被拒绝（REJECTED）或已删除，允许重新提交
        Submission exist = new Submission();
        exist.setProjectId(submission.getProjectId());
        exist.setFreelancerId(freelancer.getId());
        List<Submission> existList = submissionMapper.selectAll(exist);
        // 过滤出有效状态的稿件（排除已拒绝和已删除的）
        boolean hasValidSubmission = existList.stream()
                .anyMatch(s -> "SUBMITTED".equals(s.getStatus()) 
                        || "INTERESTED".equals(s.getStatus()) 
                        || "CONFIRMED".equals(s.getStatus()));
        if (hasValidSubmission) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR, "您已提交过稿件");
        }
        
        submission.setFreelancerId(freelancer.getId());
        submission.setStatus("SUBMITTED");
        submission.setCreatedAt(LocalDateTime.now());
        submissionMapper.insert(submission);
        // insert 后会自动设置 ID（因为配置了 useGeneratedKeys="true" keyProperty="id"）
        
        // 发送通知给企业：收到新的稿件
        com.example.entity.Enterprise enterprise = enterpriseMapper.selectById(project.getEnterpriseId());
        if (enterprise != null) {
            String title = "收到新稿件";
            String content = String.format("您的项目《%s》收到了新的稿件，请及时查看。", project.getTitle());
            notificationService.sendIndividualNotification("SUBMISSION", enterprise.getEmployId(), "ENTERPRISE",
                    title, content, project.getId(), submission.getId(), null);
        }
    }

    /**
     * 更新稿件（只能修改SUBMITTED状态的稿件，INTERESTED状态只能修改报价）
     */
    public void updateById(Submission submission) {
        Submission dbSubmission = submissionMapper.selectById(submission.getId());
        if (dbSubmission == null) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR, "稿件不存在");
        }
        
        Account currentUser = TokenUtils.getCurrentUser();
        Freelancer freelancer = freelancerMapper.selectByUserId(currentUser.getId());
        if (freelancer == null || !freelancer.getId().equals(dbSubmission.getFreelancerId())) {
            throw new CustomException(ResultCodeEnum.NO_AUTH_ERROR);
        }
        
        // 检查状态：SUBMITTED状态可以修改所有内容，INTERESTED状态只能修改报价
        if ("INTERESTED".equals(dbSubmission.getStatus())) {
            // 只能修改报价，记录报价历史
            if (submission.getQuotePrice() != null && !submission.getQuotePrice().equals(dbSubmission.getQuotePrice())) {
                // 更新报价历史（JSON格式）
                String history = dbSubmission.getQuoteHistory();
                if (history == null || history.isEmpty()) {
                    history = "[]";
                }
                // 这里简化处理，实际应该解析JSON并添加新记录
                // 暂时只更新报价，历史记录可以后续完善
                submission.setTitle(dbSubmission.getTitle());
                submission.setDescription(dbSubmission.getDescription());
            } else {
                throw new CustomException(ResultCodeEnum.PARAM_ERROR, "有意向状态的稿件只能修改报价");
            }
        } else if (!"SUBMITTED".equals(dbSubmission.getStatus())) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR, "只能修改已提交状态的稿件");
        }
        
        submissionMapper.updateById(submission);
    }

    /**
     * 删除稿件（撤回）
     */
    public void deleteById(Integer id) {
        Submission submission = submissionMapper.selectById(id);
        if (submission == null) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR, "稿件不存在");
        }
        
        Account currentUser = TokenUtils.getCurrentUser();
        Freelancer freelancer = freelancerMapper.selectByUserId(currentUser.getId());
        if (freelancer == null || !freelancer.getId().equals(submission.getFreelancerId())) {
            throw new CustomException(ResultCodeEnum.NO_AUTH_ERROR);
        }
        
        // 只有已提交状态的稿件可以撤回（有意向后不可撤回）
        if (!"SUBMITTED".equals(submission.getStatus())) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR, "只能撤回未审核的稿件，有意向状态的稿件不可撤回");
        }
        
        // 先删除相关附件
        List<SubmissionAttachment> attachments = attachmentMapper.selectBySubmissionId(id);
        for (SubmissionAttachment attachment : attachments) {
            attachmentMapper.deleteById(attachment.getId());
        }
        
        // 删除稿件
        submissionMapper.deleteById(id);
    }

    /**
     * 根据ID查询
     */
    public Submission selectById(Integer id) {
        Submission submission = submissionMapper.selectById(id);
        if (submission != null) {
            // 加载附件
            List<SubmissionAttachment> attachments = attachmentMapper.selectBySubmissionId(id);
            submission.setAttachments(attachments);
        }
        return submission;
    }

    /**
     * 查询所有
     */
    public List<Submission> selectAll(Submission submission) {
        return submissionMapper.selectAll(submission);
    }

    /**
     * 分页查询
     */
    public PageInfo<Submission> selectPage(Submission submission, Integer pageNum, Integer pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<Submission> list = submissionMapper.selectAll(submission);
        return PageInfo.of(list);
    }

    /**
     * 更新稿件状态
     */
    public void updateStatus(Integer id, String status) {
        Submission submission = submissionMapper.selectById(id);
        if (submission == null) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR, "稿件不存在");
        }
        
        // 权限校验：只有项目发布者可以修改状态
        Project project = projectMapper.selectById(submission.getProjectId());
        if (project == null) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR, "项目不存在");
        }
        Account currentUser = TokenUtils.getCurrentUser();
        if (currentUser == null) {
            throw new CustomException(ResultCodeEnum.NO_AUTH_ERROR);
        }
        // 验证是否为项目发布者
        com.example.entity.Enterprise enterprise = enterpriseMapper.selectByEmployId(currentUser.getId());
        if (enterprise == null || !enterprise.getId().equals(project.getEnterpriseId())) {
            throw new CustomException(ResultCodeEnum.NO_AUTH_ERROR, "只有项目发布者可以修改稿件状态");
        }
        
        submission.setStatus(status);
        submissionMapper.updateById(submission);
        
        // 如果确定合作，将其他稿件设为已拒绝，并且不能再设置其他稿件的状态
        if ("CONFIRMED".equals(status)) {
            // 检查项目是否已经确定合作
            if ("CONFIRMED".equals(project.getStatus()) && project.getConfirmedFreelancerId() != null) {
                throw new CustomException(ResultCodeEnum.PARAM_ERROR, "项目已确定合作，不能再设置其他稿件的状态");
            }
            
            List<Submission> allSubmissions = submissionMapper.selectByProjectId(submission.getProjectId());
            for (Submission s : allSubmissions) {
                if (!s.getId().equals(id) && !"REJECTED".equals(s.getStatus())) {
                    s.setStatus("REJECTED");
                    submissionMapper.updateById(s);
                }
            }
            // 检查是否双方都已确认
            if (submission.getEnterpriseConfirmed() != null && submission.getEnterpriseConfirmed() 
                && submission.getFreelancerConfirmed() != null && submission.getFreelancerConfirmed()) {
                // 双方都已确认，更新项目状态为已合作
                project.setStatus("CONFIRMED");
                project.setConfirmedFreelancerId(submission.getFreelancerId());
                projectMapper.updateById(project);
            }
        } else {
            // 如果项目已确定合作，不能再修改其他稿件的状态
            if ("CONFIRMED".equals(project.getStatus()) && project.getConfirmedFreelancerId() != null) {
                throw new CustomException(ResultCodeEnum.PARAM_ERROR, "项目已确定合作，不能再修改稿件状态");
            }
        }
    }

    /**
     * 企业确认合作（支付剩余尾款）
     */
    @org.springframework.transaction.annotation.Transactional
    public void enterpriseConfirm(Integer submissionId) {
        Account currentUser = TokenUtils.getCurrentUser();
        if (currentUser == null) {
            throw new CustomException(ResultCodeEnum.NO_AUTH_ERROR);
        }
        
        Submission submission = submissionMapper.selectById(submissionId);
        if (submission == null) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR, "稿件不存在");
        }
        
        if (!"INTERESTED".equals(submission.getStatus())) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR, "只能确认有意向状态的稿件");
        }
        
        Project project = projectMapper.selectById(submission.getProjectId());
        if (project == null) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR, "项目不存在");
        }
        
        // 检查权限：只有项目发布者可以确认
        com.example.entity.Enterprise enterprise = enterpriseMapper.selectByEmployId(currentUser.getId());
        if (enterprise == null || !enterprise.getId().equals(project.getEnterpriseId())) {
            throw new CustomException(ResultCodeEnum.NO_AUTH_ERROR, "只有项目发布者可以确认合作");
        }
        
        // 检查是否已确认
        if (submission.getEnterpriseConfirmed() != null && submission.getEnterpriseConfirmed()) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR, "企业已确认合作");
        }
        
        // 支付剩余尾款
        if (submission.getQuotePrice() != null) {
            paymentService.payConfirmPayment(project.getId(), submission.getQuotePrice());
        }
        
        // 标记企业已确认
        submission.setEnterpriseConfirmed(true);
        submissionMapper.updateById(submission);
        
        // 注意：不在这里更新项目状态，只有接单者确认后才更新项目状态
    }
    
    /**
     * 接单者确认合作（支付5%保证金）
     */
    @org.springframework.transaction.annotation.Transactional
    public void freelancerConfirm(Integer submissionId, Submission submissionParam) {
        Account currentUser = TokenUtils.getCurrentUser();
        if (currentUser == null) {
            throw new CustomException(ResultCodeEnum.NO_AUTH_ERROR);
        }
        
        Submission submission = submissionMapper.selectById(submissionId);
        if (submission == null) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR, "稿件不存在");
        }
        
        if (!"INTERESTED".equals(submission.getStatus())) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR, "只能确认有意向状态的稿件");
        }
        
        Project project = projectMapper.selectById(submission.getProjectId());
        if (project == null) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR, "项目不存在");
        }
        
        // 检查权限：只有稿件提交者可以确认
        Freelancer freelancer = freelancerMapper.selectByUserId(currentUser.getId());
        if (freelancer == null || !freelancer.getId().equals(submission.getFreelancerId())) {
            throw new CustomException(ResultCodeEnum.NO_AUTH_ERROR, "只能确认自己的稿件");
        }
        
        // 检查企业是否已确认
        if (submission.getEnterpriseConfirmed() == null || !submission.getEnterpriseConfirmed()) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR, "请等待企业先确认合作");
        }
        
        // 检查是否已确认
        if (submission.getFreelancerConfirmed() != null && submission.getFreelancerConfirmed()) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR, "您已确认合作");
        }
        
        // 支付5%保证金（从请求参数中获取支付方式，默认为在线支付）
        String paymentMethod = submissionParam != null ? submissionParam.getPaymentMethod() : null;
        if (paymentMethod == null || paymentMethod.isEmpty()) {
            paymentMethod = "ONLINE"; // 默认在线支付
        }
        if (submission.getQuotePrice() != null) {
            paymentService.payAcceptDeposit(project.getId(), submissionId, paymentMethod);
        }
        
        // 标记接单者已确认
        submission.setFreelancerConfirmed(true);
        submission.setDepositPaid(true);
        submissionMapper.updateById(submission);
        
        // 检查是否双方都已确认
        if (submission.getEnterpriseConfirmed() != null && submission.getEnterpriseConfirmed()
            && submission.getFreelancerConfirmed() != null && submission.getFreelancerConfirmed()) {
            
            // 双方都已确认，执行以下操作：
            // 1. 将其他稿件设为已拒绝
            List<Submission> allSubmissions = submissionMapper.selectByProjectId(submission.getProjectId());
            for (Submission s : allSubmissions) {
                if (!s.getId().equals(submissionId) && !"REJECTED".equals(s.getStatus())) {
                    s.setStatus("REJECTED");
                    submissionMapper.updateById(s);
                }
            }
            
            // 2. 取消其他接单者的接单（除了已确认合作的接单者）
            List<ProjectOrder> allOrders = orderMapper.selectByProjectId(submission.getProjectId());
            for (ProjectOrder order : allOrders) {
                if (!order.getFreelancerId().equals(submission.getFreelancerId()) 
                    && "ACCEPTED".equals(order.getStatus())) {
                    order.setStatus("CANCELLED");
                    orderMapper.updateById(order);
                }
            }
            
            // 3. 更新项目状态为已合作
            project.setStatus("CONFIRMED");
            project.setConfirmedFreelancerId(submission.getFreelancerId());
            projectMapper.updateById(project);
            
            // 4. 更新稿件状态为已确定合作
            submission.setStatus("CONFIRMED");
            submissionMapper.updateById(submission);
        }
    }
    
    /**
     * 查询当前用户的稿件
     */
    public List<Submission> selectMySubmissions() {
        Account currentUser = TokenUtils.getCurrentUser();
        if (currentUser == null) {
            throw new CustomException(ResultCodeEnum.NO_AUTH_ERROR);
        }
        
        Freelancer freelancer = freelancerMapper.selectByUserId(currentUser.getId());
        if (freelancer == null) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR, "请先完善自由职业者信息");
        }
        
        return submissionMapper.selectByFreelancerId(freelancer.getId());
    }

    /**
     * 根据项目ID查询当前用户的稿件
     */
    public Submission selectMySubmissionByProjectId(Integer projectId) {
        Account currentUser = TokenUtils.getCurrentUser();
        if (currentUser == null) {
            throw new CustomException(ResultCodeEnum.NO_AUTH_ERROR);
        }
        
        Freelancer freelancer = freelancerMapper.selectByUserId(currentUser.getId());
        if (freelancer == null) {
            return null;
        }
        
        // 查询该自由职业者在该项目下的所有稿件
        List<Submission> submissions = submissionMapper.selectByFreelancerId(freelancer.getId());
        // 过滤出该项目的稿件，并返回状态为SUBMITTED、INTERESTED或CONFIRMED的稿件
        return submissions.stream()
            .filter(s -> s.getProjectId().equals(projectId) 
                && ("SUBMITTED".equals(s.getStatus()) 
                    || "INTERESTED".equals(s.getStatus()) 
                    || "CONFIRMED".equals(s.getStatus())))
            .findFirst()
            .orElse(null);
    }
}

