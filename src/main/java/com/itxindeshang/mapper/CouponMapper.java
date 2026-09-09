package com.itxindeshang.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.itxindeshang.pojo.entity.Coupon;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface CouponMapper extends BaseMapper<Coupon> {
    /**
     * DB降级扣库存：原子UPDATE，receive_qty + quantity <= total_qty 才成功
     * 返回受影响行数，0表示库存不足
     */
    @Update("UPDATE coupon SET receive_qty = receive_qty + #{perUserQty} " +
            "WHERE id = #{couponId} AND receive_qty + #{perUserQty} <= total_qty")
    int deductStock(@Param("couponId") Long couponId,
                    @Param("perUserQty") Integer perUserQty);

    //FIXME:这里肯定有问题，至少要大于券的发放时间，不止，有的压根就没有vaildend
    @Select("SELECT * FROM coupon WHERE status = 1 AND release_time< NOW()")
    List<Coupon> selectActiveCoupons();
}
