package com.itxindeshang.common.result;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CursorCommonEntity {

    @NotNull
    private String sortType;

    private String sortValue ;

    private Long sortId;

    private Integer querySize = 20;
}
