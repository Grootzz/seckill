package edu.uestc.dao;

import edu.uestc.domain.MiaoshaOrder;
import edu.uestc.domain.OrderInfo;
import org.apache.ibatis.annotations.*;

/**
 * miaosha_order表数据访问层
 */
@Mapper
public interface OrderDao {


    /**
     * 通过用户id与商品id从订单列表中获取订单信息
     *
     * @param userId  用户id
     * @param goodsId 商品id
     * @return 秒杀订单信息
     */
    @Select("SELECT * FROM miaosha_order WHERE user_id=#{userId} AND goods_id=#{goodsId}")
    MiaoshaOrder getMiaoshaOrderByUserIdAndGoodsId(@Param("userId") Long userId, @Param("goodsId") long goodsId);

    /**
     * 将订单信息插入miaosha_order表中
     *
     * @param orderInfo 订单信息
     * @return 插入成功的订单信息id
     */
    @Insert("INSERT INTO order_info (user_id, goods_id, goods_name, goods_count, goods_price, order_channel, status, create_date)"
            + "VALUES (#{userId}, #{goodsId}, #{goodsName}, #{goodsCount}, #{goodsPrice}, #{orderChannel},#{status},#{createDate} )")
    // 查询出插入订单信息的表id，并返回
    @SelectKey(keyColumn = "id", keyProperty = "id", resultType = long.class, before = false, statement = "SELECT last_insert_id()")
    long insert(OrderInfo orderInfo);

    /**
     * 将秒杀订单信息插入到miaosha_order中
     *
     * @param miaoshaOrder 秒杀订单
     */
    @Insert("INSERT INTO miaosha_order(user_id, order_id, goods_id) VALUES (#{userId}, #{orderId}, #{goodsId})")
    void insertMiaoshaOrder(MiaoshaOrder miaoshaOrder);

    /**
     * 获取订单信息
     *
     * @param orderId
     * @return
     */
    @Select("select * from order_info where id = #{orderId}")
    OrderInfo getOrderById(@Param("orderId") long orderId);
}
