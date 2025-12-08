package com.example.service;

import com.example.entity.Account;
import com.example.entity.Deliverable;
import com.example.entity.DeliverableAttachment;
import com.example.entity.Enterprise;
import com.example.entity.Freelancer;
import com.example.entity.Project;
import com.example.entity.Submission;
import com.example.exception.CustomException;
import com.example.common.enums.ResultCodeEnum;
import com.example.mapper.DeliverableAttachmentMapper;
import com.example.mapper.DeliverableMapper;
import com.example.mapper.EnterpriseMapper;
import com.example.mapper.FreelancerMapper;
import com.example.mapper.ProjectMapper;
import com.example.mapper.SubmissionMapper;
import com.example.utils.TokenUtils;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 成品提交业务层
 */
@Service
public class DeliverableService {

    @Resource
    private DeliverableMapper deliverableMapper;
    @Resource
    private DeliverableAttachmentMapper attachmentMapper;
    @Resource
    private SubmissionMapper submissionMapper;
    @Resource
    private ProjectMapper projectMapper;
    @Resource
    private FreelancerMapper freelancerMapper;
    @Resource
    private PaymentService paymentService;
    @Resource
    private EnterpriseMapper enterpriseMapper;
    @Resource
    private MessageService messageService;

    /**
     * 提交成品
     */
    public Deliverable submitDeliverable(Deliverable deliverable) {
        Account currentUser = TokenUtils.getCurrentUser();
        if (currentUser == null) {
            throw new CustomException(ResultCodeEnum.NO_AUTH_ERROR);
        }

        // 获取自由职业者信息
        Freelancer freelancer = freelancerMapper.selectByUserId(currentUser.getId());
        if (freelancer == null) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR, "请先完善自由职业者信息");
        }

        // 检查稿件是否存在且状态为CONFIRMED
        Submission submission = submissionMapper.selectById(deliverable.getSubmissionId());
        if (submission == null) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR, "稿件不存在");
        }
        if (!"CONFIRMED".equals(submission.getStatus())) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR, "只有已确定合作的稿件才能提交成品");
        }
        if (!freelancer.getId().equals(submission.getFreelancerId())) {
            throw new CustomException(ResultCodeEnum.NO_AUTH_ERROR, "只能提交自己稿件的成品");
        }

        // 检查项目是否存在
        Project project = projectMapper.selectById(deliverable.getProjectId());
        if (project == null) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR, "项目不存在");
        }

        // 检查是否已提交过成品
        Deliverable exist = new Deliverable();
        exist.setSubmissionId(deliverable.getSubmissionId());
        List<Deliverable> existList = deliverableMapper.selectAll(exist);
        if (existList != null && !existList.isEmpty()) {
            Deliverable existing = existList.get(0);
            if ("APPROVED".equals(existing.getStatus())) {
                throw new CustomException(ResultCodeEnum.PARAM_ERROR, "成品已通过验收，无法重新提交");
            }
            // 如果状态是 SUBMITTED，说明企业还未验收，不允许重新提交
            if ("SUBMITTED".equals(existing.getStatus())) {
                throw new CustomException(ResultCodeEnum.PARAM_ERROR, "企业尚未验收，请等待企业验收后再提交");
            }
            // 如果已存在 REJECTED 或 EXPIRED 状态的成品，允许重新提交（创建新记录）
            if ("REJECTED".equals(existing.getStatus()) || "EXPIRED".equals(existing.getStatus())) {
                // 检查提交次数（统计该稿件下所有成品记录数，包括所有状态）
                List<Deliverable> allDeliverables = deliverableMapper.selectAll(exist);
                int totalCount = allDeliverables != null ? allDeliverables.size() : 0;
                if (totalCount >= 3) {
                    throw new CustomException(ResultCodeEnum.PARAM_ERROR, "已达到最大提交次数（3次），无法再次提交");
                }
                // 创建新的成品记录，而不是更新旧的
                deliverable.setFreelancerId(freelancer.getId());
                deliverable.setStatus("SUBMITTED");
                deliverable.setSubmittedAt(LocalDateTime.now());
                deliverable.setSubmitCount(totalCount + 1);
                deliverableMapper.insert(deliverable);

                // 通知企业
                Enterprise enterprise = enterpriseMapper.selectById(project.getEnterpriseId());
                if (enterprise != null) {
                    messageService.sendNotification(project.getId(), enterprise.getEmployId(), "ENTERPRISE",
                            "您的项目《" + project.getTitle() + "》收到了新的成品提交（第" + deliverable.getSubmitCount() + "次），请及时验收。");
                }

                return deliverable;
            }
        }

        deliverable.setFreelancerId(freelancer.getId());
        deliverable.setStatus("SUBMITTED");
        deliverable.setSubmittedAt(LocalDateTime.now());
        deliverable.setSubmitCount(1);
        deliverableMapper.insert(deliverable);

        // 通知企业
        Enterprise enterprise = enterpriseMapper.selectById(project.getEnterpriseId());
        if (enterprise != null) {
            messageService.sendNotification(project.getId(), enterprise.getEmployId(), "ENTERPRISE",
                    "您的项目《" + project.getTitle() + "》收到了新的成品提交，请及时验收。");
        }

        return deliverable;
    }

    /**
     * 验收成品（企业操作）
     */
    public void reviewDeliverable(Integer deliverableId, String status, String reviewComment) {
        Account currentUser = TokenUtils.getCurrentUser();
        if (currentUser == null) {
            throw new CustomException(ResultCodeEnum.NO_AUTH_ERROR);
        }

        Deliverable deliverable = deliverableMapper.selectById(deliverableId);
        if (deliverable == null) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR, "成品不存在");
        }

        if (!"SUBMITTED".equals(deliverable.getStatus())) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR, "只能验收已提交状态的成品");
        }

        // 检查是否为项目发布者
        Project project = projectMapper.selectById(deliverable.getProjectId());
        if (project == null) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR, "项目不存在");
        }
        // 验证是否为项目发布者
        com.example.entity.Enterprise enterprise = enterpriseMapper.selectByEmployId(currentUser.getId());
        if (enterprise == null || !enterprise.getId().equals(project.getEnterpriseId())) {
            throw new CustomException(ResultCodeEnum.NO_AUTH_ERROR, "只有项目发布者可以验收成品");
        }

        deliverable.setStatus(status); // APPROVED 或 REJECTED
        deliverable.setReviewedAt(LocalDateTime.now());
        deliverable.setReviewComment(reviewComment);
        deliverableMapper.updateById(deliverable);

        // 更新自由职业者信誉分
        Freelancer freelancer = freelancerMapper.selectById(deliverable.getFreelancerId());
        if (freelancer != null) {
            Integer currentScore = freelancer.getCreditScore() != null ? freelancer.getCreditScore() : 100;
            if ("APPROVED".equals(status)) {
                // 验收通过，增加信誉分（+5分，无上限）
                freelancer.setCreditScore(currentScore + 5);
                // 增加完成项目数
                freelancer.setCompletedProjects(
                        freelancer.getCompletedProjects() != null ? freelancer.getCompletedProjects() + 1 : 1);
                // 付款给接单者
                paymentService.payCompletionPayment(deliverable.getProjectId());
                // 将项目状态改为已完成
                project.setStatus("COMPLETED");
                projectMapper.updateById(project);
                // 通知接单者
                if (freelancer.getUserId() != null) {
                    messageService.sendNotification(project.getId(), freelancer.getUserId(), "FREELANCER",
                            "恭喜！您的项目《" + project.getTitle() + "》成品验收通过，项目已完成，已增加信誉分并支付项目款项。");
                }
            } else if ("REJECTED".equals(status)) {
                // 验收不通过
                Integer submitCount = deliverable.getSubmitCount() != null ? deliverable.getSubmitCount() : 1;
                LocalDateTime now = LocalDateTime.now();

                // 检查是否超过截止时间
                boolean isBeforeDeadline = project.getDeliveryDeadline() != null
                        && now.isBefore(project.getDeliveryDeadline());

                // 检查该稿件下所有成品记录，判断是否3次验收都没通过
                Deliverable query = new Deliverable();
                query.setSubmissionId(deliverable.getSubmissionId());
                List<Deliverable> allDeliverables = deliverableMapper.selectAll(query);
                long rejectedCount = allDeliverables != null
                        ? allDeliverables.stream().filter(d -> "REJECTED".equals(d.getStatus())).count()
                        : 0;
                boolean allThreeRejected = rejectedCount >= 3;

                // 只有在3次验收都没通过或者超时未提交成品时才扣除信誉分与保证金
                if (allThreeRejected || !isBeforeDeadline) {
                    // 3次验收都没通过或超时未提交成品，扣除信誉分和保证金
                    freelancer.setCreditScore(Math.max(0, currentScore - 10));
                    // 扣除保证金并赔付给企业
                    paymentService.refundToEnterprise(deliverable.getProjectId());
                    // 通知自由职业者
                    if (freelancer.getUserId() != null) {
                        String reason;
                        if (allThreeRejected && !isBeforeDeadline) {
                            reason = "3次验收均未通过且已超过截止时间";
                        } else if (allThreeRejected) {
                            reason = "3次验收均未通过";
                        } else {
                            reason = "已超过截止时间";
                        }
                        messageService.sendNotification(project.getId(), freelancer.getUserId(), "FREELANCER",
                                "您的项目《" + project.getTitle() + "》成品验收失败（" + reason + "），已扣除信誉分和保证金并赔付给企业。");
                    }
                } else {
                    // 验收不通过但还有机会重新提交，不扣除信誉分和保证金
                    // 通知自由职业者
                    if (freelancer.getUserId() != null) {
                        messageService.sendNotification(project.getId(), freelancer.getUserId(), "FREELANCER",
                                "您的项目《" + project.getTitle() + "》成品验收不通过（第" + submitCount + "次），还有" + (3 - submitCount)
                                        + "次提交机会，请及时修改并重新提交。");
                    }
                }
            }
            freelancerMapper.updateById(freelancer);
        }
    }

    /**
     * 查询成品列表
     */
    public List<Deliverable> selectAll(Deliverable deliverable) {
        return deliverableMapper.selectAll(deliverable);
    }

    /**
     * 查询我的成品（当前用户的所有成品）
     */
    public List<Deliverable> selectMyDeliverables() {
        Account currentUser = TokenUtils.getCurrentUser();
        if (currentUser == null) {
            throw new CustomException(ResultCodeEnum.NO_AUTH_ERROR);
        }

        Freelancer freelancer = freelancerMapper.selectByUserId(currentUser.getId());
        if (freelancer == null) {
            return new java.util.ArrayList<>();
        }

        Deliverable query = new Deliverable();
        query.setFreelancerId(freelancer.getId());
        return deliverableMapper.selectAll(query);
    }

    /**
     * 根据ID查询成品
     */
    public Deliverable selectById(Integer id) {
        Deliverable deliverable = deliverableMapper.selectById(id);
        if (deliverable != null) {
            // 加载附件
            List<DeliverableAttachment> attachments = attachmentMapper.selectByDeliverableId(id);
            deliverable.setAttachments(attachments);
        }
        return deliverable;
    }

    /**
     * 检查并标记过期的成品（定时任务调用）
     */
    public void checkExpiredDeliverables() {
        // 查询所有已提交但未验收的成品
        Deliverable query = new Deliverable();
        query.setStatus("SUBMITTED");
        List<Deliverable> submittedList = deliverableMapper.selectAll(query);

        LocalDateTime now = LocalDateTime.now();
        for (Deliverable deliverable : submittedList) {
            // 获取项目的成品提交截止时间
            Project project = projectMapper.selectById(deliverable.getProjectId());
            if (project != null && project.getDeliveryDeadline() != null) {
                // 如果超过截止时间，标记为过期并扣除信誉分
                if (now.isAfter(project.getDeliveryDeadline())) {
                    deliverable.setStatus("EXPIRED");
                    deliverableMapper.updateById(deliverable);

                    // 退款给企业
                    paymentService.refundToEnterprise(deliverable.getProjectId());

                    // 扣除自由职业者信誉分
                    Freelancer freelancer = freelancerMapper.selectById(deliverable.getFreelancerId());
                    if (freelancer != null) {
                        Integer currentScore = freelancer.getCreditScore() != null ? freelancer.getCreditScore() : 100;
                        freelancer.setCreditScore(Math.max(0, currentScore - 10)); // 扣除10分
                        freelancerMapper.updateById(freelancer);
                    }
                }
            }
        }
    }
}
