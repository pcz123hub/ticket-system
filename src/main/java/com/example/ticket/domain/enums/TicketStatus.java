package com.example.ticket.domain.enums;

public enum TicketStatus {
    PENDING(0, "待分配"),
    PROCESSING(1, "处理中"),
    RESOLVED(2, "已解决"),
    CLOSED(3, "已关闭"),
    TRANSFERRED(4, "已转交");

    private final int code;
    private final String desc;

    TicketStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public int getCode() { return code; }
    public String getDesc() { return desc; }

    public static TicketStatus fromCode(int code) {
        for (TicketStatus s : values()) {
            if (s.code == code) return s;
        }
        throw new IllegalArgumentException("Unknown code: " + code);
    }
}
