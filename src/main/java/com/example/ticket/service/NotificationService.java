package com.example.ticket.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.ticket.domain.entity.Notification;
import com.example.ticket.domain.enums.NotificationType;
import com.example.ticket.mapper.NotificationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationMapper notificationMapper;

    public void notify(Long userId, NotificationType type, String title, String content, Long ticketId) {
        Notification n = new Notification();
        n.setUserId(userId);
        n.setType(type.name());
        n.setTitle(title);
        n.setContent(content);
        n.setRelatedTicketId(ticketId);
        notificationMapper.insert(n);
    }

    public void notifyAssign(Long userId, Long ticketId) {
        notify(userId, NotificationType.TICKET_ASSIGN,
            "工单分配", "您有一个新工单待处理", ticketId);
    }

    public void notifyTransfer(Long userId, Long ticketId, Long operatorId, String reason) {
        notify(userId, NotificationType.TICKET_TRANSFER,
            "工单转交", "工单已转交给您处理" + (reason != null ? "：" + reason : ""), ticketId);
    }

    public void notifySlaWarning(Long userId, Long ticketId) {
        notify(userId, NotificationType.SLA_WARNING,
            "SLA预警", "工单即将超时，请尽快处理", ticketId);
    }

    public void notifySlaEscalation(Long userId, Long ticketId, int level) {
        notify(userId, NotificationType.SLA_ESCALATION,
            "SLA升级(L" + level + ")", "工单已超时，请关注", ticketId);
    }

    public Page<Notification> listByUser(Long userId, int pageNum, int pageSize) {
        return notificationMapper.selectPage(new Page<>(pageNum, pageSize),
            new LambdaQueryWrapper<Notification>()
                .eq(Notification::getUserId, userId)
                .orderByDesc(Notification::getCreatedAt));
    }

    public long countUnread(Long userId) {
        return notificationMapper.selectCount(
            new LambdaQueryWrapper<Notification>()
                .eq(Notification::getUserId, userId)
                .eq(Notification::getIsRead, 0));
    }
}
