package com.itxindeshang.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.itxindeshang.pojo.entity.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;


@Mapper
public interface OrderMapper extends BaseMapper<Order> {
    @Update("update `order` set status = 6,cancel_time = now(), cancel_reason = #{cancelReason} where order_no = #{orderNo} and status =1")
    boolean cancelOrderCommon(@Param("orderNo") String orderNo,
                              @Param("cancelReason") String cancelReason);
}



