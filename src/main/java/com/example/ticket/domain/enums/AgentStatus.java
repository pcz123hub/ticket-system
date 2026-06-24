package com.example.ticket.domain.enums;

public enum AgentStatus {
    OFFLINE(0, "离线"),
    ONLINE(1, "在线");

    private final int code;
    private final String desc;
    AgentStatus(int code, String desc) { this.code = code; this.desc = desc; }
    public int getCode() { return code; }
    public boolean isOnline() { return this == ONLINE; }
}
