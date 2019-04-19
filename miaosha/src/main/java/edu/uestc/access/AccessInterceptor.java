package edu.uestc.access;

import com.alibaba.fastjson.JSON;
import edu.uestc.controller.result.CodeMsg;
import edu.uestc.controller.result.Result;
import edu.uestc.domain.SeckillUser;
import edu.uestc.redis.AccessKeyPrefix;
import edu.uestc.redis.RedisService;
import edu.uestc.service.SeckillUserService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.OutputStream;

/**
 * 用户访问拦截器，限制用户对某一个接口的频繁访问
 */
@Service
public class AccessInterceptor extends HandlerInterceptorAdapter {

    @Autowired
    SeckillUserService userService;

    @Autowired
    RedisService redisService;

    /**
     * 目标方法执行前的处理
     * <p>
     * 查询访问次数，进行防刷请求拦截
     *
     * @param request
     * @param response
     * @param handler
     * @return
     * @throws Exception
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        // 指明拦截的是方法
        if (handler instanceof HandlerMethod) {
            SeckillUser user = this.getUser(request, response);// 获取用户对象
            UserContext.setUser(user);// 保存用户到ThreadLocal，这样，同一个线程访问的是同一个用户
            HandlerMethod hm = (HandlerMethod) handler;
            // 获取标注了@AccessLimit的方法，没有注解，则直接返回
            AccessLimit accessLimit = hm.getMethodAnnotation(AccessLimit.class);
            // 如果没有添加@AccessLimit注解，直接放行（true）
            if (accessLimit == null) {
                return true;
            }

            // 获取注解的元素值
            int seconds = accessLimit.seconds();
            int maxCount = accessLimit.maxAccessCount();
            boolean needLogin = accessLimit.needLogin();

            String key = request.getRequestURI();
            if (needLogin) {
                if (user == null) {
                    this.render(response, CodeMsg.SESSION_ERROR);
                    return false;
                }
                key += "_" + user.getId();
            } else {
                //do nothing
            }
            // 设置过期时间
            AccessKeyPrefix accessKeyPrefix = AccessKeyPrefix.withExpire(seconds);
            // 在redis中存储的访问次数的key为请求的URI
            Integer count = redisService.get(accessKeyPrefix, key, Integer.class);
            // 第一次重复点击秒杀
            if (count == null) {
                redisService.set(accessKeyPrefix, key, 1);
                // 点击次数为未达最大值
            } else if (count < maxCount) {
                redisService.incr(accessKeyPrefix, key);
                // 点击次数已满
            } else {
                this.render(response, CodeMsg.ACCESS_LIMIT_REACHED);
                return false;
            }
        }
        return true;
    }

    /**
     * 点击次数已满后，向客户端反馈一个“频繁请求”提示信息
     *
     * @param response
     * @param cm
     * @throws Exception
     */
    private void render(HttpServletResponse response, CodeMsg cm) throws Exception {
        response.setContentType("application/json;charset=UTF-8");
        OutputStream out = response.getOutputStream();
        String str = JSON.toJSONString(Result.error(cm));
        out.write(str.getBytes("UTF-8"));
        out.flush();
        out.close();
    }

    /**
     * 和UserArgumentResolver功能类似，用于解析拦截的请求，获取MiaoshaUser对象
     *
     * @param request
     * @param response
     * @return MiaoshaUser对象
     */
    private SeckillUser getUser(HttpServletRequest request, HttpServletResponse response) {

        // 从请求中获取token
        String paramToken = request.getParameter(SeckillUserService.COOKIE_NAME_TOKEN);
        String cookieToken = getCookieValue(request, SeckillUserService.COOKIE_NAME_TOKEN);
        if (StringUtils.isEmpty(cookieToken) && StringUtils.isEmpty(paramToken)) {
            return null;
        }
        String token = StringUtils.isEmpty(paramToken) ? cookieToken : paramToken;
        return userService.getMisaoshaUserByToken(response, token);
    }

    /**
     * 从众多的cookie中找出指定cookiName的cookie
     *
     * @param request
     * @param cookiName
     * @return cookiName对应的value
     */
    private String getCookieValue(HttpServletRequest request, String cookiName) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null || cookies.length <= 0) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (cookie.getName().equals(cookiName)) {
                return cookie.getValue();
            }
        }
        return null;
    }

}
