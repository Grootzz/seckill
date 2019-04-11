# Redis的集成与Redis的作用

[TOC]

本项目中，redis安装于linux。

## redis的集成

本项目中使用redis作为数据缓存。

### 添加Jedis依赖

因为需要使用在spring boot中使用redis，所以引入Jedis依赖。

```xml
<dependency>
    <groupId>redis.clients</groupId>
    <artifactId>jedis</artifactId>
</dependency>
```

### 添加Fastjson依赖

`redis`在本项目中用做缓存，为了可以对对象缓存，我们将对象转化为`json`字符串的形式存储在`redis`中。Fastjson是一个用于在`json`字符串和对象之间相互转换的工具。

```xml
<dependency>
    <groupId>com.alibaba</groupId>
    <artifactId>fastjson</artifactId>
    <version>1.2.38</version>
</dependency>
```

### 在Spring Boot中配置redis

在`application.properties`引入如下配置，并且，通过`@ConfigurationProperties(prefix = "redis")`将属性文件中配置的参数封装到`edu.uestc.redis.RedisConfig`中。

```properties
redis.host=192.168.216.128
redis.port=6379
redis.timeout=100
redis.poolMaxTotal=1000
redis.poolMaxIdle=500
redis.poolMaxWait=500
```

为了更好的区分redis中的键，我们对每一个缓存在redis中的键都加了一个前缀，前缀以类名开始，这样有利于在redis中区分不同的键来表示的数据内容。

![1554961334568](assets\1554961334568.png)

前缀的生成使用了模板设计模式，为不同的前缀提供默认的实现。

## Redis的作用

- **缓存用户请求随机秒杀地址的次数**；对用户获取秒杀地址的请求做拦截。一次防止用户频繁的向服务端发送获取秒杀地址的请求。具体过程为：用户第一次请求path的时候，在redis中存储一个记录请求次数的变量，且该变量有一定的过期时间，如果变量过期时间内访问次数大于最大的访问次数，则提示用户访问频繁，对用户的请求忽略，这样可以有效的减少对数据库的访问，也可以防止恶意用户在接口上不断地发出请求，让真正的用户请求得不到处理。
- **缓存商品信息**；具体为缓存商品列表页，缓存商品详情页，缓存商品库存（实现预减库存）。
- **缓存秒杀状态**；缓存商品是否已经秒杀结束，缓存随机的秒杀地址，缓存验证码。
- **缓存token**；缓存token，并对用户对象进行缓存（对象级缓存）。
- **缓存订单信息**。

可以看出，通常我们都会对一些不常改变的数据做一个缓存，在客户端和数据库之间加一个缓存，可以有效的减少对数据库的访问。

