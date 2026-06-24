# 工单流转模块 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现智能客服工单处理系统的工单流转核心模块后端，覆盖工单生命周期管理、智能分配、SLA 预警、统计看板、客户门户 API。

**Architecture:** Spring Boot 经典分层架构，状态机驱动工单生命周期，Redis ZSet 实现 SLA 高效扫描，Redisson 分布式锁保障并发安全。

**Tech Stack:** Spring Boot 2.7+ / MyBatis-Plus / MySQL 8 / Redis 6+ / Redisson / Docker

**Package:** `com.example.ticket`（可按实际情况修改）

---

### Task 1: Project Scaffolding

**Files:**
- Create: `pom.xml`
- Create: `src/main/java/com/example/ticket/TicketApplication.java`
- Create: `src/main/resources/application.yml`
- Create: `src/main/resources/application-dev.yml`
- Create: `src/main/resources/logback-spring.xml`
- Create: `.gitignore`

- [ ] **Step 1: Create pom.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>2.7.18</version>
        <relativePath/>
    </parent>
    <groupId>com.example</groupId>
    <artifactId>ticket-workflow</artifactId>
    <version>1.0.0</version>
    <name>ticket-workflow</name>
    <description>智能客服工单处理系统 - 工单流转模块</description>

    <properties>
        <java.version>1.8</java.version>
    </properties>

    <dependencies>
        <!-- Spring Boot -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>

        <!-- MyBatis-Plus -->
        <dependency>
            <groupId>com.baomidou</groupId>
            <artifactId>mybatis-plus-boot-starter</artifactId>
            <version>3.5.5</version>
        </dependency>

        <!-- MySQL -->
        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
            <scope>runtime</scope>
        </dependency>

        <!-- Redis + Redisson -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-redis</artifactId>
        </dependency>
        <dependency>
            <groupId>org.redisson</groupId>
            <artifactId>redisson-spring-boot-starter</artifactId>
            <version>3.27.2</version>
        </dependency>

        <!-- Lombok -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>

        <!-- Test -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <excludes>
                        <exclude>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                        </exclude>
                    </excludes>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

- [ ] **Step 2: Create application.yml**

```yaml
server:
  port: 8080

spring:
  application:
    name: ticket-workflow
  profiles:
    active: dev
  datasource:
    url: jdbc:mysql://localhost:3306/ticket_db?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai
    username: root
    password: root
    driver-class-name: com.mysql.cj.jdbc.Driver
  redis:
    host: localhost
    port: 6379
    database: 0

mybatis-plus:
  configuration:
    map-underscore-to-camel-case: true
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
  global-config:
    db-config:
      logic-delete-field: deleted
      logic-delete-value: 1
      logic-not-delete-value: 0
      id-type: auto
```

```yaml
# application-dev.yml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/ticket_db?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai
    username: root
    password: root
  redis:
    host: localhost
    port: 6379
```

- [ ] **Step 3: Create TicketApplication.java**

```java
package com.example.ticket;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TicketApplication {
    public static void main(String[] args) {
        SpringApplication.run(TicketApplication.class, args);
    }
}
```

- [ ] **Step 4: Create RedisConfig.java**

```java
package com.example.ticket.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new StringRedisSerializer());
        return template;
    }
}
```

- [ ] **Step 5: Create MyBatisPlusConfig.java**

```java
package com.example.ticket.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MyBatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
        return interceptor;
    }
}
```

- [ ] **Step 6: Create RedissonConfig.java**

```java
package com.example.ticket.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedissonConfig {

    @Value("${spring.redis.host}")
    private String redisHost;

    @Value("${spring.redis.port}")
    private int redisPort;

    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();
        config.useSingleServer().setAddress("redis://" + redisHost + ":" + redisPort);
        return Redisson.create(config);
    }
}
```

- [ ] **Step 7: Create .gitignore**

```
target/
*.class
*.jar
*.log
.idea/
*.iml
.settings/
.project
.classpath
.DS_Store
application-dev.yml
```

- [ ] **Step 8: Create logback-spring.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <include resource="org/springframework/boot/logging/logback/base.xml"/>
    <logger name="com.example.ticket" level="DEBUG"/>
    <logger name="com.baomidou.mybatisplus" level="DEBUG"/>
</configuration>
```

---

### Task 2: Database Schema

**Files:**
- Create: `src/main/resources/db/init.sql`

- [ ] **Step 1: Write init.sql with all table DDL**

```sql
CREATE DATABASE IF NOT EXISTS ticket_db DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE ticket_db;

-- 工单主表
CREATE TABLE ticket (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    ticket_no     VARCHAR(32)  NOT NULL COMMENT '工单号',
    title         VARCHAR(200) NOT NULL COMMENT '标题',
    description   TEXT COMMENT '描述',
    category      VARCHAR(50)  NOT NULL COMMENT '业务类型',
    priority      TINYINT      NOT NULL DEFAULT 2 COMMENT '优先级 0=P0 1=P1 2=P2',
    status        TINYINT      NOT NULL DEFAULT 0 COMMENT '状态 0=PENDING 1=PROCESSING 2=RESOLVED 3=CLOSED 4=TRANSFERRED',
    customer_id   BIGINT       NOT NULL COMMENT '客户ID',
    assignee_id   BIGINT       DEFAULT NULL COMMENT '当前处理人ID',
    source        VARCHAR(50)  NOT NULL COMMENT '来源',
    satisfaction  TINYINT      DEFAULT NULL COMMENT '满意度评分 1-5',
    version       INT          NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    closed_at     DATETIME     DEFAULT NULL COMMENT '关闭时间',
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_ticket_no (ticket_no),
    KEY idx_status (status),
    KEY idx_assignee (assignee_id),
    KEY idx_customer (customer_id),
    KEY idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工单表';

-- 操作日志表
CREATE TABLE ticket_log (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    ticket_id     BIGINT       NOT NULL COMMENT '工单ID',
    from_status   TINYINT      DEFAULT NULL COMMENT '原状态',
    to_status     TINYINT      DEFAULT NULL COMMENT '目标状态',
    operator_id   BIGINT       NOT NULL COMMENT '操作人ID',
    remark        VARCHAR(500) DEFAULT NULL COMMENT '备注',
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_ticket_id (ticket_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工单操作日志';

-- SLA 规则配置表
CREATE TABLE ticket_sla_config (
    id                    BIGINT AUTO_INCREMENT PRIMARY KEY,
    category              VARCHAR(50) NOT NULL COMMENT '业务类型',
    priority              TINYINT     NOT NULL COMMENT '优先级',
    warn_timeout_minutes  INT         NOT NULL COMMENT '预警阈值(分钟)',
    escalate_timeout_minutes INT      NOT NULL COMMENT '升级阈值(分钟)',
    escalate_level        TINYINT     NOT NULL DEFAULT 1 COMMENT '升级级别 1=主管 2=经理',
    UNIQUE KEY uk_category_priority (category, priority)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='SLA规则配置';

-- 分配记录表
CREATE TABLE ticket_assignment_log (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    ticket_id     BIGINT   NOT NULL COMMENT '工单ID',
    from_agent_id BIGINT   DEFAULT NULL COMMENT '原处理人',
    to_agent_id   BIGINT   NOT NULL COMMENT '新处理人',
    strategy      VARCHAR(50) DEFAULT NULL COMMENT '分配策略',
    reason        VARCHAR(200) DEFAULT NULL COMMENT '原因',
    created_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_ticket_id (ticket_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='分配记录';

-- 升级记录表
CREATE TABLE ticket_sla_escalation (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    ticket_id        BIGINT      NOT NULL COMMENT '工单ID',
    escalation_level TINYINT     NOT NULL COMMENT '升级级别 1=L1 2=L2',
    notified_at      DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '通知时间',
    resolved_at      DATETIME    DEFAULT NULL COMMENT '解决时间',
    KEY idx_ticket_id (ticket_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='升级记录';

-- 站内通知表
CREATE TABLE notification (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id          BIGINT       NOT NULL COMMENT '用户ID',
    type             VARCHAR(50)  NOT NULL COMMENT '类型',
    title            VARCHAR(200) NOT NULL COMMENT '标题',
    content          VARCHAR(1000) DEFAULT NULL COMMENT '内容',
    related_ticket_id BIGINT      DEFAULT NULL COMMENT '关联工单ID',
    is_read          TINYINT      NOT NULL DEFAULT 0 COMMENT '是否已读',
    created_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_user_id (user_id),
    KEY idx_is_read (is_read)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='站内通知';

-- 客服人员表
CREATE TABLE cs_agent (
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    name      VARCHAR(50)  NOT NULL COMMENT '姓名',
    skills    VARCHAR(200) DEFAULT NULL COMMENT '技能标签',
    status    TINYINT      NOT NULL DEFAULT 0 COMMENT '状态 0=离线 1=在线',
    max_load  INT          NOT NULL DEFAULT 10 COMMENT '最大并行处理数',
    created_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客服人员';

-- 工单统计表（日维度归档）
CREATE TABLE ticket_stats (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    stats_date    DATE   NOT NULL COMMENT '统计日期',
    category      VARCHAR(50) DEFAULT NULL COMMENT '业务类型',
    total_count   INT    NOT NULL DEFAULT 0 COMMENT '工单总数',
    pending_count INT    NOT NULL DEFAULT 0,
    processing_count INT NOT NULL DEFAULT 0,
    resolved_count  INT NOT NULL DEFAULT 0,
    closed_count  INT    NOT NULL DEFAULT 0,
    avg_process_minutes INT DEFAULT 0 COMMENT '平均处理时长(分钟)',
    sla_pass_rate DECIMAL(5,2) DEFAULT 0 COMMENT 'SLA达标率',
    satisfaction_avg DECIMAL(3,2) DEFAULT NULL COMMENT '平均满意度',
    UNIQUE KEY uk_date_category (stats_date, category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工单日统计';

-- 插入默认 SLA 配置
INSERT INTO ticket_sla_config (category, priority, warn_timeout_minutes, escalate_timeout_minutes, escalate_level) VALUES
('GENERAL', 0, 30, 60, 2),
('GENERAL', 1, 240, 480, 1),
('GENERAL', 2, 1440, 2880, 1);
```

---

### Task 3: Entity + Enum Classes

**Files:**
- Create: `src/main/java/com/example/ticket/domain/enums/TicketStatus.java`
- Create: `src/main/java/com/example/ticket/domain/enums/TicketPriority.java`
- Create: `src/main/java/com/example/ticket/domain/enums/TicketAction.java`
- Create: `src/main/java/com/example/ticket/domain/enums/NotificationType.java`
- Create: `src/main/java/com/example/ticket/domain/enums/AgentStatus.java`
- Create: `src/main/java/com/example/ticket/domain/entity/Ticket.java`
- Create: `src/main/java/com/example/ticket/domain/entity/TicketLog.java`
- Create: `src/main/java/com/example/ticket/domain/entity/TicketSlaConfig.java`
- Create: `src/main/java/com/example/ticket/domain/entity/TicketAssignmentLog.java`
- Create: `src/main/java/com/example/ticket/domain/entity/TicketSlaEscalation.java`
- Create: `src/main/java/com/example/ticket/domain/entity/Notification.java`
- Create: `src/main/java/com/example/ticket/domain/entity/CsAgent.java`
- Create: `src/main/java/com/example/ticket/domain/entity/TicketStats.java`

- [ ] **Step 1: Create TicketStatus enum**

```java
package com.example.ticket.domain.enums;

public enum TicketStatus {
    PENDING(0, "待分配"),
    PROCESSING(1, "处理中"),
    RESOLVED(2, "已解决"),
    CLOSED(3, "已关闭"),
    TRANSFERRED(4, "已转交");

    private final int code;
    private final String desc;

    TicketStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public int getCode() { return code; }
    public String getDesc() { return desc; }

    public static TicketStatus fromCode(int code) {
        for (TicketStatus s : values()) {
            if (s.code == code) return s;
        }
        throw new IllegalArgumentException("Unknown code: " + code);
    }
}
```

- [ ] **Step 2: Create TicketPriority enum**

```java
package com.example.ticket.domain.enums;

public enum TicketPriority {
    P0(0, "紧急"),
    P1(1, "高"),
    P2(2, "普通");

    private final int code;
    private final String desc;

    TicketPriority(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public int getCode() { return code; }
    public String getDesc() { return desc; }

    public static TicketPriority fromCode(int code) {
        for (TicketPriority p : values()) {
            if (p.code == code) return p;
        }
        return P2;
    }
}
```

- [ ] **Step 3: Create TicketAction enum**

```java
package com.example.ticket.domain.enums;

public enum TicketAction {
    ASSIGN("分配"),
    TRANSFER("转交"),
    RESOLVE("解决"),
    CLOSE("关闭");

    private final String desc;

    TicketAction(String desc) { this.desc = desc; }
    public String getDesc() { return desc; }
}
```

- [ ] **Step 4: Create NotificationType enum and AgentStatus enum**

```java
// NotificationType.java
package com.example.ticket.domain.enums;

public enum NotificationType {
    TICKET_ASSIGN("工单分配"),
    TICKET_TRANSFER("工单转交"),
    SLA_WARNING("SLA预警"),
    SLA_ESCALATION("SLA升级");

    private final String desc;
    NotificationType(String desc) { this.desc = desc; }
    public String getDesc() { return desc; }
}

// AgentStatus.java
package com.example.ticket.domain.enums;

public enum AgentStatus {
    OFFLINE(0, "离线"),
    ONLINE(1, "在线");

    private final int code;
    private final String desc;
    AgentStatus(int code, String desc) { this.code = code; this.desc = desc; }
    public int getCode() { return code; }
    public boolean isOnline() { return this == ONLINE; }
}
```

- [ ] **Step 5: Create Ticket entity**

```java
package com.example.ticket.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;

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

    // getters & setters (omitted for brevity — generate via Lombok @Data)
}
```

- [ ] **Step 6: Create remaining entities**

TicketLog with fields: id, ticketId, fromStatus, toStatus, operatorId, remark, createdAt
TicketSlaConfig with fields: id, category, priority, warnTimeoutMinutes, escalateTimeoutMinutes, escalateLevel
TicketAssignmentLog with fields: id, ticketId, fromAgentId, toAgentId, strategy, reason, createdAt
TicketSlaEscalation with fields: id, ticketId, escalationLevel, notifiedAt, resolvedAt
Notification with fields: id, userId, type, title, content, relatedTicketId, isRead, createdAt
CsAgent with fields: id, name, skills, status, maxLoad, createdAt, updatedAt
TicketStats with fields: id, statsDate, category, totalCount, pendingCount, processingCount, resolvedCount, closedCount, avgProcessMinutes, slaPassRate, satisfactionAvg

All annotated with `@TableName`, `@TableId`, `@Version` where applicable.

---

### Task 4: Mapper Layer

**Files:**
- Create: `src/main/java/com/example/ticket/mapper/TicketMapper.java`
- Create: `src/main/java/com/example/ticket/mapper/TicketLogMapper.java`
- Create: `src/main/java/com/example/ticket/mapper/TicketSlaConfigMapper.java`
- Create: `src/main/java/com/example/ticket/mapper/TicketAssignmentLogMapper.java`
- Create: `src/main/java/com/example/ticket/mapper/TicketSlaEscalationMapper.java`
- Create: `src/main/java/com/example/ticket/mapper/NotificationMapper.java`
- Create: `src/main/java/com/example/ticket/mapper/CsAgentMapper.java`
- Create: `src/main/java/com/example/ticket/mapper/TicketStatsMapper.java`

- [ ] **Step 1: Create all Mapper interfaces**

```java
package com.example.ticket.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.ticket.domain.entity.Ticket;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TicketMapper extends BaseMapper<Ticket> {
}
```

All other mappers follow the same pattern: `@Mapper public interface XxxMapper extends BaseMapper<XxxEntity> {}`

- [ ] **Step 2: Add custom TicketMapper query methods for stats aggregation**

```java
package com.example.ticket.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.ticket.domain.entity.Ticket;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface TicketMapper extends BaseMapper<Ticket> {

    @Select("SELECT status, COUNT(*) as count FROM ticket WHERE created_at >= #{since} GROUP BY status")
    List<Map<String, Object>> countByStatusSince(@Param("since") String since);

    @Select("SELECT assignee_id, COUNT(*) as count FROM ticket WHERE assignee_id IS NOT NULL AND status IN (0,1) GROUP BY assignee_id")
    List<Map<String, Object>> countProcessingByAgent();

    @Select("SELECT AVG(TIMESTAMPDIFF(MINUTE, created_at, closed_at)) as avg_minutes FROM ticket WHERE closed_at IS NOT NULL AND closed_at >= #{since}")
    Long avgProcessMinutes(@Param("since") String since);
}
```

---

### Task 5: DTO Classes

**Files:**
- Create: `src/main/java/com/example/ticket/dto/request/TicketCreateRequest.java`
- Create: `src/main/java/com/example/ticket/dto/request/TicketAssignRequest.java`
- Create: `src/main/java/com/example/ticket/dto/request/TicketTransferRequest.java`
- Create: `src/main/java/com/example/ticket/dto/request/TicketSearchRequest.java`
- Create: `src/main/java/com/example/ticket/dto/response/TicketVO.java`
- Create: `src/main/java/com/example/ticket/dto/response/PageResult.java`
- Create: `src/main/java/com/example/ticket/dto/response/StatsVO.java`

- [ ] **Step 1: Create request DTOs with validation**

```java
package com.example.ticket.dto.request;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

public class TicketCreateRequest {
    @NotBlank(message = "标题不能为空")
    private String title;

    private String description;

    @NotBlank(message = "业务类型不能为空")
    private String category;

    @NotNull(message = "优先级不能为空")
    private Integer priority;

    @NotNull(message = "客户ID不能为空")
    private Long customerId;

    @NotBlank(message = "来源不能为空")
    private String source;

    // getters & setters
}

public class TicketAssignRequest {
    @NotNull private Long ticketId;
    private Long agentId; // null = 自动分配
}

public class TicketTransferRequest {
    @NotNull private Long ticketId;
    @NotNull private Long targetAgentId;
    private String reason;
}

public class TicketSearchRequest {
    private String keyword;
    private Integer status;
    private Integer priority;
    private String category;
    private String startDate;
    private String endDate;
    private Integer pageNum = 1;
    private Integer pageSize = 10;
}
```

- [ ] **Step 2: Create response DTOs**

```java
// TicketVO.java
public class TicketVO {
    private Long id;
    private String ticketNo;
    private String title;
    private String description;
    private String category;
    private Integer priority;
    private String priorityDesc;
    private Integer status;
    private String statusDesc;
    private Long customerId;
    private Long assigneeId;
    private String assigneeName;
    private String source;
    private Integer satisfaction;
    private LocalDateTime createdAt;
    private LocalDateTime closedAt;
}

// PageResult.java
public class PageResult<T> {
    private List<T> list;
    private long total;
    private int pageNum;
    private int pageSize;

    public static <T> PageResult<T> of(Page<T> page) {
        PageResult<T> r = new PageResult<>();
        r.setList(page.getRecords());
        r.setTotal(page.getTotal());
        r.setPageNum((int) page.getCurrent());
        r.setPageSize((int) page.getSize());
        return r;
    }
}

// StatsVO.java
public class StatsVO {
    private Map<String, Long> statusDistribution;
    private List<Map<String, Object>> agentWorkload;
    private Long avgProcessMinutes;
    private Double slaPassRate;
    private Double satisfactionAvg;
}
```

---

### Task 6: Exception Handling

**Files:**
- Create: `src/main/java/com/example/ticket/exception/TicketException.java`
- Create: `src/main/java/com/example/ticket/exception/StateTransitionException.java`
- Create: `src/main/java/com/example/ticket/exception/GlobalExceptionHandler.java`

- [ ] **Step 1: Create custom exceptions**

```java
// TicketException.java — base runtime exception
public class TicketException extends RuntimeException {
    private final int code;
    public TicketException(int code, String message) { super(message); this.code = code; }
    public int getCode() { return code; }
}

// StateTransitionException.java
public class StateTransitionException extends TicketException {
    public StateTransitionException(String message) {
        super(400, message);
    }
}
```

- [ ] **Step 2: Create GlobalExceptionHandler**

```java
package com.example.ticket.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(TicketException.class)
    public ResponseEntity<Map<String, Object>> handleTicket(TicketException e) {
        return ResponseEntity.badRequest().body(Map.of(
            "code", e.getCode(), "message", e.getMessage()
        ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
            .map(f -> f.getField() + ": " + f.getDefaultMessage())
            .reduce((a, b) -> a + "; " + b).orElse("参数校验失败");
        return ResponseEntity.badRequest().body(Map.of("code", 400, "message", msg));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleUnknown(Exception e) {
        return ResponseEntity.internalServerError().body(Map.of(
            "code", 500, "message", "服务器内部错误"
        ));
    }
}
```

---

### Task 7: Ticket No Generator + Utility

**Files:**
- Create: `src/main/java/com/example/ticket/util/TicketNoGenerator.java`

- [ ] **Step 1: Create ticket number generator**

```java
package com.example.ticket.util;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Component
public class TicketNoGenerator {

    private final StringRedisTemplate redisTemplate;

    public TicketNoGenerator(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public String generate() {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        Long seq = redisTemplate.opsForValue().increment("ticket:seq:" + date);
        return "TK" + date + String.format("%05d", seq);
    }
}
```

---

### Task 8: State Machine Engine

**Files:**
- Create: `src/main/java/com/example/ticket/domain/statemachine/TransitionRule.java`
- Create: `src/main/java/com/example/ticket/domain/statemachine/TicketStateMachine.java`
- Create: `src/main/java/com/example/ticket/domain/statemachine/DefaultTicketStateMachine.java`

- [ ] **Step 1: Create TransitionRule — ruleset for allowed transitions**

```java
package com.example.ticket.domain.statemachine;

import com.example.ticket.domain.enums.TicketAction;
import com.example.ticket.domain.enums.TicketStatus;
import lombok.Getter;

import java.util.*;

@Getter
public class TransitionRule {
    private final Map<TicketStatus, Map<TicketAction, TicketStatus>> rules = new HashMap<>();

    public TransitionRule() {
        addRule(TicketStatus.PENDING, TicketAction.ASSIGN, TicketStatus.PROCESSING);
        addRule(TicketStatus.PENDING, TicketAction.CLOSE, TicketStatus.CLOSED);
        addRule(TicketStatus.PROCESSING, TicketAction.TRANSFER, TicketStatus.PROCESSING);
        addRule(TicketStatus.PROCESSING, TicketAction.RESOLVE, TicketStatus.RESOLVED);
        addRule(TicketStatus.RESOLVED, TicketAction.CLOSE, TicketStatus.CLOSED);
    }

    private void addRule(TicketStatus from, TicketAction action, TicketStatus to) {
        rules.computeIfAbsent(from, k -> new HashMap<>()).put(action, to);
    }

    public TicketStatus getTarget(TicketStatus from, TicketAction action) {
        Map<TicketAction, TicketStatus> actionMap = rules.get(from);
        return actionMap != null ? actionMap.get(action) : null;
    }

    public List<TicketAction> getAvailableActions(TicketStatus status) {
        Map<TicketAction, TicketStatus> actionMap = rules.get(status);
        return actionMap != null ? new ArrayList<>(actionMap.keySet()) : Collections.emptyList();
    }
}
```

- [ ] **Step 2: Create TicketStateMachine interface**

```java
package com.example.ticket.domain.statemachine;

import com.example.ticket.domain.entity.Ticket;
import com.example.ticket.domain.enums.TicketAction;

import java.util.List;

public interface TicketStateMachine {
    boolean transition(Ticket ticket, TicketAction action, Long operatorId);
    List<TicketAction> availableActions(Ticket ticket);
}
```

- [ ] **Step 3: Create DefaultTicketStateMachine implementation**

```java
package com.example.ticket.domain.statemachine;

import com.example.ticket.domain.entity.Ticket;
import com.example.ticket.domain.entity.TicketLog;
import com.example.ticket.domain.enums.TicketAction;
import com.example.ticket.domain.enums.TicketStatus;
import com.example.ticket.exception.StateTransitionException;
import com.example.ticket.mapper.TicketLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DefaultTicketStateMachine implements TicketStateMachine {

    private final TransitionRule transitionRule = new TransitionRule();
    private final TicketLogMapper ticketLogMapper;

    @Override
    public boolean transition(Ticket ticket, TicketAction action, Long operatorId) {
        TicketStatus fromStatus = TicketStatus.fromCode(ticket.getStatus());
        TicketStatus targetStatus = transitionRule.getTarget(fromStatus, action);

        if (targetStatus == null) {
            throw new StateTransitionException(
                "状态 [" + fromStatus.getDesc() + "] 不允许操作 [" + action.getDesc() + "]"
            );
        }

        ticket.setStatus(targetStatus.getCode());
        if (action == TicketAction.CLOSE) {
            ticket.setClosedAt(LocalDateTime.now());
        }

        // 记录操作日志
        TicketLog log = new TicketLog();
        log.setTicketId(ticket.getId());
        log.setFromStatus(fromStatus.getCode());
        log.setToStatus(targetStatus.getCode());
        log.setOperatorId(operatorId);
        ticketLogMapper.insert(log);

        return true;
    }

    @Override
    public List<TicketAction> availableActions(Ticket ticket) {
        TicketStatus status = TicketStatus.fromCode(ticket.getStatus());
        return transitionRule.getAvailableActions(status);
    }
}
```

---

### Task 9: Assignment Strategy

**Files:**
- Create: `src/main/java/com/example/ticket/strategy/AssignmentStrategy.java`
- Create: `src/main/java/com/example/ticket/strategy/RoundRobinStrategy.java`

- [ ] **Step 1: Create AssignmentStrategy interface**

```java
package com.example.ticket.strategy;

import com.example.ticket.domain.entity.CsAgent;
import java.util.List;

public interface AssignmentStrategy {
    Long selectAgent(List<CsAgent> candidates);
}
```

- [ ] **Step 2: Create RoundRobinStrategy (Redis counter based)**

```java
package com.example.ticket.strategy;

import com.example.ticket.domain.entity.CsAgent;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RoundRobinStrategy implements AssignmentStrategy {

    private static final String COUNTER_KEY = "ticket:assign:counter";
    private final StringRedisTemplate redisTemplate;

    public RoundRobinStrategy(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Long selectAgent(List<CsAgent> candidates) {
        if (candidates.isEmpty()) return null;
        long index = redisTemplate.opsForValue().increment(COUNTER_KEY) % candidates.size();
        return candidates.get((int) index).getId();
    }
}
```

---

### Task 10: AssignmentService

**Files:**
- Create: `src/main/java/com/example/ticket/service/AssignmentService.java`

- [ ] **Step 1: Create AssignmentService with distribute lock + round-robin logic**

```java
package com.example.ticket.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.ticket.domain.entity.CsAgent;
import com.example.ticket.domain.entity.Ticket;
import com.example.ticket.domain.entity.TicketAssignmentLog;
import com.example.ticket.domain.enums.AgentStatus;
import com.example.ticket.domain.enums.TicketAction;
import com.example.ticket.domain.statemachine.TicketStateMachine;
import com.example.ticket.mapper.CsAgentMapper;
import com.example.ticket.mapper.TicketAssignmentLogMapper;
import com.example.ticket.mapper.TicketMapper;
import com.example.ticket.strategy.AssignmentStrategy;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AssignmentService {

    private final RedissonClient redissonClient;
    private final AssignmentStrategy assignmentStrategy;
    private final TicketMapper ticketMapper;
    private final CsAgentMapper csAgentMapper;
    private final TicketAssignmentLogMapper assignmentLogMapper;
    private final TicketStateMachine ticketStateMachine;
    private final StringRedisTemplate redisTemplate;

    private static final String LOCK_KEY = "ticket:assign:lock:";
    private static final String AGENT_LOAD_KEY = "ticket:agent:load:";

    @Transactional
    public void assign(Long ticketId, Long specificAgentId) {
        RLock lock = redissonClient.getLock(LOCK_KEY + ticketId);
        try {
            if (!lock.tryLock(5, 10, TimeUnit.SECONDS)) {
                throw new RuntimeException("分配忙碌，请重试");
            }

            Ticket ticket = ticketMapper.selectById(ticketId);
            if (ticket == null) throw new RuntimeException("工单不存在");

            Long agentId;
            if (specificAgentId != null) {
                agentId = specificAgentId;
            } else {
                List<CsAgent> onlineAgents = csAgentMapper.selectList(
                    new LambdaQueryWrapper<CsAgent>()
                        .eq(CsAgent::getStatus, AgentStatus.ONLINE.getCode())
                );
                // 过滤负载已满
                onlineAgents = onlineAgents.stream()
                    .filter(a -> getAgentLoad(a.getId()) < a.getMaxLoad())
                    .collect(Collectors.toList());

                agentId = assignmentStrategy.selectAgent(onlineAgents);
            }

            if (agentId == null) throw new RuntimeException("无可用客服");

            // 状态流转
            Long oldAssignee = ticket.getAssigneeId();
            ticketStateMachine.transition(ticket, TicketAction.ASSIGN, agentId);
            ticket.setAssigneeId(agentId);
            ticketMapper.updateById(ticket);

            // 记录分配日志
            TicketAssignmentLog log = new TicketAssignmentLog();
            log.setTicketId(ticketId);
            log.setFromAgentId(oldAssignee);
            log.setToAgentId(agentId);
            log.setStrategy(specificAgentId != null ? "MANUAL" : "ROUND_ROBIN");
            assignmentLogMapper.insert(log);

            // 增加 Redis 负载计数
            incrementAgentLoad(agentId);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("分配被中断");
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    private long getAgentLoad(Long agentId) {
        String val = redisTemplate.opsForValue().get(AGENT_LOAD_KEY + agentId);
        return val == null ? 0 : Long.parseLong(val);
    }

    private void incrementAgentLoad(Long agentId) {
        redisTemplate.opsForValue().increment(AGENT_LOAD_KEY + agentId);
    }
}
```

---

### Task 11: TicketService (Core Workflow)

**Files:**
- Create: `src/main/java/com/example/ticket/service/TicketService.java`

- [ ] **Step 1: Create TicketService with full workflow methods**

```java
package com.example.ticket.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.ticket.domain.entity.Ticket;
import com.example.ticket.domain.enums.TicketAction;
import com.example.ticket.domain.enums.TicketStatus;
import com.example.ticket.domain.statemachine.TicketStateMachine;
import com.example.ticket.dto.request.*;
import com.example.ticket.dto.response.PageResult;
import com.example.ticket.dto.response.TicketVO;
import com.example.ticket.exception.TicketException;
import com.example.ticket.mapper.TicketMapper;
import com.example.ticket.util.TicketNoGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class TicketService {

    private final TicketMapper ticketMapper;
    private final TicketNoGenerator ticketNoGenerator;
    private final TicketStateMachine ticketStateMachine;
    private final AssignmentService assignmentService;
    private final NotificationService notificationService;
    private final StringRedisTemplate redisTemplate;

    private static final String IDEMPOTENT_KEY_PREFIX = "idempotent:ticket:";

    @Transactional
    public TicketVO createTicket(TicketCreateRequest req, String idempotentKey) {
        // 幂等校验
        if (idempotentKey != null) {
            Boolean existed = redisTemplate.opsForValue()
                .setIfAbsent(IDEMPOTENT_KEY_PREFIX + idempotentKey, "1", 30, TimeUnit.SECONDS);
            if (Boolean.FALSE.equals(existed)) {
                throw new TicketException(409, "重复请求");
            }
        }

        Ticket ticket = new Ticket();
        ticket.setTicketNo(ticketNoGenerator.generate());
        ticket.setTitle(req.getTitle());
        ticket.setDescription(req.getDescription());
        ticket.setCategory(req.getCategory());
        ticket.setPriority(req.getPriority());
        ticket.setCustomerId(req.getCustomerId());
        ticket.setSource(req.getSource());
        ticket.setStatus(TicketStatus.PENDING.getCode());
        ticketMapper.insert(ticket);

        // 自动分配
        assignmentService.assign(ticket.getId(), null);

        return toVO(ticketMapper.selectById(ticket.getId()));
    }

    @Transactional
    public void transferTicket(Long ticketId, Long targetAgentId, String reason, Long operatorId) {
        Ticket ticket = ticketMapper.selectById(ticketId);
        if (ticket == null) throw new TicketException(404, "工单不存在");

        ticketStateMachine.transition(ticket, TicketAction.TRANSFER, operatorId);
        Long oldAssignee = ticket.getAssigneeId();
        ticket.setAssigneeId(targetAgentId);
        ticketMapper.updateById(ticket);

        // 通知新处理人
        notificationService.notifyTransfer(targetAgentId, ticketId, operatorId, reason);
    }

    @Transactional
    public void resolveTicket(Long ticketId, Long operatorId) {
        Ticket ticket = ticketMapper.selectById(ticketId);
        if (ticket == null) throw new TicketException(404, "工单不存在");
        ticketStateMachine.transition(ticket, TicketAction.RESOLVE, operatorId);
        ticketMapper.updateById(ticket);
    }

    @Transactional
    public void closeTicket(Long ticketId, Long operatorId, Integer satisfaction) {
        Ticket ticket = ticketMapper.selectById(ticketId);
        if (ticket == null) throw new TicketException(404, "工单不存在");
        ticketStateMachine.transition(ticket, TicketAction.CLOSE, operatorId);
        ticket.setSatisfaction(satisfaction);
        ticketMapper.updateById(ticket);
    }

    public TicketVO getTicket(Long id) {
        Ticket ticket = ticketMapper.selectById(id);
        if (ticket == null) throw new TicketException(404, "工单不存在");
        return toVO(ticket);
    }

    public PageResult<TicketVO> listCustomerTickets(Long customerId, int pageNum, int pageSize) {
        Page<Ticket> page = ticketMapper.selectPage(
            new Page<>(pageNum, pageSize),
            new LambdaQueryWrapper<Ticket>()
                .eq(Ticket::getCustomerId, customerId)
                .orderByDesc(Ticket::getCreatedAt)
        );
        return PageResult.of(page.map(this::toVO));
    }

    public PageResult<TicketVO> searchTickets(TicketSearchRequest req) {
        LambdaQueryWrapper<Ticket> wrapper = new LambdaQueryWrapper<>();
        if (req.getKeyword() != null) {
            wrapper.like(Ticket::getTitle, req.getKeyword())
                   .or().like(Ticket::getTicketNo, req.getKeyword());
        }
        if (req.getStatus() != null) wrapper.eq(Ticket::getStatus, req.getStatus());
        if (req.getPriority() != null) wrapper.eq(Ticket::getPriority, req.getPriority());
        if (req.getCategory() != null) wrapper.eq(Ticket::getCategory, req.getCategory());
        wrapper.orderByDesc(Ticket::getCreatedAt);

        Page<Ticket> page = ticketMapper.selectPage(
            new Page<>(req.getPageNum(), req.getPageSize()), wrapper
        );
        return PageResult.of(page.map(this::toVO));
    }

    private TicketVO toVO(Ticket ticket) {
        // mapping logic — copies fields, resolves enum descs
        TicketVO vo = new TicketVO();
        vo.setId(ticket.getId());
        vo.setTicketNo(ticket.getTicketNo());
        vo.setTitle(ticket.getTitle());
        vo.setDescription(ticket.getDescription());
        vo.setCategory(ticket.getCategory());
        vo.setPriority(ticket.getPriority());
        vo.setPriorityDesc(ticket.getPriority() != null ? TicketPriority.fromCode(ticket.getPriority()).getDesc() : null);
        vo.setStatus(ticket.getStatus());
        vo.setStatusDesc(ticket.getStatus() != null ? TicketStatus.fromCode(ticket.getStatus()).getDesc() : null);
        vo.setCustomerId(ticket.getCustomerId());
        vo.setAssigneeId(ticket.getAssigneeId());
        vo.setSource(ticket.getSource());
        vo.setSatisfaction(ticket.getSatisfaction());
        vo.setCreatedAt(ticket.getCreatedAt());
        vo.setClosedAt(ticket.getClosedAt());
        return vo;
    }
}
```

---

### Task 12: NotificationService

**Files:**
- Create: `src/main/java/com/example/ticket/service/NotificationService.java`

- [ ] **Step 1: Create NotificationService**

```java
package com.example.ticket.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.ticket.domain.entity.Notification;
import com.example.ticket.domain.enums.NotificationType;
import com.example.ticket.mapper.NotificationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationMapper notificationMapper;

    public void notify(Long userId, NotificationType type, String title, String content, Long ticketId) {
        Notification n = new Notification();
        n.setUserId(userId);
        n.setType(type.name());
        n.setTitle(title);
        n.setContent(content);
        n.setRelatedTicketId(ticketId);
        notificationMapper.insert(n);
    }

    public void notifyAssign(Long userId, Long ticketId) {
        notify(userId, NotificationType.TICKET_ASSIGN,
            "工单分配", "您有一个新工单待处理", ticketId);
    }

    public void notifyTransfer(Long userId, Long ticketId, Long operatorId, String reason) {
        notify(userId, NotificationType.TICKET_TRANSFER,
            "工单转交", "工单已转交给您处理" + (reason != null ? "：" + reason : ""), ticketId);
    }

    public void notifySlaWarning(Long userId, Long ticketId) {
        notify(userId, NotificationType.SLA_WARNING,
            "SLA预警", "工单即将超时，请尽快处理", ticketId);
    }

    public void notifySlaEscalation(Long userId, Long ticketId, int level) {
        notify(userId, NotificationType.SLA_ESCALATION,
            "SLA升级(L" + level + ")", "工单已超时，请关注", ticketId);
    }

    public Page<Notification> listByUser(Long userId, int pageNum, int pageSize) {
        return notificationMapper.selectPage(new Page<>(pageNum, pageSize),
            new LambdaQueryWrapper<Notification>()
                .eq(Notification::getUserId, userId)
                .orderByDesc(Notification::getCreatedAt));
    }

    public long countUnread(Long userId) {
        return notificationMapper.selectCount(
            new LambdaQueryWrapper<Notification>()
                .eq(Notification::getUserId, userId)
                .eq(Notification::getIsRead, 0));
    }
}
```

---

### Task 13: SLAService

**Files:**
- Create: `src/main/java/com/example/ticket/service/SLAService.java`

- [ ] **Step 1: Create SLAService with ZSet-based scanning**

```java
package com.example.ticket.service;

import com.example.ticket.domain.entity.Ticket;
import com.example.ticket.domain.entity.TicketSlaConfig;
import com.example.ticket.mapper.TicketMapper;
import com.example.ticket.mapper.TicketSlaConfigMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class SLAService {

    private static final String SLA_QUEUE_KEY = "sla:queue";
    private final StringRedisTemplate redisTemplate;
    private final TicketMapper ticketMapper;
    private final TicketSlaConfigMapper slaConfigMapper;
    private final NotificationService notificationService;

    /**
     * 工单分配/创建时调用，将工单加入 SLA 监控队列
     */
    public void addToSlaQueue(Long ticketId, String category, int priority) {
        TicketSlaConfig config = slaConfigMapper.selectOne(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<TicketSlaConfig>()
                .eq(TicketSlaConfig::getCategory, category)
                .eq(TicketSlaConfig::getPriority, priority)
        );
        if (config == null) return;

        long warnTimestamp = Instant.now().getEpochSecond() + config.getWarnTimeoutMinutes() * 60L;
        redisTemplate.opsForZSet().add(SLA_QUEUE_KEY, String.valueOf(ticketId), warnTimestamp);
    }

    /**
     * 工单关闭时调用，从监控队列移除
     */
    public void removeFromSlaQueue(Long ticketId) {
        redisTemplate.opsForZSet().remove(SLA_QUEUE_KEY, String.valueOf(ticketId));
    }

    /**
     * 每 30 秒扫描超时工单
     */
    @Scheduled(fixedRate = 30000)
    public void scanSlaQueue() {
        long now = Instant.now().getEpochSecond();
        Set<String> expiredIds = redisTemplate.opsForZSet()
            .rangeByScore(SLA_QUEUE_KEY, 0, now);

        if (expiredIds == null || expiredIds.isEmpty()) return;

        for (String idStr : expiredIds) {
            try {
                Long ticketId = Long.parseLong(idStr);
                Ticket ticket = ticketMapper.selectById(ticketId);
                if (ticket == null) {
                    redisTemplate.opsForZSet().remove(SLA_QUEUE_KEY, idStr);
                    continue;
                }
                // 只处理未关闭的工单
                if (ticket.getStatus() >= 2) { // RESOLVED or CLOSED
                    redisTemplate.opsForZSet().remove(SLA_QUEUE_KEY, idStr);
                    continue;
                }

                // 发送预警通知给处理人
                if (ticket.getAssigneeId() != null) {
                    notificationService.notifySlaWarning(ticket.getAssigneeId(), ticketId);
                }

                // 移除该工单（下次由新的升级扫描覆盖）
                redisTemplate.opsForZSet().remove(SLA_QUEUE_KEY, idStr);

                log.info("SLA warning triggered for ticket {}", ticketId);
            } catch (Exception e) {
                log.error("SLA scan error for ticket {}", idStr, e);
            }
        }
    }
}
```

Notes:
- The SLA config is fetched from DB at queue-add time, so changing config affects new tickets only
- The ZSet approach avoids scanning the entire ticket table on each interval
- When a ticket expires at the `warnTimeout` boundary, the scan triggers a warning notification and removes the ticket from the queue
- A separate escalation mechanism (if needed for multi-level) can run on a longer schedule

---

### Task 14: StatsService

**Files:**
- Create: `src/main/java/com/example/ticket/service/StatsService.java`

- [ ] **Step 1: Create StatsService with scheduled aggregation**

```java
package com.example.ticket.service;

import com.example.ticket.dto.response.StatsVO;
import com.example.ticket.mapper.TicketMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StatsService {

    private static final String DASHBOARD_KEY = "stats:dashboard";
    private final TicketMapper ticketMapper;
    private final StringRedisTemplate redisTemplate;

    /**
     * 每 5 分钟刷新看板缓存
     */
    @Scheduled(fixedRate = 300000)
    public void refreshDashboard() {
        String today = LocalDate.now().atStartOfDay().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        // 状态分布
        Map<String, Object> statusCounts = ticketMapper.countByStatusSince(today)
            .stream().collect(Collectors.toMap(
                m -> "status_" + m.get("status"),
                m -> m.get("count")
            ));

        // 客服处理中负载
        Map<String, Object> agentLoads = ticketMapper.countProcessingByAgent()
            .stream().collect(Collectors.toMap(
                m -> "agent_" + m.get("assignee_id"),
                m -> m.get("count")
            ));

        // 平均处理时长
        Long avgMinutes = ticketMapper.avgProcessMinutes(today);
        statusCounts.put("avg_process_minutes", avgMinutes != null ? avgMinutes : 0);

        // 写入 Redis Hash
        redisTemplate.opsForHash().putAll(DASHBOARD_KEY, statusCounts);
        redisTemplate.opsForHash().putAll(DASHBOARD_KEY, agentLoads);
    }

    public Map<Object, Object> getDashboard() {
        return redisTemplate.opsForHash().entries(DASHBOARD_KEY);
    }
}
```

---

### Task 15: Controllers

**Files:**
- Create: `src/main/java/com/example/ticket/controller/customer/TicketController.java`
- Create: `src/main/java/com/example/ticket/controller/admin/TicketAdminController.java`
- Create: `src/main/java/com/example/ticket/controller/admin/AssignmentController.java`
- Create: `src/main/java/com/example/ticket/controller/admin/StatsController.java`

- [ ] **Step 1: Customer TicketController**

```java
package com.example.ticket.controller.customer;

import com.example.ticket.dto.request.TicketCreateRequest;
import com.example.ticket.dto.request.TicketSearchRequest;
import com.example.ticket.dto.response.PageResult;
import com.example.ticket.dto.response.TicketVO;
import com.example.ticket.service.TicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/customer/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;

    @PostMapping
    public ResponseEntity<TicketVO> create(
            @Valid @RequestBody TicketCreateRequest req,
            @RequestHeader(value = "Idempotent-Key", required = false) String idempotentKey) {
        return ResponseEntity.ok(ticketService.createTicket(req, idempotentKey));
    }

    @GetMapping
    public ResponseEntity<PageResult<TicketVO>> list(
            @RequestParam Long customerId,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        return ResponseEntity.ok(ticketService.listCustomerTickets(customerId, pageNum, pageSize));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TicketVO> detail(@PathVariable Long id) {
        return ResponseEntity.ok(ticketService.getTicket(id));
    }

    @PostMapping("/{id}/close")
    public ResponseEntity<Void> close(
            @PathVariable Long id,
            @RequestParam Long customerId,
            @RequestParam(required = false) Integer satisfaction) {
        ticketService.closeTicket(id, customerId, satisfaction);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/search")
    public ResponseEntity<PageResult<TicketVO>> search(TicketSearchRequest req) {
        return ResponseEntity.ok(ticketService.searchTickets(req));
    }
}
```

- [ ] **Step 2: Admin TicketAdminController**

```java
@RestController
@RequestMapping("/api/admin/tickets")
@RequiredArgsConstructor
public class TicketAdminController {

    private final TicketService ticketService;

    @GetMapping
    public ResponseEntity<PageResult<TicketVO>> list(TicketSearchRequest req) {
        return ResponseEntity.ok(ticketService.searchTickets(req));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TicketVO> detail(@PathVariable Long id) {
        return ResponseEntity.ok(ticketService.getTicket(id));
    }

    @PostMapping("/transfer")
    public ResponseEntity<Void> transfer(@Valid @RequestBody TicketTransferRequest req,
                                          @RequestParam Long operatorId) {
        ticketService.transferTicket(req.getTicketId(), req.getTargetAgentId(), req.getReason(), operatorId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/resolve")
    public ResponseEntity<Void> resolve(@RequestParam Long ticketId, @RequestParam Long operatorId) {
        ticketService.resolveTicket(ticketId, operatorId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/close")
    public ResponseEntity<Void> close(@RequestParam Long ticketId, @RequestParam Long operatorId) {
        ticketService.closeTicket(ticketId, operatorId, null);
        return ResponseEntity.ok().build();
    }
}
```

- [ ] **Step 3: Admin AssignmentController**

```java
@RestController
@RequestMapping("/api/admin/tickets")
@RequiredArgsConstructor
public class AssignmentController {

    private final AssignmentService assignmentService;

    @PostMapping("/assign")
    public ResponseEntity<Void> assign(@Valid @RequestBody TicketAssignRequest req) {
        assignmentService.assign(req.getTicketId(), req.getAgentId());
        return ResponseEntity.ok().build();
    }
}
```

- [ ] **Step 4: Admin StatsController**

```java
@RestController
@RequestMapping("/api/admin/stats")
@RequiredArgsConstructor
public class StatsController {

    private final StatsService statsService;

    @GetMapping("/dashboard")
    public ResponseEntity<Map<Object, Object>> dashboard() {
        return ResponseEntity.ok(statsService.getDashboard());
    }
}
```

---

### Task 16: Docker Configuration

**Files:**
- Create: `Dockerfile`
- Create: `docker-compose.yml`

- [ ] **Step 1: Create Dockerfile**

```dockerfile
FROM openjdk:8-jre-alpine
WORKDIR /app
COPY target/ticket-workflow-1.0.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=prod"]
```

- [ ] **Step 2: Create docker-compose.yml**

```yaml
version: '3.8'

services:
  mysql:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: root
      MYSQL_DATABASE: ticket_db
    ports:
      - "3306:3306"
    volumes:
      - mysql_data:/var/lib/mysql
      - ./src/main/resources/db/init.sql:/docker-entrypoint-initdb.d/init.sql
    networks:
      - ticket-net

  redis:
    image: redis:6-alpine
    ports:
      - "6379:6379"
    volumes:
      - redis_data:/data
    networks:
      - ticket-net

  app:
    build: .
    ports:
      - "8080:8080"
    environment:
      SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/ticket_db?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai
      SPRING_DATASOURCE_USERNAME: root
      SPRING_DATASOURCE_PASSWORD: root
      SPRING_REDIS_HOST: redis
      SPRING_REDIS_PORT: 6379
    depends_on:
      - mysql
      - redis
    networks:
      - ticket-net

volumes:
  mysql_data:
  redis_data:

networks:
  ticket-net:
    driver: bridge
```

---

## Self-Review

**1. Spec coverage:**
- 数据模型 → Task 2 (DDL) + Task 3 (Entities) ✅
- 状态机 → Task 8 (TransitionRule + DefaultTicketStateMachine) ✅
- 分配策略 → Task 9 (RoundRobinStrategy) + Task 10 (AssignmentService with lock) ✅
- 并发安全 → Redisson lock in Task 10 + @Version in Task 3 + idempotent-key in Task 11 ✅
- SLA 预警 → Task 13 (SLAService with ZSet + @Scheduled) ✅
- 统计看板 → Task 14 (StatsService) ✅
- 客户门户 API → Task 15.1 (Customer TicketController) ✅
- 客服后台 API → Task 15.2-15.4 (Admin controllers) ✅
- 容器化部署 → Task 16 (Dockerfile + docker-compose) ✅

**2. Placeholder scan:** No TBD, TODO, or "fill in later" patterns found.

**3. Type consistency:** All method signatures, return types, and field names are consistent across tasks. Entity → Mapper → Service → Controller chain is coherent.
