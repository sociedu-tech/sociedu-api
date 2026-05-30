package com.unishare.api.modules.admin.service.impl;

import com.unishare.api.common.constants.Roles;
import com.unishare.api.common.dto.AppException;
import com.unishare.api.common.dto.PageResponse;
import com.unishare.api.modules.admin.dto.AdminUserSummaryResponse;
import com.unishare.api.modules.admin.service.AdminUserService;
import com.unishare.api.modules.auth.dto.UserAccountBrief;
import com.unishare.api.modules.auth.entity.User;
import com.unishare.api.modules.auth.entity.UserRole;
import com.unishare.api.modules.auth.exception.AuthErrorCode;
import com.unishare.api.modules.auth.repository.UserRepository;
import com.unishare.api.modules.auth.service.UserAccountService;
import com.unishare.api.modules.user.dto.UserProfileNames;
import com.unishare.api.modules.user.service.UserService;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminUserServiceImpl implements AdminUserService {

    private final UserAccountService userAccountService;
    private final UserService userService;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AdminUserSummaryResponse> listUsers(String role, String status, String q, Pageable pageable) {
        Specification<User> spec = buildUserSpec(role, status, q);
        Page<User> page = userRepository.findAll(spec, pageable);
        List<UUID> userIds = page.getContent().stream().map(User::getId).toList();
        Map<UUID, UserProfileNames> names = userService.getProfileNamesByUserIds(userIds);
        return PageResponse.of(page.map(user -> toSummary(toBrief(user), names.get(user.getId()))));
    }

    private Specification<User> buildUserSpec(String role, String status, String q) {
        return (root, query, cb) -> {
            if (query != null) {
                query.distinct(true);
            }
            List<Predicate> predicates = new ArrayList<>();
            if (role != null && !role.isBlank() && !"all".equalsIgnoreCase(role)) {
                String roleName = switch (role.toLowerCase()) {
                    case "mentor" -> Roles.MENTOR;
                    case "admin" -> Roles.ADMIN;
                    default -> Roles.USER;
                };
                Join<User, UserRole> ur = root.join("userRoles");
                predicates.add(cb.equal(ur.get("role").get("name"), roleName));
            }
            if (status != null && !status.isBlank() && !"all".equalsIgnoreCase(status)) {
                predicates.add(cb.equal(root.get("status"), status.toLowerCase()));
            }
            if (q != null && !q.isBlank()) {
                String pattern = "%" + q.trim().toLowerCase() + "%";
                predicates.add(cb.like(cb.lower(root.get("email")), pattern));
            }
            return predicates.isEmpty() ? cb.conjunction() : cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    @Override
    @Transactional
    public AdminUserSummaryResponse updateUserRole(UUID userId, String roleName) {
        UserAccountBrief b = userAccountService.replaceSingleRole(userId, roleName);
        UserProfileNames pn = userService.getProfileNamesByUserIds(List.of(b.userId())).get(b.userId());
        return toSummary(b, pn);
    }

    @Override
    @Transactional
    public AdminUserSummaryResponse updateUserStatus(UUID userId, String status) {
        User user = userRepository.findByIdWithRoles(userId)
                .orElseThrow(() -> new AppException(AuthErrorCode.USER_NOT_FOUND, "User not found"));
        user.setStatus(status.toLowerCase());
        User saved = userRepository.save(user);
        UserProfileNames pn = userService.getProfileNamesByUserIds(List.of(saved.getId())).get(saved.getId());
        return toSummary(toBrief(saved), pn);
    }

    private UserAccountBrief toBrief(User user) {
        return new UserAccountBrief(
                user.getId(),
                user.getEmail(),
                user.getStatus(),
                user.getCreatedAt(),
                user.getUserRoles().stream().map(ur -> ur.getRole().getName()).toList()
        );
    }

    private AdminUserSummaryResponse toSummary(UserAccountBrief b, UserProfileNames pn) {
        return AdminUserSummaryResponse.builder()
                .userId(b.userId())
                .email(b.email())
                .profile(pn != null ? pn : UserProfileNames.EMPTY)
                .status(b.status())
                .createdAt(b.createdAt())
                .roles(b.roles())
                .build();
    }
}
