# seckill-simple

一个课程作业友好的最简骨架：Spring Boot + Spring Data JPA + MySQL + Redis + Nginx。

## 1. 环境要求
- JDK 17+
- Maven 3.9+
- Docker（用于启动 MySQL、Redis、后端和 Nginx）

## 2. 容器化启动（推荐）
在项目根目录执行：

docker compose up -d

查看容器状态：

docker compose ps

访问地址：
- Nginx 网关（前端静态页面）：http://localhost
- 后端实例1直连：http://localhost:8081
- 后端实例2直连：http://localhost:8082
- 通过 Nginx 代理的 API 示例：http://localhost/api/products
- 商品详情缓存接口示例：http://localhost/api/products/1

Nginx 已配置动静分离：
- 静态资源路径 / 由 Nginx 直接读取 frontend 目录
- 动态接口路径 /api/ 转发到后端集群

Redis 缓存说明：
- 商品详情接口 GET /api/products/{productId} 使用 Redis 缓存（缓存名 productDetail）
- 秒杀扣库存后会自动删除对应商品详情缓存，确保读到最新库存

缓存异常防护策略：
- 缓存穿透：对不存在商品写入短期空值缓存（60s），避免重复穿透数据库
- 缓存击穿：热点 key 重建时使用互斥锁（setIfAbsent + 过期时间）
- 缓存雪崩：商品详情缓存使用基础 TTL + 随机抖动，错开大量 key 同时过期

## 3. Nginx 负载均衡算法切换
Nginx 配置文件在 [nginx/default.conf](nginx/default.conf)。

默认使用轮询（round robin）。

如果要切换算法：
- 最少连接：取消 `least_conn;` 注释
- IP 哈希：取消 `ip_hash;` 注释

切换后重启 Nginx：

docker compose restart nginx

## 4. 本地开发启动（可选）
如果只想本地跑后端，也可以执行：

mvn spring-boot:run

## 5. 测试接口
查询商品：

GET http://localhost/api/products

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

## 6. 说明
- 使用 JPA 悲观锁避免并发下超卖。
- 用户密码使用 BCrypt 加密后存储，不明文入库。
- 当前是教学最小实现，后续可以加 Redis 与 MQ 做削峰和异步下单。
