package com.example.ticket.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("ticket")
public class Ticket {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String ticketNo;
    private String title;
    private String description;
    private String category;
    private Integer priority;
    private Integer status;
    private Long customerId;
    private Long assigneeId;
    private String source;
    private Integer satisfaction;

    @Version
    private Integer version;

    private LocalDateTime closedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
