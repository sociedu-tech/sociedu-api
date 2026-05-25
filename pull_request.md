# Pull Request: Phase 2 — Backend API Gaps Implementation

> **Branch**: `backend-api-gaps` → `main`
> **Author**: @huyth
> **Date**: 2026-05-23

---

## 📋 Summary

PR này triển khai toàn bộ **Phase 2 (P2) Backend Gaps** cho SocieDu API, bổ sung 3 phân hệ nghiệp vụ còn thiếu:

1. **Review & Rating** — Đánh giá và phản hồi sau buổi học
2. **Notification Center** — Trung tâm thông báo + quản lý Device Token (FCM)
3. **Mentor Finance & Payouts** — Quản lý doanh thu và rút tiền cho Mentor

> [!IMPORTANT]
> PR bao gồm **3 Flyway migrations mới**, cần review kỹ schema trước khi merge.

---

## 🔗 Related

- Tài liệu kỹ thuật chi tiết: [P2_TECHNICAL_HANDOVER.md](file:///d:/Projects/Sociedu/sociedu-api/P2_TECHNICAL_HANDOVER.md)

---

## 📊 Scope of Changes

| Loại | Số lượng |
|------|----------|
| Files modified | 6 |
| Files added (new) | ~30+ |
| DB Migrations | 3 |
| Test files | 2 |
| Lines changed (tracked) | +33 / -3 |

---

## 🏗️ Chi tiết thay đổi

### 1. Module: Review & Rating (`modules/booking`)

#### New Files
| File | Mô tả |
|------|--------|
| [ReviewController.java](file:///d:/Projects/Sociedu/sociedu-api/src/main/java/com/unishare/api/modules/booking/controller/ReviewController.java) | REST controller cho đánh giá booking |
| [CreateReviewRequest.java](file:///d:/Projects/Sociedu/sociedu-api/src/main/java/com/unishare/api/modules/booking/dto/CreateReviewRequest.java) | DTO request tạo đánh giá |
| [ReviewResponse.java](file:///d:/Projects/Sociedu/sociedu-api/src/main/java/com/unishare/api/modules/booking/dto/ReviewResponse.java) | DTO response đánh giá |
| [RatingSummaryResponse.java](file:///d:/Projects/Sociedu/sociedu-api/src/main/java/com/unishare/api/modules/booking/dto/RatingSummaryResponse.java) | DTO tóm tắt điểm đánh giá (avg, count, distribution) |
| [BookingReview.java](file:///d:/Projects/Sociedu/sociedu-api/src/main/java/com/unishare/api/modules/booking/entity/BookingReview.java) | JPA Entity cho bảng `booking_reviews` |
| [BookingReviewRepository.java](file:///d:/Projects/Sociedu/sociedu-api/src/main/java/com/unishare/api/modules/booking/repository/BookingReviewRepository.java) | Spring Data JPA repository |
| [ReviewService.java](file:///d:/Projects/Sociedu/sociedu-api/src/main/java/com/unishare/api/modules/booking/service/ReviewService.java) | Service interface |
| [ReviewServiceImpl.java](file:///d:/Projects/Sociedu/sociedu-api/src/main/java/com/unishare/api/modules/booking/service/impl/ReviewServiceImpl.java) | Service implementation với atomic rating update |

#### Modified Files

- [BookingErrorCode.java](file:///d:/Projects/Sociedu/sociedu-api/src/main/java/com/unishare/api/modules/booking/exception/BookingErrorCode.java) — Thêm 3 error codes: `REVIEW_ALREADY_EXISTS` (409), `BOOKING_NOT_COMPLETED` (400), `REVIEW_ACCESS_DENIED` (403)
- [BookingRepository.java](file:///d:/Projects/Sociedu/sociedu-api/src/main/java/com/unishare/api/modules/booking/repository/BookingRepository.java) — Thêm `calculateTotalEarnedByMentor()` JPQL query cho module Finance

#### API Endpoints

| Method | Path | Auth |
|--------|------|------|
| `POST` | `/api/v1/bookings/{bookingId}/reviews` | Buyer (JWT) |
| `GET` | `/api/v1/mentors/{mentorId}/reviews` | Public |
| `GET` | `/api/v1/mentors/{mentorId}/rating-summary` | Public |

#### Thiết kế đáng chú ý
- **Atomic SQL Update** trên `rating_total`, `rating_count`, `rating_avg` trong `MentorProfileRepository` để tránh race condition (Lost Update)
- **Immutable Review** — Học viên chỉ được gửi 1 review/booking, không sửa được từ client
- **Batch fetching** reviewer info để tránh N+1

---

### 2. Module: Notification Center (`modules/notification`)

#### New Files
| File | Mô tả |
|------|--------|
| [NotificationController.java](file:///d:/Projects/Sociedu/sociedu-api/src/main/java/com/unishare/api/modules/notification/controller/NotificationController.java) | REST controller cho thông báo |
| [DeviceTokenController.java](file:///d:/Projects/Sociedu/sociedu-api/src/main/java/com/unishare/api/modules/notification/controller/DeviceTokenController.java) | REST controller đăng ký/hủy push token |
| [NotificationResponse.java](file:///d:/Projects/Sociedu/sociedu-api/src/main/java/com/unishare/api/modules/notification/dto/NotificationResponse.java) | DTO response thông báo |
| [UnreadCountResponse.java](file:///d:/Projects/Sociedu/sociedu-api/src/main/java/com/unishare/api/modules/notification/dto/UnreadCountResponse.java) | DTO số thông báo chưa đọc |
| [RegisterDeviceRequest.java](file:///d:/Projects/Sociedu/sociedu-api/src/main/java/com/unishare/api/modules/notification/dto/RegisterDeviceRequest.java) | DTO đăng ký device token |
| [UnregisterDeviceRequest.java](file:///d:/Projects/Sociedu/sociedu-api/src/main/java/com/unishare/api/modules/notification/dto/UnregisterDeviceRequest.java) | DTO hủy device token |
| [Notification.java](file:///d:/Projects/Sociedu/sociedu-api/src/main/java/com/unishare/api/modules/notification/entity/Notification.java) | JPA Entity (hỗ trợ JSONB metadata) |
| [DeviceToken.java](file:///d:/Projects/Sociedu/sociedu-api/src/main/java/com/unishare/api/modules/notification/entity/DeviceToken.java) | JPA Entity cho push tokens |
| [NotificationErrorCode.java](file:///d:/Projects/Sociedu/sociedu-api/src/main/java/com/unishare/api/modules/notification/exception/NotificationErrorCode.java) | Error codes cho module |
| [NotificationEventListener.java](file:///d:/Projects/Sociedu/sociedu-api/src/main/java/com/unishare/api/modules/notification/listener/NotificationEventListener.java) | `@TransactionalEventListener` + `@Async` cho FCM push |
| [NotificationRepository.java](file:///d:/Projects/Sociedu/sociedu-api/src/main/java/com/unishare/api/modules/notification/repository/NotificationRepository.java) | Repository |
| [DeviceTokenRepository.java](file:///d:/Projects/Sociedu/sociedu-api/src/main/java/com/unishare/api/modules/notification/repository/DeviceTokenRepository.java) | Repository |
| [NotificationService.java](file:///d:/Projects/Sociedu/sociedu-api/src/main/java/com/unishare/api/modules/notification/service/NotificationService.java) | Service interface |
| [NotificationServiceImpl.java](file:///d:/Projects/Sociedu/sociedu-api/src/main/java/com/unishare/api/modules/notification/service/impl/NotificationServiceImpl.java) | Service implementation |

#### API Endpoints

| Method | Path | Auth |
|--------|------|------|
| `GET` | `/api/v1/notifications` | JWT |
| `GET` | `/api/v1/notifications/unread-count` | JWT |
| `PATCH` | `/api/v1/notifications/{id}/read` | JWT |
| `POST` | `/api/v1/notifications/read-all` | JWT |
| `POST` | `/api/v1/devices/register` | JWT |
| `POST` | `/api/v1/devices/unregister` | JWT |

#### Thiết kế đáng chú ý
- **Transactional Event Listener** (`AFTER_COMMIT`) + `@Async` — Push notification được gửi bất đồng bộ sau khi transaction nghiệp vụ chính commit thành công, không ảnh hưởng luồng chính nếu FCM lỗi
- **JSONB metadata** — Hỗ trợ deep linking trên mobile

---

### 3. Module: Mentor Finance & Payouts (`modules/finance`)

#### New Files
| File | Mô tả |
|------|--------|
| [PayoutController.java](file:///d:/Projects/Sociedu/sociedu-api/src/main/java/com/unishare/api/modules/finance/controller/PayoutController.java) | REST controller phía Mentor (tạo yêu cầu rút tiền, xem lịch sử) |
| [AdminPayoutController.java](file:///d:/Projects/Sociedu/sociedu-api/src/main/java/com/unishare/api/modules/finance/controller/AdminPayoutController.java) | REST controller phía Admin (duyệt/từ chối/chuyển tiền) |
| [CreatePayoutRequest.java](file:///d:/Projects/Sociedu/sociedu-api/src/main/java/com/unishare/api/modules/finance/dto/CreatePayoutRequest.java) | DTO yêu cầu rút tiền |
| [AdminReviewPayoutRequest.java](file:///d:/Projects/Sociedu/sociedu-api/src/main/java/com/unishare/api/modules/finance/dto/AdminReviewPayoutRequest.java) | DTO admin review payout |
| [PayoutRequestResponse.java](file:///d:/Projects/Sociedu/sociedu-api/src/main/java/com/unishare/api/modules/finance/dto/PayoutRequestResponse.java) | DTO response payout |
| [RevenueSummaryResponse.java](file:///d:/Projects/Sociedu/sociedu-api/src/main/java/com/unishare/api/modules/finance/dto/RevenueSummaryResponse.java) | DTO tóm tắt doanh thu |
| [PayoutRequest.java](file:///d:/Projects/Sociedu/sociedu-api/src/main/java/com/unishare/api/modules/finance/entity/PayoutRequest.java) | JPA Entity yêu cầu rút tiền |
| [PayoutAuditLog.java](file:///d:/Projects/Sociedu/sociedu-api/src/main/java/com/unishare/api/modules/finance/entity/PayoutAuditLog.java) | JPA Entity audit log cho mỗi thay đổi trạng thái |
| [FinanceErrorCode.java](file:///d:/Projects/Sociedu/sociedu-api/src/main/java/com/unishare/api/modules/finance/exception/FinanceErrorCode.java) | Error codes cho module |
| [PayoutRequestRepository.java](file:///d:/Projects/Sociedu/sociedu-api/src/main/java/com/unishare/api/modules/finance/repository/PayoutRequestRepository.java) | Repository |
| [PayoutAuditLogRepository.java](file:///d:/Projects/Sociedu/sociedu-api/src/main/java/com/unishare/api/modules/finance/repository/PayoutAuditLogRepository.java) | Repository |
| [PayoutService.java](file:///d:/Projects/Sociedu/sociedu-api/src/main/java/com/unishare/api/modules/finance/service/PayoutService.java) | Service interface |
| [PayoutServiceImpl.java](file:///d:/Projects/Sociedu/sociedu-api/src/main/java/com/unishare/api/modules/finance/service/impl/PayoutServiceImpl.java) | Service implementation với pessimistic locking |

#### API Endpoints — Mentor

| Method | Path | Auth |
|--------|------|------|
| `GET` | `/api/v1/mentors/me/revenue-summary` | Mentor (JWT) |
| `POST` | `/api/v1/mentors/me/payouts` | Mentor (JWT) |
| `GET` | `/api/v1/mentors/me/payouts` | Mentor (JWT) |

#### API Endpoints — Admin

| Method | Path | Auth |
|--------|------|------|
| `GET` | `/api/v1/admin/payouts` | Admin (JWT) |
| `POST` | `/api/v1/admin/payouts/{id}/approve` | Admin (JWT) |
| `POST` | `/api/v1/admin/payouts/{id}/reject` | Admin (JWT) |
| `POST` | `/api/v1/admin/payouts/{id}/pay` | Admin (JWT) |
| `POST` | `/api/v1/admin/payouts/{id}/fail` | Admin (JWT) |

#### Thiết kế đáng chú ý
- **Pessimistic Locking** (`PESSIMISTIC_WRITE`) trên `MentorProfile` khi tạo payout — ngăn chặn double-spending/overdraft
- **AES/GCM/NoPadding Encryption** cho số tài khoản ngân hàng qua [EncryptedStringConverter.java](file:///d:/Projects/Sociedu/sociedu-api/src/main/java/com/unishare/api/infrastructure/security/EncryptedStringConverter.java) (JPA `@Converter`)
- **Data Masking** — API phía Mentor trả về account number dạng `*******1234`, Admin thấy full
- **State Machine**: `PENDING → APPROVED/REJECTED`, `APPROVED → PROCESSING → PAID/FAILED` (irreversible terminal states)
- **Minimum payout**: 50,000 VND
- **Platform fee**: Trừ phí trước khi tính net amount

---

### 4. Cross-cutting Changes

#### Modified: [SecurityConfig.java](file:///d:/Projects/Sociedu/sociedu-api/src/main/java/com/unishare/api/config/SecurityConfig.java)
- Cho phép truy cập public (không cần JWT) đến:
  - `GET /api/v1/mentors/*/reviews`
  - `GET /api/v1/mentors/*/rating-summary`

#### Modified: [MentorProfile.java](file:///d:/Projects/Sociedu/sociedu-api/src/main/java/com/unishare/api/modules/mentor/entity/MentorProfile.java)
- Thêm `ratingCount`, `ratingTotal` (hỗ trợ atomic incremental update)
- Thêm `@Version` (optimistic locking)
- Đổi `ratingAvg` từ `Float` → `Double` (tăng precision)

#### Modified: [MentorResponse.java](file:///d:/Projects/Sociedu/sociedu-api/src/main/java/com/unishare/api/modules/mentor/dto/MentorResponse.java)
- Đổi `ratingAvg` từ `Float` → `Double` cho khớp với entity

#### Modified: [MentorProfileRepository.java](file:///d:/Projects/Sociedu/sociedu-api/src/main/java/com/unishare/api/modules/mentor/repository/MentorProfileRepository.java)
- Thêm `updateRatingIncrementally()` — atomic SQL update cho rating
- Thêm `findAndLockByUserId()` — pessimistic lock query cho finance module

#### New: [EncryptedStringConverter.java](file:///d:/Projects/Sociedu/sociedu-api/src/main/java/com/unishare/api/infrastructure/security/EncryptedStringConverter.java)
- JPA `@Converter` mã hóa AES/GCM cho sensitive data trước khi persist vào DB

---

### 5. Database Migrations (Flyway)

| Migration | Mô tả |
|-----------|--------|
| [V202605230003__create_notifications_and_devices.sql](file:///d:/Projects/Sociedu/sociedu-api/src/main/resources/db/migration/V202605230003__create_notifications_and_devices.sql) | Tạo bảng `notifications` (JSONB metadata) + `device_tokens` |
| [V202605230004__create_reviews.sql](file:///d:/Projects/Sociedu/sociedu-api/src/main/resources/db/migration/V202605230004__create_reviews.sql) | Tạo bảng `booking_reviews` + thêm cột `rating_count`, `rating_total` vào `mentor_profiles` |
| [V202605230005__create_mentor_finance.sql](file:///d:/Projects/Sociedu/sociedu-api/src/main/resources/db/migration/V202605230005__create_mentor_finance.sql) | Tạo bảng `payout_requests` + `payout_audit_logs` |

---

### 6. Tests

| Test File | Coverage |
|-----------|----------|
| [ReviewServiceImplTest.java](file:///d:/Projects/Sociedu/sociedu-api/src/test/java/com/unishare/api/modules/booking/service/impl/ReviewServiceImplTest.java) | Chặn duplicate review, validate booking status, tính toán rating distribution |
| [PayoutServiceImplTest.java](file:///d:/Projects/Sociedu/sociedu-api/src/test/java/com/unishare/api/modules/finance/service/impl/PayoutServiceImplTest.java) | Cộng/trừ số dư, đóng băng giao dịch, ngăn rút quá hạn mức |

---

## 🔒 Security Considerations

- [x] Bank account numbers encrypted at rest (AES/GCM) via `EncryptedStringConverter`
- [x] Bank info masked in Mentor-facing APIs (`*******1234`)
- [x] Pessimistic locking prevents overdraft/double-spending
- [x] Review endpoints are public (read) but write requires authenticated buyer ownership
- [x] Admin-only endpoints protected by role-based access control
- [x] Push notifications sent async after transaction commit — no FCM failure can rollback business logic

---

## ⚠️ Breaking Changes

> [!WARNING]
> `MentorResponse.ratingAvg` type changed from `Float` to `Double`. Frontend clients consuming this field should update their type definitions accordingly.

---

## ✅ Reviewer Checklist

- [ ] Review 3 Flyway migration scripts cho schema correctness (indexes, constraints, JSONB)
- [ ] Verify `EncryptedStringConverter` encryption key configuration (env variable hoặc config)
- [ ] Kiểm tra logic pessimistic lock trong `PayoutServiceImpl` có xử lý deadlock timeout
- [ ] Kiểm tra `@TransactionalEventListener` + `@Async` config có thread pool phù hợp
- [ ] Review atomic rating update JPQL query cho edge cases (concurrent writes)
- [ ] Verify `Float → Double` migration có cần ALTER COLUMN trong DB không
- [ ] Chạy test suite: `ReviewServiceImplTest` + `PayoutServiceImplTest`

---

## 🧪 How to Test

```bash
# Run all tests
./mvnw test

# Run specific test classes
./mvnw test -Dtest=ReviewServiceImplTest
./mvnw test -Dtest=PayoutServiceImplTest
```
