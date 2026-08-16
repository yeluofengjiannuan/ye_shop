package com.itxindeshang.common.result;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 游标查询通用返回类
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CursorCommonResult {
    /**
     * 游标返回实体
     */
    private CursorCommonEntity cursorCommonEntity;

    /**
     * 查询列表
     */
    private List<?> list;

    private Boolean isEnd = false;
}
