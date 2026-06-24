package com.example.ticket.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.ticket.domain.entity.CsAgent;
import com.example.ticket.domain.entity.Ticket;
import com.example.ticket.domain.entity.TicketAssignmentLog;
import com.example.ticket.domain.enums.AgentStatus;
import com.example.ticket.domain.enums.TicketAction;
import com.example.ticket.domain.statemachine.TicketStateMachine;
import com.example.ticket.mapper.CsAgentMapper;
import com.example.ticket.mapper.TicketAssignmentLogMapper;
import com.example.ticket.mapper.TicketMapper;
import com.example.ticket.strategy.AssignmentStrategy;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AssignmentService {

    private final RedissonClient redissonClient;
    private final AssignmentStrategy assignmentStrategy;
    private final TicketMapper ticketMapper;
    private final CsAgentMapper csAgentMapper;
    private final TicketAssignmentLogMapper assignmentLogMapper;
    private final TicketStateMachine ticketStateMachine;
    private final StringRedisTemplate redisTemplate;

    private static final String LOCK_KEY = "ticket:assign:lock:";
    private static final String AGENT_LOAD_KEY = "ticket:agent:load:";

    @Transactional
    public void assign(Long ticketId, Long specificAgentId) {
        RLock lock = redissonClient.getLock(LOCK_KEY + ticketId);
        try {
            if (!lock.tryLock(5, 10, TimeUnit.SECONDS)) {
                throw new RuntimeException("分配忙碌，请重试");
            }

            Ticket ticket = ticketMapper.selectById(ticketId);
            if (ticket == null) throw new RuntimeException("工单不存在");

            Long agentId;
            if (specificAgentId != null) {
                agentId = specificAgentId;
            } else {
                List<CsAgent> onlineAgents = csAgentMapper.selectList(
                    new LambdaQueryWrapper<CsAgent>()
                        .eq(CsAgent::getStatus, AgentStatus.ONLINE.getCode())
                );
                onlineAgents = onlineAgents.stream()
                    .filter(a -> getAgentLoad(a.getId()) < a.getMaxLoad())
                    .collect(Collectors.toList());

                agentId = assignmentStrategy.selectAgent(onlineAgents);
            }

            if (agentId == null) throw new RuntimeException("无可用客服");

            Long oldAssignee = ticket.getAssigneeId();
            ticketStateMachine.transition(ticket, TicketAction.ASSIGN, agentId);
            ticket.setAssigneeId(agentId);
            ticketMapper.updateById(ticket);

            TicketAssignmentLog log = new TicketAssignmentLog();
            log.setTicketId(ticketId);
            log.setFromAgentId(oldAssignee);
            log.setToAgentId(agentId);
            log.setStrategy(specificAgentId != null ? "MANUAL" : "ROUND_ROBIN");
            assignmentLogMapper.insert(log);

            incrementAgentLoad(agentId);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("分配被中断");
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    private long getAgentLoad(Long agentId) {
        String val = redisTemplate.opsForValue().get(AGENT_LOAD_KEY + agentId);
        return val == null ? 0 : Long.parseLong(val);
    }

    private void incrementAgentLoad(Long agentId) {
        redisTemplate.opsForValue().increment(AGENT_LOAD_KEY + agentId);
    }
}
