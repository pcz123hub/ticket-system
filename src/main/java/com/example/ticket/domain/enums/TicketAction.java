package com.example.ticket.domain.enums;

public enum TicketAction {
    ASSIGN("分配"),
    TRANSFER("转交"),
    RESOLVE("解决"),
    CLOSE("关闭");

    private final String desc;

    TicketAction(String desc) { this.desc = desc; }
    public String getDesc() { return desc; }
}
