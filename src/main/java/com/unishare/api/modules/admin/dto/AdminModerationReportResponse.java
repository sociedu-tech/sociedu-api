package com.unishare.api.modules.admin.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class AdminModerationReportResponse {
    private UUID id;
    private Instant createdAt;
    private String reporterName;
    private UUID reporterId;
    private String targetType;
    private String targetLabel;
    private String category;
    private String summary;
    private String status;
    private String priority;
    private SessionDisputeDetailDto sessionDispute;

    @Data
    @Builder
    public static class SessionDisputeDetailDto {
        private String sessionCode;
        private Instant sessionAt;
        private String menteeName;
        private String mentorName;
        private String openedByParty;
        private String openerStatement;
        private String counterStatement;
        private String currentPhase;
        private List<SessionDisputeStageDto> stages;
        private List<SessionDisputeEvidenceDto> evidence;
        private String adminResolutionNote;
    }

    @Data
    @Builder
    public static class SessionDisputeStageDto {
        private String phase;
        private String label;
        private String description;
        private boolean done;
        private Instant completedAt;
    }

    @Data
    @Builder
    public static class SessionDisputeEvidenceDto {
        private UUID id;
        private String party;
        private Instant uploadedAt;
        private String title;
        private String detail;
        private String fileLabel;
    }
}
