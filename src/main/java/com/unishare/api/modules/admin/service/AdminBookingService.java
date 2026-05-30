package com.unishare.api.modules.admin.service;

import com.unishare.api.common.dto.PageResponse;
import com.unishare.api.modules.admin.dto.AdminBookingResponse;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface AdminBookingService {

    PageResponse<AdminBookingResponse> list(String status, String q, Pageable pageable);

    AdminBookingResponse getById(UUID id);
}
