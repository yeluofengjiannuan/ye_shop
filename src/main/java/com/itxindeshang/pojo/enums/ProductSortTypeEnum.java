package com.itxindeshang.pojo.enums;


import com.itxindeshang.pojo.entity.Product;
import com.itxindeshang.util.DateUtils;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public enum ProductSortTypeEnum {
    //TODO:sortfield换成那个product.field.---
    DEFAULT("default", "sales_count DESC", Product.Fields.salesCount,CommonSortTypeEnum.DESC, "默认按照销量排行"),
    PRICE_ASC("priceAsc", "price ASC", Product.Fields.price,CommonSortTypeEnum.ASC, "价格升序"),
    PRICE_DESC("priceDesc", "price DESC",  Product.Fields.price,CommonSortTypeEnum.DESC, "价格降序"),
    NEWEST("newest", "update_time DESC",Product.Fields.updateTime,  CommonSortTypeEnum.DESC, "最新上架");
    /**
     * 前端传的 String
     */
    private final String value;
    /**
     * 拼接的 sql 字段
     */
    private final String dbValue;

    /**
     * 排序字段
     */
    private final String sortField;

    /**
     * 排序方式
     */
    private final CommonSortTypeEnum commonSortTypeEnum;


    /**
     * 文字描述
     */
    private final String desc;


    /**
     * static 根据 value 返回枚举
     * @param value 前端传参字段
     * @return 枚举
     */
    public static ProductSortTypeEnum getByValue(String value) {
        for (ProductSortTypeEnum productSortTypeEnum : values()) {
            if (productSortTypeEnum.value.equals(value)) {
                return productSortTypeEnum;
            }
        }
        throw new IllegalArgumentException("无效的ProductSortType.value:" + value);
    }

    public static int compare(Product p1, Product p2, ProductSortTypeEnum productSortTypeEnum) {
        switch (productSortTypeEnum) {
            case DEFAULT -> {
                return p2.getSalesCount().compareTo(p1.getSalesCount());
            }
            case PRICE_ASC -> {
                return p1.getPrice().compareTo(p2.getPrice());
            }
            case PRICE_DESC -> {
                return p2.getPrice().compareTo(p1.getPrice());
            }
            case NEWEST -> {
                return p2.getUpdateTime().compareTo(p1.getUpdateTime());
            }
        }
        throw new IllegalArgumentException("无效的ProductSortType: " + productSortTypeEnum);
    }

    /**
     * 根据枚举格式化 游标排序值
     * @param productSortTypeEnum 枚举
     * @param sortValue 游标排序值
     * @return 格式化 游标排序值
     */
    public static String filterFormatSortValue(ProductSortTypeEnum productSortTypeEnum , String sortValue){
        if (StringUtils.isBlank(sortValue)) {
            return sortValue;
        }
        if (productSortTypeEnum==ProductSortTypeEnum.NEWEST){
            LocalDateTime localDateTime = DateUtils.parseToLocalDateTime(sortValue);
            sortValue=localDateTime.toString();
        }
        return sortValue;
    }
}
