package edu.uestc.redis;

/**
 * 秒杀用户信息的key前缀
 */

public class MiaoshaUserKeyPrefix extends BaseKeyPrefix {


    public static final int TOKEN_EXPIRE = 3600 * 24 * 2;// 缓存有效时间为两天

    public MiaoshaUserKeyPrefix(int expireSeconds, String prefix) {
        super(expireSeconds, prefix);
    }

    public static MiaoshaUserKeyPrefix token = new MiaoshaUserKeyPrefix(TOKEN_EXPIRE, "token");
    // 用于存储用户对象到redis的key前缀
    public static MiaoshaUserKeyPrefix getMiaoshaUserById = new MiaoshaUserKeyPrefix(0, "id");

}
