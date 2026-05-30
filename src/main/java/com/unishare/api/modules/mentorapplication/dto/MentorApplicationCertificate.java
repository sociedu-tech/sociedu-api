package com.unishare.api.modules.mentorapplication.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MentorApplicationCertificate {
    private String name;
    private String issuer;
    private Integer year;
    private String url;
}
