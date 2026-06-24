package com.example.ticket.strategy;

import com.example.ticket.domain.entity.CsAgent;
import java.util.List;

public interface AssignmentStrategy {
    Long selectAgent(List<CsAgent> candidates);
}
