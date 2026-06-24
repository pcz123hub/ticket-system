package com.example.ticket.exception;

public class TicketException extends RuntimeException {
    private final int code;

    public TicketException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() { return code; }
}
