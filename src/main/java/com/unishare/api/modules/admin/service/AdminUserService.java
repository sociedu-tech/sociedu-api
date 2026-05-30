package com.unishare.api.modules.admin.service;

import com.unishare.api.common.dto.PageResponse;
import com.unishare.api.modules.admin.dto.AdminUserSummaryResponse;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface AdminUserService {

    PageResponse<AdminUserSummaryResponse> listUsers(String role, String status, String q, Pageable pageable);

    AdminUserSummaryResponse updateUserRole(UUID userId, String roleName);

    AdminUserSummaryResponse updateUserStatus(UUID userId, String status);
}
