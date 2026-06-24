package com.example.ticket.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("ticket_sla_config")
public class TicketSlaConfig {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String category;
    private Integer priority;
    private Integer warnTimeoutMinutes;
    private Integer escalateTimeoutMinutes;
    private Integer escalateLevel;
}
