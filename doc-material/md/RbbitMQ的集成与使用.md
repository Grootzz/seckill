# RbbitMQ的集成与使用

[TOC]

## RabbitMQ安装与Spring Boot集成

RabbitMQ是一个开源的消息代理软件（面向消息的中间件），它的核心作用就是创建消息队列，异步接收和发送消息，MQ的全程是：Message Queue中文的意思是消息队列。本项目中，其用于串行化秒杀请求。

RabbitMQ与Spring Boot的集成：

引用amqp的starter

```xml
<dependency>
	<groupId>org.springframework.boot</groupId>
	<artifactId>spring-boot-starter-amqp</artifactId>
</dependency>
```

在Spring Boot配置文件中配置RabbitMQ

```properties
#rabbitmq
spring.application.name=springboot-rabbitmq
spring.rabbitmq.host=192.168.216.128
spring.rabbitmq.port=5672
spring.rabbitmq.username=anon
spring.rabbitmq.password=000
spring.rabbitmq.virtual-host=/
# 消费者数量
spring.rabbitmq.listener.simple.concurrency= 10
spring.rabbitmq.listener.simple.max-concurrency=10
# 每次从队列中取的消息个数
spring.rabbitmq.listener.simple.prefetch= 1
# 消费者默认启动
spring.rabbitmq.listener.simple.auto-startup=true
#允许消费者消费失败后，将消息重新压入队列中
spring.rabbitmq.listener.simple.default-requeue-rejected=true
#失败重试
spring.rabbitmq.template.retry.enabled=true
spring.rabbitmq.template.retry.initial-interval=1s
spring.rabbitmq.template.retry.max-attempts=3
spring.rabbitmq.template.retry.max-interval=10s
spring.rabbitmq.template.retry.multiplier=1.0
```

在本项目中，RabbitMQ的配置和使用在`edu.uestc.rabbitmq`包下。

## RbbitMQ的使用

本项目使用MQ的主要目的在于串行化秒杀请求，提供一个友好的用户体验。

所使用的`Exchange`模式为`Direct`模式。所有发送到`Direct Exchange`的秒杀请求消息被转发到`RouteKey`中指定的`Queue`。

秒杀请求消息投递到`MIAOSHA_QUEUE`中，`edu.uestc.rabbitmq.MQReceiver#receiveMiaoshaInfo()`接收消息并完成秒杀操作。

MQ的配置：

```java
@Configuration
public class MQConfig {
    // 消息队列名
    public static final String MIAOSHA_QUEUE = "miaosha.queue";

    /**
     * Direct模式 交换机exchange
     * 生成用于秒杀的queue
     *
     * @return
     */
    @Bean
    public Queue miaoshaQueue() {
        return new Queue(MIAOSHA_QUEUE, true);
    }
}
```

MQ的消息生产者：

```java
@Service
public class MQSender {
    private static Logger logger = LoggerFactory.getLogger(MQSender.class);
    
    @Autowired
    AmqpTemplate amqpTemplate;
    
    /**
     * 将用户秒杀信息投递到MQ中（使用direct模式的exchange）
     *
     * @param message
     */
    public void sendMiaoshaMessage(MiaoshaMessage message) {
        String msg = RedisService.beanToString(message);
        logger.info("MQ send message: " + msg);
        // 第一个参数为消息队列名，第二个参数为发送的消息
        amqpTemplate.convertAndSend(MQConfig.MIAOSHA_QUEUE, msg);
    }
}
```

MQ的消息消费者：

```java
@Service
public class MQReceiver {
    private static Logger logger = LoggerFactory.getLogger(MQReceiver.class);
    
    @Autowired
    GoodsService goodsService;

    @Autowired
    OrderService orderService;

    @Autowired
    MiaoshaService miaoshaService;
    /**
     * 处理收到的秒杀成功信息
     *
     * @param message
     */
    @RabbitListener(queues = MQConfig.MIAOSHA_QUEUE)
    public void receiveMiaoshaInfo(String message) {
        logger.info("MQ: message: " + message);
        MiaoshaMessage miaoshaMessage = RedisService.stringToBean(message, MiaoshaMessage.class);
        // 获取秒杀用户信息与商品id
        MiaoshaUser user = miaoshaMessage.getUser();
        long goodsId = miaoshaMessage.getGoodsId();

        // 获取商品的库存
        GoodsVo goods = goodsService.getGoodsVoByGoodsId(goodsId);
        Integer stockCount = goods.getStockCount();
        if (stockCount <= 0)
            return;

        // 判断是否已经秒杀到了
        MiaoshaOrder order = orderService.getMiaoshaOrderByUserIdAndGoodsId(user.getId(), goodsId);
        if (order != null)
            return;

        // 减库存 下订单 写入秒杀订单
        miaoshaService.miaosha(user, goods);
    }
}
```

