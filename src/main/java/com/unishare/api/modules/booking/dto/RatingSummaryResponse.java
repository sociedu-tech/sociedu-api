package com.unishare.api.modules.booking.dto;

import lombok.Builder;
import lombok.Data;
import java.util.Map;

@Data
@Builder
public class RatingSummaryResponse {
    private Double ratingAvg;
    private Integer ratingCount;
    private Map<Integer, Long> distribution;
}
