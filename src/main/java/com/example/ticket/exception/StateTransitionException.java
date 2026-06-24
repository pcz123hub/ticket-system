package com.example.ticket.exception;

public class StateTransitionException extends TicketException {
    public StateTransitionException(String message) {
        super(400, message);
    }
}
