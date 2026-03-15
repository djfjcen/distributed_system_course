# seckill-simple

一个课程作业友好的最简骨架：Spring Boot + Spring Data JPA + MySQL。

## 1. 环境要求
- JDK 17+
- Maven 3.9+
- Docker（用于启动 MySQL）

## 2. 启动 MySQL
在项目根目录执行：

docker compose up -d

## 3. 启动应用
在项目根目录执行：

mvn spring-boot:run

应用默认端口：8080

## 4. 测试接口
查询商品：

GET http://localhost:8080/api/products

用户注册：

POST http://localhost:8080/api/auth/register

```json
{
	"username": "alice",
	"email": "alice@example.com",
	"password": "123456"
}
```

用户登录：

POST http://localhost:8080/api/auth/login

```json
{
	"username": "alice",
	"password": "123456"
}
```

秒杀扣减库存（productId=1，扣减 1 件）：

POST http://localhost:8080/api/seckill/1?quantity=1

## 5. 说明
- 使用 JPA 悲观锁避免并发下超卖。
- 用户密码使用 BCrypt 加密后存储，不明文入库。
- 当前是教学最小实现，后续可以加 Redis 与 MQ 做削峰和异步下单。
