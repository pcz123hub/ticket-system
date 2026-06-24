package com.example.ticket.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("ticket_sla_escalation")
public class TicketSlaEscalation {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long ticketId;
    private Integer escalationLevel;
    private LocalDateTime notifiedAt;
    private LocalDateTime resolvedAt;
}
