package com.unishare.api.modules.service.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class UpdateServicePackageVersionRequest {
    @DecimalMin("0.0") private BigDecimal price;
    @Min(1) private Integer duration;
    private String deliveryType; // ONLINE, OFFLINE, HYBRID
}
