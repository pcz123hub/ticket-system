# 智能客服工单处理系统

> 基于 Spring Boot + Vue 3 的工单流转系统，支持客户提单、客服分配处理、SLA 监控与统计看板。

[![Java](https://img.shields.io/badge/Java-17-blue)](https://adoptium.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-2.7.18-brightgreen)](https://spring.io/projects/spring-boot)
[![Vue](https://img.shields.io/badge/Vue-3.4-4FC08D)](https://vuejs.org/)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-orange)](https://mysql.com/)
[![Redis](https://img.shields.io/badge/Redis-6-red)](https://redis.io/)

---

## 功能特性

- **客户端**
  - 提交工单（支持幂等去重）
  - 查看我的工单列表与详情
  - 关闭工单并评分

- **管理端**
  - 工单列表检索（关键词、状态、优先级筛选）
  - 工单指派（自动轮询分配 / 手动指定）
  - 工单转交与解决
  - 客服人员管理
  - 统计看板（工单分布、客服负载、平均处理时长）

- **SLA 监控**
  - 基于优先级和业务类型的超时预警
  - 定时扫描临近超时工单并推送通知
  - 支持多级升级通知

- **技术特性**
  - 状态机驱动的工单流转（待分配 → 处理中 → 已解决 → 已关闭）
  - Redisson 分布式锁保障分配一致性
  - Redis 缓存工单处理人负载与统计看板
  - Idempotent-Key 接口幂等性
  - 操作日志与分配记录全链路审计

## 技术栈

| 层次        | 技术                    | 版本       |
| ----------- | ----------------------- | ---------- |
| 后端框架    | Spring Boot             | 2.7.18     |
| ORM         | MyBatis-Plus            | 3.5.5      |
| 数据库      | MySQL                   | 8.0        |
| 缓存        | Redis + Redisson        | 6 / 3.27.2 |
| 前端框架    | Vue 3                   | 3.4        |
| 构建工具    | Vite                    | 5.4        |
| UI 组件     | Element Plus            | 2.7        |
| 路由        | Vue Router              | 4.3        |
| HTTP 客户端 | Axios                   | 1.7        |
| JDK         | OpenJDK                 | 17         |
| 容器化      | Docker / Docker Compose | —          |

## 目录结构

```
├── pom.xml                          # Maven 后端构建
├── Dockerfile                       # 后端容器镜像
├── docker-compose.yml               # 一键启动 (MySQL + Redis + App)
├── src/main/
│   ├── java/com/example/ticket/
│   │   ├── TicketApplication.java   # 启动入口
│   │   ├── config/                  # MyBatis-Plus / Redis / Redisson 配置
│   │   ├── controller/
│   │   │   ├── customer/            # 客户 API
│   │   │   └── admin/               # 管理 API
│   │   ├── domain/
│   │   │   ├── entity/              # 数据实体
│   │   │   ├── enums/               # 状态 / 优先级 / 操作枚举
│   │   │   └── statemachine/        # 工单状态机
│   │   ├── dto/                     # 请求 / 响应 DTO
│   │   ├── mapper/                  # MyBatis-Plus Mapper
│   │   ├── service/                 # 业务逻辑
│   │   ├── strategy/                # 分配策略 (轮询)
│   │   ├── exception/               # 全局异常处理
│   │   └── util/                    # 工单号生成器
│   └── resources/
│       ├── application.yml          # 应用配置
│       ├── db/init.sql              # 数据库初始化
│       ├── db/seed.sql              # 测试数据
│       └── logback-spring.xml       # 日志配置
└── frontend/
    ├── package.json                 # 前端依赖
    ├── vite.config.js               # Vite 配置 (代理 /api → :8080)
    ├── index.html
    └── src/
        ├── main.js                  # 入口
        ├── App.vue
        ├── router/index.js          # 路由
        ├── api/                     # HTTP 请求封装
        ├── layouts/                 # 客户 / 管理布局
        └── views/
            ├── customer/            # 客户页面
            └── admin/               # 管理页面
```

## 快速开始

### 环境要求

- JDK 17+
- Maven 3.8+
- MySQL 8.0
- Redis 6+
- Node.js 18+ (前端)

### 1. 数据库初始化

```sql
-- 执行建表脚本
mysql -u root -p < src/main/resources/db/init.sql

-- (可选) 插入测试数据
mysql -u root -p ticket_db < src/main/resources/db/seed.sql
```

### 2. 修改数据库配置

编辑 `src/main/resources/application.yml`，修改数据库和 Redis 连接信息：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/ticket_db?...
    username: root
    password: your_password
  redis:
    host: localhost
    port: 6379
```

### 3. 启动后端

```bash
# 编译打包
mvn clean package -DskipTests

# 启动
java -jar target/ticket-workflow-1.0.0.jar
```

后端默认运行在 `http://localhost:8080`。

### 4. 启动前端

```bash
cd frontend
npm install
npm run dev
```

前端默认运行在 `http://localhost:3000`，已配置代理转发 `/api` 到后端。

### 5. 使用 Docker Compose 一键启动

```bash
docker-compose up -d
```

自动启动 MySQL 8.0、Redis 6 和应用服务，初始化建表脚本。

## API 概览

### 客户接口

| 方法 | 路径                                   | 说明           |
| ---- | -------------------------------------- | -------------- |
| POST | `/api/customer/tickets`                | 创建工单       |
| GET  | `/api/customer/tickets?customerId=xxx` | 我的工单列表   |
| GET  | `/api/customer/tickets/{id}`           | 工单详情       |
| POST | `/api/customer/tickets/{id}/close`     | 关闭工单并评分 |
| GET  | `/api/customer/tickets/search`         | 搜索工单       |

### 管理接口

| 方法 | 路径                          | 说明               |
| ---- | ----------------------------- | ------------------ |
| GET  | `/api/admin/tickets`          | 工单列表（含筛选） |
| GET  | `/api/admin/tickets/{id}`     | 工单详情           |
| POST | `/api/admin/tickets/assign`   | 指派工单给客服     |
| POST | `/api/admin/tickets/transfer` | 转交工单           |
| POST | `/api/admin/tickets/resolve`  | 标记为已解决       |
| POST | `/api/admin/tickets/close`    | 关闭工单           |
| GET  | `/api/admin/stats/dashboard`  | 统计看板           |

## 构建与部署

### 后端打包

```bash
mvn clean package -DskipTests
```

生成 `target/ticket-workflow-1.0.0.jar`。

### 前端构建

```bash
cd frontend
npm install
npm run build
```

产物输出到 `frontend/dist/`，可直接部署到 Nginx 等静态服务器。

### Docker 部署

```bash
# 构建后端镜像
docker build -t ticket-workflow .

# Docker Compose 启动全套服务
docker-compose up -d
```

## 许可证

[MIT](LICENSE)
