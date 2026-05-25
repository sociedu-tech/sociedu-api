package com.unishare.api.modules.service.service.impl;

import com.unishare.api.common.dto.AppException;
import com.unishare.api.modules.mentor.service.MentorService;
import com.unishare.api.modules.service.dto.request.CreateReportRequest;
import com.unishare.api.modules.service.dto.request.ReviewReportRequest;
import com.unishare.api.modules.service.dto.response.ProgressReportResponse;
import com.unishare.api.modules.service.entity.ProgressReport;
import com.unishare.api.modules.service.entity.ReportStatus;
import com.unishare.api.modules.service.exception.ProgressReportErrorCode;
import com.unishare.api.modules.service.repository.ProgressReportRepository;
import com.unishare.api.modules.user.dto.UserProfileResponse;
import com.unishare.api.modules.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProgressReportServiceImplTest {

    @Mock
    private ProgressReportRepository progressReportRepository;

    @Mock
    private MentorService mentorProfileService;

    @Mock
    private UserService userService;

    @InjectMocks
    private ProgressReportServiceImpl progressReportService;

    private UUID mentorId;
    private UUID menteeId;
    private UUID reportId;

    @BeforeEach
    void setUp() {
        mentorId = UUID.randomUUID();
        menteeId = UUID.randomUUID();
        reportId = UUID.randomUUID();
    }

    @Test
    void createReport_whenMentorExists_shouldPersistPendingReport() {
        CreateReportRequest request = new CreateReportRequest();
        request.setMentorId(mentorId);
        request.setTitle("Week 1");
        request.setContent("Initial content");
        request.setAttachmentUrl("https://cdn.example.com/report.pdf");

        when(mentorProfileService.mentorProfileExists(mentorId)).thenReturn(true);
        when(progressReportRepository.save(any(ProgressReport.class))).thenAnswer(invocation -> {
            ProgressReport report = invocation.getArgument(0);
            report.setId(reportId);
            report.setCreatedAt(Instant.parse("2026-04-17T00:00:00Z"));
            report.setUpdatedAt(Instant.parse("2026-04-17T00:00:00Z"));
            return report;
        });
        when(userService.getProfile(menteeId)).thenReturn(profile(menteeId, "Jane", "Doe"));
        when(userService.getProfile(mentorId)).thenReturn(profile(mentorId, "John", "Mentor"));

        ProgressReportResponse response = progressReportService.createReport(menteeId, request);

        assertEquals(reportId, response.getId());
        assertEquals(ReportStatus.PENDING, response.getStatus());
        assertEquals("Jane Doe", response.getMenteeName());
        assertEquals("John Mentor", response.getMentorName());
        verify(progressReportRepository).save(any(ProgressReport.class));
    }

    @Test
    void createReport_whenMentorDoesNotExist_shouldThrowMentorNotFound() {
        CreateReportRequest request = new CreateReportRequest();
        request.setMentorId(mentorId);
        request.setTitle("Week 1");
        request.setContent("Initial content");

        when(mentorProfileService.mentorProfileExists(mentorId)).thenReturn(false);

        AppException exception = assertThrows(AppException.class,
                () -> progressReportService.createReport(menteeId, request));

        assertSame(ProgressReportErrorCode.PROGRESS_REPORT_MENTOR_NOT_FOUND, exception.getExceptionCode());
        verify(progressReportRepository, never()).save(any());
    }

    @Test
    void createReport_whenMenteeTargetsSelf_shouldThrowSelfTargetNotAllowed() {
        CreateReportRequest request = new CreateReportRequest();
        request.setMentorId(menteeId);
        request.setTitle("Week 1");
        request.setContent("Initial content");

        AppException exception = assertThrows(AppException.class,
                () -> progressReportService.createReport(menteeId, request));

        assertSame(ProgressReportErrorCode.PROGRESS_REPORT_SELF_TARGET_NOT_ALLOWED, exception.getExceptionCode());
        verify(progressReportRepository, never()).save(any());
    }

    @Test
    void getMenteeReports_whenPaged_shouldMapPageContent() {
        ProgressReport report = existingReport();
        PageRequest pageable = PageRequest.of(0, 10);

        when(progressReportRepository.findByMenteeId(menteeId, pageable))
                .thenReturn(new PageImpl<>(List.of(report), pageable, 1));
        when(userService.getProfile(menteeId)).thenReturn(profile(menteeId, "Jane", "Doe"));
        when(userService.getProfile(mentorId)).thenReturn(profile(mentorId, "John", "Mentor"));

        Page<ProgressReportResponse> response = progressReportService.getMenteeReports(menteeId, pageable);

        assertEquals(1, response.getTotalElements());
        assertEquals(1, response.getContent().size());
        assertEquals(reportId, response.getContent().get(0).getId());
        assertEquals("John Mentor", response.getContent().get(0).getMentorName());
    }

    @Test
    void getMentorReports_whenPaged_shouldMapPageContent() {
        ProgressReport report = existingReport();
        PageRequest pageable = PageRequest.of(0, 10);

        when(progressReportRepository.findByMentorId(mentorId, pageable))
                .thenReturn(new PageImpl<>(List.of(report), pageable, 1));
        when(userService.getProfile(menteeId)).thenReturn(profile(menteeId, "Jane", "Doe"));
        when(userService.getProfile(mentorId)).thenReturn(profile(mentorId, "John", "Mentor"));

        Page<ProgressReportResponse> response = progressReportService.getMentorReports(mentorId, pageable);

        assertEquals(1, response.getTotalElements());
        assertEquals(1, response.getContent().size());
        assertEquals(reportId, response.getContent().get(0).getId());
        assertEquals("Jane Doe", response.getContent().get(0).getMenteeName());
    }

    @Test
    void reviewReport_whenRequesterIsAssignedMentor_shouldUpdateStatusAndFeedback() {
        ProgressReport report = existingReport();
        ReviewReportRequest request = new ReviewReportRequest();
        request.setStatus(ReportStatus.REVIEWED);
        request.setMentorFeedback("Strong progress");

        when(progressReportRepository.findById(reportId)).thenReturn(Optional.of(report));
        when(progressReportRepository.save(any(ProgressReport.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(userService.getProfile(menteeId)).thenReturn(profile(menteeId, "Jane", "Doe"));
        when(userService.getProfile(mentorId)).thenReturn(profile(mentorId, "John", "Mentor"));

        ProgressReportResponse response = progressReportService.reviewReport(mentorId, reportId, request);

        assertEquals(ReportStatus.REVIEWED, response.getStatus());
        assertEquals("Strong progress", response.getMentorFeedback());
        assertEquals("Jane Doe", response.getMenteeName());
        assertEquals("John Mentor", response.getMentorName());
        verify(progressReportRepository).save(report);
    }

    @Test
    void reviewReport_whenRequesterIsNotAssignedMentor_shouldThrowAccessDenied() {
        ProgressReport report = existingReport();
        ReviewReportRequest request = new ReviewReportRequest();
        request.setStatus(ReportStatus.REVIEWED);
        request.setMentorFeedback("Feedback");

        when(progressReportRepository.findById(reportId)).thenReturn(Optional.of(report));

        AppException exception = assertThrows(AppException.class,
                () -> progressReportService.reviewReport(UUID.randomUUID(), reportId, request));

        assertSame(ProgressReportErrorCode.PROGRESS_REPORT_ACCESS_DENIED, exception.getExceptionCode());
        verify(progressReportRepository, never()).save(any());
    }

    @Test
    void reviewReport_whenStatusIsPending_shouldThrowInvalidReviewStatus() {
        ProgressReport report = existingReport();
        ReviewReportRequest request = new ReviewReportRequest();
        request.setStatus(ReportStatus.PENDING);
        request.setMentorFeedback("Feedback");

        when(progressReportRepository.findById(reportId)).thenReturn(Optional.of(report));

        AppException exception = assertThrows(AppException.class,
                () -> progressReportService.reviewReport(mentorId, reportId, request));

        assertSame(ProgressReportErrorCode.PROGRESS_REPORT_INVALID_REVIEW_STATUS, exception.getExceptionCode());
        verify(progressReportRepository, never()).save(any());
    }

    @Test
    void reviewReport_whenReportNotPending_shouldThrowInvalidState() {
        ProgressReport report = existingReport();
        report.setStatus(ReportStatus.REVIEWED); // already reviewed
        ReviewReportRequest request = new ReviewReportRequest();
        request.setStatus(ReportStatus.REVIEWED);
        request.setMentorFeedback("Good");

        when(progressReportRepository.findById(reportId)).thenReturn(Optional.of(report));

        AppException exception = assertThrows(AppException.class,
                () -> progressReportService.reviewReport(mentorId, reportId, request));

        assertSame(ProgressReportErrorCode.PROGRESS_REPORT_INVALID_STATE, exception.getExceptionCode());
        verify(progressReportRepository, never()).save(any());
    }

    @Test
    void getReportById_whenUserIsMentee_shouldReturnReport() {
        ProgressReport report = existingReport();
        when(progressReportRepository.findById(reportId)).thenReturn(Optional.of(report));
        when(userService.getProfile(menteeId)).thenReturn(profile(menteeId, "Jane", "Doe"));
        when(userService.getProfile(mentorId)).thenReturn(profile(mentorId, "John", "Mentor"));

        ProgressReportResponse response = progressReportService.getReportById(menteeId, reportId);

        assertEquals(reportId, response.getId());
        assertEquals("Jane Doe", response.getMenteeName());
        assertEquals("John Mentor", response.getMentorName());
    }

    @Test
    void getReportById_whenUserIsMentor_shouldReturnReport() {
        ProgressReport report = existingReport();
        when(progressReportRepository.findById(reportId)).thenReturn(Optional.of(report));
        when(userService.getProfile(menteeId)).thenReturn(profile(menteeId, "Jane", "Doe"));
        when(userService.getProfile(mentorId)).thenReturn(profile(mentorId, "John", "Mentor"));

        ProgressReportResponse response = progressReportService.getReportById(mentorId, reportId);

        assertEquals(reportId, response.getId());
    }

    @Test
    void getReportById_whenUserIsNeither_shouldThrowAccessDenied() {
        ProgressReport report = existingReport();
        when(progressReportRepository.findById(reportId)).thenReturn(Optional.of(report));

        AppException exception = assertThrows(AppException.class,
                () -> progressReportService.getReportById(UUID.randomUUID(), reportId));

        assertSame(ProgressReportErrorCode.PROGRESS_REPORT_ACCESS_DENIED, exception.getExceptionCode());
    }

    @Test
    void getReportById_whenReportNotFound_shouldThrowNotFound() {
        when(progressReportRepository.findById(reportId)).thenReturn(Optional.empty());

        AppException exception = assertThrows(AppException.class,
                () -> progressReportService.getReportById(menteeId, reportId));

        assertSame(ProgressReportErrorCode.PROGRESS_REPORT_NOT_FOUND, exception.getExceptionCode());
    }

    private ProgressReport existingReport() {
        ProgressReport report = new ProgressReport();
        report.setId(reportId);
        report.setMenteeId(menteeId);
        report.setMentorId(mentorId);
        report.setTitle("Week 1");
        report.setContent("Initial content");
        report.setStatus(ReportStatus.PENDING);
        report.setCreatedAt(Instant.parse("2026-04-17T00:00:00Z"));
        report.setUpdatedAt(Instant.parse("2026-04-17T00:00:00Z"));
        return report;
    }

    private UserProfileResponse profile(UUID userId, String firstName, String lastName) {
        UserProfileResponse profile = new UserProfileResponse();
        profile.setUserId(userId);
        profile.setFirstName(firstName);
        profile.setLastName(lastName);
        return profile;
    }
}
