package com.proconnect.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ServiceDTO {
    private Long id;
    private String title;
    private String description;
    private BigDecimal priceMin;
    private BigDecimal priceMax;
    private String currency;
    private String priceUnit;
    private String duration;
}
