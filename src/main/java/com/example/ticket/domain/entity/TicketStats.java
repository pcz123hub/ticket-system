package com.example.ticket.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;

@Data
@TableName("ticket_stats")
public class TicketStats {
    @TableId(type = IdType.AUTO)
    private Long id;
    private LocalDate statsDate;
    private String category;
    private Integer totalCount;
    private Integer pendingCount;
    private Integer processingCount;
    private Integer resolvedCount;
    private Integer closedCount;
    private Integer avgProcessMinutes;
    private java.math.BigDecimal slaPassRate;
    private java.math.BigDecimal satisfactionAvg;
}
