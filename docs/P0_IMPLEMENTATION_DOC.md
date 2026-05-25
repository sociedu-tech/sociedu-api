# Tài liệu tích hợp API P0 (Cập nhật) — Khắc phục lỗi Mismatch đường dẫn API giữa Backend và Mobile App

Tài liệu này tổng hợp toàn bộ các thay đổi và cấu hình API mới ở độ ưu tiên **P0** nhằm khắc phục sự không khớp về đường dẫn API và cơ chế hoạt động của một số tính năng chính, có tính đến khả năng tương thích ngược và cấu trúc mã nguồn tốt nhất.

---

## 1. Module Trò chuyện (Chat / Conversations)
*   **Vấn đề**: Mobile App gọi tới namespace `/api/v1/conversations/...` trong khi Backend ban đầu cấu hình `/api/v1/chat/conversations/...`. Đồng thời Mobile App yêu cầu phân trang cho tin nhắn và lấy chi tiết cuộc trò chuyện.
*   **Giải pháp**:
    *   Hỗ trợ song song cả 2 prefix `/api/v1/chat/conversations` và `/api/v1/conversations`.
    *   Thêm endpoint lấy chi tiết cuộc hội thoại: `GET /api/v1/conversations/{conversationId}`.
    *   Thêm API tải tin nhắn phân trang: `GET /api/v1/conversations/{conversationId}/messages` nhận `page`, `size` và `sort` (trả về JSON dạng `Page`). Để đảm bảo UI hiển thị tốt nhất (tin nhắn mới nhất tải đầu tiên), danh sách này được sắp xếp theo **DESC** (`OrderByCreatedAtDesc`).
    *   Giữ nguyên API cũ `GET /api/v1/chat/conversations/{conversationId}/messages` trả về dạng `List` (đã được đánh dấu `@Deprecated`) để đảm bảo các phiên bản cũ hoạt động bình thường.

### Danh sách các API Chat:
*   `POST /api/v1/conversations` & `POST /api/v1/chat/conversations` — Tạo cuộc hội thoại.
*   `GET /api/v1/conversations` & `GET /api/v1/chat/conversations` — Danh sách cuộc hội thoại của tôi.
*   `GET /api/v1/conversations/{conversationId}` & `GET /api/v1/chat/conversations/{conversationId}` — Lấy chi tiết cuộc hội thoại.
*   `GET /api/v1/conversations/{conversationId}/messages` — Tin nhắn phân trang (Mới, Sắp xếp DESC).
*   `GET /api/v1/chat/conversations/{conversationId}/messages` (Deprecated) — Tin nhắn dạng mảng `List` (Cũ).
*   `POST /api/v1/conversations/{conversationId}/messages` & `POST /api/v1/chat/conversations/{conversationId}/messages` — Gửi tin nhắn.

---

## 2. Module Upload File
*   **Vấn đề**: Mobile App gọi `POST /api/v1/files/upload` trong khi Backend cấu hình `POST /api/v1/files`.
*   **Giải pháp**: Cập nhật controller hỗ trợ cả 2 đường dẫn. Bổ sung kiểm tra định dạng và dung lượng file để đảm bảo an toàn.
*   **Chi tiết validation**:
    *   Kích thước tối đa: **10MB** (Vượt quá trả về mã lỗi `FILE_SIZE_LIMIT_EXCEEDED` - HTTP 400).
    *   Phần mở rộng được phép: `jpg, jpeg, png, gif, pdf, doc, docx, xls, xlsx, ppt, pptx, txt, zip, rar` (Định dạng sai trả về `INVALID_FILE_TYPE` - HTTP 400).
*   **Endpoint hỗ trợ**:
    *   `POST /api/v1/files`
    *   `POST /api/v1/files/upload`

---

## 3. Module Báo cáo & Tranh chấp (Trust)
*   **Vấn đề**: Mobile App gọi trực tiếp `/api/v1/reports/...` và `/api/v1/disputes/...` thay vì qua prefix `/trust`.
*   **Giải pháp**: Giữ nguyên `@RequestMapping("/api/v1/trust")` tại `TrustController` để cô lập domain rõ ràng. Tạo riêng 2 Controller alias kế thừa (marked `@Deprecated`):
    *   `ReportAliasController` (ánh xạ `/api/v1/reports`)
    *   `DisputeAliasController` (ánh xạ `/api/v1/disputes`)
    Các alias controller này sẽ ủy quyền toàn bộ lời gọi API sang `TrustService`.

---

## 4. Báo cáo Tiến độ Học tập (Progress Reports)
*   **Vấn đề**: Mobile App cần một API chung `/api/v1/progress-reports` cho cả Mentee và Mentor, tự động xử lý trường hợp tài khoản có cả hai vai trò.
*   **Giải pháp**: Tạo `ProgressReportController` mới tại `/api/v1/progress-reports`.
*   **Các endpoint hỗ trợ**:
    *   `POST /api/v1/progress-reports` — Mentee nộp báo cáo tiến độ.
    *   `GET /api/v1/progress-reports/me` — Nhận tham số tùy chọn `role` (`?role=mentor` hoặc `?role=mentee`).
        *   Nếu là Mentor (hoặc chỉ định `role=mentor`): trả về danh sách được gán.
        *   Nếu là Mentee (hoặc chỉ định `role=mentee`): trả về danh sách đã gửi.
        *   Trường hợp có cả hai vai trò và không truyền `role`: mặc định trả về danh sách đã gửi (Mentee view).
    *   `GET /api/v1/progress-reports/{id}` — Lấy chi tiết báo cáo tiến độ (yêu cầu quyền sở hữu: người gửi hoặc người nhận).
    *   `POST /api/v1/progress-reports/{id}/mentor-feedback` (Hỗ trợ cả PUT/POST) — Mentor phản hồi báo cáo tiến độ.
        *   **Validation**: Chỉ cho phép phản hồi khi báo cáo ở trạng thái `PENDING`. Nếu đã được duyệt hoặc từ chối trước đó, trả về lỗi `PROGRESS_REPORT_INVALID_STATE` (HTTP 400).

---

## 5. Vòng đời Nộp Hồ sơ Mentor (Mentor Profile Submission)
*   **Vấn đề**: Cần API kiểm tra profile hiện tại và API gửi yêu cầu duyệt hồ sơ an toàn, idempotent.
*   **Các endpoint hỗ trợ**:
    *   `GET /api/v1/mentors/me/profile` — Lấy thông tin profile mentor cá nhân.
    *   `POST /api/v1/mentors/me/profile/submit` — Gửi duyệt hồ sơ.
        *   **State Machine Validation**: Nếu trạng thái đã là `verified` -> trả về lỗi `PROFILE_ALREADY_VERIFIED` (HTTP 400).
        *   **Completeness Validation**: Các thông tin `headline`, `expertise`, `basePrice` bắt buộc không được để trống -> nếu thiếu trả về lỗi `PROFILE_INCOMPLETE` (HTTP 400).
        *   **Idempotency**: Nếu trạng thái hiện tại đã là `pending`, trả về thành công ngay lập tức mà không lưu đè DB để phòng tránh race condition.

---

## 🧪 Kết quả Kiểm thử tự động (Unit/Integration Tests)
Toàn bộ logic nghiệp vụ mới đã được bao phủ bởi các unit test và chạy thành công:
1.  **ChatControllerTest**: Kiểm thử tạo, lấy chi tiết, tải tin nhắn phân trang (DESC) và tương thích ngược.
2.  **ProgressReportControllerTest**: Kiểm thử nộp báo cáo, xem chi tiết, danh sách động theo vai trò với bộ lọc tham số `role` và phản hồi.
3.  **MentorProfileControllerTest**: Kiểm thử xem profile cá nhân, gửi duyệt thành công, chặn gửi khi hồ sơ chưa đầy đủ (`PROFILE_INCOMPLETE`) hoặc đã được duyệt (`PROFILE_ALREADY_VERIFIED`).
4.  **ProgressReportServiceImplTest**: Kiểm thử phân quyền xem chi tiết và kiểm tra trạng thái báo cáo trước khi feedback.
