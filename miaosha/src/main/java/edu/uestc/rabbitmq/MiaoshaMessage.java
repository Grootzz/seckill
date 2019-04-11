package edu.uestc.rabbitmq;


import edu.uestc.domain.MiaoshaUser;

/**
 * 在MQ中传递的秒杀信息
 * 包含参与秒杀的用户和商品的id
 */
public class MiaoshaMessage {

    private MiaoshaUser user;

    private long goodsId;

    public MiaoshaUser getUser() {
        return user;
    }

    public void setUser(MiaoshaUser user) {
        this.user = user;
    }

    public long getGoodsId() {
        return goodsId;
    }

    public void setGoodsId(long goodsId) {
        this.goodsId = goodsId;
    }

    @Override
    public String toString() {
        return "MiaoshaMessage{" +
                "user=" + user +
                ", goodsId=" + goodsId +
                '}';
    }
}
