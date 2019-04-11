package edu.uestc.vo;

import edu.uestc.domain.Goods;

import java.util.Date;

/**
 * 商品信息（并且包含商品的秒杀信息）
 * 商品信息和商品的秒杀信息是存储在两个表中的（goods和miaosha_goods）
 * 继承Goods便具有了goods表的信息，再额外添加上miaosha_goods的信息即可
 */
public class GoodsVo extends Goods{

    /*只包含了部分miaosha_goods表的信息*/
    private Double miaoshaPrice;
    private Integer stockCount;
    private Date startDate;
    private Date endDate;

    public Double getMiaoshaPrice() {
        return miaoshaPrice;
    }

    public void setMiaoshaPrice(Double miaoshaPrice) {
        this.miaoshaPrice = miaoshaPrice;
    }

    public Integer getStockCount() {
        return stockCount;
    }

    public void setStockCount(Integer stockCount) {
        this.stockCount = stockCount;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }
}
