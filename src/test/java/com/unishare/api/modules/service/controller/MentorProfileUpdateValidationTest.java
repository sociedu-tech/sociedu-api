package com.unishare.api.modules.service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.unishare.api.config.GlobalExceptionHandler;
import com.unishare.api.modules.mentor.controller.MentorController;
import com.unishare.api.modules.mentor.dto.MentorRequest;
import com.unishare.api.modules.mentor.service.MentorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.math.BigDecimal;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MentorProfileUpdateValidationTest {

    private MockMvc mockMvc;
    private MentorService mentorService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mentorService = Mockito.mock(MentorService.class);
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

        mockMvc = MockMvcBuilders.standaloneSetup(new MentorController(mentorService))
                .setCustomArgumentResolvers(principalResolver)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void updateMyProfile_whenHeadlineBlank_shouldFailValidation() throws Exception {
        MentorRequest request = validRequest();
        request.setHeadline(" ");

        mockMvc.perform(put("/api/v1/mentors/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.fields.headline").value("Headline không được để trống"));

        verifyNoInteractions(mentorService);
    }

    @Test
    void updateMyProfile_whenExpertiseBlank_shouldFailValidation() throws Exception {
        MentorRequest request = validRequest();
        request.setExpertise(" ");

        mockMvc.perform(put("/api/v1/mentors/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.fields.expertise").value("Expertise không được để trống"));

        verifyNoInteractions(mentorService);
    }

    @Test
    void updateMyProfile_whenBasePriceNegative_shouldFailValidation() throws Exception {
        MentorRequest request = validRequest();
        request.setBasePrice(new BigDecimal("-1"));

        mockMvc.perform(put("/api/v1/mentors/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.fields.basePrice").value("Base price phải lớn hơn hoặc bằng 0"));

        verifyNoInteractions(mentorService);
    }

    private MentorRequest validRequest() {
        MentorRequest request = new MentorRequest();
        request.setHeadline("Career mentor");
        request.setExpertise("Product");
        request.setBasePrice(new BigDecimal("100.00"));
        return request;
    }
}
