package com.itxindeshang.pojo.dto;

import com.itxindeshang.pojo.vo.SimpleProductVO;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CursorProduct {
    SimpleProductVO vo;
    Long sortId;
    String sortValue;
}
