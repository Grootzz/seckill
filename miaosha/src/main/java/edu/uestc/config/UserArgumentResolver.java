package edu.uestc.config;

import edu.uestc.domain.SeckillUser;
import edu.uestc.service.SeckillUserService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 解析请求，并将请求的参数设置到方法参数中
 */
@Service// 使用Spring管理起来
public class UserArgumentResolver implements HandlerMethodArgumentResolver {

    @Autowired
    SeckillUserService seckillUserService;

    /**
     * 当请求参数为MiaoshaUser时，使用这个解析器处理
     * 客户端的请求到达某个Controller的方法时，判断这个方法的参数是否为MiaoshaUser，
     * 如果是，则这个MiaoshaUser参数对象通过下面的resolveArgument()方法获取，
     * 然后，该Controller方法继续往下执行时所看到的MiaoshaUser对象就是在这里的resolveArgument()方法处理过的对象
     *
     * @param methodParameter
     * @return
     */
    @Override
    public boolean supportsParameter(MethodParameter methodParameter) {
        Class<?> parameterType = methodParameter.getParameterType();
        return parameterType == SeckillUser.class;
    }


    /**
     * 获取MiaoshaUser对象
     *
     * @param methodParameter
     * @param modelAndViewContainer
     * @param nativeWebRequest
     * @param webDataBinderFactory
     * @return
     * @throws Exception
     */
    @Override
    public Object resolveArgument(MethodParameter methodParameter,
                                  ModelAndViewContainer modelAndViewContainer,
                                  NativeWebRequest nativeWebRequest,
                                  WebDataBinderFactory webDataBinderFactory) throws Exception {
        // 获取请求和响应对象
        HttpServletRequest request = nativeWebRequest.getNativeRequest(HttpServletRequest.class);
        HttpServletResponse response = nativeWebRequest.getNativeResponse(HttpServletResponse.class);

        // 从请求对象中获取token（token可能有两种方式从客户端返回，1：通过url的参数，2：通过set-Cookie字段）
        String paramToken = request.getParameter(SeckillUserService.COOKIE_NAME_TOKEN);
        String cookieToken = getCookieValue(request, SeckillUserService.COOKIE_NAME_TOKEN);

        if (StringUtils.isEmpty(cookieToken) && StringUtils.isEmpty(paramToken)) {
            return null;
        }

        // 判断是哪种方式返回的token，并由该种方式获取token（cookie）
        String token = StringUtils.isEmpty(paramToken) ? cookieToken : paramToken;
        // 通过token就可以在redis中查出该token对应的用户对象
        return seckillUserService.getMisaoshaUserByToken(response, token);
    }

    /**
     * 根据cookie名获取相应的cookie值
     *
     * @param request
     * @param cookiName
     * @return
     */
    private String getCookieValue(HttpServletRequest request, String cookiName) {
        Cookie[] cookies = request.getCookies();
        // null判断，否则并发时会发生异常
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
