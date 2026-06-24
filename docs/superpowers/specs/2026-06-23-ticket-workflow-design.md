# 智能客服工单处理系统 — 工单流转模块设计

> 日期：2026-06-23
> 状态：已定稿

## 1. 概述

智能客服工单处理系统，实现客户问题提交、智能分派、工单流转、处理反馈的全流程闭环管理。本文档聚焦**工单流转核心模块**的后端设计。

### 技术栈

- Spring Boot（后端框架）
- MyBatis-Plus（ORM）
- MySQL（持久化）
- Redis + Redisson（缓存、分布式锁）
- Vue 3 + Element-Plus（前端）
- Docker（容器化部署）

## 2. 系统架构

经典分层架构：

```
Controller 层      — TicketController / AssignmentController / StatsController
Service 层         — TicketService / AssignmentService / SLAService / StatsService / NotificationService
Domain 层          — TicketStateMachine / AssignmentStrategy / TicketFactory
Infrastructure 层  — Mapper / Redis / Redisson / @Scheduled（定时任务）
```

## 3. 数据模型

### 3.1 ticket（工单主表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | bigint PK | |
| ticket_no | varchar | 唯一工单号 |
| title | varchar | 标题 |
| description | text | 描述 |
| category | varchar | 业务类型（投诉/咨询/售后…） |
| priority | tinyint | 优先级：0=P0 / 1=P1 / 2=P2 |
| status | tinyint | 状态：PENDING / PROCESSING / RESOLVED / CLOSED / TRANSFERRED |
| customer_id | bigint | 客户ID |
| assignee_id | bigint | 当前处理人ID |
| source | varchar | 来源（门户/客服录入） |
| satisfaction | tinyint | 满意度评分（1-5） |
| version | int | 乐观锁版本号 |
| created_at / updated_at / closed_at | datetime | 时间戳 |

### 3.2 ticket_log（操作日志）

ticket_id + from_status + to_status + operator_id + remark + created_at

### 3.3 ticket_sla_config（SLA 规则配置）

category + priority + warn_timeout_minutes + escalate_timeout_minutes + escalate_level

### 3.4 ticket_assignment_log（分配记录）

ticket_id + from_agent_id + to_agent_id + strategy + reason + created_at

### 3.5 ticket_sla_escalation（升级记录）

ticket_id + escalation_level + notified_at + resolved_at

### 3.6 notification（站内通知）

user_id + type（预警/升级/分配/转交）+ title + content + related_ticket_id + is_read + created_at

### 3.7 cs_agent（客服人员）

name + skills（标签）+ status（在线/离线）+ max_load

客服当前负载存 Redis，不持久化到数据库。

## 4. 状态机设计

### 状态枚举

- PENDING（待分配）
- PROCESSING（处理中）
- RESOLVED（已解决）
- CLOSED（已关闭）
- TRANSFERRED（已转交）

### 流转规则

| 操作 | 起始状态 | 目标状态 | 说明 |
|------|----------|----------|------|
| 分配 | PENDING | PROCESSING | 自动/手动分配后 |
| 转交 | PROCESSING | PROCESSING | assignee 变更，状态不变 |
| 解决 | PROCESSING | RESOLVED | 客服标记解决 |
| 关闭 | PENDING | CLOSED | 取消工单 |
| 关闭 | RESOLVED | CLOSED | 客户确认/超时自动关闭 |

### 代码设计

```java
interface TicketStateMachine {
    boolean transition(Ticket ticket, TicketAction action, Long operatorId);
    List<TicketAction> availableActions(Ticket ticket);
}
```

不同工单类型可通过 `@ConditionalOnProperty` 注入不同实现（如 ComplaintStateMachine, ConsultationStateMachine）。

## 5. 分配策略

### 分配流程

1. 接收分配请求
2. 获取 Redisson 分布式锁（key: `ticket:assign:锁`）
3. 筛选在线客服 → 轮询分配
4. 更新 ticket.assignee_id + Redis 负载计数
5. 释放锁 + 记录分配日志

### 策略接口

```java
interface AssignmentStrategy {
    Long selectAgent(Ticket ticket, List<Agent> candidates);
}
```

首期实现 RoundRobinStrategy（Redis AtomicLong 计数器取模），预留扩展点。

### 手动改派

管理员可直接调用改派接口，同一把分布式锁防并发。

## 6. 并发安全

| 机制 | 场景 | 实现 |
|------|------|------|
| 分布式锁 | 分配/转交/改派 | Redisson，key 粒度到工单 |
| 乐观锁 | 所有状态变更 | MyBatis-Plus `@Version`，UPDATE WHERE version=old |
| 幂等校验 | 关键写接口 | Redis SETNX（30s 过期）+ 状态机校验 |

## 7. SLA 时效预警

### SLA 阈值（示例，配置化）

| 优先级 | 预警 | 升级 | 通知对象 |
|--------|------|------|----------|
| P0 | 30min | 60min | 主管+经理 |
| P1 | 4h | 8h | 主管 |
| P2 | 24h | 48h | 处理人 |

### 扫描方案（Redis ZSet）

- 工单创建/分配时，计算截止时间戳写入 ZSet（`ZADD sla:queue 时间戳 ticket_id`）
- `@Scheduled` 每 30s 扫描（`ZRANGEBYSCORE sla:queue 0 当前时间戳`）
- 命中的工单检查状态 → 触发升级通知
- 工单关闭后从 ZSet 移除（`ZREM`）

### 升级链路

处理人（逾期未处理）→ 主管（L1 升级）→ 经理（L2 升级）

## 8. 统计看板

### 数据维度

- 客服工作量（处理数、当前负载、平均处理时长）
- 满意度评分（平均分趋势、客服分布）
- 处理时效分布（按优先级、SLA 达标率）
- 工单状态概览（各状态数量、新增/关闭趋势）

### 数据刷新

- 每 5 分钟：汇总写入 Redis Hash（`stats:dashboard`）
- 每天凌晨 2 点：日维度数据持久化到 `ticket_stats` 表
- 前端直接读取 Redis 缓存

## 9. API 接口

### 客户门户（`/api/customer/`）

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /tickets | 提交工单 |
| GET | /tickets | 我的工单列表（分页+排序） |
| GET | /tickets/{id} | 工单详情+进度 |
| POST | /tickets/{id}/close | 确认关闭 |
| GET | /tickets/search | 多条件查询 |

### 客服后台（`/api/admin/`）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /tickets | 工单列表（全量+筛选） |
| GET | /tickets/{id} | 工单详情 |
| POST | /tickets/assign | 分配/改派 |
| POST | /tickets/transfer | 转交 |
| POST | /tickets/resolve | 标记已解决 |
| POST | /tickets/close | 强制关闭 |
| GET | /stats/dashboard | 看板数据（读Redis） |
| GET | /stats/history | 历史统计数据 |
| GET | /agents | 客服列表+负载 |

## 10. 容器化部署

- Docker Compose 编排：MySQL + Redis + 业务服务
- 各服务独立容器，通过 Compose 网络互通
- 配置外挂卷持久化数据库和缓存数据

## 11. 未入选功能（YAGNI）

以下功能经评估未纳入本次设计，后续按需引入：

- 外部 IM 通知（企微/钉钉/飞书）
- 加权轮询分配（技能标签+负载）
- 邮件/短信通知
- 实时 WebSocket 推送
