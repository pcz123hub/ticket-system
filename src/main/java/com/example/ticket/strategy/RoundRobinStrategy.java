package com.example.ticket.strategy;

import com.example.ticket.domain.entity.CsAgent;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RoundRobinStrategy implements AssignmentStrategy {

    private static final String COUNTER_KEY = "ticket:assign:counter";
    private final StringRedisTemplate redisTemplate;

    public RoundRobinStrategy(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Long selectAgent(List<CsAgent> candidates) {
        if (candidates.isEmpty()) return null;
        long index = redisTemplate.opsForValue().increment(COUNTER_KEY) % candidates.size();
        return candidates.get((int) index).getId();
    }
}
