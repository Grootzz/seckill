package edu.uestc.controller;

import edu.uestc.controller.result.Result;
import edu.uestc.domain.SeckillUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;


/**
 * 用于压测的controller，只有一个返回用户信息的功能
 * 这样，请求的压力会全部集中在数据库
 */

@Controller
@RequestMapping("/user")
public class UserController {

    // 日志记录：Logger是由slf4j接口规范创建的，对象有具体的实现类创建
    private static Logger logger = LoggerFactory.getLogger(LoginController.class);

    /**
     * 返回用户信息
     *
     * @param user
     * @return
     */
    @RequestMapping("/user_info")
    @ResponseBody
    public Result<SeckillUser> userInfo(SeckillUser user) {
        logger.info(user.toString());
        return Result.success(user);
    }
}
