package com.example.ticket.dto.request;

import javax.validation.constraints.NotNull;

public class TicketTransferRequest {
    @NotNull
    private Long ticketId;
    @NotNull
    private Long targetAgentId;
    private String reason;

    public Long getTicketId() { return ticketId; }
    public void setTicketId(Long ticketId) { this.ticketId = ticketId; }
    public Long getTargetAgentId() { return targetAgentId; }
    public void setTargetAgentId(Long targetAgentId) { this.targetAgentId = targetAgentId; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
