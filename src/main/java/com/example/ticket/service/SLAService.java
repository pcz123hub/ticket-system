package com.example.ticket.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.ticket.domain.entity.Ticket;
import com.example.ticket.domain.entity.TicketSlaConfig;
import com.example.ticket.mapper.TicketMapper;
import com.example.ticket.mapper.TicketSlaConfigMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class SLAService {

    private static final String SLA_QUEUE_KEY = "sla:queue";
    private final StringRedisTemplate redisTemplate;
    private final TicketMapper ticketMapper;
    private final TicketSlaConfigMapper slaConfigMapper;
    private final NotificationService notificationService;

    public void addToSlaQueue(Long ticketId, String category, int priority) {
        TicketSlaConfig config = slaConfigMapper.selectOne(
            new LambdaQueryWrapper<TicketSlaConfig>()
                .eq(TicketSlaConfig::getCategory, category)
                .eq(TicketSlaConfig::getPriority, priority)
        );
        if (config == null) return;

        long warnTimestamp = Instant.now().getEpochSecond() + config.getWarnTimeoutMinutes() * 60L;
        redisTemplate.opsForZSet().add(SLA_QUEUE_KEY, String.valueOf(ticketId), warnTimestamp);
    }

    public void removeFromSlaQueue(Long ticketId) {
        redisTemplate.opsForZSet().remove(SLA_QUEUE_KEY, String.valueOf(ticketId));
    }

    @Scheduled(fixedRate = 30000)
    public void scanSlaQueue() {
        long now = Instant.now().getEpochSecond();
        Set<String> expiredIds = redisTemplate.opsForZSet()
            .rangeByScore(SLA_QUEUE_KEY, 0, now);

        if (expiredIds == null || expiredIds.isEmpty()) return;

        for (String idStr : expiredIds) {
            try {
                Long ticketId = Long.parseLong(idStr);
                Ticket ticket = ticketMapper.selectById(ticketId);
                if (ticket == null) {
                    redisTemplate.opsForZSet().remove(SLA_QUEUE_KEY, idStr);
                    continue;
                }
                if (ticket.getStatus() >= 2) {
                    redisTemplate.opsForZSet().remove(SLA_QUEUE_KEY, idStr);
                    continue;
                }

                if (ticket.getAssigneeId() != null) {
                    notificationService.notifySlaWarning(ticket.getAssigneeId(), ticketId);
                }

                redisTemplate.opsForZSet().remove(SLA_QUEUE_KEY, idStr);
                log.info("SLA warning triggered for ticket {}", ticketId);
            } catch (Exception e) {
                log.error("SLA scan error for ticket {}", idStr, e);
            }
        }
    }
}
