package org.whiteicetea.warehousemanagementsystem.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;


@Data
public class Goods {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String goodsCode;
    private String goodsName;
    private String category;
    private String unit;
    private BigDecimal price;
    private Integer stockQuantity;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
