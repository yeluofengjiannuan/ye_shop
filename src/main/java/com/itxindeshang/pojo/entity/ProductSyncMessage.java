package com.itxindeshang.pojo.entity;

import com.itxindeshang.pojo.enums.CommonStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductSyncMessage implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long productId;
    private CommonStatus status;
}
