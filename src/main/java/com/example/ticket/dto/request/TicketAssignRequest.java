package com.example.ticket.dto.request;

import javax.validation.constraints.NotNull;

public class TicketAssignRequest {
    @NotNull
    private Long ticketId;
    private Long agentId;

    public Long getTicketId() { return ticketId; }
    public void setTicketId(Long ticketId) { this.ticketId = ticketId; }
    public Long getAgentId() { return agentId; }
    public void setAgentId(Long agentId) { this.agentId = agentId; }
}
