# Security & RBAC — Unishare API

> File này là **nguồn sự thật duy nhất** về phân quyền trong hệ thống.
> Agent PHẢI đọc file này trước khi thêm `@PreAuthorize` vào bất kỳ endpoint nào.

---

## 1. Model RBAC

Hệ thống sử dụng **Role-Based Access Control (RBAC)** hai tầng:

```
User ──→ Role ──→ Capability
```

- **Role**: Vai trò tổng quát của người dùng trong hệ thống.
- **Capability**: Quyền hạn cụ thể cho từng hành động nghiệp vụ.
- Một Role có nhiều Capability. Một User có thể có nhiều Role.

**Ưu tiên kiểm tra**: Kiểm tra `Role` trước. Nếu cần kiểm tra hành động hạt nhân, dùng `Capability`.

---

## 2. Danh sách Roles

| Role | Mô tả |
|---|---|
| `ADMIN` | Quản trị viên hệ thống. Toàn quyền. |
| `MENTOR` | Người hướng dẫn, tạo gói dịch vụ và dạy học. |
| `MENTEE` | Người học, mua gói dịch vụ và đặt lịch học. |

> **Lưu ý**: Một User có thể vừa là `MENTOR` vừa là `MENTEE` (đặc thù marketplace).

---

## 3. Danh sách Capabilities

### Auth & User Management
| Capability | Mô tả | Roles mặc định |
|---|---|---|
| `USER_READ_ANY` | Xem thông tin bất kỳ user | ADMIN |
| `USER_SUSPEND` | Đình chỉ tài khoản | ADMIN |
| `ROLE_MANAGE` | Tạo/sửa/gán Role + Capability | ADMIN |

### Service Package
| Capability | Mô tả | Roles mặc định |
|---|---|---|
| `SERVICE_CREATE` | Tạo gói dịch vụ | MENTOR |
| `SERVICE_EDIT_OWN` | Sửa gói dịch vụ của mình | MENTOR |
| `SERVICE_TOGGLE` | Bật/tắt gói dịch vụ | MENTOR |

### Order & Payment
| Capability | Mô tả | Roles mặc định |
|---|---|---|
| `ORDER_CREATE` | Đặt mua gói dịch vụ | MENTEE |
| `ORDER_CANCEL_OWN` | Hủy đơn hàng của mình (khi còn pending) | MENTEE |
| `ORDER_READ_ANY` | Xem mọi đơn hàng trong hệ thống | ADMIN |

### Booking & Session
| Capability | Mô tả | Roles mặc định |
|---|---|---|
| `SESSION_SCHEDULE` | Lên lịch và cập nhật buổi học | MENTOR |
| `SESSION_CONFIRM` | Xác nhận lịch học | MENTEE |
| `SESSION_COMPLETE` | Đánh dấu buổi học hoàn thành | MENTOR |
| `EVIDENCE_UPLOAD` | Tải lên bằng chứng buổi học | MENTOR, MENTEE |

### Dispute & Report
| Capability | Mô tả | Roles mặc định |
|---|---|---|
| `DISPUTE_CREATE` | Khởi tạo tranh chấp | MENTOR, MENTEE |
| `DISPUTE_RESOLVE` | Phân xử và giải quyết tranh chấp | ADMIN |
| `REPORT_REVIEW` | Xem xét và xử lý báo cáo vi phạm | ADMIN |

---

## 4. Bảng Phân quyền Endpoint theo Module

### Module: Auth (tất cả Public)
```
POST /api/v1/auth/register          → Public
POST /api/v1/auth/login             → Public
POST /api/v1/auth/refresh           → Public
POST /api/v1/auth/verify-otp        → Public
POST /api/v1/auth/forgot-password   → Public
POST /api/v1/auth/reset-password    → Public
```

### Module: User
```
GET  /api/v1/users/me               → isAuthenticated()
PUT  /api/v1/users/me               → isAuthenticated()
GET  /api/v1/users/{id}             → isAuthenticated()
GET  /api/v1/users/{id}/educations  → isAuthenticated()
POST /api/v1/users/me/educations    → isAuthenticated()
```

### Module: Service
```
GET  /api/v1/service-packages           → Public
GET  /api/v1/service-packages/{id}      → Public
POST /api/v1/service-packages           → hasRole('MENTOR')
PUT  /api/v1/service-packages/{id}      → hasRole('MENTOR')
PATCH /api/v1/service-packages/{id}/toggle → hasRole('MENTOR')
```

### Module: Order
```
POST /api/v1/orders                 → hasRole('MENTEE')
GET  /api/v1/orders                 → isAuthenticated()
GET  /api/v1/orders/{id}            → isAuthenticated()
```

### Module: Payment
```
POST /api/v1/payments/checkout      → isAuthenticated()
POST /api/v1/payments/webhook       → permitAll() (xác minh bằng signature, không phải JWT)
GET  /api/v1/payments/{id}          → isAuthenticated()
```

### Module: Booking
```
GET  /api/v1/bookings               → isAuthenticated()
GET  /api/v1/bookings/{id}          → isAuthenticated()
POST /api/v1/bookings/{id}/sessions/{sid}/schedule   → hasRole('MENTOR')
POST /api/v1/bookings/{id}/sessions/{sid}/confirm    → hasRole('MENTEE')
POST /api/v1/bookings/{id}/sessions/{sid}/complete   → hasRole('MENTOR')
POST /api/v1/bookings/{id}/sessions/{sid}/evidences  → isAuthenticated()
```

### Module: Chat
```
GET  /api/v1/conversations          → isAuthenticated()
POST /api/v1/conversations/{id}/messages → isAuthenticated()
WS   /api/v1/ws                     → isAuthenticated() (JWT in header)
```

### Module: Dispute
```
POST /api/v1/disputes               → isAuthenticated()
GET  /api/v1/disputes               → hasRole('ADMIN')
GET  /api/v1/disputes/{id}          → isAuthenticated()
PATCH /api/v1/disputes/{id}/resolve → hasRole('ADMIN')
```

### Module: Report
```
POST /api/v1/reports                → isAuthenticated()
GET  /api/v1/reports                → hasRole('ADMIN')
PATCH /api/v1/reports/{id}/review   → hasRole('ADMIN')
```

### Module: File
```
POST /api/v1/files/upload           → isAuthenticated()
GET  /api/v1/files/{id}             → isAuthenticated() (kiểm tra visibility trong Service)
DELETE /api/v1/files/{id}           → isAuthenticated() (kiểm tra ownership trong Service)
```

---

## 5. Quy tắc áp dụng Security trong Code

### Controller — @PreAuthorize
```java
// Kiểm tra Role (phổ biến nhất)
@PreAuthorize("hasRole('MENTOR')")

// Kiểm tra nhiều role (OR logic)
@PreAuthorize("hasAnyRole('MENTOR', 'ADMIN')")

// Kiểm tra Capability (dùng khi cần hạt nhân hơn)
@PreAuthorize("hasAuthority('SERVICE_CREATE')")

// Mọi user đã đăng nhập
@PreAuthorize("isAuthenticated()")

// Webhook — không dùng JWT, verify bằng signature trong Service
// Không đặt @PreAuthorize — khai báo permitAll() trong SecurityConfig
```

### Service — Ownership Check
```java
// KHÔNG check ownership ở Controller — làm ở Service
public OrderResponse getOrder(Long orderId, Long requesterId) {
    Order order = findOrderById(orderId);

    boolean isBuyer  = order.getBuyerId().equals(requesterId);
    boolean isMentor = packageService.isMentorOfPackage(order.getServiceId(), requesterId);

    if (!isBuyer && !isMentor) {
        throw new UnauthorizedException("Bạn không có quyền xem đơn hàng này");
    }
    return orderMapper.toResponse(order);
}
```

### SecurityConfig — Khai báo Public + Webhook endpoints
```java
.authorizeHttpRequests(auth -> auth
    .requestMatchers(
        "/api/v1/auth/**",
        "/api/v1/service-packages",
        "/api/v1/service-packages/**",
        "/api/v1/payments/webhook",  // Không dùng JWT, dùng signature
        "/swagger-ui/**",
        "/v3/api-docs/**"
    ).permitAll()
    .anyRequest().authenticated()
)
```

---

## 6. Cấu trúc JWT Claims

```json
{
  "sub": "123",
  "email": "user@example.com",
  "roles": ["MENTOR", "MENTEE"],
  "capabilities": ["SERVICE_CREATE", "SERVICE_EDIT_OWN", "ORDER_CREATE"],
  "iat": 1712345678,
  "exp": 1712349278
}
```

- **Access Token TTL**: 15 phút
- **Refresh Token TTL**: 7 ngày
- Capabilities được nhúng vào token để tránh query DB mỗi request.
- Khi Admin thay đổi quyền, cần thu hồi Refresh Token của user đó để force re-login.

---

## 7. Rule cho Agent

- Webhook endpoints (`/payments/webhook`) KHÔNG dùng JWT → `permitAll()` trong SecurityConfig + xác minh signature trong Service.
- Kiểm tra **ownership** (user chỉ xem data của mình) → luôn thực hiện trong **Service layer**, không phải Controller.
- Kiểm tra **role** → dùng `@PreAuthorize` trong Controller.
- Khi tạo endpoint mới → phải cập nhật bảng phân quyền trong file này.
