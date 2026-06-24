package com.example.ticket.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("cs_agent")
public class CsAgent {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String skills;
    private Integer status;
    private Integer maxLoad;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
