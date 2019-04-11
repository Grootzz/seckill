package edu.uestc.redis;


/**
 * 判断秒杀状态的key前缀
 */
public class MiaoshaKeyPrefix extends BaseKeyPrefix {
    public MiaoshaKeyPrefix(String prefix) {
        super(prefix);
    }

    public MiaoshaKeyPrefix(int expireSeconds, String prefix) {
        super(expireSeconds, prefix);
    }

    public static MiaoshaKeyPrefix isGoodsOver = new MiaoshaKeyPrefix("isGoodsOver");
    public static MiaoshaKeyPrefix miaoshaPath = new MiaoshaKeyPrefix(60, "miaoshaPath");
    // 验证码5分钟有效
    public static MiaoshaKeyPrefix miaoshaVerifyCode = new MiaoshaKeyPrefix(300, "miaoshaVerifyCode");
}
