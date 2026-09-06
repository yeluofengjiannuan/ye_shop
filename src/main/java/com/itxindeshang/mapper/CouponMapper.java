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

    @Select("SELECT * FROM coupon WHERE status = 1 AND valid_end > NOW()")
    List<Coupon> selectActiveCoupons();
}
