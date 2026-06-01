package com.unishare.api.modules.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.unishare.api.config.GlobalExceptionHandler;
import com.unishare.api.modules.user.dto.*;
import com.unishare.api.modules.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Unit tests cho {@link PublicUserController}.
 * Endpoint công khai - không cần xác thực.
 */
@ExtendWith(MockitoExtension.class)
class PublicUserControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private UserService userService;

    @InjectMocks
    private PublicUserController publicUserController;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        mockMvc = MockMvcBuilders.standaloneSetup(publicUserController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /{id}/profile - Trả về full profile 200 với đầy đủ thông tin")
    void getFullProfile_ReturnsSuccess_WithFullData() throws Exception {
        UUID userId = UUID.randomUUID();

        UserProfileResponse profile = new UserProfileResponse();
        profile.setUserId(userId);
        profile.setFirstName("Nguyen");
        profile.setLastName("Van A");
        profile.setHeadline("Software Engineer");

        UserEducationResponse education = new UserEducationResponse();
        education.setId(UUID.randomUUID());
        education.setMajorName("Computer Science");

        UserLanguageResponse language = new UserLanguageResponse();
        language.setId(UUID.randomUUID());
        language.setLanguage("VI");
        language.setLevel("NATIVE");

        UserExperienceResponse experience = new UserExperienceResponse();
        experience.setId(UUID.randomUUID());
        experience.setCompany("FPT Software");

        UserCertificateResponse certificate = new UserCertificateResponse();
        certificate.setId(UUID.randomUUID());
        certificate.setName("AWS Certified");

        UserFullProfileResponse fullProfile = UserFullProfileResponse.builder()
                .profile(profile)
                .educations(List.of(education))
                .languages(List.of(language))
                .experiences(List.of(experience))
                .certificates(List.of(certificate))
                .build();

        when(userService.getFullProfile(userId)).thenReturn(fullProfile);

        mockMvc.perform(get("/api/v1/users/{id}/profile", userId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Success"))
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.data.profile.firstName").value("Nguyen"))
                .andExpect(jsonPath("$.data.profile.lastName").value("Van A"))
                .andExpect(jsonPath("$.data.profile.headline").value("Software Engineer"))
                .andExpect(jsonPath("$.data.educations[0].majorName").value("Computer Science"))
                .andExpect(jsonPath("$.data.languages[0].language").value("VI"))
                .andExpect(jsonPath("$.data.languages[0].level").value("NATIVE"))
                .andExpect(jsonPath("$.data.experiences[0].company").value("FPT Software"))
                .andExpect(jsonPath("$.data.certificates[0].name").value("AWS Certified"));
    }

    @Test
    @DisplayName("GET /{id}/profile - User chưa có dữ liệu trả về 200 với mảng rỗng")
    void getFullProfile_NewUser_ReturnsEmptyLists() throws Exception {
        UUID userId = UUID.randomUUID();

        UserProfileResponse emptyProfile = new UserProfileResponse();
        emptyProfile.setUserId(userId);

        UserFullProfileResponse fullProfile = UserFullProfileResponse.builder()
                .profile(emptyProfile)
                .educations(Collections.emptyList())
                .languages(Collections.emptyList())
                .experiences(Collections.emptyList())
                .certificates(Collections.emptyList())
                .build();

        when(userService.getFullProfile(userId)).thenReturn(fullProfile);

        mockMvc.perform(get("/api/v1/users/{id}/profile", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.profile.userId").value(userId.toString()))
                .andExpect(jsonPath("$.data.educations").isArray())
                .andExpect(jsonPath("$.data.educations").isEmpty())
                .andExpect(jsonPath("$.data.languages").isEmpty())
                .andExpect(jsonPath("$.data.experiences").isEmpty())
                .andExpect(jsonPath("$.data.certificates").isEmpty());
    }

    @Test
    @DisplayName("GET /{id}/profile - Format UUID sai trả về 400")
    void getFullProfile_InvalidUUID_Returns400() throws Exception {
        mockMvc.perform(get("/api/v1/users/{id}/profile", "not-a-valid-uuid"))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }
}
