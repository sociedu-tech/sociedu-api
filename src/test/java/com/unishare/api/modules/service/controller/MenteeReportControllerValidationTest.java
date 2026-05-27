package com.unishare.api.modules.service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.unishare.api.config.GlobalExceptionHandler;
import com.unishare.api.modules.service.dto.request.CreateReportRequest;
import com.unishare.api.modules.service.service.ProgressReportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.UUID;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MenteeReportControllerValidationTest {

    private MockMvc mockMvc;
    private ProgressReportService progressReportService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        progressReportService = Mockito.mock(ProgressReportService.class);
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
                            java.util.List.of("USER"),
                            java.util.List.of(),
                            true
                    );
                }
            };

        mockMvc = MockMvcBuilders.standaloneSetup(new MenteeReportController(progressReportService))
                .setCustomArgumentResolvers(principalResolver)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void submitReport_whenMentorIdMissing_shouldFailValidation() throws Exception {
        CreateReportRequest request = validRequest();
        request.setMentorId(null);

        mockMvc.perform(post("/api/v1/mentee/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.fields.mentorId").value("ID Mentor không được để trống"));

        verifyNoInteractions(progressReportService);
    }

    @Test
    void submitReport_whenTitleBlank_shouldFailValidation() throws Exception {
        CreateReportRequest request = validRequest();
        request.setTitle(" ");

        mockMvc.perform(post("/api/v1/mentee/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.fields.title").value("Tiêu đề không được để trống"));

        verifyNoInteractions(progressReportService);
    }

    @Test
    void submitReport_whenContentBlank_shouldFailValidation() throws Exception {
        CreateReportRequest request = validRequest();
        request.setContent(" ");

        mockMvc.perform(post("/api/v1/mentee/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.fields.content").value("Nội dung báo cáo không được để trống"));

        verifyNoInteractions(progressReportService);
    }

    private CreateReportRequest validRequest() {
        CreateReportRequest request = new CreateReportRequest();
        request.setMentorId(UUID.randomUUID());
        request.setTitle("Week 1");
        request.setContent("Initial content");
        request.setAttachmentUrl("https://cdn.example.com/report.pdf");
        return request;
    }
}
