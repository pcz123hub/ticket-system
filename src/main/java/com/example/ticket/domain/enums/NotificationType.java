package com.example.ticket.domain.enums;

public enum NotificationType {
    TICKET_ASSIGN("工单分配"),
    TICKET_TRANSFER("工单转交"),
    SLA_WARNING("SLA预警"),
    SLA_ESCALATION("SLA升级");

    private final String desc;
    NotificationType(String desc) { this.desc = desc; }
    public String getDesc() { return desc; }
}
