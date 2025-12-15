package com.example.controller;

import com.example.common.Result;
import com.example.entity.Message;
import com.example.service.MessageService;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 消息相关接口
 */
@RestController
@RequestMapping("/api/messages")
public class MessageController {

    @Resource
    private MessageService messageService;

    /**
     * 发送消息
     */
    @PostMapping
    public Result add(@RequestBody Message message) {
        messageService.add(message);
        return Result.success();
    }

    /**
     * 根据ID查询
     */
    @GetMapping("/{id}")
    public Result selectById(@PathVariable Integer id) {
        Message message = messageService.selectById(id);
        return Result.success(message);
    }

    /**
     * 查询所有
     */
    @GetMapping
    public Result selectAll(Message message,
                            @RequestParam(defaultValue = "1") Integer pageNum,
                            @RequestParam(defaultValue = "10") Integer pageSize,
                            @RequestParam(required = false) Boolean excludeSubmissionId) {
        if (excludeSubmissionId != null) {
            message.setExcludeSubmissionId(excludeSubmissionId);
        }
        PageInfo<Message> pageInfo = messageService.selectPage(message, pageNum, pageSize);
        return Result.success(pageInfo);
    }

    /**
     * 根据项目ID查询消息
     */
    @GetMapping("/project/{projectId}")
    public Result selectByProjectId(@PathVariable Integer projectId) {
        List<Message> list = messageService.selectByProjectId(projectId);
        return Result.success(list);
    }

    /**
     * 查询未读消息
     */
    @GetMapping("/unread")
    public Result selectUnread() {
        List<Message> list = messageService.selectUnread();
        return Result.success(list);
    }
    
    /**
     * 统计未读通知数量（排除聊天消息）
     */
    @GetMapping("/unread/count")
    public Result countUnreadNotifications() {
        Integer count = messageService.countUnreadNotifications();
        return Result.success(count);
    }

    /**
     * 标记消息已读
     */
    @PutMapping("/{id}/read")
    public Result markAsRead(@PathVariable Integer id) {
        messageService.markAsRead(id);
        return Result.success();
    }

    /**
     * 根据稿件ID查询消息
     */
    @GetMapping("/submission/{submissionId}")
    public Result selectBySubmissionId(@PathVariable Integer submissionId) {
        List<Message> list = messageService.selectBySubmissionId(submissionId);
        return Result.success(list);
    }

    /**
     * 删除消息
     */
    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Integer id) {
        messageService.deleteById(id);
        return Result.success();
    }

    /**
     * 获取聊天列表（按submission分组）
     */
    @GetMapping("/chats")
    public Result getChatList() {
        List<Message> list = messageService.getChatList();
        return Result.success(list);
    }
}

