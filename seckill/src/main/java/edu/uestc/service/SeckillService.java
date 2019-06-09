package edu.uestc.service;

import edu.uestc.domain.SeckillOrder;
import edu.uestc.domain.SeckillUser;
import edu.uestc.domain.OrderInfo;
import edu.uestc.redis.SeckillKeyPrefix;
import edu.uestc.redis.RedisService;
import edu.uestc.util.MD5Util;
import edu.uestc.util.UUIDUtil;
import edu.uestc.vo.GoodsVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Random;

/**
 * 秒杀业务处理
 */
@Service
public class SeckillService {

    @Autowired
    GoodsService goodsService;

    @Autowired
    OrderService orderService;

    @Autowired
    RedisService redisService;

    // 用于生成验证码中的运算符
    private char[] ops = new char[]{'+', '-', '*'};

    /**
     * 执行秒杀操作，包含以下两步：
     * 1. 从goods表中减库存
     * 2. 将生成的订单写入miaosha_order表中
     *
     * @param user  秒杀商品的用户
     * @param goods 所秒杀的商品
     * @return 生成的订单信息
     */
    @Transactional
    public OrderInfo seckill(SeckillUser user, GoodsVo goods) {
        // 1. 减库存
        boolean success = goodsService.reduceStock(goods);
        if (!success) {
            setGoodsOver(goods.getId());
            return null;
        }
        // 2. 生成订单；向order_info表和maiosha_order表中写入订单信息
        return orderService.createOrder(user, goods);
    }

    /**
     * 获取秒杀结果
     *
     * @param userId
     * @param goodsId
     * @return
     */
    public long getSeckillResult(Long userId, long goodsId) {

        SeckillOrder order = orderService.getSeckillOrderByUserIdAndGoodsId(userId, goodsId);
        if (order != null) {//秒杀成功
            return order.getOrderId();
        } else {
            boolean isOver = getGoodsOver(goodsId);
            if (isOver) {
                return -1;
            } else {
                return 0;
            }
        }
    }

    /**
     * 验证路径是否正确
     *
     * @param user
     * @param goodsId
     * @param path
     * @return
     */
    public boolean checkPath(SeckillUser user, long goodsId, String path) {
        if (user == null || path == null)
            return false;
        // 从redis中读取出秒杀的path变量是否为本次秒杀操作执行前写入redis中的path
        String oldPath = redisService.get(SeckillKeyPrefix.seckillPath, "" + user.getId() + "_" + goodsId, String.class);
        return path.equals(oldPath);
    }

    /**
     * 创建验证码
     *
     * @param user
     * @param goodsId
     * @return
     */
    public BufferedImage createVerifyCode(SeckillUser user, long goodsId) {

        if (user == null || goodsId <= 0) {
            return null;
        }

        // 验证码的宽高
        int width = 80;
        int height = 32;

        //create the image
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics g = image.getGraphics();
        // set the background color
        g.setColor(new Color(0xDCDCDC));
        g.fillRect(0, 0, width, height);
        // draw the border
        g.setColor(Color.black);
        g.drawRect(0, 0, width - 1, height - 1);
        // create a random instance to generate the codes
        Random rdm = new Random();
        // make some confusion
        for (int i = 0; i < 50; i++) {
            int x = rdm.nextInt(width);
            int y = rdm.nextInt(height);
            g.drawOval(x, y, 0, 0);
        }
        // generate a random code
        String verifyCode = generateVerifyCode(rdm);
        g.setColor(new Color(0, 100, 0));
        g.setFont(new Font("Candara", Font.BOLD, 24));
        g.drawString(verifyCode, 8, 24);
        g.dispose();

        // 计算表达式值，并把把验证码值存到redis中
        int expResult = calc(verifyCode);
        redisService.set(SeckillKeyPrefix.seckillVerifyCode, user.getId() + "," + goodsId, expResult);
        //输出图片
        return image;
    }

    /**
     * 生成验证码，只含有+/-/*
     * <p>
     * 随机生成三个数字，然后生成表达式
     *
     * @param rdm
     * @return 验证码中的数学表达式
     */
    private String generateVerifyCode(Random rdm) {
        int num1 = rdm.nextInt(10);
        int num2 = rdm.nextInt(10);
        int num3 = rdm.nextInt(10);
        char op1 = ops[rdm.nextInt(3)];
        char op2 = ops[rdm.nextInt(3)];
        String exp = "" + num1 + op1 + num2 + op2 + num3;
        return exp;
    }

    /**
     * 使用ScriptEngine计算验证码中的数学表达式的值
     *
     * @param exp
     * @return
     */
    private int calc(String exp) {
        try {
            ScriptEngineManager manager = new ScriptEngineManager();
            ScriptEngine engine = manager.getEngineByName("JavaScript");
            return (Integer) engine.eval(exp);// 表达式计算
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * 检验检验码的计算结果
     *
     * @param user
     * @param goodsId
     * @param verifyCode
     * @return
     */
    public boolean checkVerifyCode(SeckillUser user, long goodsId, int verifyCode) {
        if (user == null || goodsId <= 0) {
            return false;
        }

        // 从redis中获取验证码计算结果
        Integer oldCode = redisService.get(SeckillKeyPrefix.seckillVerifyCode, user.getId() + "," + goodsId, Integer.class);
        if (oldCode == null || oldCode - verifyCode != 0) {// !!!!!!
            return false;
        }

        // 如果校验不成功，则说明校验码过期
        redisService.delete(SeckillKeyPrefix.seckillVerifyCode, user.getId() + "," + goodsId);
        return true;
    }

    /**
     * 创建秒杀地址, 并将其存储在redis中
     *
     * @param user
     * @param goodsId
     * @return
     */
    public String createSeckillPath(SeckillUser user, long goodsId) {

        if (user == null || goodsId <= 0) {
            return null;
        }

        // 随机生成秒杀地址
        String path = MD5Util.md5(UUIDUtil.uuid() + "123456");
        // 将随机生成的秒杀地址存储在redis中（保证不同的用户和不同商品的秒杀地址是不一样的）
        redisService.set(SeckillKeyPrefix.seckillPath, "" + user.getId() + "_" + goodsId, path);
        return path;
    }

    private boolean getGoodsOver(long goodsId) {
        return redisService.exists(SeckillKeyPrefix.isGoodsOver, "" + goodsId);
    }

    public void setGoodsOver(long goodsId) {
        redisService.set(SeckillKeyPrefix.isGoodsOver, "" + goodsId, true);
    }
}
