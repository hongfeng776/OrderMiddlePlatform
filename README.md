# 企业级分布式电商订单履约中台

基于 Spring Cloud Alibaba + Vue3 的微服务架构订单履约系统。

## 项目架构

```
order-middle-platform/
├── common-core/          # 公共核心模块
│   └── 统一返回结果、全局异常处理、业务异常等
├── user-service/         # 用户服务 (8001)
│   └── 用户登录、用户信息管理
├── inventory-service/    # 库存服务 (8002)
│   └── 商品库存查询、库存锁定/解锁
├── order-service/        # 订单服务 (8003)
│   └── 订单创建、订单查询、订单明细管理
├── payment-service/      # 支付服务 (8004)
│   └── 支付流水创建、支付处理
├── gateway-service/      # API网关 (8000)
│   └── 统一入口、服务聚合转发
├── frontend/             # 前端项目
│   └── Vue3 + Element Plus
└── sql/                  # 数据库脚本
```

## 技术栈

### 后端
- Spring Boot 2.7.18
- Spring Cloud Alibaba 2021.0.5.0
- Nacos (服务注册与配置中心)
- OpenFeign (服务间调用)
- MyBatis-Plus 3.5.3.1
- MySQL 8.0
- Druid 数据库连接池
- Hutool 工具类库
- Lombok

### 前端
- Vue 3
- Vue Router 4
- Vuex 4
- Element Plus
- Axios

## 核心功能

1. **用户服务**
   - 用户登录
   - 用户信息查询

2. **库存服务**
   - 商品库存列表查询
   - 商品库存详情
   - 库存锁定/解锁
   - 库存扣减

3. **订单服务**
   - 创建订单（下单时自动锁定库存）
   - 订单列表查询
   - 订单详情查询
   - 订单状态管理

4. **支付服务**
   - 创建支付流水
   - 模拟支付处理

5. **前端页面**
   - 登录页面
   - 首页（数据统计）
   - 订单列表
   - 创建订单
   - 个人中心

## 环境准备

### 1. 安装 Nacos

下载 Nacos 2.x 版本，启动命令：
```bash
# Windows
startup.cmd -m standalone

# Linux/Mac
sh startup.sh -m standalone
```

访问: http://localhost:8848/nacos

### 2. 数据库初始化

执行 `sql/init.sql` 脚本创建数据库和表：
```bash
mysql -u root -p < sql/init.sql
```

默认测试账号：
- 用户名: admin
- 密码: 123456

## 启动说明

### 后端启动

按以下顺序启动各个微服务：

1. **启动 Nacos**
   - 确保 Nacos 在 8848 端口正常运行

2. **启动 common-core**
   ```bash
   cd common-core
   mvn clean install
   ```

3. **启动用户服务 (8001)**
   ```bash
   cd user-service
   mvn spring-boot:run
   ```

4. **启动库存服务 (8002)**
   ```bash
   cd inventory-service
   mvn spring-boot:run
   ```

5. **启动订单服务 (8003)**
   ```bash
   cd order-service
   mvn spring-boot:run
   ```

6. **启动支付服务 (8004)**
   ```bash
   cd payment-service
   mvn spring-boot:run
   ```

7. **启动网关服务 (8000)**
   ```bash
   cd gateway-service
   mvn spring-boot:run
   ```

### 前端启动

```bash
cd frontend
npm install
npm run serve
```

访问: http://localhost:8080

## 接口说明

### 网关统一入口 (http://localhost:8000/api)

#### 用户接口
- POST /api/user/login - 用户登录
- GET /api/user/{id} - 获取用户信息

#### 库存接口
- GET /api/inventory/list - 库存列表
- GET /api/inventory/{productId} - 库存详情
- POST /api/inventory/lock - 锁定库存
- POST /api/inventory/unlock - 解锁库存

#### 订单接口
- POST /api/order/create - 创建订单
- GET /api/order/list/{userId} - 用户订单列表
- GET /api/order/{orderNo} - 订单详情

#### 支付接口
- POST /api/payment/create - 创建支付
- POST /api/payment/process/{payNo} - 处理支付

## 业务流程

1. **下单流程**
   - 用户选择商品
   - 填写收货信息
   - 提交订单
   - 订单服务调用库存服务锁定库存
   - 生成订单和订单明细

2. **支付流程**
   - 创建支付流水
   - 调用支付接口
   - 支付成功后更新订单状态
   - 扣减库存（释放锁定）

## 配置说明

各服务的数据库连接配置在 `src/main/resources/application-dev.yml` 中：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/order_platform?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=Asia/Shanghai
    username: root
    password: root
```

## 注意事项

1. 确保 MySQL 服务已启动，用户名密码正确
2. 确保 Nacos 服务已启动
3. 各服务按顺序启动，确保服务注册成功
4. 前端调用的是网关地址 (8000)，不是各微服务的端口

## 开发说明

- 公共模块 `common-core` 被所有微服务引用，修改后需要重新 `mvn install`
- 所有微服务都注册到 Nacos，通过 OpenFeign 进行服务间调用
- 数据库操作使用 MyBatis-Plus，提供基础 CRUD 能力
- 全局异常统一处理，返回标准的 Result 格式

## 后续优化方向

1. 引入 Spring Cloud Gateway 替代目前的 HTTP 聚合方式
2. 增加分布式事务支持（Seata）
3. 增加服务熔断降级（Sentinel）
4. 增加链路追踪（SkyWalking）
5. 增加消息队列（RocketMQ）
6. 增加缓存（Redis）
7. 完善权限认证（JWT）
8. 增加单元测试和集成测试
