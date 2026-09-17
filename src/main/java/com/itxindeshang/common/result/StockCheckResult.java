package com.itxindeshang.common.result;

import com.itxindeshang.pojo.entity.StockInsufficientItem;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class StockCheckResult {

    private boolean success;

    /**
     * 库存不足的商品明细
     */
    private List<StockInsufficientItem> insufficientItems;
}
