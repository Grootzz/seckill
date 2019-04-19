package edu.uestc.service;

import edu.uestc.dao.OrderDao;
import edu.uestc.domain.SeckillOrder;
import edu.uestc.domain.SeckillUser;
import edu.uestc.domain.OrderInfo;
import edu.uestc.redis.OrderKeyPrefix;
import edu.uestc.redis.RedisService;
import edu.uestc.vo.GoodsVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

@Service
public class OrderService {

    @Autowired
    OrderDao orderDao;

    @Autowired
    RedisService redisService;

    /**
     * 通过用户id与商品id从订单列表中获取订单信息，这个地方用到了唯一索引（unique index!!!!!）
     * <p>
     * c5: 优化，不同每次都去数据库中读取秒杀订单信息，而是在第一次生成秒杀订单成功后，
     * 将订单存储在redis中，再次读取订单信息的时候就直接从redis中读取
     *
     * @param userId
     * @param goodsId
     * @return 秒杀订单信息
     */
    public SeckillOrder getSeckillOrderByUserIdAndGoodsId(Long userId, long goodsId) {

        // 从redis中取缓存，减少数据库的访问
        SeckillOrder seckillOrder = redisService.get(OrderKeyPrefix.getSeckillOrderByUidGid, ":" + userId + "_" + goodsId, SeckillOrder.class);
        if (seckillOrder != null) {
            return seckillOrder;
        }

        return orderDao.getSeckillOrderByUserIdAndGoodsId(userId, goodsId);
//        return seckillOrder;
    }


    /**
     * 获取订单信息
     *
     * @param orderId
     * @return
     */
    public OrderInfo getOrderById(long orderId) {
        return orderDao.getOrderById(orderId);
    }

    /**
     * 创建订单
     * <p>
     * c5: 增加redis缓存
     *
     * @param user
     * @param goods
     * @return
     */
    @Transactional
    public OrderInfo createOrder(SeckillUser user, GoodsVo goods) {
        OrderInfo orderInfo = new OrderInfo();
        SeckillOrder seckillOrder = new SeckillOrder();

        orderInfo.setCreateDate(new Date());
        orderInfo.setDeliveryAddrId(0L);
        orderInfo.setGoodsCount(1);// 订单中商品的数量
        orderInfo.setGoodsId(goods.getId());
        orderInfo.setGoodsName(goods.getGoodsName());
        orderInfo.setGoodsPrice(goods.getSeckillPrice());// 秒杀价格
        orderInfo.setOrderChannel(1);
        orderInfo.setStatus(0);
        orderInfo.setUserId(user.getId());

        // 将订单信息插入order_info表中
        long orderId = orderDao.insert(orderInfo);

        seckillOrder.setGoodsId(goods.getId());
        seckillOrder.setOrderId(orderInfo.getId());
        seckillOrder.setUserId(user.getId());
        // 将秒杀订单插入miaosha_order表中
        orderDao.insertSeckillOrder(seckillOrder);

        // 将秒杀订单信息存储于redis中
        redisService.set(OrderKeyPrefix.getSeckillOrderByUidGid, ":" + user.getId() + "_" + goods.getId(), seckillOrder);

        return orderInfo;
    }
}
