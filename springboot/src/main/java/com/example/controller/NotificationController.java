package com.example.controller;

import com.example.common.Result;
import com.example.entity.Notification;
import com.example.service.NotificationService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 通知相关接口
 */
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    @Resource
    private NotificationService notificationService;

    /**
     * 查询当前用户的通知列表
     */
    @GetMapping
    public Result selectByCurrentUser() {
        List<Notification> list = notificationService.selectByCurrentUser();
        return Result.success(list);
    }

    /**
     * 统计当前用户的未读通知数量
     */
    @GetMapping("/unread/count")
    public Result countUnread() {
        Integer count = notificationService.countUnreadByCurrentUser();
        return Result.success(count);
    }

    /**
     * 标记通知为已读
     */
    @PutMapping("/{id}/read")
    public Result markAsRead(@PathVariable Integer id) {
        notificationService.markAsRead(id);
        return Result.success();
    }

    /**
     * 删除通知
     */
    @DeleteMapping("/{id}")
    public Result deleteById(@PathVariable Integer id) {
        notificationService.deleteById(id);
        return Result.success();
    }
}
















