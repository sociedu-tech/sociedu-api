# Phân quyền API (Unishare)

RBAC **chỉ theo role**: `USER`, `MENTOR`, `ADMIN`. Spring Security map thành `ROLE_USER`, `ROLE_MENTOR`, `ROLE_ADMIN`.

## Khái niệm

| Thành phần | Mô tả |
|------------|--------|
| **Role** | Nhóm người dùng — dùng `@PreAuthorize("hasRole('MENTOR')")` hoặc `hasAnyRole('USER','ADMIN')`. |
| **Endpoint công khai** | `SecurityConfig` permitAll + `@PermitAll`. |
| **Đã đăng nhập** | `isAuthenticated()` — ownership kiểm tra trong service. |

## Ánh xạ endpoint (tóm tắt)

| Module | Yêu cầu |
|--------|---------|
| Auth public | register, login, refresh, verify-email, forgot/reset password |
| Auth protected | `isAuthenticated()` |
| Admin `*` | `hasRole('ADMIN')` |
| Mentor catalog/packages/payout | `hasRole('MENTOR')` |
| Orders checkout | `hasAnyRole('USER','ADMIN')` |
| Orders/payments read | `hasAnyRole('USER','MENTOR','ADMIN')` |
| Bookings (buyer) | `hasAnyRole('USER','MENTOR','ADMIN')` |
| Bookings (mentor list/sessions) | `hasRole('MENTOR')` |
| Users `/me/**` | `isAuthenticated()` |
| Chat, files upload | `isAuthenticated()` |
| Reports/disputes (user) | `hasAnyRole('USER','MENTOR','ADMIN')` hoặc `isAuthenticated()` |
| Reports/disputes resolve | `hasRole('ADMIN')` |
| Progress report submit | `hasRole('USER')` |
| Progress report review | `hasRole('MENTOR')` |

## DB

- `roles`, `user_roles` — không còn `capabilities` / `role_capabilities`.
- Seed: `init/data.sql` hoặc `src/main/resources/data.sql`.
- DB cũ: chạy `init/migrate_drop_capabilities.sql`.
