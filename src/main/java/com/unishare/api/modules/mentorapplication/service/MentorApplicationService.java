package com.unishare.api.modules.mentorapplication.service;

import com.unishare.api.common.dto.PageResponse;
import com.unishare.api.modules.mentorapplication.dto.*;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface MentorApplicationService {

    MentorApplicationResponse submit(UUID userId, MentorApplicationPayload payload);

    MentorApplicationResponse resubmit(UUID userId, MentorApplicationPayload payload);

    MentorApplicationResponse getMyCurrent(UUID userId);

    PageResponse<MentorApplicationResponse> adminList(String status, String q, Pageable pageable);

    MentorApplicationResponse adminGet(UUID id);

    MentorApplicationResponse adminApprove(UUID id, UUID adminId, AdminApproveMentorApplicationRequest request);

    MentorApplicationResponse adminReject(UUID id, UUID adminId, AdminRejectMentorApplicationRequest request);
}
