package com.unishare.api.modules.service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.unishare.api.common.dto.ApiResponse;
import com.unishare.api.config.GlobalExceptionHandler;
import com.unishare.api.infrastructure.security.CustomUserPrincipal;
import com.unishare.api.modules.service.dto.request.CreateReportRequest;
import com.unishare.api.modules.service.dto.request.ReviewReportRequest;
import com.unishare.api.modules.service.dto.response.ProgressReportResponse;
import com.unishare.api.modules.service.entity.ReportStatus;
import com.unishare.api.modules.service.service.ProgressReportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ProgressReportControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private ProgressReportService reportService;

    @InjectMocks
    private ProgressReportController progressReportController;

    private UUID userId;
    private CustomUserPrincipal principalMentee;
    private CustomUserPrincipal principalMentor;

    private HandlerMethodArgumentResolver activePrincipalResolver;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        principalMentee = new CustomUserPrincipal(
                userId,
                "mentee@gmail.com",
                "hashedpassword",
                List.of("USER"),
                List.of("CREATE_REPORT", "VIEW_OWN_REPORT"),
                true
        );

        principalMentor = new CustomUserPrincipal(
                userId,
                "mentor@gmail.com",
                "hashedpassword",
                List.of("MENTOR"),
                List.of("VIEW_OWN_REPORT"),
                true
        );

        objectMapper = new ObjectMapper();

        activePrincipalResolver = new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return parameter.getParameterType().equals(CustomUserPrincipal.class);
            }

            @Override
            public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                           NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
                // Return principalMentee by default, but tests can swap it or we can check header/attribute if needed.
                // For simplicity, we will just use a mutable field or default to principalMentee.
                return activePrincipalResolverSource();
            }
        };

        mockMvc = MockMvcBuilders.standaloneSetup(progressReportController)
                .setCustomArgumentResolvers(activePrincipalResolver, new PageableHandlerMethodArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private CustomUserPrincipal currentPrincipal;

    private CustomUserPrincipal activePrincipalResolverSource() {
        return currentPrincipal != null ? currentPrincipal : principalMentee;
    }

    @Test
    @DisplayName("POST /api/v1/progress-reports - Nộp báo cáo thành công")
    void submitReport_ReturnsSuccess() throws Exception {
        currentPrincipal = principalMentee;

        CreateReportRequest request = new CreateReportRequest();
        request.setMentorId(UUID.randomUUID());
        request.setTitle("Mid-term Report");
        request.setContent("Details");

        ProgressReportResponse response = ProgressReportResponse.builder()
                .id(UUID.randomUUID())
                .title("Mid-term Report")
                .status(ReportStatus.PENDING)
                .build();

        when(reportService.createReport(eq(userId), any(CreateReportRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/progress-reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Mid-term Report"))
                .andExpect(jsonPath("$.isSuccess").value(true));
    }

    @Test
    @DisplayName("GET /api/v1/progress-reports/me - Lấy danh sách với tư cách Mentee")
    void getMyReportsAsMentee_ReturnsMenteeReports() throws Exception {
        currentPrincipal = principalMentee;

        ProgressReportResponse response = ProgressReportResponse.builder()
                .id(UUID.randomUUID())
                .title("Mentee view")
                .build();

        PageRequest pageable = PageRequest.of(0, 10);
        when(reportService.getMenteeReports(eq(userId), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(response), pageable, 1));

        mockMvc.perform(get("/api/v1/progress-reports/me")
                        .param("page", "0")
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].title").value("Mentee view"));
    }

    @Test
    @DisplayName("GET /api/v1/progress-reports/me - Lấy danh sách với tư cách Mentor")
    void getMyReportsAsMentor_ReturnsMentorReports() throws Exception {
        currentPrincipal = principalMentor;

        ProgressReportResponse response = ProgressReportResponse.builder()
                .id(UUID.randomUUID())
                .title("Mentor view")
                .build();

        PageRequest pageable = PageRequest.of(0, 10);
        when(reportService.getMentorReports(eq(userId), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(response), pageable, 1));

        mockMvc.perform(get("/api/v1/progress-reports/me")
                        .param("page", "0")
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].title").value("Mentor view"));
    }

    @Test
    @DisplayName("GET /api/v1/progress-reports/{id} - Lấy chi tiết báo cáo thành công")
    void getReportById_ReturnsSuccess() throws Exception {
        currentPrincipal = principalMentee;

        UUID reportId = UUID.randomUUID();
        ProgressReportResponse response = ProgressReportResponse.builder()
                .id(reportId)
                .title("Report Detail")
                .build();

        when(reportService.getReportById(userId, reportId)).thenReturn(response);

        mockMvc.perform(get("/api/v1/progress-reports/{id}", reportId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(reportId.toString()))
                .andExpect(jsonPath("$.data.title").value("Report Detail"));
    }

    @Test
    @DisplayName("POST /api/v1/progress-reports/{id}/mentor-feedback - Mentor phản hồi thành công")
    void reviewReport_ReturnsSuccess() throws Exception {
        currentPrincipal = principalMentor;

        UUID reportId = UUID.randomUUID();
        ReviewReportRequest request = new ReviewReportRequest();
        request.setStatus(ReportStatus.REVIEWED);
        request.setMentorFeedback("Good job");

        ProgressReportResponse response = ProgressReportResponse.builder()
                .id(reportId)
                .status(ReportStatus.REVIEWED)
                .mentorFeedback("Good job")
                .build();

        when(reportService.reviewReport(eq(userId), eq(reportId), any(ReviewReportRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/progress-reports/{id}/mentor-feedback", reportId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REVIEWED"))
                .andExpect(jsonPath("$.data.mentorFeedback").value("Good job"));
    }

    @Test
    @DisplayName("GET /api/v1/progress-reports/me?role=mentor - Trả về báo cáo mentor")
    void getMyReports_withRoleMentorParam_ReturnsMentorReports() throws Exception {
        currentPrincipal = principalMentor;

        ProgressReportResponse response = ProgressReportResponse.builder()
                .id(UUID.randomUUID())
                .title("Mentor parameter view")
                .build();

        PageRequest pageable = PageRequest.of(0, 10);
        when(reportService.getMentorReports(eq(userId), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(response), pageable, 1));

        mockMvc.perform(get("/api/v1/progress-reports/me")
                        .param("role", "mentor")
                        .param("page", "0")
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].title").value("Mentor parameter view"));
    }

    @Test
    @DisplayName("GET /api/v1/progress-reports/me?role=mentee - Trả về báo cáo mentee")
    void getMyReports_withRoleMenteeParam_ReturnsMenteeReports() throws Exception {
        currentPrincipal = principalMentee;

        ProgressReportResponse response = ProgressReportResponse.builder()
                .id(UUID.randomUUID())
                .title("Mentee parameter view")
                .build();

        PageRequest pageable = PageRequest.of(0, 10);
        when(reportService.getMenteeReports(eq(userId), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(response), pageable, 1));

        mockMvc.perform(get("/api/v1/progress-reports/me")
                        .param("role", "mentee")
                        .param("page", "0")
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].title").value("Mentee parameter view"));
    }
}
