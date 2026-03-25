# seckill-simple

一个课程作业友好的最简骨架：Spring Boot + Spring Data JPA + MySQL 主从 + Redis + Elasticsearch + Nginx。

## 1. 环境要求
- JDK 17+
- Maven 3.9+
- Docker（用于启动 MySQL 主从、Redis、Elasticsearch、后端和 Nginx）

## 2. 容器化启动
在项目根目录执行：

docker compose up -d

查看容器状态：

docker compose ps

访问地址：
- Nginx 网关（前端静态页面）：http://localhost
- 后端实例1直连：http://localhost:8081
- 后端实例2直连：http://localhost:8082
- MySQL 主库：localhost:3307
- MySQL 从库：localhost:3308
- Redis：localhost:6379
- Elasticsearch：http://localhost:9200
- 通过 Nginx 代理的 API 示例：http://localhost/api/products
- 商品详情缓存接口示例：http://localhost/api/products/1
- 商品搜索接口示例：http://localhost/api/search/products?keyword=键盘

## 3. 分布式特性说明

### 3.1 MySQL 读写分离
- 写库（主库）：mysql-master（server_id=1）
- 读库（从库）：mysql-slave（server_id=2, read_only=1）
- 应用通过动态数据源路由：
	- 默认走 WRITE
	- 标注 @ReadOnlyDataSource 的方法走 READ

读写探针接口：
- 写路由探针：GET http://localhost:8081/api/rw/write-host
- 读路由探针：GET http://localhost:8081/api/rw/read-host

### 3.2 Nginx 动静分离 + 负载均衡
Nginx 已配置动静分离：
- 静态资源路径 / 由 Nginx 直接读取 frontend 目录
- 动态接口路径 /api/ 转发到后端集群

Nginx 负载均衡算法切换：
- 配置文件：nginx/default.conf
- 默认轮询（round robin）
- 最少连接：取消 least_conn; 注释
- IP 哈希：取消 ip_hash; 注释
- 生效命令：docker compose restart nginx

### 3.3 Redis 商品详情缓存
Redis 缓存说明：
- 商品详情接口 GET /api/products/{productId} 使用 Redis 缓存（key 前缀 product:detail:）
- 秒杀扣库存后会自动删除对应商品详情缓存，确保读到最新库存

缓存异常防护策略：
- 缓存穿透：对不存在商品写入短期空值缓存（60s），避免重复穿透数据库
- 缓存击穿：热点 key 重建时使用互斥锁（setIfAbsent + 过期时间）
- 缓存雪崩：商品详情缓存使用基础 TTL + 随机抖动，错开大量 key 同时过期

### 3.4 Elasticsearch 商品搜索
- 搜索索引：products
- 启动后自动将 MySQL 商品数据同步到 Elasticsearch
- 秒杀写操作后会同步更新商品索引，保证库存搜索结果一致

搜索接口：
- GET http://localhost/api/search/products?keyword=键盘
- GET http://localhost/api/search/products（不传 keyword 返回全部索引商品）
- POST http://localhost/api/search/rebuild（手动重建索引）

## 4. 读写分离验证步骤
1. 读写路由验证
- 访问写探针：GET http://localhost:8081/api/rw/write-host
- 预期：route=WRITE，db.serverId=1，db.readOnly=0
- 访问读探针：GET http://localhost:8081/api/rw/read-host
- 预期：route=READ，db.serverId=2，db.readOnly=1

2. 主从复制验证
- 调用写接口：POST http://localhost/api/seckill/1?quantity=1
- 查看主库库存：
	docker compose exec -T mysql-master mysql -uroot -proot -D seckill_demo -N -e "SELECT stock FROM product WHERE id=1;"
- 查看从库库存：
	docker compose exec -T mysql-slave mysql -uroot -proot -D seckill_demo -N -e "SELECT stock FROM product WHERE id=1;"
- 预期：主库与从库库存一致

## 5. 秒杀下单功能（消息队列 + 幂等性 + 削峰填谷）

### 5.1 核心特性
- **消息队列异步处理**：使用 Kafka 削峰填谷，快速响应用户
- **幂等性保证**：同一用户同一商品只能秒杀一次，使用 Redis 防重
- **库存保护**：Redis 缓存扣减 + 数据库最终一致
- **分布式订单 ID**：雪花算法生成全局唯一订单 ID，支持分布式扩展

### 5.2 核心接口

#### POST /api/orders/seckill （秒杀下单）
**快速测试示例（支持 userId 查询参数）**
```bash
curl -X POST 'http://localhost/api/orders/seckill?userId=1' \
  -H "Content-Type: application/json" \
  -d '{"productId":1,"quantity":5}'
```

**PowerShell 示例**
```powershell
Invoke-RestMethod -Uri 'http://localhost/api/orders/seckill?userId=1' `
  -Method Post -ContentType 'application/json' `
  -Body '{"productId":1,"quantity":5}' | ConvertTo-Json -Depth 8
```

**响应示例**
```json
{
  "success": true,
  "message": "seckill order created",
  "data": {
    "id": 724517141545086976,
    "userId": 1,
    "productId": 1,
    "quantity": 5,
    "price": 399.00,
    "status": 0,
    "createdAt": "2026-03-21T10:15:30",
    "updatedAt": "2026-03-21T10:15:30"
  }
}
```

**幂等性测试**：相同用户下单同一商品，返回已进行的订单（ID 相同）

#### GET /api/orders/user/{userId} （按用户查询订单）
```bash
curl http://localhost/api/orders/user/1
```

#### GET /api/orders/product/{productId} （按商品查询订单）
```bash
curl http://localhost/api/orders/product/1
```

#### GET /api/orders/{orderId} （按订单ID查询）
```bash
curl http://localhost/api/orders/724517141545086976
```

### 5.3 秒杀流程图
```
用户请求
  ↓
秒杀接口 (OrderController.seckillOrder)
  ↓
检查幂等性 (Redis key: order:idempotent:{userId}:{productId})
  ├─ 已存在 → 返回已有订单 ✓
  └─ 不存在 → 继续
  ↓
检查库存（Redis 缓存）
  ├─ 库存不足 → 返回错误信息 ✗
  └─ 库存充足 → 继续
  ↓
扣减 Redis 库存
  ↓
生成雪花 ID 创建本地订单
  ↓
发送 Kafka 消息 → seckill-orders 主题
  ↓
快速返回给用户
  ↓
后端异步消费 (SeckillOrderConsumer)
  ├─ 消费 Kafka 消息
  ├─ 更新订单状态为 success
  └─ 扣减数据库库存（最终一致性）
```

### 5.4 流量削峰示意
```
高峰期秒杀流量
    │
    ├─ 请求 1 → Kafka 队列 ↓
    ├─ 请求 2 → Kafka 队列 ↓ 均衡消费
    ├─ 请求 3 → Kafka 队列↓
    ├─ 请求 4 → Kafka 队列↓
    ...
    
消息队列缓解后端压力，控制数据库写入速率
```

### 5.5 验证工作流

**1. 注册两个用户**
```bash
curl -X POST http://localhost/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"user1","password":"pwd123","email":"u1@test.com"}'

curl -X POST http://localhost/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"user2","password":"pwd123","email":"u2@test.com"}'
```

**2. 商品列表**
```bash
curl http://localhost/api/products | grep -E '"id"|"name"|"stock"'
```

**3. 两个用户同时秒杀**
```bash
# 用户 1 秒杀商品 1，数量 10
curl -X POST 'http://localhost/api/orders/seckill?userId=1' \
  -H "Content-Type: application/json" \
  -d '{"productId":1,"quantity":10}'

# 用户 2 秒杀商品 1，数量 15
curl -X POST 'http://localhost/api/orders/seckill?userId=2' \
  -H "Content-Type: application/json" \
  -d '{"productId":1,"quantity":15}'
```

**4. 幂等性验证（用户 1 再秒杀商品 1）**
```bash
curl -X POST 'http://localhost/api/orders/seckill?userId=1' \
  -H "Content-Type: application/json" \
  -d '{"productId":1,"quantity":5}'
  
# 返回相同订单 ID，说明幂等性生效
```

**5. 查询用户订单**
```bash
curl http://localhost/api/orders/user/1
curl http://localhost/api/orders/user/2
```

**6. 观察消息队列处理**
```bash
# 查看后端消费日志（应该看到异步消息处理）
docker logs seckill-backend-1 | grep "Consume seckill order"
```

### 5.6 选做：订单分库分表（ShardingSphere-Proxy）
- 方案：ShardingSphere-Proxy
- 分库规则：按 user_id 分库，ds${user_id % 2}
- 分表规则：按订单ID高位分表，orders_${(id / 4096) % 2}
- 逻辑库名：seckill_proxy_db

关键配置文件：
- shardingsphere-proxy/conf/server.yaml
- shardingsphere-proxy/conf/config-sharding.yaml

初始化脚本：
- mysql/master/init/03-init-sharding.sql

验证命令：
```bash
# 创建订单（不同 userId）
curl -X POST 'http://localhost/api/orders/seckill?userId=121' -H "Content-Type: application/json" -d '{"productId":2,"quantity":1}'
curl -X POST 'http://localhost/api/orders/seckill?userId=122' -H "Content-Type: application/json" -d '{"productId":2,"quantity":1}'

# 查看四个物理分片表数据量
docker exec seckill-mysql-master mysql -uroot -proot -e "SELECT 'seckill_demo.orders_0' AS shard, COUNT(*) AS cnt FROM seckill_demo.orders_0 UNION ALL SELECT 'seckill_demo.orders_1', COUNT(*) FROM seckill_demo.orders_1 UNION ALL SELECT 'seckill_demo_1.orders_0', COUNT(*) FROM seckill_demo_1.orders_0 UNION ALL SELECT 'seckill_demo_1.orders_1', COUNT(*) FROM seckill_demo_1.orders_1;"
```

---

## 6. 本地开发启动（可选）
如果只想本地跑后端，也可以执行：

mvn spring-boot:run

## 7. 基础接口测试
查询商品：

GET http://localhost/api/products

新增商品：

POST http://localhost/api/products

```json
{
	"name": "游戏手柄",
	"stock": 66,
	"price": 239.00
}
```

用户注册：

POST http://localhost/api/auth/register

```json
{
	"username": "alice",
	"email": "alice@example.com",
	"password": "123456"
}
```

用户登录：

POST http://localhost/api/auth/login

```json
{
	"username": "alice",
	"password": "123456"
}
```

秒杀扣减库存（productId=1，扣减 1 件）：

POST http://localhost/api/seckill/1?quantity=1

## 8. 说明
- 使用 JPA 悲观锁避免并发下超卖。
- 用户密码使用 BCrypt 加密后存储，不明文入库。
- 秒杀订单使用 Kafka 异步处理，消息队列路由键为 `{userId}_{productId}` 支持顺序消费。
- 订单 ID 由雪花算法生成，无需依赖 MySQL 自增，支持分布式部署。
