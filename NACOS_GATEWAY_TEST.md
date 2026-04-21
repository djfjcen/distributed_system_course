# Nacos + Gateway 验证说明

## 1. 启动 Nacos 与后端服务

在项目目录执行：

```powershell
cd d:\distributed_system_course\seckill-simple
docker compose up -d nacos mysql-master mysql-slave mysql-replica-setup redis elasticsearch zookeeper kafka shardingsphere-proxy backend-1 backend-2
```

访问 Nacos 控制台：

- 地址: http://localhost:8848/nacos
- 默认账号: nacos
- 默认密码: nacos

检查服务列表中是否出现 `seckill-simple`。

## 2. 启动 Gateway

```powershell
cd d:\distributed_system_course\seckill-gateway
$env:NACOS_ADDR="127.0.0.1:8848"
.\mvnw spring-boot:run
```

如果本目录没有 mvnw，可用系统 Maven：

```powershell
mvn spring-boot:run
```

检查 Nacos 服务列表中是否出现 `seckill-gateway`。

## 3. 网关动态路由测试

通过网关调用后端服务（路由前缀 `/seckill`）：

```powershell
curl http://localhost:8088/seckill/api/products
```

预期：请求成功返回后端商品列表，说明 `lb://seckill-simple` 路由生效。

## 4. Nacos 配置动态更新测试

### 4.1 在 Nacos 创建配置

- Data ID: `seckill-simple.yaml`
- Group: `DEFAULT_GROUP`
- 配置内容：

```yaml
seckill:
  dynamic:
    message: hello-from-nacos-v1
```

发布后访问：

```powershell
curl http://localhost:8088/seckill/api/config/message
```

预期返回 `dynamicMessage=hello-from-nacos-v1`。

### 4.2 在线修改配置

把配置改为：

```yaml
seckill:
  dynamic:
    message: hello-from-nacos-v2
```

重新发布后再次请求：

```powershell
curl http://localhost:8088/seckill/api/config/message
```

预期无需重启服务，直接看到 `dynamicMessage=hello-from-nacos-v2`，说明动态刷新成功。
