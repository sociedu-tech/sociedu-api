package com.unishare.api.modules.mentor.service;

import com.unishare.api.modules.mentor.dto.MentorRequest;
import com.unishare.api.modules.mentor.dto.MentorResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.UUID;

public interface MentorService {

    MentorResponse getMentorProfile(UUID mentorId);

    MentorResponse createOrUpdateProfile(UUID userId, MentorRequest request);

    Page<MentorResponse> getAllVerifiedMentors(Pageable pageable);

    /**
     * Danh sách mentor theo trạng thái xác minh + lọc từ khóa (headline/expertise),
     * khoảng giá base.
     */
    Page<MentorResponse> searchMentors(String verificationStatus, String keyword,
            BigDecimal minBasePrice, BigDecimal maxBasePrice, Pageable pageable);

    boolean mentorProfileExists(UUID userId);

    MentorResponse submitProfileVerification(UUID userId);
}
