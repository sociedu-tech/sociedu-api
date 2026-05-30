package com.unishare.api.modules.admin.service.impl;

import com.unishare.api.common.constants.MentorRequestStatuses;
import com.unishare.api.common.constants.ReportStatuses;
import com.unishare.api.common.constants.Roles;
import com.unishare.api.common.constants.SessionStatuses;
import com.unishare.api.modules.admin.dto.AdminStatsResponse;
import com.unishare.api.modules.admin.service.AdminStatsService;
import com.unishare.api.modules.auth.repository.UserRepository;
import com.unishare.api.modules.booking.repository.BookingRepository;
import com.unishare.api.modules.booking.repository.BookingSessionRepository;
import com.unishare.api.modules.mentorapplication.repository.MentorApplicationRepository;
import com.unishare.api.modules.trust.repository.ModerationReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminStatsServiceImpl implements AdminStatsService {

    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final BookingSessionRepository sessionRepository;
    private final MentorApplicationRepository mentorApplicationRepository;
    private final ModerationReportRepository moderationReportRepository;

    @Override
    @Transactional(readOnly = true)
    public AdminStatsResponse getStats() {
        long mentors = userRepository.findAllWithRoleName(Roles.MENTOR).size();
        long learners = userRepository.findAllWithRoleName(Roles.USER).size();
        long totalUsers = userRepository.count();
        long liveSessions = sessionRepository.findAll().stream()
                .filter(s -> SessionStatuses.SCHEDULED.equalsIgnoreCase(s.getStatus()))
                .count();
        long pendingMentorRequests = mentorApplicationRepository.findAll().stream()
                .filter(m -> MentorRequestStatuses.SUBMITTED.equals(m.getStatus())
                        || MentorRequestStatuses.UNDER_REVIEW.equals(m.getStatus()))
                .count();
        long openReports = moderationReportRepository.findAll().stream()
                .filter(r -> ReportStatuses.OPEN.equalsIgnoreCase(r.getStatus())
                        || ReportStatuses.UNDER_REVIEW.equalsIgnoreCase(r.getStatus()))
                .count();

        return AdminStatsResponse.builder()
                .totalUsers(totalUsers)
                .totalMentors(mentors)
                .totalLearners(learners)
                .totalBookings(bookingRepository.count())
                .liveSessions(liveSessions)
                .pendingMentorRequests(pendingMentorRequests)
                .openReports(openReports)
                .build();
    }
}
