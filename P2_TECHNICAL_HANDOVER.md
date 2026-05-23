# Tài liệu Chuyển giao Kỹ thuật - Phase 2 Backend Gaps

Tài liệu này cung cấp chi tiết kỹ thuật về các tính năng đã được thiết kế và triển khai trong **Phase 2 (P2)** của SocieDu API, bao gồm ba phân hệ chính:
1. Phân hệ Đánh giá & Phản hồi (Review & Rating Module)
2. Trung tâm Thông báo & Thiết bị (Notification Center & Device Tokens)
3. Phân hệ Quản lý Tài chính & Rút tiền Mentor (Mentor Finance & Payouts)

---

## 1. Phân hệ Đánh giá & Phản hồi (Review & Rating)

### 1.1 Nguyên tắc Thiết kế & Chống Race Condition
* **Atomic SQL Update**: Để tránh tình trạng Lost Update và Race Condition khi nhiều học viên gửi đánh giá cho cùng một Mentor cùng lúc, hệ thống sử dụng truy vấn SQL nguyên tử trực tiếp trong cơ sở dữ liệu thay vì cơ chế đọc-sửa-ghi đè (Read-Modify-Write) thông thường ở tầng Service:
  ```sql
  UPDATE mentor_profiles 
  SET rating_total = rating_total + :newRating,
      rating_count = rating_count + 1,
      rating_avg = CAST(rating_total + :newRating AS double) / (rating_count + 1)
  WHERE user_id = :mentorId
  ```
* **Chính sách Bất biến (Immutability)**: Học viên chỉ được phép gửi đánh giá một lần cho mỗi `Booking` đã hoàn thành và không được tự ý sửa đổi (Immutable) từ phía client để đảm bảo tính khách quan của dữ liệu. Cột `edited_at` và `deleted_at` được dự phòng cho mục đích kiểm duyệt của Admin (Moderation).
* **Truy vấn Hiệu năng cao (N+1 Prevention)**: Khi lấy danh sách đánh giá của Mentor/Gói dịch vụ, hệ thống thực hiện gom ID người dùng (Reviewer) và truy vấn thông tin hiển thị theo lô (Batch fetching) từ `user_profiles` để tối ưu số lượng truy vấn xuống DB.

### 1.2 Danh sách API Endpoints

#### Gửi đánh giá Booking
* **Method**: `POST`
* **Path**: `/api/v1/bookings/{bookingId}/reviews`
* **Quyền hạn**: Học viên sở hữu booking (`buyerId`). Booking phải có trạng thái `completed`.
* **Request Body**:
  ```json
  {
    "rating": 5,
    "comment": "Mentor hướng dẫn rất nhiệt tình và dễ hiểu!"
  }
  ```
* **Response**:
  ```json
  {
    "code": 200,
    "message": "Gửi đánh giá thành công",
    "data": {
      "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
      "bookingId": "a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d",
      "mentorId": "f1e2d3c4-b5a6-9788-7766-554433221100",
      "packageId": "b5a6f1e2-9788-7766-5544-332211000000",
      "reviewerId": "77665544-3322-1100-f1e2-d3c4b5a69788",
      "reviewerName": "Nguyễn Văn A",
      "rating": 5,
      "comment": "Mentor hướng dẫn rất nhiệt tình và dễ hiểu!",
      "createdAt": "2026-05-23T09:44:00Z"
    }
  }
  ```

#### Lấy danh sách đánh giá của Mentor (Công khai)
* **Method**: `GET`
* **Path**: `/api/v1/mentors/{mentorId}/reviews?page=0&size=10`
* **Response**: Trả về dữ liệu phân trang danh sách đánh giá, mặc định sắp xếp giảm dần theo thời gian tạo (`createdAt DESC`).

#### Lấy tóm tắt điểm đánh giá của Mentor (Công khai)
* **Method**: `GET`
* **Path**: `/api/v1/mentors/{mentorId}/rating-summary`
* **Response**:
  ```json
  {
    "code": 200,
    "data": {
      "ratingAvg": 4.8,
      "ratingCount": 15,
      "distribution": {
        "1": 0,
        "2": 0,
        "3": 1,
        "4": 1,
        "5": 13
      }
    }
  }
  ```

---

## 2. Trung tâm Thông báo (Notification Center)

### 2.1 Kiến trúc Xử lý Thông báo Không đồng bộ (Async & Transactional Event Listener)
* **Toàn vẹn Giao dịch**: Thông báo đẩy (Push Notifications via Firebase/FCM) có thể gặp lỗi mạng hoặc trễ từ phía nhà cung cấp thứ ba. Để đảm bảo không ảnh hưởng hoặc rollback các luồng nghiệp vụ cốt lõi (thanh toán đơn hàng, hoàn thành buổi học), hệ thống áp dụng cơ chế:
  1. Persist thông báo vào cơ sở dữ liệu đồng bộ (Sync) cùng giao dịch nghiệp vụ chính.
  2. Lắng nghe sự kiện bằng `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)`.
  3. Kích hoạt FCM Push gửi đi bất đồng bộ (`@Async`) sau khi giao dịch chính đã được ghi nhận thành công trong DB.
* **Cấu trúc Dữ liệu Động (JSONB)**: Thông báo hỗ trợ trường `metadata` dạng `JSONB` trong cơ sở dữ liệu PostgreSQL (`Map<String, Object>` trong Java) để phục vụ cho các tính năng Deep Linking trên ứng dụng Mobile (ví dụ: chuyển hướng người dùng đến đúng màn hình chi tiết buổi học khi nhấn vào thông báo).

### 2.2 Danh sách API Endpoints

#### Lấy danh sách thông báo của tôi (Đã phân trang)
* **Method**: `GET`
* **Path**: `/api/v1/notifications?page=0&size=20`
* **Quyền hạn**: Đã xác thực (JWT).

#### Lấy số lượng thông báo chưa đọc
* **Method**: `GET`
* **Path**: `/api/v1/notifications/unread-count`
* **Response**:
  ```json
  {
    "code": 200,
    "data": {
      "unreadCount": 5
    }
  }
  ```

#### Đánh dấu đọc một thông báo
* **Method**: `PATCH`
* **Path**: `/api/v1/notifications/{id}/read`

#### Đánh dấu đọc tất cả thông báo (Idempotent)
* **Method**: `POST`
* **Path**: `/api/v1/notifications/read-all`

#### Đăng ký Token Thiết bị đẩy (Push Token)
* **Method**: `POST`
* **Path**: `/api/v1/devices/register`
* **Request Body**:
  ```json
  {
    "token": "fcm_token_string_here...",
    "platform": "ANDROID" // IOS, ANDROID, WEB
  }
  ```

#### Hủy đăng ký Token Thiết bị
* **Method**: `POST`
* **Path**: `/api/v1/devices/unregister`
* **Request Body**:
  ```json
  {
    "token": "fcm_token_string_here..."
  }
  ```

---

## 3. Phân hệ Quản lý Tài chính & Rút tiền Mentor (Mentor Finance)

### 3.1 Quy tắc Nghiệp vụ, Khóa Bi quan & Bảo mật dữ liệu
* **Mô hình Quản lý Số dư khả dụng (Reserve Semantics)**: Số tiền hiển thị của Mentor được quản lý chặt chẽ theo dòng đời yêu cầu rút tiền:
  $$\text{Số dư khả dụng} = \text{Tổng thu nhập (Completed Bookings)} - \text{Đã rút (PAID)} - \text{Đang xử lý (PENDING/APPROVED/PROCESSING)}$$
* **Khóa Bi quan (Pessimistic Concurrency Safety)**: Khi tạo một yêu cầu rút tiền mới, hệ thống kích hoạt khóa bi quan (`@Lock(LockModeType.PESSIMISTIC_WRITE)`) trên bản ghi `MentorProfile`. Việc này đảm bảo tại một thời điểm chỉ có duy nhất một giao dịch rút tiền của Mentor được xử lý, loại bỏ hoàn toàn khả năng rút tiền vượt quá hạn mức do gửi nhiều request đồng thời (Double-spending/Overdraft).
* **Mã hóa Thông tin tài khoản Ngân hàng (Application-level Encryption)**: Số tài khoản ngân hàng của Mentor được tự động mã hóa bằng thuật toán **AES/GCM/NoPadding** ở lớp ứng dụng trước khi lưu vào DB thông qua `EncryptedStringConverter` trong JPA. Trường hợp DB bị rò rỉ, thông tin nhạy cảm của người dùng vẫn được bảo vệ tuyệt đối.
* **Mặt nạ hiển thị (Data Masking)**: API trả về thông tin cho phía Mentor sẽ tự động che đi số tài khoản (ví dụ: `*******1234`), chỉ Admin mới có quyền xem thông tin đầy đủ để thực hiện giao dịch chuyển tiền.
* **Idempotency máy trạng thái rút tiền**:
  * `PENDING` -> `APPROVED` / `REJECTED`.
  * `APPROVED` -> `PROCESSING` -> `PAID` / `FAILED`.
  * Trạng thái đã chuyển sang `PAID` hoặc `FAILED`/`REJECTED` thì không thể thay đổi lại để tránh duplicate transfer.

### 3.2 Danh sách API Endpoints dành cho Mentor

#### Xem tóm tắt doanh thu và số dư khả dụng
* **Method**: `GET`
* **Path**: `/api/v1/mentors/me/revenue-summary`
* **Response**:
  ```json
  {
    "code": 200,
    "data": {
      "totalEarned": 10000000.00,
      "totalWithdrawn": 4000000.00,
      "lockedBalance": 1000000.00,
      "availableBalance": 5000000.00
    }
  }
  ```

#### Yêu cầu rút tiền mới (Tối thiểu 50,000 VND)
* **Method**: `POST`
* **Path**: `/api/v1/mentors/me/payouts`
* **Request Body**:
  ```json
  {
    "amount": 1000000.00,
    "bankName": "Vietcombank",
    "accountNumber": "1902830192830",
    "accountHolder": "NGUYEN VAN A"
  }
  ```
* **Response**:
  ```json
  {
    "code": 200,
    "message": "Gửi yêu cầu rút tiền thành công",
    "data": {
      "id": "e2f3a4b5-c6d7-48e9-b0f1-2a3b4c5d6e7f",
      "mentorId": "77665544-3322-1100-f1e2-d3c4b5a69788",
      "grossAmount": 1000000.00,
      "platformFeeRate": 10.00,
      "netAmount": 900000.00,
      "status": "PENDING",
      "bankName": "Vietcombank",
      "accountNumber": "*******9283", // Đã được che
      "accountHolder": "NGUYEN VAN A",
      "createdAt": "2026-05-23T16:45:00Z"
    }
  }
  ```

#### Lấy lịch sử yêu cầu rút tiền của Mentor (Phân trang)
* **Method**: `GET`
* **Path**: `/api/v1/mentors/me/payouts?page=0&size=10`

---

### 3.3 Danh sách API Endpoints dành cho Admin
*(Yêu cầu Header Authorization Bearer JWT có vai trò `ADMIN`)*

#### Lấy toàn bộ danh sách yêu cầu rút tiền (Lọc theo trạng thái)
* **Method**: `GET`
* **Path**: `/api/v1/admin/payouts?status=PENDING&page=0&size=20`
* **Lưu ý**: Dữ liệu tài khoản ngân hàng trả về từ endpoint này ở dạng **giải mã hoàn toàn** để Admin nhìn thấy thông tin chuyển tiền thực tế.

#### Duyệt chấp thuận yêu cầu rút tiền
* **Method**: `POST`
* **Path**: `/api/v1/admin/payouts/{id}/approve`

#### Từ chối yêu cầu rút tiền
* **Method**: `POST`
* **Path**: `/api/v1/admin/payouts/{id}/reject`
* **Request Body**:
  ```json
  {
    "rejectReason": "Tên chủ tài khoản không trùng khớp với hồ sơ đăng ký."
  }
  ```

#### Đánh dấu đã chuyển tiền thành công
* **Method**: `POST`
* **Path**: `/api/v1/admin/payouts/{id}/pay`
* **Request Body**:
  ```json
  {
    "transactionReference": "FT26143928139210" // Mã tham chiếu giao dịch ngân hàng
  }
  ```

#### Đánh dấu chuyển tiền thất bại (Hoàn lại số dư khả dụng cho Mentor)
* **Method**: `POST`
* **Path**: `/api/v1/admin/payouts/{id}/fail`
* **Request Body**:
  ```json
  {
    "failureReason": "Tài khoản ngân hàng của người nhận đã bị khóa hoặc sai thông tin."
  }
  ```

---

## 4. Kiểm thử Concurrency & Độ tin cậy (Testing & Reliability)
Hệ thống đi kèm bộ kiểm thử tự động kiểm soát lỗi đồng thời tại `src/test/java`:
1. **PayoutServiceImplTest**: Xác minh luồng cộng dồn/trừ số dư, đóng băng giao dịch và ngăn chặn rút quá hạn mức.
2. **ReviewServiceImplTest**: Xác minh điều kiện chặn spam đánh giá trùng lặp trên cùng một Booking, tính toán phân bổ sao và trung bình điểm số.
