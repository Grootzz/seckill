package edu.uestc.service;


import edu.uestc.controller.result.CodeMsg;
import edu.uestc.dao.SeckillUserDao;
import edu.uestc.domain.SeckillUser;
import edu.uestc.exception.GlobalException;
import edu.uestc.redis.SeckillUserKeyPrefix;
import edu.uestc.redis.RedisService;
import edu.uestc.util.MD5Util;
import edu.uestc.util.UUIDUtil;
import edu.uestc.vo.LoginVo;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

/**
 * 秒杀用户业务层
 * <p>
 * c5: 缓存更新套路 http://blog.csdn.net/tTU1EvLDeLFq5btqiK/article/details/78693323
 */
@Service
public class SeckillUserService {

    public static final String COOKIE_NAME_TOKEN = "token";

    @Autowired
    SeckillUserDao seckillUserDao;
    // 由于需要将一个cookie对应的用户存入第三方缓存中，这里用redis，所以需要引入redis serice
    @Autowired
    RedisService redisService;

    /**
     * 用户登录, 要么处理成功返回true，否则会抛出全局异常
     * 抛出的异常信息会被全局异常接收，全局异常会将异常信息传递到全局异常处理器
     *
     * @param loginVo 封装了客户端请求传递过来的数据（即账号密码）
     *                （使用post方式，请求参数放在了请求体中，这个参数就是获取请求体中的数据）
     * @return 登录成功与否
     */
    public String login(HttpServletResponse response, LoginVo loginVo) {
        if (loginVo == null) {
            // 抛出的异常信息会被全局异常接收，全局异常会将异常信息传递到全局异常处理器
            throw new GlobalException(CodeMsg.SERVER_ERROR);
        }
        // 获取用户提交的手机号码和密码
        String mobile = loginVo.getMobile();
        String password = loginVo.getPassword();
        // 判断手机号是否存在
        SeckillUser user = this.getMiaoshaUserById(Long.parseLong(mobile));
        if (user == null)
            throw new GlobalException(CodeMsg.MOBILE_NOT_EXIST);


        // 判断手机号对应的密码是否一致
        String dbPassword = user.getPassword();
        String dbSalt = user.getSalt();
        String calcPass = MD5Util.formPassToDbPass(password, dbSalt);
        if (!calcPass.equals(dbPassword))
            throw new GlobalException(CodeMsg.PASSWORD_ERROR);

        // 执行到这里表明登录成功了
        // 生成cookie
        String token = UUIDUtil.uuid();
        // 每次访问都会生成一个新的session存储于redis和反馈给客户端，一个session对应存储一个user对象
        redisService.set(SeckillUserKeyPrefix.token, token, user);
        // 将token写入cookie中, 然后传给客户端（一个cookie对应一个用户，这里将这个cookie的用户信息写入redis中）
        Cookie cookie = new Cookie(COOKIE_NAME_TOKEN, token);
        cookie.setMaxAge(SeckillUserKeyPrefix.token.expireSeconds());// 保持与redis中的session一致
        cookie.setPath("/");
        response.addCookie(cookie);

        return token;
    }

    /**
     * 根据id查询秒杀用户信息
     * <p>
     * c5: 对象级缓存
     * 从缓存中查询MiaoshaUser对象，如果MiaoshaUser在缓存中存在，则直接返回，否则从数据库返回
     *
     * @param id
     * @return
     */
    private SeckillUser getMiaoshaUserById(Long id) {

        // 1. 从redis中获取用户数据缓存
        SeckillUser user = redisService.get(SeckillUserKeyPrefix.getSeckillUserById, "" + id, SeckillUser.class);
        if (user != null)
            return user;

        // 2. 如果缓存中没有用户数据，则将数据写入缓存
        // 先从数据库中取出数据
        user = seckillUserDao.getById(id);
        // 然后将数据返回并将数据缓存在redis中
        if (user != null)
            redisService.set(SeckillUserKeyPrefix.getSeckillUserById, "" + id, user);
        return user;
    }

    /**
     * 根据token从redis中取出 SeckillUser 对象
     *
     * @param response 在获取 SeckillUser 的同时，需要将新的cookie设置到 response 对象中
     * @param token    用于在redis中获取MiaoshaUser对象的key
     * @return
     */
    public SeckillUser getMisaoshaUserByToken(HttpServletResponse response, String token) {
        if (StringUtils.isEmpty(token)) {
            return null;
        }
        SeckillUser miaoshaUser = redisService.get(SeckillUserKeyPrefix.token, token, SeckillUser.class);
        // 在有效期内从redis获取到key之后，需要将key重新设置一下，从而达到延长有效期的效果
        if (miaoshaUser != null) {
            addCookie(response, token, miaoshaUser);
        }
        return miaoshaUser;
    }

    /**
     * 将cookie存入redis，并将cookie写入到请求的响应中
     *
     * @param response
     * @param token
     * @param user
     */
    private void addCookie(HttpServletResponse response, String token, SeckillUser user) {
        redisService.set(SeckillUserKeyPrefix.token, token, user);
        Cookie cookie = new Cookie(COOKIE_NAME_TOKEN, token);
        cookie.setMaxAge(SeckillUserKeyPrefix.token.expireSeconds());
        cookie.setPath("/");
        response.addCookie(cookie);
    }

    /**
     * c5: 更新密码
     * 1. 从数据库中查询用户是否存在，如果存在则继续，不存在这抛出用户不存在错误
     * 2. 如果用户存在，则调用dao层更新用户的密码字段
     * 3. 数据库中的数据更新后，redis中的数据也需要更新，保持数据的一致性
     *
     * @param token           存储在redis中的用户token
     * @param id              待更新用户的id
     * @param updatedPassword 从表单中接收的更新密码
     * @return 更新成功与否
     */
    public boolean updatePassword(String token, long id, String updatedPassword) {
        // 1. 从缓存或者数据库中取id对应的用户数据
        SeckillUser user = this.getMiaoshaUserById(id);// 这一步的数据也有可能是从缓存中读取的数据
        if (user == null) {
            // 如果用户数据不存在，则抛出异常
            throw new GlobalException(CodeMsg.MOBILE_NOT_EXIST);
        }

        // 2. 如果用户存在，则更新数据
        // 更新数据库中的数据
        SeckillUser updatedUser = new SeckillUser();
        updatedUser.setId(id);
        updatedUser.setPassword(MD5Util.formPassToDbPass(updatedPassword, user.getSalt()));
        seckillUserDao.updatePassword(updatedUser);
        // 更新缓存中的数据（先删除，再添加）
        // 如果不删除，以前的用户数据仍然存在于缓存中，则通过以前的token依旧可以访问到用户之前的数据，这会造成信息泄露
        redisService.delete(SeckillUserKeyPrefix.getSeckillUserById, "" + id);
        user.setPassword(updatedUser.getPassword());
        redisService.set(SeckillUserKeyPrefix.token, token, user);// 为新的用户数据重新生成缓存
        return true;
    }
}
