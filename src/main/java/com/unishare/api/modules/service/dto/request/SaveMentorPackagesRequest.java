package com.unishare.api.modules.service.dto.request;

import jakarta.validation.Valid;
import lombok.Data;
import java.util.List;

@Data
public class SaveMentorPackagesRequest {
    @Valid
    private List<MentorPackageRequest> packages;

    @Data
    public static class MentorPackageRequest {
        private String id;
        private String title;
        private String description;
        private java.math.BigDecimal price;
        private String duration;
    }
}
