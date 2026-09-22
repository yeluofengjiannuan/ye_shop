package com.itxindeshang.common.mapstruct;

import com.itxindeshang.infrastructure.es.document.ProductDocument;
import com.itxindeshang.pojo.UserInfo;
import com.itxindeshang.pojo.dto.*;
import com.itxindeshang.pojo.entity.*;
import com.itxindeshang.pojo.vo.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE, // 忽略字段不匹配警告
        unmappedSourcePolicy = ReportingPolicy.IGNORE)  // 忽略源对象多余字段)
public interface CopyMapper {

    UserInfo sysUserToUserInfo(SysUser sysUser);

    Category categoryDTOToCategroy(CategoryDTO categoryDTO);

    Product productDTOToProduct(ProductDTO productDTO);

    Product productUpdateDTOToProduct(ProductUpdateDTO productUpdateDTO);

    @Mapping(source = "cart.id", target = "id")
    @Mapping(source = "cart.userId", target = "userId")
    @Mapping(source = "cart.productId", target = "productId")
    @Mapping(source = "cart.specId", target = "specId")
    @Mapping(source = "cart.quantity", target = "quantity")
    @Mapping(source = "cart.createTime", target = "createTime")
    @Mapping(source = "cart.updateTime", target = "updateTime")

    @Mapping(source = "detail.price", target = "price")
    @Mapping(source = "detail.stock", target = "stock")
    @Mapping(source = "detail.specText", target = "specText")
    @Mapping(source = "detail.productName", target = "productName")
    @Mapping(source = "detail.productImage", target = "productImage")
    CartItem toCartItem(Cart cart, CartProductSpecDTO detail);

    Address addressDTOToAddress(AddressDTO addressDTO);

    Coupon couponCreateDTOToCoupon(CouponCreateDTO couponCreateDTO);

    @Mapping(source = "coupon.id", target = "couponId")
    @Mapping(source = "coupon.name", target = "couponName")
    @Mapping(source = "coupon.type", target = "type")
    @Mapping(source = "coupon.conditionAmount", target = "conditionAmount")
    @Mapping(source = "coupon.discountAmount",target = "discountAmount")
    @Mapping(source = "couponUser.id",target = "couponUserId")
    @Mapping(source = "couponUser.unusedCount",target = "unusedCount")
    @Mapping(source = "couponUser.lockedCount",target = "lockedCount")
    @Mapping(source = "couponUser.expireTime",target = "expireTime")
    CouponUserVO toCouponUserVO(Coupon coupon, CouponUser couponUser);

    Order orderDTOToOrder(OrderDTO orderDTO);

    OrderItem orderItemDTOToOrderItem(OrderItemDTO orderItemDTO);
    //这里有orderItem->orderItemVO(这算机制吗？好像是自己生成了
    OrderWithItemVO orderToOrderWithItemVO(Order order);

    @Mapping(source = "orderDTO.couponUserId",target = "couponUserId")
    @Mapping(source = "orderDTO.remark",target = "remark")
    @Mapping(source = "orderDTO.freight",target = "freight")
    @Mapping(source = "orderDTO.totalAmount",target = "totalAmount")
    @Mapping(source = "address.receiver",target = "receiverName")
    @Mapping(source = "address.phone",target = "receiverPhone")
    @Mapping(source = "address.province",target = "receiverProvince")
    @Mapping(source = "address.city",target = "receiverCity")
    @Mapping(source = "address.district",target = "receiverDistrict")
    @Mapping(source = "address.detailAddress",target = "receiverDetailAddress")
    @Mapping(target = "id", ignore = true)  // 加上这一行
    @Mapping(target = "createTime", ignore = true)  // 加上这一行
    @Mapping(target = "updateTime", ignore = true)  // 加上这一行
    Order orderDTOAndAddressToOrder(OrderDTO orderDTO, Address address);

    @Mapping(source = "order.receiverName",target = "receiver")
    @Mapping(source = "order.receiverPhone",target = "phone")
    @Mapping(source = "order.receiverProvince",target = "province")
    @Mapping(source = "order.receiverCity",target = "city")
    @Mapping(source = "order.receiverDistrict",target = "district")
    @Mapping(source = "order.receiverDetailAddress",target = "detailAddress")
    OrderAddressVO orderToOrderAddressVO(Order order);

    ProductDocument productToProductDocument(Product product);

//    ProductVO ProductDocumentToProductVO(ProductDocument productDocument);

    SimpleProductVO ProductDocumentToSimpleProductVO(ProductDocument productDocument);

    ProductSpecVO productSpecToProductSpecVO(ProductSpec resultProductSpec);
}
