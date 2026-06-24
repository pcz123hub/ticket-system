package com.example.ticket.domain.statemachine;

import com.example.ticket.domain.entity.Ticket;
import com.example.ticket.domain.entity.TicketLog;
import com.example.ticket.domain.enums.TicketAction;
import com.example.ticket.domain.enums.TicketStatus;
import com.example.ticket.exception.StateTransitionException;
import com.example.ticket.mapper.TicketLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DefaultTicketStateMachine implements TicketStateMachine {

    private final TransitionRule transitionRule = new TransitionRule();
    private final TicketLogMapper ticketLogMapper;

    @Override
    public boolean transition(Ticket ticket, TicketAction action, Long operatorId) {
        TicketStatus fromStatus = TicketStatus.fromCode(ticket.getStatus());
        TicketStatus targetStatus = transitionRule.getTarget(fromStatus, action);

        if (targetStatus == null) {
            throw new StateTransitionException(
                "状态 [" + fromStatus.getDesc() + "] 不允许操作 [" + action.getDesc() + "]"
            );
        }

        ticket.setStatus(targetStatus.getCode());
        if (action == TicketAction.CLOSE) {
            ticket.setClosedAt(LocalDateTime.now());
        }

        TicketLog log = new TicketLog();
        log.setTicketId(ticket.getId());
        log.setFromStatus(fromStatus.getCode());
        log.setToStatus(targetStatus.getCode());
        log.setOperatorId(operatorId);
        ticketLogMapper.insert(log);

        return true;
    }

    @Override
    public List<TicketAction> availableActions(Ticket ticket) {
        TicketStatus status = TicketStatus.fromCode(ticket.getStatus());
        return transitionRule.getAvailableActions(status);
    }
}
