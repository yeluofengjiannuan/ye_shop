package com.itxindeshang.service.impl;


import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itxindeshang.common.constant.MessageConstant;
import com.itxindeshang.common.exception.BusinessException;
import com.itxindeshang.common.exception.OrderException;
import com.itxindeshang.common.generator.SnowflakeIdGenerator;
import com.itxindeshang.common.mapstruct.CopyMapper;
import com.itxindeshang.common.result.Result;
import com.itxindeshang.common.result.StockCheckResult;
import com.itxindeshang.context.BaseContext;
import com.itxindeshang.infrastructure.mq.utils.MqProducerUtils;
import com.itxindeshang.infrastructure.redis.connect.RedisConnector;
import com.itxindeshang.infrastructure.redis.generator.RedisKeyGenerator;
import com.itxindeshang.job.delay.CancelUnpaidOrderDelayJob;
import com.itxindeshang.mapper.OrderMapper;
import com.itxindeshang.mapper.ProductMapper;
import com.itxindeshang.pojo.dto.OrderDTO;
import com.itxindeshang.pojo.dto.OrderItemDTO;
import com.itxindeshang.pojo.entity.*;
import com.itxindeshang.pojo.enums.OrderStatusEnum;
import com.itxindeshang.pojo.enums.PayTypeEnum;
import com.itxindeshang.pojo.vo.OrderAddressVO;
import com.itxindeshang.pojo.vo.OrderWithItemVO;
import com.itxindeshang.pojo.vo.ProductSpecVO;
import com.itxindeshang.service.*;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements OrderService {
    @Resource
    private CopyMapper copyMapper;

    @Resource
    private  SnowflakeIdGenerator snowflakeIdGenerator;

    @Resource
    private OrderItemService orderItemService;

    @Resource
    private AddressService addressService;

    @Resource
    private CouponUserService couponUserService;

    @Resource
    private CancelUnpaidOrderDelayJob cancelUnpaidOrderDelayJob;

    @Resource
    private ProductMapper productMapper;

    @Resource
    private CouponProductService couponProductService;

    @Resource
    private  CouponCategoryService couponCategoryService;

    @Resource
    private MqProducerUtils mqProducerUtils;

    /**
     * 新增订单
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<OrderWithItemVO> insertOrder(OrderDTO orderDTO) {
        // 1. 基础校验
        if (Objects.isNull(orderDTO.getOrderItems()) || orderDTO.getOrderItems().isEmpty()) {
            return Result.error(MessageConstant.DATA_ERROR);
        }

        try {
            // 2. 准备数据 (查地址、查券、查商品)
            OrderContext context = prepareOrderData(orderDTO);
            if (!context.isSuccess()) return Result.error(context.getMsg());

            // 3. 校验并锁定资源 (扣库存、锁优惠券)
            String errorMsg = validateAndLockResources(context);
            if (errorMsg != null) {
                return Result.error(errorMsg);
            }

            // 4. 计算金额 (总价、分摊优惠)
            calculateOrderAmount(context);

            // 5. 落库 (保存订单、订单项)
            OrderWithItemVO resultVO = persistOrder(context);

            // 6. 后置动作 (发消息)
            cancelUnpaidOrderDelayJob.setUnpaidOrderNoToDelayQueue(context.getOrder().getOrderNo());

            return Result.success(resultVO);

        } catch (Exception e) {
            // 如果有业务异常需要回滚特定操作（比如解锁优惠券），可以在这里处理
            // 但因为有 @Transactional，数据库操作会自动回滚
            throw e;
        }
    }

    /**
     * 主动取消订单
     * @param orderNo 订单号
     * @param cancelReason 取消原因
     */
    @Override
    public Result<?> cancelOrder(String orderNo, String cancelReason) {
        Order order = lambdaQuery().eq(Order::getOrderNo, orderNo).one();
        if (order == null) {
            return Result.error(MessageConstant.ORDER_NOT_EXIST);
        }
        if (order.getStatus() != OrderStatusEnum.PENDING_PAYMENT) {
            return Result.error(MessageConstant.ORDER_NOT_CANCEL);
            //TODO:这里只要处于支付状态后就先不得取消，更多逻辑后续优化，因为现在支付模块没处理好，后续回调和退款再优化
        }
        //这里加什么判断啊
        cancelUnpaidOrderDelayJob.cancelOrder(orderNo,cancelReason);
        //取消的情形需要多样考虑，根据订单情况考虑是取消，或者说取消要退钱，status==7是处理售后这里估计就可以是退款
        return Result.success();
    }

    /**
     * 支付成功
     * @param orderNo 订单号
     */
    @Override
    public Result<?> paySuccess(String orderNo) {
        //1.判断是否存在
        Order order = lambdaQuery().eq(Order::getOrderNo, orderNo).one();
        if (order == null) {
            throw new OrderException(MessageConstant.ORDER_NOT_EXIST);
        }
        //订单状态一点要改为支付成功就是待确认
        // 3. 记录支付时间
        boolean updateSuccess = lambdaUpdate()
                .set(Order::getPayTime, LocalDateTime.now())
                .set(Order::getPayType, PayTypeEnum.WECHAT_PAY)//这里先写微信支付，后续再看怎么优化
                .set(Order::getStatus, OrderStatusEnum.PENDING_CONFIRM)
                .eq(Order::getStatus, OrderStatusEnum.PENDING_PAYMENT) // 乐观锁思想：检查旧状态
                .eq(Order::getOrderNo, orderNo)
                .update();
        if (!updateSuccess) {
            // 如果更新失败，说明订单可能已经支付过了，或者状态被修改了
            // 这里直接返回成功，告诉支付平台“我收到了”，避免平台一直重试回调
            log.warn("订单 {} 状态非未支付，可能是重复回调，当前状态：{}", orderNo, order.getStatus());
            //TODO：这里失败的话金额会返回（这里因为没有开始接触微信支付接口先保持现在的逻辑
            return Result.success(MessageConstant.ORDER_PROCESSED);
        }
        cancelUnpaidOrderDelayJob.setPaidOrderNoToCancelDelayQueue(orderNo);
        //这里异步究竟要干嘛？后面再考虑吧TODO：1.发消息更新coupon的locked为used
        mqProducerUtils.sendOrderPaySuccess(orderNo);
        //你说支付回调失败怎么办？我怎么知道，现在只是测试后续第三方接口我再try...
        return Result.success();
    }

    /**
     * 根据订单号查看订单详情
     * @param orderNo 订单号
     */
    @Override
    public Result<OrderWithItemVO> getOrderDesc(String orderNo) {
        Order order = lambdaQuery().eq(Order::getOrderNo, orderNo).one();
        if (order == null) {
            return Result.error(MessageConstant.ORDER_NOT_EXIST);
        }
        List<OrderItem> items = orderItemService.lambdaQuery().eq(OrderItem::getOrderNo, orderNo).list();
        order.setOrderItems(items);
        OrderWithItemVO result = copyMapper.orderToOrderWithItemVO(order);
        OrderAddressVO addressVO =copyMapper.orderToOrderAddressVO(order);
        result.setAddress(addressVO);
        return Result.success(result);
    }



    // --- 拆分出的私有方法 ---

    /**
     * 1. 准备数据：查地址、查优惠券、查商品信息
     */
    private OrderContext prepareOrderData(OrderDTO orderDTO) {
        OrderContext context = new OrderContext();
        context.setOrderDTO(orderDTO);

        // 1.1 处理地址
        Address address = addressService.getById(orderDTO.getAddressId());
        if (address == null || !address.getUserId().equals(Long.valueOf(BaseContext.getUserId()))) {
            context.setFail("收货地址无效或不属于当前用户");
            return context;
        }
        context.setAddress(address);

        // 1.3 处理商品信息 (批量查询)
        List<Long> specIds = orderDTO.getOrderItems().stream().map(OrderItemDTO::getSpecId).distinct().toList();
        List<ProductSpecVO> specList = productMapper.selectSpecsBatch(specIds);
        Map<Long, ProductSpecVO> specMap = specList.stream().collect(Collectors.toMap(ProductSpecVO::getSpecId, Function.identity()));
        context.setSpecMap(specMap);

        if (orderDTO.getCouponUserId()!=null) {
            // 1.2 处理优惠券
            CouponUser couponUser = couponUserService.getById(orderDTO.getCouponUserId());
            if (couponUser == null) {
                context.setFail("优惠券不存在");
                return context;
            }
            //FIXME:这里和后续可以看情况，看看这个refund的数量究竟要怎么处理，refund就在这里先作为冗余字段，后续可以使用，现在退款先返回数量给unused吧
            if (couponUser.getUnusedCount().equals(0)) {
                context.setFail("优惠券不可用");
                return context;
            }
            // 这里只做读取，不做更新（更新放到 validate 阶段）
            context.setCouponUser(couponUser);
            Coupon coupon = RedisConnector.getHashObject(RedisKeyGenerator.couponDetail(couponUser.getCouponId()), Coupon.class);
            if (coupon == null) {
                context.setFail("优惠券信息异常");
                return context;
            }
            context.setCoupon(coupon);
            //1.4校验优惠券是否能覆盖商品
            List<Long> productIds = orderDTO.getOrderItems().stream().map(OrderItemDTO::getProductId).distinct().toList();
            List<OrderItemDTO> items = orderDTO.getOrderItems();
            if (coupon.getUseScope() == 1) {
                // 全场通用：所有商品都符合
                context.setApplicableProductIds(
                        items.stream().map(OrderItemDTO::getProductId).collect(Collectors.toSet())
                );
            } else if (coupon.getUseScope() == 2) {
                //指定商品
                List<CouponProduct> list = couponProductService.lambdaQuery().eq(CouponProduct::getCouponId, coupon.getId()).list();
                Set<Long> relatedProductIds = list.stream().map(CouponProduct::getProductId).collect(Collectors.toSet());
                // 取交集：订单商品中，哪些在优惠券关联的商品列表里
                Set<Long> applicableIds = items.stream()
                        .map(OrderItemDTO::getProductId)
                        .filter(relatedProductIds::contains)
                        .collect(Collectors.toSet());
                if (applicableIds.isEmpty()) {
                    context.setFail("订单商品不符合优惠券使用范围");
                    return context;
                }
                context.setApplicableProductIds(applicableIds);
            } else {
                // 指定分类：一条SQL直接查出符合条件的商品ID
                Set<Long> applicableIds = couponCategoryService
                        .selectApplicableProductIds(coupon.getId(), productIds)
                        .stream().collect(Collectors.toSet());

                if (applicableIds.isEmpty()) {
                    context.setFail("订单商品不符合优惠券使用范围");
                    return context;
                }
                context.setApplicableProductIds(applicableIds);
            }
        }

        return context;
    }

    /**
     * 2. 校验并锁定资源：校验商品状态、扣库存、锁优惠券
     */
    private String validateAndLockResources(OrderContext context) {
        OrderDTO orderDTO = context.getOrderDTO();
        List<String> errors = new ArrayList<>();

        // 2.1 校验商品信息 (价格、库存、上下架、归属)
        for (OrderItemDTO item : orderDTO.getOrderItems()) {
            ProductSpecVO dbSpec = context.getSpecMap().get(item.getSpecId());
            if (dbSpec == null) {
                //FIXME：好歹加哪个商品无效吧
                errors.add("商品规格无效");
                continue;
            }
            if (!dbSpec.getProductId().equals(item.getProductId())) {
                //FIXME:同理
                errors.add(MessageConstant.PRODUCT_SPEC_NOT_MATCH_PRODUCT);
            } else if (dbSpec.getProductStatus() != 1) {
                errors.add("商品【" + dbSpec.getProductName() + "】已下架");
            } else if (dbSpec.getPrice().compareTo(item.getPrice()) != 0) {
                errors.add("商品【" + dbSpec.getProductName() + "】价格已变动");
            } else if (dbSpec.getStock() < item.getQuantity()) {
                errors.add("商品【" + dbSpec.getProductName() + "】库存不足");
            }
        }
        if (!errors.isEmpty()) {
            return String.join("；", errors);
        }


        // 2.2 扣减库存 (原子操作)
        StockCheckResult stockResult = decareStock(orderDTO.getOrderItems());
        if (!stockResult.isSuccess()) {
            List<String> messages = stockResult.getInsufficientItems().stream()
                    .map(i ->i.getProductName()+ "商品库存不足")
                    .toList();
            return String.join("；", messages);
        }
        //扣除product的库存

        // 2.3 锁定优惠券 (更新数据库)
        CouponUser cu = context.getCouponUser();

        if (cu !=null) {
            if (cu.getUnusedCount() <= 0) {
                return MessageConstant.COUPON_EXPIRED;
            }
            boolean updated = couponUserService.lambdaUpdate()
                    .setSql("unused_count = unused_count - 1")
                    .setSql("locked_count = locked_count + 1")
                    .eq(CouponUser::getId, cu.getId())
                    .update();
            if (!updated) {
                return MessageConstant.COUPON_STATUS_ERROR;
            }
        }

        return null;
    }

    /**
     * 3. 计算金额
     */
    private void calculateOrderAmount(OrderContext context) {

        List<OrderItemDTO> itemDTOs = context.getOrderDTO().getOrderItems();
        Map<Long, ProductSpecVO> specMap = context.getSpecMap();
        // --- 优化开始 ---
        // 1. 获取优惠券ID，可能为null
        Long couponUserId = context.getOrderDTO().getCouponUserId();
        // 2. 获取适用商品ID集合，如果没有优惠券，则为空集合
        Set<Long> applicableProductIds = (couponUserId != null) ? context.getApplicableProductIds() : Collections.emptySet();
        // --- 优化结束 ---

        // 转换为 OrderItem 实体，计算小计
        List<OrderItem> orderItems = itemDTOs.stream().map(dto -> {
            OrderItem item = copyMapper.orderItemDTOToOrderItem(dto);
            ProductSpecVO spec = specMap.get(dto.getSpecId());

            item.setProductName(spec.getProductName());
            item.setPrice(spec.getPrice());
            //先设置金额，后续有优惠的会修改
            item.setPayAmount(item.getSubtotal());
            item.setDiscountAmount(BigDecimal.ZERO);
            return item;
        }).collect(Collectors.toCollection(ArrayList::new)); // 显式转为可变集合

        // 计算总价
        BigDecimal totalGoodsAmount = orderItems.stream()
                .map(OrderItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        //FIXME:这里加dto的运费金额
        // 3. 如果没有使用优惠券，直接设置总金额并返回，跳过所有优惠券计算逻辑
        if (couponUserId == null) {
            Order order = copyMapper.orderDTOAndAddressToOrder(context.getOrderDTO(), context.getAddress());
            order.setOrderItems(orderItems);
            order.setOrderNo(snowflakeIdGenerator.generateOrderNo());
            order.setUserId(Long.valueOf(BaseContext.getUserId()));
            order.setTotalGoodsAmount(totalGoodsAmount);
            // 优惠券相关金额设为0
            order.setCouponDiscountAmount(BigDecimal.ZERO);
            order.setTotalAmount(totalGoodsAmount.add(order.getFreight()));
            context.setOrder(order);
            return;
        }
        // --- 优化结束 ---
        // 计算优惠金额：只针对适用优惠券的商品
        BigDecimal applicableAmount = orderItems.stream()
                .filter(item -> applicableProductIds.contains(item.getProductId()))
                .map(OrderItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal couponDiscount = calculateDiscount(context.getCoupon(), applicableAmount);

        // 分摊优惠（只分摊到适用商品上）
        List<OrderItem> applicableItems = orderItems.stream()
                .filter(item -> applicableProductIds.contains(item.getProductId()))
                .toList();
        allocateDiscountToItems(applicableItems, couponDiscount);

        // 将更新后的 applicableItems 转为 Map，方便快速查找
        Map<Long, OrderItem> updatedItemsMap = applicableItems.stream()
                .collect(Collectors.toMap(OrderItem::getSpecId, Function.identity()));

        // 遍历主列表 orderItems，用更新后的对象替换掉旧的
        for (int i = 0; i < orderItems.size(); i++) {
            OrderItem item = orderItems.get(i);
            // 如果这个商品在更新后的列表里，就进行替换
            if (updatedItemsMap.containsKey(item.getSpecId())) {
                orderItems.set(i, updatedItemsMap.get(item.getSpecId()));
            }
        }


        //FIXME:这里的优惠之类的全在这里不对劲

        // 封装 Order 对象
        Order order = copyMapper.orderDTOAndAddressToOrder(context.getOrderDTO(), context.getAddress());
        order.setOrderItems(orderItems);
        order.setTotalGoodsAmount(totalGoodsAmount);
        order.setCouponDiscountAmount(couponDiscount);
        order.setTotalAmount(totalGoodsAmount.add(order.getFreight()).subtract(couponDiscount));
        order.setOrderNo(snowflakeIdGenerator.generateOrderNo());
        order.setUserId(Long.valueOf(BaseContext.getUserId()));

        context.setOrder(order);
    }

    /**
     * 4. 落库
     */
    private OrderWithItemVO persistOrder(OrderContext context) {
        Order order = context.getOrder();
        if (context.getCoupon()!=null) {
            order.setCouponTemplateId(context.getCoupon().getId());
        }
        //FIXME:数据库新增coupon字段
        // 保存订单主表
        this.save(order);

        // 设置外键
        List<OrderItem> orderItems = order.getOrderItems();
//        orderItems.forEach(item -> item.setOrderId(order.getId()));
        orderItems  = orderItems.stream()
                .map(item -> item.setOrderId(order.getId()))
                .map(item -> item.setOrderNo(order.getOrderNo()))
                .toList();
        // 保存订单项
        orderItemService.saveBatch(orderItems);
        //FIXME：这里的addressVO肯定没弄好
        OrderWithItemVO result = copyMapper.orderToOrderWithItemVO(order);
        OrderAddressVO addressVO = copyMapper.orderToOrderAddressVO(order);
        result.setAddress(addressVO);
        return result;
    }

    // ... 原有的 decareStock, calculateDiscount, allocateDiscountToItems 保持不变 ...

    /**
     * 计算折扣价格
     * @param coupon 优惠券
     * @param totalGoodsAmount 总金额
     * TODO：这里的优惠券还区分use_scope
     */
    public BigDecimal calculateDiscount(Coupon coupon, BigDecimal totalGoodsAmount) {
        BigDecimal discount = BigDecimal.ZERO;

        switch (coupon.getType()) {
            case 1: // 满减
                if (totalGoodsAmount.compareTo(coupon.getConditionAmount()) >= 0) {
                    discount = coupon.getDiscountAmount();
                }
                break;

            case 2: // 折扣
                // discountAmount 存的是折扣率，如 0.8 表示 8 折
                discount = totalGoodsAmount.multiply(
                        BigDecimal.ONE.subtract(coupon.getDiscountAmount())
                        //策略舍去
                ).setScale(2, RoundingMode.HALF_UP);
                // 封顶
                if (coupon.getMaxDiscount() != null && discount.compareTo(coupon.getMaxDiscount()) > 0) {
                    discount = coupon.getMaxDiscount();
                }
                break;

            case 3: // 无门槛
                discount = coupon.getDiscountAmount();
                break;
        }

        // 兜底：优惠不能超过商品总价
        if (discount.compareTo(totalGoodsAmount) > 0) {
            discount = totalGoodsAmount;
        }

        return discount;
    }
    public void allocateDiscountToItems(List<OrderItem> items, BigDecimal totalDiscount) {
        // 计算传入商品的总金额（调用方已保证只传入适用优惠券的商品）
        BigDecimal totalGoodsAmount = items.stream()
                .map(OrderItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalGoodsAmount.compareTo(BigDecimal.ZERO) == 0) {
            return;
        }
        BigDecimal allocatedSum = BigDecimal.ZERO;

        for (int i = 0; i < items.size(); i++) {
            OrderItem item = items.get(i);
            BigDecimal itemSubtotal = item.getSubtotal();
            BigDecimal itemDiscount;

            if (i == items.size() - 1) {
                // 最后一项兜底，保证总和精确
                itemDiscount = totalDiscount.subtract(allocatedSum);
                // 兜底项的优惠不能超过它自己的小计
                if (itemDiscount.compareTo(itemSubtotal) > 0) {
                    itemDiscount = itemSubtotal;
                }
            } else {
                // 按比例分摊，向下取整
                itemDiscount = totalDiscount
                        .multiply(itemSubtotal)
                        .divide(totalGoodsAmount, 2, RoundingMode.DOWN);
            }

            // 写入 OrderItem
            item.setDiscountAmount(itemDiscount);
            item.setPayAmount(itemSubtotal.subtract(itemDiscount));

            allocatedSum = allocatedSum.add(itemDiscount);
        }
    }


    /**
     * 库存原子减少
     */
    private StockCheckResult decareStock(List<OrderItemDTO> orderItems) {
        List<StockInsufficientItem> insufficientItems = orderItems.stream()
                .filter(item -> {
                    int affected = productMapper.deductProductAndSpecStock(
                            item.getProductId(),
                            item.getSpecId(),
                            item.getQuantity()
                    );
                    return affected == 0; // 扣减失败的留下来
                })
                .map(item -> {
                    // 扣减失败，查一下当前库存用于提示
                    return StockInsufficientItem.builder()
                            .productId(item.getProductId())
                            .specId(item.getSpecId())
                            .productName(item.getProductName())
                            .requiredQuantity(item.getQuantity())
                            .build();
                })
                .toList();

        return StockCheckResult.builder()
                .success(insufficientItems.isEmpty())
                .insufficientItems(insufficientItems)
                .build();

    }


}
