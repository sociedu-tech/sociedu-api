package com.unishare.api.modules.service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.unishare.api.config.GlobalExceptionHandler;
import com.unishare.api.modules.service.dto.request.CreateServicePackageVersionRequest;
import com.unishare.api.modules.service.service.CatalogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ServicePackageVersionControllerValidationTest {

    private MockMvc mockMvc;
    private CatalogService catalogService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        catalogService = Mockito.mock(CatalogService.class);
        objectMapper = new ObjectMapper();

        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        org.springframework.web.method.support.HandlerMethodArgumentResolver principalResolver = 
            new org.springframework.web.method.support.HandlerMethodArgumentResolver() {
                @Override
                public boolean supportsParameter(org.springframework.core.MethodParameter parameter) {
                    return parameter.getParameterType().equals(com.unishare.api.infrastructure.security.CustomUserPrincipal.class);
                }

                @Override
                public Object resolveArgument(org.springframework.core.MethodParameter parameter, 
                                               org.springframework.web.method.support.ModelAndViewContainer mavContainer,
                                               org.springframework.web.context.request.NativeWebRequest webRequest, 
                                               org.springframework.web.bind.support.WebDataBinderFactory binderFactory) {
                    return new com.unishare.api.infrastructure.security.CustomUserPrincipal(
                            UUID.randomUUID(),
                            "user@gmail.com",
                            "hashedpassword",
                            java.util.List.of("MENTOR"),
                            java.util.List.of(),
                            true
                    );
                }
            };

        mockMvc = MockMvcBuilders.standaloneSetup(new ServicePackageController(catalogService))
                .setCustomArgumentResolvers(principalResolver)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void createPackageVersion_whenPriceNegative_shouldFailValidation() throws Exception {
        CreateServicePackageVersionRequest request = new CreateServicePackageVersionRequest();
        request.setPrice(new BigDecimal("-1"));
        request.setDuration(3);
        request.setDeliveryType("ONLINE");

        mockMvc.perform(post("/api/v1/service-packages/{id}/versions", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.fields.price").value("Giá version phải lớn hơn hoặc bằng 0"));

        verifyNoInteractions(catalogService);
    }

    @Test
    void createPackageVersion_whenDurationLessThanOne_shouldFailValidation() throws Exception {
        CreateServicePackageVersionRequest request = new CreateServicePackageVersionRequest();
        request.setPrice(new BigDecimal("100"));
        request.setDuration(0);
        request.setDeliveryType("ONLINE");

        mockMvc.perform(post("/api/v1/service-packages/{id}/versions", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.fields.duration").value("Thời lượng version phải lớn hơn hoặc bằng 1"));

        verifyNoInteractions(catalogService);
    }
}
