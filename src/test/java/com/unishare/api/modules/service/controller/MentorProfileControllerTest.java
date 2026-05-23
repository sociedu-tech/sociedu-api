package com.unishare.api.modules.service.controller;

import com.unishare.api.common.dto.AppException;
import com.unishare.api.config.GlobalExceptionHandler;
import com.unishare.api.infrastructure.security.CustomUserPrincipal;
import com.unishare.api.modules.mentor.controller.MentorController;
import com.unishare.api.modules.mentor.exception.MentorErrorCode;
import com.unishare.api.modules.mentor.dto.MentorResponse;
import com.unishare.api.modules.mentor.service.MentorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MentorProfileControllerTest {

    private MockMvc mockMvc;
    private MentorService mentorProfileService;
    private UUID userId;
    private CustomUserPrincipal principal;

    @BeforeEach
    void setUp() {
        mentorProfileService = Mockito.mock(MentorService.class);
        userId = UUID.randomUUID();
        principal = new CustomUserPrincipal(
                userId,
                "mentor@gmail.com",
                "hashedpassword",
                List.of("MENTOR"),
                List.of("VIEW_PROFILE"),
                true
        );

        HandlerMethodArgumentResolver principalResolver = new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return parameter.getParameterType().equals(CustomUserPrincipal.class);
            }

            @Override
            public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                           NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
                return principal;
            }
        };

        mockMvc = MockMvcBuilders.standaloneSetup(new MentorController(mentorProfileService))
                .setCustomArgumentResolvers(principalResolver)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void getMentorProfile_shouldReturnProfileWithoutPackages() throws Exception {
        UUID mentorId = UUID.randomUUID();
        MentorResponse response = MentorResponse.builder()
                .userId(mentorId)
                .headline("Career mentor")
                .verificationStatus("verified")
                .build();

        when(mentorProfileService.getMentorProfile(eq(mentorId))).thenReturn(response);

        mockMvc.perform(get("/api/v1/mentors/{id}", mentorId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userId").value(mentorId.toString()))
                .andExpect(jsonPath("$.data.headline").value("Career mentor"))
                .andExpect(jsonPath("$.data.packages").doesNotExist());
    }

    @Test
    void getMyProfile_shouldReturnProfile() throws Exception {
        MentorResponse response = MentorResponse.builder()
                .userId(userId)
                .headline("My headline")
                .verificationStatus("verified")
                .build();

        when(mentorProfileService.getMentorProfile(eq(userId))).thenReturn(response);

        mockMvc.perform(get("/api/v1/mentors/me/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userId").value(userId.toString()))
                .andExpect(jsonPath("$.data.headline").value("My headline"));
    }

    @Test
    void submitProfileVerification_shouldSetPendingAndReturnResponse() throws Exception {
        MentorResponse response = MentorResponse.builder()
                .userId(userId)
                .headline("My headline")
                .verificationStatus("PENDING")
                .build();

        when(mentorProfileService.submitProfileVerification(eq(userId))).thenReturn(response);

        mockMvc.perform(post("/api/v1/mentors/me/profile/submit"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userId").value(userId.toString()))
                .andExpect(jsonPath("$.data.verificationStatus").value("PENDING"));
    }

    @Test
    void submitProfileVerification_whenIncomplete_shouldReturnBadRequest() throws Exception {
        when(mentorProfileService.submitProfileVerification(eq(userId)))
                .thenThrow(new AppException(MentorErrorCode.PROFILE_INCOMPLETE, "Hồ sơ chưa hoàn thiện"));

        mockMvc.perform(post("/api/v1/mentors/me/profile/submit"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("Hồ sơ chưa hoàn thiện"));
    }

    @Test
    void submitProfileVerification_whenAlreadyVerified_shouldReturnBadRequest() throws Exception {
        when(mentorProfileService.submitProfileVerification(eq(userId)))
                .thenThrow(new AppException(MentorErrorCode.PROFILE_ALREADY_VERIFIED, "Hồ sơ mentor đã được duyệt rồi."));

        mockMvc.perform(post("/api/v1/mentors/me/profile/submit"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("Hồ sơ mentor đã được duyệt rồi."));
    }
}
