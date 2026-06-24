package com.example.ticket.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.ticket.domain.entity.Ticket;
import com.example.ticket.domain.enums.TicketAction;
import com.example.ticket.domain.enums.TicketPriority;
import com.example.ticket.domain.enums.TicketStatus;
import com.example.ticket.domain.statemachine.TicketStateMachine;
import com.example.ticket.dto.request.TicketCreateRequest;
import com.example.ticket.dto.request.TicketSearchRequest;
import com.example.ticket.dto.response.PageResult;
import com.example.ticket.dto.response.TicketVO;
import com.example.ticket.exception.TicketException;
import com.example.ticket.mapper.TicketMapper;
import com.example.ticket.util.TicketNoGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class TicketService {

    private final TicketMapper ticketMapper;
    private final TicketNoGenerator ticketNoGenerator;
    private final TicketStateMachine ticketStateMachine;
    private final AssignmentService assignmentService;
    private final NotificationService notificationService;
    private final StringRedisTemplate redisTemplate;

    private static final String IDEMPOTENT_KEY_PREFIX = "idempotent:ticket:";

    @Transactional
    public TicketVO createTicket(TicketCreateRequest req, String idempotentKey) {
        if (idempotentKey != null) {
            Boolean existed = redisTemplate.opsForValue()
                .setIfAbsent(IDEMPOTENT_KEY_PREFIX + idempotentKey, "1", 30, TimeUnit.SECONDS);
            if (Boolean.FALSE.equals(existed)) {
                throw new TicketException(409, "重复请求");
            }
        }

        Ticket ticket = new Ticket();
        ticket.setTicketNo(ticketNoGenerator.generate());
        ticket.setTitle(req.getTitle());
        ticket.setDescription(req.getDescription());
        ticket.setCategory(req.getCategory());
        ticket.setPriority(req.getPriority());
        ticket.setCustomerId(req.getCustomerId());
        ticket.setSource(req.getSource());
        ticket.setStatus(TicketStatus.PENDING.getCode());
        ticketMapper.insert(ticket);

        assignmentService.assign(ticket.getId(), null);

        return toVO(ticketMapper.selectById(ticket.getId()));
    }

    @Transactional
    public void transferTicket(Long ticketId, Long targetAgentId, String reason, Long operatorId) {
        Ticket ticket = ticketMapper.selectById(ticketId);
        if (ticket == null) throw new TicketException(404, "工单不存在");

        ticketStateMachine.transition(ticket, TicketAction.TRANSFER, operatorId);
        ticket.setAssigneeId(targetAgentId);
        ticketMapper.updateById(ticket);

        notificationService.notifyTransfer(targetAgentId, ticketId, operatorId, reason);
    }

    @Transactional
    public void resolveTicket(Long ticketId, Long operatorId) {
        Ticket ticket = ticketMapper.selectById(ticketId);
        if (ticket == null) throw new TicketException(404, "工单不存在");
        ticketStateMachine.transition(ticket, TicketAction.RESOLVE, operatorId);
        ticketMapper.updateById(ticket);
    }

    @Transactional
    public void closeTicket(Long ticketId, Long operatorId, Integer satisfaction) {
        Ticket ticket = ticketMapper.selectById(ticketId);
        if (ticket == null) throw new TicketException(404, "工单不存在");
        ticketStateMachine.transition(ticket, TicketAction.CLOSE, operatorId);
        ticket.setSatisfaction(satisfaction);
        ticketMapper.updateById(ticket);
    }

    public TicketVO getTicket(Long id) {
        Ticket ticket = ticketMapper.selectById(id);
        if (ticket == null) throw new TicketException(404, "工单不存在");
        return toVO(ticket);
    }

    public PageResult<TicketVO> listCustomerTickets(Long customerId, int pageNum, int pageSize) {
        Page<Ticket> page = ticketMapper.selectPage(
            new Page<>(pageNum, pageSize),
            new LambdaQueryWrapper<Ticket>()
                .eq(Ticket::getCustomerId, customerId)
                .orderByDesc(Ticket::getCreatedAt)
        );
        return PageResult.of(page.convert(this::toVO));
    }

    public PageResult<TicketVO> searchTickets(TicketSearchRequest req) {
        LambdaQueryWrapper<Ticket> wrapper = new LambdaQueryWrapper<>();
        if (req.getKeyword() != null) {
            wrapper.like(Ticket::getTitle, req.getKeyword())
                   .or().like(Ticket::getTicketNo, req.getKeyword());
        }
        if (req.getStatus() != null) wrapper.eq(Ticket::getStatus, req.getStatus());
        if (req.getPriority() != null) wrapper.eq(Ticket::getPriority, req.getPriority());
        if (req.getCategory() != null) wrapper.eq(Ticket::getCategory, req.getCategory());
        wrapper.orderByDesc(Ticket::getCreatedAt);

        Page<Ticket> page = ticketMapper.selectPage(
            new Page<>(req.getPageNum(), req.getPageSize()), wrapper
        );
        return PageResult.of(page.convert(this::toVO));
    }

    private TicketVO toVO(Ticket ticket) {
        TicketVO vo = new TicketVO();
        vo.setId(ticket.getId());
        vo.setTicketNo(ticket.getTicketNo());
        vo.setTitle(ticket.getTitle());
        vo.setDescription(ticket.getDescription());
        vo.setCategory(ticket.getCategory());
        vo.setPriority(ticket.getPriority());
        vo.setPriorityDesc(ticket.getPriority() != null ? TicketPriority.fromCode(ticket.getPriority()).getDesc() : null);
        vo.setStatus(ticket.getStatus());
        vo.setStatusDesc(ticket.getStatus() != null ? TicketStatus.fromCode(ticket.getStatus()).getDesc() : null);
        vo.setCustomerId(ticket.getCustomerId());
        vo.setAssigneeId(ticket.getAssigneeId());
        vo.setSource(ticket.getSource());
        vo.setSatisfaction(ticket.getSatisfaction());
        vo.setCreatedAt(ticket.getCreatedAt());
        vo.setClosedAt(ticket.getClosedAt());
        return vo;
    }
}
