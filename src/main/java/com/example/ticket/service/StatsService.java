package com.example.ticket.service;

import com.example.ticket.mapper.TicketMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StatsService {

    private static final String DASHBOARD_KEY = "stats:dashboard";
    private final TicketMapper ticketMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    @Scheduled(fixedRate = 300000)
    public void refreshDashboard() {
        String today = LocalDate.now().atStartOfDay().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        Map<String, Object> statusCounts = ticketMapper.countByStatusSince(today)
            .stream().collect(Collectors.toMap(
                m -> "status_" + m.get("status"),
                m -> m.get("count")
            ));

        Map<String, Object> agentLoads = ticketMapper.countProcessingByAgent()
            .stream().collect(Collectors.toMap(
                m -> "agent_" + m.get("assignee_id"),
                m -> m.get("count")
            ));

        Long avgMinutes = ticketMapper.avgProcessMinutes(today);
        statusCounts.put("avg_process_minutes", avgMinutes != null ? avgMinutes : 0);

        redisTemplate.opsForHash().putAll(DASHBOARD_KEY, statusCounts);
        redisTemplate.opsForHash().putAll(DASHBOARD_KEY, agentLoads);
    }

    public Map<Object, Object> getDashboard() {
        return redisTemplate.opsForHash().entries(DASHBOARD_KEY);
    }
}
