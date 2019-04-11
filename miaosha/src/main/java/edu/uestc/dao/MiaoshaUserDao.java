package edu.uestc.dao;

import edu.uestc.domain.MiaoshaUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 秒杀用户表miaosha_user的SQl Mapper
 */
@Mapper
public interface MiaoshaUserDao {
    /**
     * 根据id查询秒杀用户信息
     *
     * @param id
     * @return
     */
    @Select("SELECT * FROM miaosha_user WHERE id=#{id}")
    MiaoshaUser getById(@Param("id") Long id);

    /**
     *
     * @param updatedUser
     */
    @Update("UPDATE miaosha_user SET password=#{password} WHERE id=#{id}")
    void updatePassword(MiaoshaUser updatedUser);

}
