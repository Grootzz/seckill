package edu.uestc.controller;

import edu.uestc.access.AccessLimit;
import edu.uestc.controller.result.CodeMsg;
import edu.uestc.controller.result.Result;
import edu.uestc.domain.SeckillOrder;
import edu.uestc.domain.SeckillUser;
import edu.uestc.domain.OrderInfo;
import edu.uestc.rabbitmq.MQSender;
import edu.uestc.rabbitmq.SeckillMessage;
import edu.uestc.redis.GoodsKeyPrefix;
import edu.uestc.redis.RedisService;
import edu.uestc.service.GoodsService;
import edu.uestc.service.SeckillService;
import edu.uestc.service.OrderService;
import edu.uestc.vo.GoodsVo;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.imageio.ImageIO;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * 秒杀按钮的业务逻辑控制
 * c6: 在秒杀接口上做优化，使用MQ将请求入队
 */

@Controller
@RequestMapping("/miaosha")
public class SeckillController implements InitializingBean {

    @Autowired
    GoodsService goodsService;
    @Autowired
    OrderService orderService;
    @Autowired
    SeckillService seckillService;
    @Autowired
    RedisService redisService;
    @Autowired
    MQSender sender;

    // 用于内存标记，标记库存是否为空，从而减少对redis的访问
    private Map<Long, Boolean> localOverMap = new HashMap<>();

    /**
     * 秒杀逻辑
     * 用户点击秒杀按钮后的逻辑控制
     * <p>
     * c6: 使用MQ优化
     *
     * @param model   页面model，用于存储带给页面的变量
     * @param user    秒杀用户
     * @param goodsId 秒杀的商品id
     * @return 执行秒杀后的跳转
     */
    @RequestMapping("/do_miaosha")
    public String doMiaosha(Model model, SeckillUser user, @RequestParam("goodsId") long goodsId) {

        model.addAttribute("user", user);
        // 1. 如果用户为空，则返回登录界面
        if (user == null)
            return "login";

        // 2. 用户不为空，说明用户已登录, 可以继续执行下面的操作

        // 2.1 判断库存，库存有才可以继续往下执行
        GoodsVo goods = goodsService.getGoodsVoByGoodsId(goodsId);
        int stockCount = goods.getStockCount();// 获取秒杀商品的库存
        if (stockCount <= 0) {
            model.addAttribute("errmsg", CodeMsg.SECKILL_OVER.getMsg());
            return "miaosha_fail";
        }

        // 2.2 判断用户是否已经完成秒杀，如果没有秒杀成功，继续执行
        SeckillOrder order = orderService.getSeckillOrderByUserIdAndGoodsId(user.getId(), goodsId);
        if (order != null) {
            model.addAttribute("errmsg", CodeMsg.REPEATE_SECKILL.getMsg());
            return "miaosha_fail";
        }
        // 2.3 完成秒杀操作：减库存，下订单，写入秒杀订单
        OrderInfo orderInfo = seckillService.seckill(user, goods);
        model.addAttribute("orderInfo", orderInfo);
        model.addAttribute("goods", goods);
        return "order_detail";
    }

    /**
     * c5: 秒杀逻辑（页面静态化分离，不需要直接将页面返回给客户端，而是返回客户端需要的页面动态数据，返回数据时json格式）
     * <p>
     * QPS:1306
     * 5000 * 10
     * <p>
     * GET/POST的@RequestMapping是有区别的
     * <p>
     * c6： 通过随机的path，客户端隐藏秒杀接口
     *
     * @param model
     * @param user
     * @param goodsId
     * @param path    隐藏的秒杀地址，为客户端回传的path，最初也是有服务端产生的
     * @return 订单详情或错误码
     */
    // {path}为客户端回传的path，最初也是有服务端产生的
    @RequestMapping(value = "/{path}/do_miaosha_static", method = RequestMethod.POST)
    @ResponseBody
    public Result<Integer> doMiaoshaStatic(Model model, SeckillUser user,
                                           @RequestParam("goodsId") long goodsId,
                                           @PathVariable("path") String path) {

        model.addAttribute("user", user);
        // 1. 如果用户为空，则返回登录界面
        if (user == null)
            return Result.error(CodeMsg.SESSION_ERROR);

        // c6: 验证path是否正确
        boolean check = seckillService.checkPath(user, goodsId, path);
        if (!check)
            return Result.error(CodeMsg.REQUEST_ILLEGAL);// 请求非法

        // 通过内存标记，减少对redis的访问，秒杀未结束才继续访问redis
        Boolean over = localOverMap.get(goodsId);
        if (over)
            return Result.error(CodeMsg.SECKILL_OVER);

        // 预减库存
        Long stock = redisService.decr(GoodsKeyPrefix.seckillGoodsStockPrefix, "" + goodsId);
        if (stock < 0) {
            localOverMap.put(goodsId, true);// 秒杀结束。标记该商品已经秒杀结束
            return Result.error(CodeMsg.SECKILL_OVER);
        }

        // 判断是否重复秒杀
        SeckillOrder order = orderService.getSeckillOrderByUserIdAndGoodsId(user.getId(), goodsId);
        if (order != null) {
            return Result.error(CodeMsg.REPEATE_SECKILL);
        }

        // 商品有库存且用户为秒杀商品，则将秒杀请求放入MQ
        SeckillMessage message = new SeckillMessage();
        message.setUser(user);
        message.setGoodsId(goodsId);

        // 放入MQ
        sender.sendMiaoshaMessage(message);
        return Result.success(0); // 排队中
    }

    /**
     * 用于返回用户秒杀的结果
     *
     * @param model
     * @param user
     * @param goodsId
     * @return orderId：成功, -1：秒杀失败, 0： 排队中
     */
    @RequestMapping(value = "/result", method = RequestMethod.GET)
    @ResponseBody
    public Result<Long> miaoshaResult(Model model, SeckillUser user,
                                      @RequestParam("goodsId") long goodsId) {
        model.addAttribute("user", user);
        if (user == null) {
            return Result.error(CodeMsg.SESSION_ERROR);
        }
        long result = seckillService.getSeckillResult(user.getId(), goodsId);
        return Result.success(result);
    }

    /**
     * 获取秒杀接口地址
     * 每一次点击秒杀，都会生成一个随机的秒杀地址返回给客户端
     * 对秒杀的次数做限制（通过自定义拦截器注解完成）
     *
     * @param model
     * @param user
     * @param goodsId    秒杀的商品id
     * @param verifyCode 验证码
     * @return 被隐藏的秒杀接口路径
     */
    @AccessLimit(seconds = 5, maxAccessCount = 5, needLogin = true)
    @RequestMapping(value = "/path", method = RequestMethod.GET)
    @ResponseBody
    public Result<String> getMiaoshaPath(Model model, SeckillUser user,
                                         @RequestParam("goodsId") long goodsId,
                                         @RequestParam(value = "verifyCode", defaultValue = "0") int verifyCode
    ) {

        // 在执行下面的逻辑之前，会相对path请求进行拦截处理（@AccessLimit， AccessInterceptor），防止访问次数过于频繁，对服务器造成过大的压力

        model.addAttribute("user", user);

        if (user == null) {
            return Result.error(CodeMsg.SESSION_ERROR);
        }

        // 校验验证码
        boolean check = seckillService.checkVerifyCode(user, goodsId, verifyCode);
        if (!check)
            return Result.error(CodeMsg.REQUEST_ILLEGAL);// 检验不通过，请求非法

        // 检验通过，获取秒杀路径
        String path = seckillService.createSeckillPath(user, goodsId);
        // 向客户端回传随机生成的秒杀地址
        return Result.success(path);
    }


    /**
     * goods_detail.htm: $("#verifyCodeImg").attr("src", "/seckill/verifyCode?goodsId=" + $("#goodsId").val());
     * 使用HttpServletResponse的输出流返回客户端异步获取的验证码（异步获取的代码如上所示）
     *
     * @param response
     * @param user
     * @param goodsId
     * @return
     */
    @RequestMapping(value = "/verifyCode", method = RequestMethod.GET)
    @ResponseBody
    public Result<String> getMiaoshaVerifyCode(HttpServletResponse response, SeckillUser user,
                                               @RequestParam("goodsId") long goodsId) {
        if (user == null)
            return Result.error(CodeMsg.SESSION_ERROR);

        // 创建验证码
        try {
            BufferedImage image = seckillService.createVerifyCode(user, goodsId);
            ServletOutputStream out = response.getOutputStream();
            // 将图片写入到resp对象中
            ImageIO.write(image, "JPEG", out);
            out.close();
            out.flush();
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error(CodeMsg.SECKILL_FAIL);
        }
    }

    /**
     * c6:
     * 系统初始化的时候执行
     * <p>
     * 系统初始化的时候从数据库中将商品信息查询出来（包含商品的秒杀信息miaosha_goods和商品的基本信息goods）
     *
     */
    @Override
    public void afterPropertiesSet() {
        //
        List<GoodsVo> goods = goodsService.listGoodsVo();
        if (goods == null) {
            return;
        }

        // 将商品的库存信息存储在redis中
        for (GoodsVo good : goods) {
            redisService.set(GoodsKeyPrefix.seckillGoodsStockPrefix, "" + good.getId(), good.getStockCount());
            localOverMap.put(good.getId(), false);// 在系统启动时，标记库存不为空
        }
    }
}
