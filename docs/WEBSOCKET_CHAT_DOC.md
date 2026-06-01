# Tài liệu Tích hợp WebSocket Realtime Chat (Spring Boot + STOMP)

Tài liệu này hướng dẫn cách kết nối, xác thực và tích hợp luồng tin nhắn realtime trên mobile/web client với hệ thống backend của **Sociedu**.

---

## 1. Kiến trúc luồng tin nhắn (REST Send + WS Receive)

Để đảm bảo hiệu năng, tính ổn định và khả năng tích hợp linh hoạt (retry, file upload, offline queue), hệ thống áp dụng kiến trúc phối hợp:
- **Write Path (Gửi tin nhắn)**: Client sử dụng HTTP REST API standard để gửi tin nhắn.
- **Read Path (Nhận tin nhắn realtime)**: Client duy trì kết nối WebSocket dài hạn và đăng ký lắng nghe (subscribe) cuộc hội thoại để nhận tin nhắn của người khác phát sóng (broadcast) tức thời.

```mermaid
sequenceDiagram
    participant Client A
    participant Backend (REST/JPA)
    participant Simple Message Broker
    participant Client B

    Note over Client B: Đã kết nối WS & subscribe topic phòng chat
    Client A->>Backend (REST/JPA): POST /api/v1/conversations/{id}/messages
    Note over Backend (REST/JPA): Lưu tin nhắn vào DB thành công
    Backend (REST/JPA)->>Simple Message Broker: Phát tin nhắn (wrapped in ChatEventEnvelope)
    Simple Message Broker->>Client B: Đẩy tin nhắn realtime qua WebSocket
    Backend (REST/JPA)-->>Client A: Phản hồi REST 200 OK (Message DTO)
```

---

## 2. Thông tin Kết nối & Endpoint

### Endpoint WebSocket
- **URL**: `{API_BASE}/api/v1/ws` — cùng host/port với REST (`wss://` trên production)
- **Hỗ trợ kết nối**:
  1. Giao thức WebSocket thuần (Raw WebSocket).
  2. Thư viện SockJS Fallback (cho các trình duyệt hoặc môi trường mạng chặn raw socket).

---

## 3. Cơ chế Xác thực & Phân quyền (STOMP Layer Security)

Do quá trình handshake WebSocket của HTTP không carry được auth header một cách chuẩn hóa trên một số nền tảng di động, hệ thống hỗ trợ trích xuất JWT Token theo thứ tự ưu tiên giảm dần:

1. **Header STOMP `Authorization`**: Định dạng `Bearer <JWT_TOKEN>` (Khuyên dùng).
2. **Header STOMP `token`**: Định dạng `<JWT_TOKEN>` (Khuyên dùng).
3. **Query Parameter `?token=<JWT_TOKEN>`**: Dự phòng cho các client di động/web không gán được header khi handshake.

### A. Xác thực Kết nối (CONNECT)
Khi client gửi frame `CONNECT`, backend sẽ trích xuất token theo thứ tự trên. Nếu token hợp lệ, kết nối được chấp nhận và thiết lập Security Principal cho session đó. Nếu token thiếu hoặc không hợp lệ, kết nối sẽ bị **ngắt ngay lập tức** (Access Denied).

*Ví dụ Headers truyền khi kết nối:*
```json
{
  "Authorization": "Bearer eyJhbGciOiJIUzI1NiIsInR..."
}
```

### B. Phân quyền Đăng ký (SUBSCRIBE)
Client đăng ký nhận tin nhắn của cuộc hội thoại bằng cách subscribe vào topic:
- **Destination**: `/topic/conversations/{conversationId}` (Với `{conversationId}` là UUID của cuộc hội thoại).

> [!IMPORTANT]
> Khi nhận được lệnh `SUBSCRIBE`, backend kiểm tra quyền sở hữu (`participant`) của user tương ứng với `conversationId` đó.
> - Nếu **Hợp lệ**: Chấp nhận subscription.
> - Nếu **Không hợp lệ (hoặc không phải thành viên)**: Trả về lỗi `AccessDeniedException` và tự động ngắt kết nối WebSocket của client này để bảo mật dữ liệu.

### C. Phân quyền Gửi tin (SEND)
Theo thiết kế hệ thống, client gửi tin nhắn qua REST API. Nếu client cố tình bypass để gửi tin trực tiếp qua WebSocket bằng frame `SEND` tới `/app/...` hoặc `/topic/...`, backend sẽ kiểm tra tư cách thành viên phòng chat tương ứng. Nếu vi phạm, backend sẽ block và ngắt session.

---

## 4. Định dạng Dữ liệu (Payload Envelope)

Tất cả các sự kiện gửi qua topic WebSocket không phải là tin nhắn thô, mà được bọc (envelope) trong cấu trúc chuẩn để dễ dàng mở rộng các tính năng tương lai (như typing indicator, read receipts, v.v.).

### Cấu trúc `ChatEventEnvelope`
```json
{
  "eventType": "NEW_MESSAGE",
  "conversationId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "serverTimestamp": "2026-05-25T14:30:00Z",
  "payload": {
    "id": "782ca8b4-82a1-4322-83bc-42b781198ae4",
    "senderId": "1a2b3c4d-5e6f-7a8b-9c0d-1e2f3a4b5c6d",
    "content": "Xin chào, tôi cần tư vấn về khóa học này.",
    "type": "text",
    "edited": false,
    "createdAt": "2026-05-25T14:29:59Z",
    "attachmentFileIds": null
  }
}
```

| Trường | Kiểu dữ liệu | Mô tả |
| :--- | :--- | :--- |
| `eventType` | String | Loại sự kiện (Hiện tại mặc định là `NEW_MESSAGE`) |
| `conversationId` | String (UUID) | ID của cuộc hội thoại |
| `serverTimestamp` | String (ISO-8601) | Thời gian hệ thống phát đi sự kiện tại backend |
| `payload` | Object | Payload chi tiết của sự kiện (Message DTO) |

---

## 5. Hướng dẫn Tích hợp dành cho Client (Mobile & Web)

Để đảm bảo kết nối mượt mà và không bị gián đoạn, Client cần tuân thủ các khuyến nghị thiết kế sau:

### A. Quản lý trạng thái kết nối
Client nên duy trì một máy trạng thái (state machine) cho kết nối WebSocket:
`DISCONNECTED` ➔ `CONNECTING` ➔ `CONNECTED` ➔ `RECONNECTING`.

### B. Cơ chế kết nối lại (Reconnection) & Backoff
- Khi bị ngắt kết nối đột ngột (do mất mạng, đổi IP, wifi yếu), client không nên spam reconnect liên tục mà cần thực hiện **Exponential Backoff** (kết nối lại sau 1s, 2s, 5s, 10s, tối đa 30s) để tránh làm nghẽn máy chủ.

### C. Đồng bộ hóa Offline (Offline Sync)
- Khi client chuyển từ mất mạng sang có mạng trở lại (`RECONNECTED`), client **không được** chỉ chờ tin nhắn tiếp theo từ socket.
- Thay vào đó, client cần gọi lại REST API `GET /api/v1/conversations/{conversationId}/messages?size=20` để lấy danh sách tin nhắn mới nhất nhằm đồng bộ hóa (offline sync) các tin nhắn bị bỏ lỡ trong thời gian ngắt kết nối.

### D. Chống trùng lặp tin nhắn (Message Deduplication)
- Khi Client A gửi một tin nhắn thành công qua REST API, Client A sẽ nhận được phản hồi chứa Message DTO đồng thời cũng sẽ nhận được chính tin nhắn đó được broadcast lại qua kênh WebSocket.
- Client cần sử dụng thuộc tính `id` (UUID) của tin nhắn để chống hiển thị trùng lặp tin nhắn trên giao diện:
  - Nếu `id` tin nhắn nhận được qua WebSocket đã tồn tại trong local state, client chỉ cập nhật trạng thái (nếu cần) chứ không chèn thêm dòng mới vào danh sách chat.

### E. Xử lý khi Token hết hạn (JWT Expiration)
- Heartbeat của STOMP được cấu hình định kỳ 10 giây/lần.
- Nếu JWT token hết hạn, backend sẽ từ chối kết nối hoặc ngắt kết nối hiện tại. Khi phát hiện token hết hạn, client cần:
  1. Thực hiện gọi API Refresh Token để lấy Access Token mới.
  2. Sử dụng Access Token mới cấu hình lại connect headers và khởi động kết nối lại WebSocket.

---

## 6. Scale production (multi-instance)

### Broker modes

| `APP_WS_BROKER_TYPE` | Mô tả | Khi nào dùng |
|---|---|---|
| `simple` (mặc định) | In-memory broker trong 1 JVM | Dev, staging, 1 pod |
| `relay` | RabbitMQ STOMP relay | Nhiều instance API phía sau load balancer |

### Biến môi trường (API)

```env
APP_WS_HEARTBEAT_MS=10000
APP_WS_BROKER_TYPE=simple          # relay khi scale ngang
APP_WS_RELAY_HOST=localhost
APP_WS_RELAY_PORT=61613
APP_WS_RELAY_LOGIN=guest
APP_WS_RELAY_PASSCODE=guest
```

Endpoint STOMP cố định **`/api/v1/ws`** trên cùng host/port với API — không cần biến URL WS riêng.

Khi bật `relay`, cần RabbitMQ có plugin STOMP (port 61613). Mọi instance API publish/subscribe qua cùng relay nên client kết nối tới bất kỳ pod nào vẫn nhận được broadcast.

### Topic naming (đồng bộ client)

Định nghĩa tập trung tại `RealtimeTopics.java` (API) và `src/lib/realtime/topics.ts` (web):

- Chat: `/topic/conversations/{conversationId}`
- Notification user: `/user/queue/notifications`

### Web client (sociedu-web)

- **Một** kết nối SockJS/STOMP toàn app qua `StompProvider`
- Subscription notification global: `GlobalRealtimeSubscriptions` → `realtimeEventBus`
- Chat subscribe động theo conversation qua cùng provider (không mở thêm socket)
- URL: `{NEXT_PUBLIC_API_BASE_URL}/api/v1/ws`

### Lưu ý triển khai

- Load balancer: bật sticky session **hoặc** dùng `relay` broker (khuyến nghị khi >1 replica)
- Heartbeat client/server: 10s (khớp `APP_WS_HEARTBEAT_MS`)
- Mobile (`sociedu-mobile`) vẫn dùng STOMP riêng — topic naming giữ nguyên như trên
