package com.itxindeshang.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itxindeshang.common.constant.MessageConstant;
import com.itxindeshang.common.exception.CouponException;
import com.itxindeshang.common.generator.SnowflakeIdGenerator;
import com.itxindeshang.common.mapstruct.CopyMapper;
import com.itxindeshang.common.result.Result;
import com.itxindeshang.mapper.CouponMapper;
import com.itxindeshang.pojo.dto.CouponCreateDTO;
import com.itxindeshang.pojo.entity.Coupon;
import com.itxindeshang.service.CouponService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class CouponServiceImpl extends ServiceImpl<CouponMapper, Coupon> implements CouponService {
    @Resource
    private CopyMapper copyMapper;

    @Resource
    private CouponMapper couponMapper;

    @Resource
    private SnowflakeIdGenerator snowflakeIdGenerator;

    /**
     * 管理员分发优惠券
     */
    @Override
    public Result<?> saveCouponAdmin(CouponCreateDTO couponCreateDTO) {
        // 1. 条件校验
        validateCouponCreateDTO(couponCreateDTO);

        // 2. DTO → Entity 转换
        Coupon coupon = copyMapper.couponCreateDTOToCoupon(couponCreateDTO);

        // 3. 生成优惠券编号
        coupon.setCouponNo(snowflakeIdGenerator.generateCouponNo());

        // 4. 初始化数量字段
        coupon.setReceiveQty(0);
        coupon.setUsedQty(0);
        coupon.setStatus(0);

        // 5. 无门槛券强制门槛为0
        if (couponCreateDTO.getType() == 3) {
            coupon.setConditionAmount(BigDecimal.ZERO);
        }
        // 6. 入库
        couponMapper.insert(coupon);

        return Result.success();
    }
    /**
     * 条件校验
     */
    private void validateCouponCreateDTO(CouponCreateDTO dto) {
        // validMode = 1（固定时间）：validStart 和 validEnd 必填
        if (dto.getValidMode() == 1) {
            if (dto.getValidStart() == null || dto.getValidEnd() == null) {
                throw new CouponException(MessageConstant.VALID_MODE_FIXED_TIME_REQUIRED);
            }
            if (dto.getValidEnd().isBefore(dto.getValidStart())) {
                throw new CouponException(MessageConstant.VALID_END_BEFORE_START);
            }
        }

        // validMode = 2（领券后N天）：receiveValidDays 必填
        if (dto.getValidMode() == 2) {
            if (dto.getValidDays() == null || dto.getValidDays() <= 0) {
                throw new CouponException(MessageConstant.VALID_MODE_RECEIVE_DAYS_REQUIRED);
            }
        }

        // type = 1（满减）：conditionAmount 必填且大于0
        if (dto.getType() == 1) {
            if (dto.getConditionAmount() == null || dto.getConditionAmount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new CouponException(MessageConstant.FULL_REDUCTION_CONDITION_REQUIRED);
            }
        }

        // type = 2（折扣）：discountAmount 应在 0~1 之间
        if (dto.getType() == 2) {
            if (dto.getDiscountAmount() == null

                    || dto.getDiscountAmount().compareTo(BigDecimal.ZERO) <= 0
                    || dto.getDiscountAmount().compareTo(BigDecimal.ONE) >= 0) {
                throw new CouponException(MessageConstant.DISCOUNT_RATE_INVALID);
            }
        }

        // type = 3（无门槛）：discountAmount 必须大于0
        if (dto.getType() == 3) {
            if (dto.getDiscountAmount() == null || dto.getDiscountAmount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new CouponException(MessageConstant.NO_THRESHOLD_DISCOUNT_REQUIRED);
            }
        }
    }
}
