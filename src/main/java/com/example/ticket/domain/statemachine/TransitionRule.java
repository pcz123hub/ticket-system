package com.example.ticket.domain.statemachine;

import com.example.ticket.domain.enums.TicketAction;
import com.example.ticket.domain.enums.TicketStatus;

import java.util.*;

public class TransitionRule {
    private final Map<TicketStatus, Map<TicketAction, TicketStatus>> rules = new HashMap<>();

    public TransitionRule() {
        addRule(TicketStatus.PENDING, TicketAction.ASSIGN, TicketStatus.PROCESSING);
        addRule(TicketStatus.PENDING, TicketAction.CLOSE, TicketStatus.CLOSED);
        addRule(TicketStatus.PROCESSING, TicketAction.TRANSFER, TicketStatus.PROCESSING);
        addRule(TicketStatus.PROCESSING, TicketAction.RESOLVE, TicketStatus.RESOLVED);
        addRule(TicketStatus.RESOLVED, TicketAction.CLOSE, TicketStatus.CLOSED);
    }

    private void addRule(TicketStatus from, TicketAction action, TicketStatus to) {
        rules.computeIfAbsent(from, k -> new HashMap<>()).put(action, to);
    }

    public TicketStatus getTarget(TicketStatus from, TicketAction action) {
        Map<TicketAction, TicketStatus> actionMap = rules.get(from);
        return actionMap != null ? actionMap.get(action) : null;
    }

    public List<TicketAction> getAvailableActions(TicketStatus status) {
        Map<TicketAction, TicketStatus> actionMap = rules.get(status);
        return actionMap != null ? new ArrayList<>(actionMap.keySet()) : Collections.emptyList();
    }
}
