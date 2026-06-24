package com.example.ticket.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("ticket_assignment_log")
public class TicketAssignmentLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long ticketId;
    private Long fromAgentId;
    private Long toAgentId;
    private String strategy;
    private String reason;
    private LocalDateTime createdAt;
}
