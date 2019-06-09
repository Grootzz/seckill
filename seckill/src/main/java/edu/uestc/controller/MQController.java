package edu.uestc.controller;

import edu.uestc.controller.result.Result;
import edu.uestc.rabbitmq.MQSender;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * c6： MQ测试
 */

@Controller
@RequestMapping("/mq")
public class MQController {

    @Autowired
    MQSender sender;

    /**
     * @return
     */
    @RequestMapping("/hellomq")
    @ResponseBody
    public Result<String> helloMQ() {
        sender.send("Hello, RabbitMQ");

        return Result.success("Hello World!");
    }

    /**
     * 将请求发送到topic exchange
     *
     * @return
     */
    @RequestMapping("/topic_mq")
    @ResponseBody
    public Result<String> helloMQTopic() {
        sender.send("Hello, RabbitMQ");

        return Result.success("Hello World!");
    }

    /**
     * 将请求发送到fanout exchange
     *
     * @return
     */
    @RequestMapping("/fanout_mq")
    @ResponseBody
    public Result<String> fanout() {
        sender.sendFanout("Hello, RabbitMQ");
        return Result.success("Hello World!");
    }


}
