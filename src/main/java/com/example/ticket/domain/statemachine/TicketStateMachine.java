package com.example.ticket.domain.statemachine;

import com.example.ticket.domain.entity.Ticket;
import com.example.ticket.domain.enums.TicketAction;

import java.util.List;

public interface TicketStateMachine {
    boolean transition(Ticket ticket, TicketAction action, Long operatorId);
    List<TicketAction> availableActions(Ticket ticket);
}
