package com.example.ticket.domain.enums;

public enum TicketPriority {
    P0(0, "紧急"),
    P1(1, "高"),
    P2(2, "普通");

    private final int code;
    private final String desc;

    TicketPriority(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public int getCode() { return code; }
    public String getDesc() { return desc; }

    public static TicketPriority fromCode(int code) {
        for (TicketPriority p : values()) {
            if (p.code == code) return p;
        }
        return P2;
    }
}
