# Tài liệu Triển khai các Hạng mục P1 (SocieDu API)

Tài liệu này mô tả chi tiết các thay đổi, luồng xử lý (business logic), mã lỗi (error codes) và database migration cho các hạng mục P1 đã hoàn thành triển khai.

---

## 1. Hạng mục #7 — Public Package Version / Curriculum Read

### Mô tả
Cho phép khách (người dùng không cần đăng nhập) xem danh sách phiên bản gói dịch vụ và curriculum của các gói dịch vụ đang hoạt động (`isActive = true` và `deletedAt IS NULL`).

### Danh sách API
*   **Danh sách phiên bản gói dịch vụ (Public)**
    *   `GET /api/v1/service-packages/{id}/versions`
    *   Không yêu cầu Token.
    *   Chỉ hiển thị các phiên bản thuộc gói dịch vụ đang hoạt động.
*   **Chi tiết phiên bản gói dịch vụ (Public)**
    *   `GET /api/v1/service-packages/{id}/versions/{versionId}`
    *   Không yêu cầu Token.
*   **Danh sách curriculum của phiên bản gói (Public)**
    *   `GET /api/v1/service-packages/{id}/versions/{versionId}/curriculums`
    *   Không yêu cầu Token.

---

## 2. Hạng mục #6 — Service Package Version CRUD

### Mô tả
Cho phép Mentor quản lý (cập nhật, xóa, thiết lập mặc định) các phiên bản gói dịch vụ của mình.

### Danh sách API & Business Rules
*   **Cập nhật phiên bản gói dịch vụ**
    *   `PUT /api/v1/service-packages/{id}/versions/{versionId}`
    *   Yêu cầu Role: `MENTOR` (phải là chủ sở hữu gói dịch vụ).
    *   Cho phép cập nhật từng phần (partial updates): giá tiền (`price`), thời lượng (`duration`), loại hình dạy học (`deliveryType` với các giá trị: `ONLINE`, `OFFLINE`, `HYBRID`).
*   **Đặt phiên bản làm mặc định**
    *   `PATCH /api/v1/service-packages/{id}/versions/{versionId}/default`
    *   Yêu cầu Role: `MENTOR`.
    *   Hệ thống sẽ tự động gỡ trạng thái mặc định (`isDefault = false`) của tất cả các phiên bản khác thuộc gói dịch vụ này và thiết lập phiên bản chỉ định làm mặc định (`isDefault = true`).
*   **Xóa phiên bản gói dịch vụ**
    *   `DELETE /api/v1/service-packages/{id}/versions/{versionId}`
    *   Yêu cầu Role: `MENTOR`.
    *   **Ràng buộc logic:**
        *   Không cho phép xóa phiên bản mặc định (`isDefault = true`). Phải đặt phiên bản khác làm mặc định trước.
        *   Không cho phép xóa phiên bản khi có bất kỳ đơn hàng (Order) nào đang tham chiếu tới (dựa trên `orders.service_id`).
        *   Không cho phép xóa nếu gói dịch vụ chỉ còn duy nhất 1 phiên bản (gói dịch vụ phải luôn có ít nhất một phiên bản hoạt động).

---

## 3. Hạng mục #8 — Booking Cancel & Complete Session

### Mô tả
Quản lý vòng đời Booking và các buổi học (Booking Sessions) bao gồm quy trình hủy đặt lịch và xác nhận hoàn thành buổi học.

### Danh sách API & Business Rules
*   **Hủy đặt lịch học (Cancel Booking)**
    *   `POST /api/v1/bookings/{bookingId}/cancel`
    *   Yêu cầu Role: Mentee (Buyer) hoặc Mentor có quyền truy cập vào Booking (`VIEW_BOOKING`).
    *   **Logic xử lý:**
        *   Yêu cầu lý do hủy (`reason` không được để trống).
        *   Chỉ cho phép hủy khi Booking ở trạng thái `PENDING`, `SCHEDULED` hoặc `IN_PROGRESS`.
        *   Tự động chuyển trạng thái của tất cả các buổi học (sessions) chưa diễn ra sang `CANCELED` (ngoại trừ các sessions đã `COMPLETED`, đã `CANCELED`, hoặc đang diễn ra - `IN_PROGRESS`).
        *   Cập nhật trạng thái Booking sang `CANCELED`.
        *   Publish sự kiện `BookingCanceledEvent` để hỗ trợ hệ thống Event-driven (ví dụ: thông báo hoặc hoàn tiền tự động ở P2).
*   **Hoàn thành buổi học (Complete Session)**
    *   `POST /api/v1/bookings/{bookingId}/sessions/{sessionId}/complete`
    *   Yêu cầu Role: Mentor hoặc người có quyền quản lý buổi học (`MANAGE_SESSIONS`).
    *   **Logic xử lý:**
        *   Buổi học bắt buộc phải đã được bắt đầu (có `actualStartedAt` khác null).
        *   Yêu cầu thời gian từ lúc bắt đầu/lên lịch đến lúc hoàn thành tối thiểu là 15 phút.
        *   Cập nhật trạng thái Session sang `COMPLETED`.
        *   **Tự động hoàn thành Booking:** Sau khi cập nhật session, hệ thống tự động kiểm tra xem tất cả các sessions trong Booking đã hoàn thành hay chưa, nếu tất cả đã hoàn thành (`COMPLETED` hoặc `CANCELED`), trạng thái Booking sẽ tự động chuyển sang `COMPLETED`.

---

## 4. Cơ sở dữ liệu (Database Schema changes)

### Bảng `service_package_versions`
Được thêm các cột mới qua Flyway migration (`V202605230002__add_service_package_version_fields.sql`):
*   `version` (`BIGINT NOT NULL DEFAULT 0`): Hỗ trợ cơ chế Optimistic Locking (`@Version` trong JPA) để tránh tranh chấp dữ liệu khi cập nhật đồng thời.
*   `deleted_at` (`TIMESTAMP NULL`): Hỗ trợ lưu trữ thời gian xóa phiên bản (chuẩn bị cho cơ chế soft-delete).

---

## 5. Mã lỗi mới (Error Codes)

### Tầng Dịch vụ (Service Package Module)
*   `VERSION_HAS_ACTIVE_ORDERS (409)`: Không thể xóa phiên bản đang có đơn hàng tham chiếu.
*   `CANNOT_DELETE_DEFAULT_VERSION (409)`: Không thể xóa phiên bản đang được đặt làm mặc định.
*   `PACKAGE_MUST_HAVE_VERSION (400)`: Gói dịch vụ phải giữ lại ít nhất một phiên bản.

### Tầng Đặt lịch (Booking Module)
*   `BOOKING_CANNOT_CANCEL (400)`: Đơn đặt lịch không ở trạng thái hợp lệ để hủy.
