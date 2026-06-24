package com.example.ticket.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("ticket_log")
public class TicketLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long ticketId;
    private Integer fromStatus;
    private Integer toStatus;
    private Long operatorId;
    private String remark;
    private LocalDateTime createdAt;
}
