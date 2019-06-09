package edu.uestc.controller;

import edu.uestc.controller.result.CodeMsg;
import edu.uestc.controller.result.Result;
import edu.uestc.domain.User;
import edu.uestc.redis.RedisService;
import edu.uestc.redis.UserKey;
import edu.uestc.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;


@Controller
public class HelloController {

    @Autowired
    UserService userService;
    @Autowired
    RedisService redisService;


    @ResponseBody
    @RequestMapping("/")
    public String index() {
        return "Greetings from Spring Boot!";
    }

    //1.rest api json输出 2.页面
    @RequestMapping("/hello")
    @ResponseBody
    public Result<String> hello() {
        return Result.success("hello, spring-boot!");
    }

    @ResponseBody
    @RequestMapping("/helloError")
    public Result<String> helloError() {
        return Result.error(CodeMsg.SERVER_ERROR);
    }

    @RequestMapping("/thymeleaf")
    public String thymeleaf(Model model) {
        model.addAttribute("name", "noodle");
        return "hello";// 返回给客户端的html文件名
    }

    @RequestMapping("/db/get")
    @ResponseBody
    public Result<User> dbGet() {
        User user = userService.getById(1);
        return Result.success(user);
    }

    @ResponseBody
    @RequestMapping("/db/tx")
    public Result<Boolean> dbTX() {
        boolean tx = userService.tx();
        return Result.success(tx);
    }


    // 测试RedisService的get方法
    @RequestMapping("/redis/get")
    @ResponseBody
    public Result<User> redisGet() {
        User user = redisService.get(UserKey.getById, "" + 1, User.class);
        return Result.success(user);
    }

    // 测试RedisService的set方法
    @ResponseBody
    @RequestMapping("redis/set")
    public Result<Boolean> redisSet() {
        User user = new User();
        user.setId(1);

        user.setName("caxacax");
        boolean set = redisService.set(UserKey.getById, "" + 1, user);
        return Result.success(set);
    }

}
