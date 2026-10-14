# StaticWorld 微服务项目

> 一个基于 Spring Cloud Alibaba 的微服务项目。目前已完成 **用户模块（UserSystem）** 的开发，它是后续所有功能模块的核心与基础，负责统一身份认证、RBAC 权限、设备管理与登录审计。

## 项目结构

```
StaticWorld/
├── Doc/                    # 项目文档（数据库设计、设备信息说明等）
├── StaticWorldService/     # 后端微服务（Maven 多模块）
│   ├── common/             # 公共模块：统一返回、异常处理、JWT、身份传递、通用实体
│   ├── UserSystem/         # 用户服务（核心业务模块，端口 8081）
│   ├── service-api/        # 服务接口模块：OpenFeign 客户端接口定义
│   └── service-gateway/    # 网关服务（端口 8080，统一鉴权与路由）
└── UserSystemWeb/          # 前端（Vue 3 + Vite + Element Plus）
```

## 技术栈

- **语言/框架**：Java 17、Spring Boot 3.2、Spring Cloud 2023.0、Spring Cloud Alibaba
- **注册/配置中心**：Nacos（服务发现 + 配置管理）
- **通信**：Spring Cloud Gateway（网关）、OpenFeign（服务间调用）、LoadBalancer（负载均衡）
- **数据层**：MyBatis-Plus、PostgreSQL、Redis
- **安全**：Spring Security（RBAC 方法级鉴权）、JWT 双 Token（短 Token + 长 Token 滑动过期）
- **存储/通知**：RustFS（S3 兼容对象存储）、SMTP 邮件验证码
- **前端**：Vue 3、Vite、Element Plus、Pinia、Vue Router、device-fingerprint-js

## 用户模块功能

| 分类       | 说明                                                         |
| :--------- | :----------------------------------------------------------- |
| 注册与登录 | 邮箱注册、验证码登录、密码登录、Token 刷新、退出登录         |
| 用户管理   | 用户信息查询/修改、用户状态管理、用户分页列表                |
| 角色与权限 | 角色管理、权限管理、用户-角色分配、角色-权限分配、前端权限同步（RBAC） |
| 设备管理   | 设备指纹采集、设备信息查询、用户设备绑定与启用/禁用          |
| 登录审计   | 登录日志记录（IP、时区、经纬度、登录方式与结果）             |
| 文件上传   | 头像上传（RustFS / S3 兼容存储）                             |

## 快速开始

### 环境要求

- JDK 17+
- Maven 3.6+
- PostgreSQL
- Redis
- Nacos（服务注册与配置中心）
- RustFS 或任意 S3 兼容对象存储
- 可用的 SMTP 邮箱账号

### 构建与运行

1. 安装公共依赖到本地仓库：

   ```bash
   mvn clean install
   ```

2. 启动依赖服务（Nacos、PostgreSQL、Redis、RustFS），并在 Nacos 中配置 `common.yaml`（含 `jwt.secret`、`jwt.refresh-secret`、`rustfs.access-key`、`rustfs.secret-key` 等密钥）。

3. 依次启动服务：

   ```bash
   # 用户服务（8081）
   mvn -pl UserSystem spring-boot:run
   
   # 网关服务（8080）
   mvn -pl service-gateway spring-boot:run
   ```

4. 启动前端：

   ```bash
   cd UserSystemWeb
   npm install
   npm run dev
   ```

## 配置说明

- 服务端口：网关 `8080`、用户服务 `8081`。
- 网关将 `/api/userservice/**` 路由转发至 `user-service`，并对请求做统一 JWT 鉴权；`/api/userservice/public/**` 为免鉴权路径。
- 密钥与敏感配置通过 Nacos `common.yaml` 注入，本地仅通过环境变量提供 `NACOS_PASSWORD`、`MAIL_PASSWORD`、`RUSTFS_REGION` 等。

## 相关文档

- [用户模块数据库设计](./Doc/用户模块-数据库设计.md)
- [浏览器获取设备信息](./Doc/浏览器获取设备信息.md)
