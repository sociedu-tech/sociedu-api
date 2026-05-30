package com.unishare.api.modules.admin.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdminStatsResponse {
    private long totalUsers;
    private long totalMentors;
    private long totalLearners;
    private long totalBookings;
    private long liveSessions;
    private long pendingMentorRequests;
    private long openReports;
}
