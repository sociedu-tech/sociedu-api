# SocieDu API — Roadmap & Tổng hợp Tài liệu Triển khai

> **Cập nhật**: 2026-05-25  
> **Tổng số hạng mục**: 11 (P0: 5 | P1: 3 | P2: 3)  
> **Trạng thái chung**: ✅ Tất cả hoàn thành

---

## Bảng tổng hợp ưu tiên

| Ưu tiên | # | Hạng mục | Trạng thái | Tài liệu |
|:-------:|:-:|----------|:----------:|----------|
| **P0** | 1 | Chat / Conversation path mismatch | ✅ | [P0_IMPLEMENTATION_DOC.md](file:///d:/Projects/Sociedu/sociedu-api/docs/P0_IMPLEMENTATION_DOC.md) |
| **P0** | 2 | File upload path mismatch | ✅ | ↑ |
| **P0** | 3 | Report / Dispute path mismatch | ✅ | ↑ |
| **P0** | 4 | Progress report path mismatch | ✅ | ↑ |
| **P0** | 5 | Mentor profile lifecycle | ✅ | ↑ |
| **P1** | 6 | Service package version CRUD | ✅ | [P1_IMPLEMENTATION_DOC.md](file:///d:/Projects/Sociedu/sociedu-api/docs/P1_IMPLEMENTATION_DOC.md) |
| **P1** | 7 | Public version/curriculum read | ✅ | ↑ |
| **P1** | 8 | Booking cancel & complete session | ✅ | ↑ |
| **P2** | 9 | Notification Center | ✅ | [P2_IMPLEMENTATION_DOC.md](file:///d:/Projects/Sociedu/sociedu-api/docs/P2_IMPLEMENTATION_DOC.md) |
| **P2** | 10 | Review & Rating | ✅ | ↑ |
| **P2** | 11 | Mentor Finance & Payouts | ✅ | ↑ |

---

## Định nghĩa mức ưu tiên

| Mức | Ý nghĩa | Tiêu chí |
|:---:|---------|----------|
| **P0** | Chặn tích hợp | Mobile sẽ lỗi ngay khi tắt mock nếu không sửa |
| **P1** | Cần cho MVP | Mobile có UI/logic nhưng backend chưa có API tương ứng |
| **P2** | Nâng cấp sau MVP | Quan trọng cho UX/vận hành nhưng không chặn core flow |

---

## Tổng hợp API Endpoints theo Module

### Auth (Có sẵn + P0 bổ sung OTP)
| Method | Path | Auth |
|--------|------|:----:|
| `POST` | `/api/v1/auth/phone/send-otp` | 🔒 JWT |
| `POST` | `/api/v1/auth/phone/verify-otp` | 🔒 JWT |
| `POST` | `/api/v1/auth/otp/send` | 🌐 Public |
| `POST` | `/api/v1/auth/otp/login` | 🌐 Public |

### Chat / Conversations (P0)
| Method | Path | Auth |
|--------|------|:----:|
| `POST` | `/api/v1/conversations` | 🔒 JWT |
| `GET` | `/api/v1/conversations` | 🔒 JWT |
| `GET` | `/api/v1/conversations/{id}` | 🔒 JWT |
| `GET` | `/api/v1/conversations/{id}/messages` | 🔒 JWT |
| `POST` | `/api/v1/conversations/{id}/messages` | 🔒 JWT |

### File Upload (P0)
| Method | Path | Auth |
|--------|------|:----:|
| `POST` | `/api/v1/files` | 🔒 JWT |
| `POST` | `/api/v1/files/upload` | 🔒 JWT |

### Trust — Reports & Disputes (P0)
| Method | Path | Auth |
|--------|------|:----:|
| `POST` | `/api/v1/trust/reports` | 🔒 JWT |
| `GET` | `/api/v1/trust/reports/me` | 🔒 JWT |
| `POST` | `/api/v1/trust/reports/{id}/evidences` | 🔒 JWT |
| `PUT` | `/api/v1/trust/reports/{id}/resolve` | 🔒 JWT |
| `POST` | `/api/v1/trust/disputes` | 🔒 JWT |
| `GET` | `/api/v1/trust/disputes/me` | 🔒 JWT |
| `PUT` | `/api/v1/trust/disputes/{id}/resolve` | 🔒 JWT |

### Progress Reports (P0)
| Method | Path | Auth |
|--------|------|:----:|
| `POST` | `/api/v1/progress-reports` | 🔒 JWT |
| `GET` | `/api/v1/progress-reports/me` | 🔒 JWT |
| `GET` | `/api/v1/progress-reports/{id}` | 🔒 JWT |
| `POST` | `/api/v1/progress-reports/{id}/mentor-feedback` | 🔒 JWT |

### Mentor Profile (P0)
| Method | Path | Auth |
|--------|------|:----:|
| `GET` | `/api/v1/mentors/me/profile` | 🔒 MENTOR |
| `POST` | `/api/v1/mentors/me/profile/submit` | 🔒 MENTOR |

### Service Package Versions (P1)
| Method | Path | Auth |
|--------|------|:----:|
| `GET` | `/api/v1/service-packages/{id}/versions` | 🌐 Public |
| `GET` | `/api/v1/service-packages/{id}/versions/{vId}` | 🌐 Public |
| `GET` | `/api/v1/service-packages/{id}/versions/{vId}/curriculums` | 🌐 Public |
| `PUT` | `/api/v1/service-packages/{id}/versions/{vId}` | 🔒 MENTOR |
| `PATCH` | `/api/v1/service-packages/{id}/versions/{vId}/default` | 🔒 MENTOR |
| `DELETE` | `/api/v1/service-packages/{id}/versions/{vId}` | 🔒 MENTOR |

### Booking (P1)
| Method | Path | Auth |
|--------|------|:----:|
| `POST` | `/api/v1/bookings/{id}/cancel` | 🔒 JWT |
| `POST` | `/api/v1/bookings/{id}/sessions/{sId}/complete` | 🔒 MENTOR |

### Notification (P2)
| Method | Path | Auth |
|--------|------|:----:|
| `GET` | `/api/v1/notifications` | 🔒 JWT |
| `GET` | `/api/v1/notifications/unread-count` | 🔒 JWT |
| `PATCH` | `/api/v1/notifications/{id}/read` | 🔒 JWT |
| `POST` | `/api/v1/notifications/read-all` | 🔒 JWT |
| `POST` | `/api/v1/devices/register` | 🔒 JWT |
| `POST` | `/api/v1/devices/unregister` | 🔒 JWT |

### Review & Rating (P2)
| Method | Path | Auth |
|--------|------|:----:|
| `POST` | `/api/v1/bookings/{id}/reviews` | 🔒 JWT (Buyer) |
| `GET` | `/api/v1/mentors/{id}/reviews` | 🌐 Public |
| `GET` | `/api/v1/mentors/{id}/rating-summary` | 🌐 Public |

### Mentor Finance & Payouts (P2)
| Method | Path | Auth |
|--------|------|:----:|
| `GET` | `/api/v1/mentors/me/revenue-summary` | 🔒 MENTOR |
| `POST` | `/api/v1/mentors/me/payouts` | 🔒 MENTOR |
| `GET` | `/api/v1/mentors/me/payouts` | 🔒 MENTOR |
| `GET` | `/api/v1/admin/payouts` | 🔒 ADMIN |
| `POST` | `/api/v1/admin/payouts/{id}/approve` | 🔒 ADMIN |
| `POST` | `/api/v1/admin/payouts/{id}/reject` | 🔒 ADMIN |
| `POST` | `/api/v1/admin/payouts/{id}/pay` | 🔒 ADMIN |
| `POST` | `/api/v1/admin/payouts/{id}/fail` | 🔒 ADMIN |

---

## Database Migrations

| # | File | Phase | Mô tả |
|:-:|------|:-----:|--------|
| 1 | `V202605230002__add_service_package_version_fields.sql` | P1 | Thêm `version` + `deleted_at` vào `service_package_versions` |
| 2 | `V202605230003__create_notifications_and_devices.sql` | P2 | Tạo bảng `notifications` + `device_tokens` |
| 3 | `V202605230004__create_reviews.sql` | P2 | Tạo bảng `booking_reviews` + rating columns |
| 4 | `V202605230005__create_mentor_finance.sql` | P2 | Tạo bảng `payout_requests` + `payout_audit_logs` |

---

## Tổng hợp Error Codes mới

| Module | Error Code | HTTP | Phase |
|--------|-----------|:----:|:-----:|
| File | `FILE_SIZE_LIMIT_EXCEEDED` | 400 | P0 |
| File | `INVALID_FILE_TYPE` | 400 | P0 |
| Progress Report | `PROGRESS_REPORT_INVALID_STATE` | 400 | P0 |
| Mentor | `PROFILE_ALREADY_VERIFIED` | 400 | P0 |
| Mentor | `PROFILE_INCOMPLETE` | 400 | P0 |
| Service | `VERSION_HAS_ACTIVE_ORDERS` | 409 | P1 |
| Service | `CANNOT_DELETE_DEFAULT_VERSION` | 409 | P1 |
| Service | `PACKAGE_MUST_HAVE_VERSION` | 400 | P1 |
| Booking | `BOOKING_CANNOT_CANCEL` | 400 | P1 |
| Booking | `REVIEW_ALREADY_EXISTS` | 409 | P2 |
| Booking | `BOOKING_NOT_COMPLETED` | 400 | P2 |
| Booking | `REVIEW_ACCESS_DENIED` | 403 | P2 |
| Finance | `INSUFFICIENT_BALANCE` | 400 | P2 |
| Finance | `INVALID_PAYOUT_AMOUNT` | 400 | P2 |
| Finance | `PAYOUT_REQUEST_NOT_FOUND` | 404 | P2 |
| Finance | `INVALID_PAYOUT_STATUS_TRANSITION` | 400 | P2 |
| Finance | `PAYOUT_ACCESS_DENIED` | 403 | P2 |

---

## Tổng hợp Kiểm thử

| Test File | Module | Phase | Số lượng case |
|-----------|--------|:-----:|:------------:|
| `ChatControllerTest` | Chat | P0 | 4+ |
| `ProgressReportControllerTest` | Progress Report | P0 | 5+ |
| `MentorProfileControllerTest` | Mentor | P0 | 4+ |
| `ProgressReportServiceImplTest` | Progress Report | P0 | 3+ |
| `ReviewServiceImplTest` | Review | P2 | 5+ |
| `PayoutServiceImplTest` | Finance | P2 | 5+ |

---

## Tài liệu tham khảo khác

| File | Mô tả |
|------|--------|
| [BACKEND_API_GAPS.md](file:///d:/Projects/Sociedu/sociedu-api/BACKEND_API_GAPS.md) | Tài liệu gốc bàn giao API gaps (đã được triển khai hết) |
| [P2_TECHNICAL_HANDOVER.md](file:///d:/Projects/Sociedu/sociedu-api/P2_TECHNICAL_HANDOVER.md) | Tài liệu chuyển giao kỹ thuật P2 (chi tiết thiết kế) |
| [PROJECT_RULES.md](file:///d:/Projects/Sociedu/sociedu-api/PROJECT_RULES.md) | Quy tắc coding standards |
| [SECURITY.md](file:///d:/Projects/Sociedu/sociedu-api/SECURITY.md) | Tài liệu bảo mật |
