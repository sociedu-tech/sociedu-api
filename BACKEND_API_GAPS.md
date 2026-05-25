# Backend API Gaps / Tai lieu ban giao cho dev backend

## Muc dich

Tai lieu nay tong hop cac API backend con thieu hoac chua khop voi mobile app SocieDu hien tai. Muc tieu la giup backend dev uu tien sua contract tich hop, tranh tinh trang mobile co man hinh/service nhung goi sai endpoint hoac backend chua co API tuong ung.

## Pham vi

Nguon doi chieu:

- Backend controller trong `sociedu-api/src/main/java/com/unishare/api/modules`.
- Mobile service layer trong `sociedu-mobile/src/core/services`.
- Mobile API path helper trong `sociedu-mobile/src/core/backend.ts`.
- Mobile docs hien co: `API_CONTRACT.md`, `KNOWN_GAPS.md`, `SCREEN_MAP.md`.

Tai lieu nay tap trung vao API can cho mobile MVP, khong bao gom toan bo admin/internal API.

## Nguyen tac contract de xuat

- Tien to API thong nhat: `/api/v1`.
- Response wrapper nen co toi thieu field `data`; cac field `code`, `isSuccess`, `message`, `errors`, `timestamp` co the giu theo convention backend hien tai.
- Mobile dang unwrap theo pattern `response.data.data`, nen backend can tra object co `data`.
- ID nen thong nhat UUID string tren API.
- Status enum can thong nhat mot cach viet, vi hien mobile/docs co ca `canceled` va `cancelled`.

## Uu tien P0 - can sua truoc de mobile tich hop duoc

### 1. Chat / Conversation path mismatch

Trang thai hien tai:

- Backend co controller tai `/api/v1/chat`.
- Mobile dang goi `/api/v1/conversations` va `/api/v1/conversations/{id}/messages`.

Backend hien co:

- `POST /api/v1/chat/conversations`
- `GET /api/v1/chat/conversations`
- `GET /api/v1/chat/conversations/{conversationId}/messages`
- `POST /api/v1/chat/conversations/{conversationId}/messages`

Mobile dang ky vong:

- `GET /api/v1/conversations`
- `GET /api/v1/conversations/{id}`
- `GET /api/v1/conversations/{id}/messages?page=&limit=`
- `POST /api/v1/conversations/{id}/messages`
- `POST /api/v1/conversations`

De xuat:

- Cach 1: backend them alias `/api/v1/conversations...` cho mobile.
- Cach 2: mobile doi `API_PATHS.conversations` sang `/api/v1/chat/conversations`.
- Backend nen bo sung `GET conversation detail by id`.
- Backend nen ho tro pagination cho messages neu danh sach tin nhan co the dai.

### 2. File upload path mismatch

Trang thai hien tai:

- Backend co `POST /api/v1/files`.
- Mobile config dang dung `POST /api/v1/files/upload`.

Can thong nhat:

- `POST /api/v1/files` hoac `POST /api/v1/files/upload`.
- Field multipart can khop `BACKEND_CONFIG.uploadFieldName`.
- Response can tra `id`, `url`, `contentType`, `size`, `visibility` neu mobile dung file cho avatar, certificate, evidence, message attachment.

De xuat contract:

```http
POST /api/v1/files
Content-Type: multipart/form-data
Authorization: Bearer <token>
```

Response:

```json
{
  "data": {
    "id": "uuid",
    "url": "https://...",
    "fileName": "certificate.pdf",
    "contentType": "application/pdf",
    "size": 123456,
    "visibility": "private"
  }
}
```

### 3. Report / Dispute path mismatch

Trang thai hien tai:

- Backend gom report/dispute trong `/api/v1/trust`.
- Mobile service dang goi truc tiep `/reports` va `/disputes`.

Backend hien co:

- `POST /api/v1/trust/reports`
- `GET /api/v1/trust/reports/me`
- `POST /api/v1/trust/reports/{reportId}/evidences`
- `PUT /api/v1/trust/reports/{reportId}/resolve`
- `POST /api/v1/trust/disputes`
- `GET /api/v1/trust/disputes/me`
- `PUT /api/v1/trust/disputes/{disputeId}/resolve`

Can lam:

- Mobile doi path sang `/api/v1/trust/...`, hoac backend them alias `/api/v1/reports` va `/api/v1/disputes`.
- Can xac nhan response DTO cho mobile: `id`, `status`, `type`, `targetType`, `targetId`, `reason`, `description`, `evidences`, `createdAt`, `resolvedAt`.

### 4. Progress report path mismatch

Trang thai hien tai:

- Backend tach endpoint theo role:
- Mentee: `/api/v1/mentee/reports`
- Mentor: `/api/v1/mentors/me/reports`
- Mobile dang goi `/api/v1/progress-reports/...`.

Backend hien co:

- `POST /api/v1/mentee/reports`
- `GET /api/v1/mentee/reports`
- `GET /api/v1/mentors/me/reports`
- `PUT /api/v1/mentors/me/reports/{id}/feedback`

Mobile dang ky vong:

- `GET /api/v1/progress-reports/me`
- `GET /api/v1/progress-reports/{id}`
- `POST /api/v1/progress-reports`
- `POST /api/v1/progress-reports/{id}/mentor-feedback`

Can lam:

- Chon mot namespace chinh thuc va cap nhat ben con lai.
- Bo sung `GET progress report detail by id`.
- Thong nhat method feedback: backend dang `PUT`, mobile dang `POST`.
- Response list nen la `Page` hoac array nhat quan; neu la `Page`, mobile can doc `content`.

### 5. Mentor profile lifecycle thieu endpoint

Trang thai hien tai:

- Backend co `GET /api/v1/mentors`, `GET /api/v1/mentors/{id}`, `PUT /api/v1/mentors/me`.
- Mobile dang goi them `/api/v1/mentors/me/profile` va `/api/v1/mentors/me/profile/submit`.

Can bo sung hoac doi mobile:

- `GET /api/v1/mentors/me/profile`: lay ho so mentor cua user dang nhap.
- `POST /api/v1/mentors/me/profile/submit`: gui ho so mentor vao trang thai cho duyet.

Ly do:

- Mobile co man hinh edit mentor profile va luong submit verification.
- Backend hien co event `MentorRequestApprovedEvent`, `MentorRequestRejectedEvent`, nen lifecycle approve/reject da nam trong domain nhung API submit chua ro.

De xuat endpoint:

```http
GET /api/v1/mentors/me/profile
POST /api/v1/mentors/me/profile/submit
```

## Uu tien P1 - can cho MVP hoan chinh

### 6. Service package version management chua du

Backend hien co:

- `POST /api/v1/service-packages/{id}/versions`
- `GET /api/v1/service-packages/{id}/versions`
- `GET /api/v1/service-packages/{id}/versions/{versionId}`

Mobile co logic nhung backend chua thay endpoint tuong ung:

- Update version.
- Delete/archive version.
- Set default version.

De xuat bo sung:

```http
PUT /api/v1/service-packages/{packageId}/versions/{versionId}
DELETE /api/v1/service-packages/{packageId}/versions/{versionId}
PATCH /api/v1/service-packages/{packageId}/versions/{versionId}/default
```

Business rule can co:

- Khong xoa version da co order/booking.
- Khong xoa version default neu package con active.
- Moi package nen co it nhat mot version hop le.

### 7. Public package version/curriculum read

Trang thai hien tai:

- Public API co `GET /api/v1/service-packages` va `GET /api/v1/service-packages/{id}`.
- Version/curriculum detail hien dang co `@PreAuthorize("hasRole('MENTOR')")` trong `ServicePackageController`.

Can xac nhan:

- Buyer co can xem version/curriculum truoc checkout khong.
- Neu co, backend can public read cho active package/version.

De xuat:

```http
GET /api/v1/service-packages/{packageId}/versions
GET /api/v1/service-packages/{packageId}/versions/{versionId}
GET /api/v1/service-packages/{packageId}/versions/{versionId}/curriculums
```

Chi cho phep tra version/curriculum cua package active/public.

### 8. Booking scheduling va session action

Backend hien co:

- `GET /api/v1/bookings/me/buyer`
- `GET /api/v1/bookings/me/mentor`
- `GET /api/v1/bookings/{id}`
- `PATCH /api/v1/bookings/{bookingId}/sessions/{sessionId}`
- `POST /api/v1/bookings/{bookingId}/sessions/{sessionId}/evidences`

Con thieu neu muon booking flow day du:

- Mentor availability slots.
- Request reschedule.
- Approve/reject reschedule.
- Cancel booking/session co ly do.
- Mark completed/no-show rieng neu khong muon dung PATCH tong quat.

De xuat endpoint:

```http
GET /api/v1/mentors/{mentorId}/availability
PUT /api/v1/mentors/me/availability
POST /api/v1/bookings/{bookingId}/sessions/{sessionId}/reschedule-requests
POST /api/v1/bookings/{bookingId}/sessions/{sessionId}/reschedule-requests/{requestId}/approve
POST /api/v1/bookings/{bookingId}/sessions/{sessionId}/reschedule-requests/{requestId}/reject
POST /api/v1/bookings/{bookingId}/cancel
POST /api/v1/bookings/{bookingId}/sessions/{sessionId}/complete
```

## Uu tien P2 - nen co sau khi P0/P1 on dinh

### 9. Notification center

Mobile co route notification, backend chua thay module notification.

Can API:

```http
GET /api/v1/notifications
GET /api/v1/notifications/unread-count
PATCH /api/v1/notifications/{id}/read
POST /api/v1/notifications/read-all
POST /api/v1/devices/push-token
DELETE /api/v1/devices/push-token/{id}
```

Event nen tao notification:

- Order paid/payment failed.
- Booking created/canceled/completed.
- Mentor approved/rejected.
- New message.
- Report/dispute status changed.
- Progress report submitted/reviewed.

### 10. Review / Rating

Marketplace mentor nen co review/rating sau booking hoan tat.

Can API:

```http
POST /api/v1/bookings/{bookingId}/reviews
GET /api/v1/mentors/{mentorId}/reviews
GET /api/v1/service-packages/{packageId}/reviews
GET /api/v1/mentors/{mentorId}/rating-summary
```

Business rule:

- Chi mentee da mua va booking/session hoan tat moi duoc review.
- Moi booking nen review mot lan, hoac cho update trong thoi gian gioi han.

### 11. Mentor finance / payout

Backend hien co placeholder:

- `GET /api/v1/mentors/me/stats`
- `GET /api/v1/mentors/me/withdrawals`

Can API that:

```http
GET /api/v1/mentors/me/revenue-summary
GET /api/v1/mentors/me/payouts
POST /api/v1/mentors/me/payouts
GET /api/v1/mentors/me/payouts/{id}
```

Response can bao gom:

- Tong doanh thu.
- So tien pending.
- So tien co the rut.
- Phi nen tang.
- Lich su payout va trang thai.

## Cac contract can thong nhat lai

### Auth

Backend da co day du cac endpoint chinh:

- Register/login/refresh/logout.
- Verify email/resend verification.
- Forgot/reset password.
- Phone OTP va login OTP.
- Session list/revoke.

Can xac nhan:

- `POST /auth/refresh` tra ve cung shape voi login: `accessToken`, `refreshToken`, `user`.
- `GET /auth/me` tra ve du role/capabilities/status de mobile route guard.

### Order/payment

Backend da co:

- `POST /api/v1/orders/checkout`
- `GET /api/v1/orders/me`
- `GET /api/v1/orders/{id}`
- `GET /api/v1/payments/order/{orderId}`
- VNPay IPN/return.

Can xac nhan:

- Checkout body dung `packageVersionId`.
- `paymentUrl` nam trong `OrderResponse` hay `PaymentResponse`.
- Order status dung mot bo enum duy nhat.
- Sau payment success backend tao booking/session tu dong hay mobile can goi API rieng.

### Status naming

Can chot enum chinh thuc:

- `canceled` hay `cancelled`.
- `pending_payment`, `paid`, `failed`, `expired`.
- `scheduled`, `completed`, `cancelled`, `no_show`.
- `pending`, `approved`, `rejected`, `resolved`.

Khuyen nghi: backend cong bo enum trong API docs/OpenAPI va mobile map theo enum do.

## Checklist ban giao cho backend

- Sua hoac them alias cho cac path mismatch P0.
- Bo sung API detail/submit dang thieu: conversation detail, progress report detail, mentor profile me/submit.
- Chot namespace chinh thuc cho chat, trust report/dispute, progress report.
- Chot response DTO cho file upload, chat message, progress report, dispute/report.
- Chot enum status va cach viet.
- Cap nhat OpenAPI/Swagger sau khi sua.
- Tao test controller cho cac endpoint P0.
- Gui lai mobile team danh sach endpoint cuoi cung kem sample request/response.

## Bang tom tat uu tien

| Uu tien | Hang muc | Ly do |
| --- | --- | --- |
| P0 | Chat path mismatch | Mobile messages se loi ngay khi tat mock |
| P0 | File upload path mismatch | Avatar/certificate/evidence/attachment can upload |
| P0 | Report/dispute path mismatch | Mobile service dang goi sai namespace |
| P0 | Progress report path mismatch | Man hinh report co service nhung khong khop backend |
| P0 | Mentor me profile/submit | Can cho mentor onboarding/verification |
| P1 | Package version CRUD | Mobile co UI/logic quan ly version |
| P1 | Public version/curriculum read | Buyer can xem noi dung goi truoc checkout |
| P1 | Booking scheduling/reschedule/cancel | Can cho booking flow thuc te |
| P2 | Notification | Nang chat UX, khong chan MVP core |
| P2 | Review/rating | Quan trong cho marketplace trust |
| P2 | Mentor finance/payout | Can cho van hanh, co the sau MVP core |

