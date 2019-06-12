package edu.uestc.controller;

import edu.uestc.controller.result.Result;
import edu.uestc.service.SeckillUserService;
import edu.uestc.vo.LoginVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;


/**
 * 登录页面控制器
 */
@Controller
@RequestMapping("/login")
public class LoginController {

    @Autowired
    SeckillUserService seckillUserService;


    // 日志记录：Logger是由slf4j接口规范创建的，对象有具体的实现类创建
    private static Logger logger = LoggerFactory.getLogger(LoginController.class);

    /**
     * 这个方法没有加@ResponseBody注解，在return指示请求的是资源名称，即login/to_login请求的是login.html
     *
     * @return
     */
    @RequestMapping("/to_login")
    public String toLogin() {
        return "login";// login页面
    }

    /**
     * 用户登录
     *
     * @param loginVo 用户登录请求的表单数据（将表单数据封装为了一个Vo：Value Object）
     *                注解@Valid用于校验表单参数，校验成功才会继续执行doLogin
     *                否则，请求参数校验不成功时会发生异常
     * @return
     */
    @RequestMapping("/do_login")
    @ResponseBody
    public Result<Boolean> doLogin(HttpServletResponse response, @Valid LoginVo loginVo) {
        logger.info(loginVo.toString());// 打印接收的表单数据

        // 登录
        seckillUserService.login(response, loginVo);
        return Result.success(true);
    }

    /**
     * 这个请求处理用于生成token，压测时候用
     *
     * @param response
     * @param loginVo
     * @return
     */
    @RequestMapping("/create_token")
    @ResponseBody
    public Result<String> createToken(HttpServletResponse response, @Valid LoginVo loginVo) {
        logger.info(loginVo.toString());
        String token = seckillUserService.login(response, loginVo);
        return Result.success(token);
    }

}
