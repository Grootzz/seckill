package edu.uestc.service;

import edu.uestc.dao.GoodsDao;
import edu.uestc.domain.SeckillGoods;
import edu.uestc.vo.GoodsVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 *
 */

@Service
public class GoodsService {

    @Autowired
    GoodsDao goodsDao;

    /**
     * 查出商品信息（包含该商品的秒杀信息）
     *
     * @return
     */
    public List<GoodsVo> listGoodsVo() {
        return goodsDao.listGoodsVo();
    }

    /**
     * 通过商品的id查出商品的所有信息（包含该商品的秒杀信息）
     *
     * @param goodsId
     * @return
     */
    public GoodsVo getGoodsVoByGoodsId(Long goodsId) {
        return goodsDao.getGoodsVoByGoodsId(goodsId);
    }

    /**
     * order表减库存
     *
     * @param goods
     */
    public boolean reduceStock(GoodsVo goods) {
        SeckillGoods seckillGoods = new SeckillGoods();
        seckillGoods.setGoodsId(goods.getId());// 秒杀商品的id和商品的id是一样的
        int ret = goodsDao.reduceStack(seckillGoods);
        return ret > 0;
    }
}
