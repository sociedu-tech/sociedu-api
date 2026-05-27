package com.unishare.api.modules.service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.unishare.api.config.GlobalExceptionHandler;
import com.unishare.api.modules.service.dto.request.CreateServicePackageRequest;
import com.unishare.api.modules.service.service.CatalogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MentorControllerValidationTest {

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
                            java.util.UUID.randomUUID(),
                            "user@gmail.com",
                            "hashedpassword",
                            java.util.List.of("MENTOR"),
                            java.util.List.of(),
                            true
                    );
                }
            };

        mockMvc = MockMvcBuilders.standaloneSetup(new MentorCatalogController(catalogService))
                .setCustomArgumentResolvers(principalResolver)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void addPackage_whenCurriculumsEmpty_shouldFailValidation() throws Exception {
        CreateServicePackageRequest request = validRequest();
        request.setCurriculums(List.of());

        mockMvc.perform(post("/api/v1/mentors/me/packages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.fields.curriculums").value("Danh sách curriculum không được để trống"));

        verifyNoInteractions(catalogService);
    }

    @Test
    void addPackage_whenPriceNegative_shouldFailValidation() throws Exception {
        CreateServicePackageRequest request = validRequest();
        request.setPrice(new BigDecimal("-1.00"));

        mockMvc.perform(post("/api/v1/mentors/me/packages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.fields.price").value("Giá gói dịch vụ phải lớn hơn hoặc bằng 0"));

        verifyNoInteractions(catalogService);
    }

    @Test
    void addPackage_whenDurationLessThanOne_shouldFailValidation() throws Exception {
        CreateServicePackageRequest request = validRequest();
        request.setDuration(0);

        mockMvc.perform(post("/api/v1/mentors/me/packages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.fields.duration").value("Thời lượng gói dịch vụ phải lớn hơn hoặc bằng 1"));

        verifyNoInteractions(catalogService);
    }

    private CreateServicePackageRequest validRequest() {
        CreateServicePackageRequest request = new CreateServicePackageRequest();
        request.setName("Career Planning");
        request.setDescription("Package description");
        request.setPrice(new BigDecimal("120.00"));
        request.setDuration(3);
        request.setDeliveryType("ONLINE");

        CreateServicePackageRequest.CurriculumRequest curriculum = new CreateServicePackageRequest.CurriculumRequest();
        curriculum.setTitle("Session 1");
        curriculum.setDescription("Intro");
        curriculum.setOrderIndex(1);
        curriculum.setDuration(60);
        request.setCurriculums(List.of(curriculum));
        return request;
    }
}
