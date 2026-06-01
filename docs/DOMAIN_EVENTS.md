# Domain events & notifications

## Kiến trúc

```
[Service] --publish--> DomainEventPublisher
                           |
           +---------------+---------------+
           v                               v
 ApplicationEventPublisher      IntegrationEventPublisher
 (in-process listeners)          (logging | none | kafka stub)
           |
           +-- DomainNotificationEventListener → in-app + push
           +-- DomainMailEventListener → mail queue events
           +-- OrderPaidEventListener, PaymentProcessedEventListener, ...
```

- **Business code** chỉ inject `DomainEventPublisher` — không dùng `ApplicationEventPublisher` trực tiếp.
- **Notification**: một listener `DomainNotificationEventListener` + `DomainNotificationHandler` (bảng ánh xạ nghiệp vụ).
- **Mail**: giữ `DomainMailEventListener` (OTP, verify email không tạo in-app notification).

## Cấu hình integration bus

```yaml
app:
  events:
    integration:
      type: logging   # logging | none | kafka
```

- `logging` (mặc định): debug JSON payload.
- `kafka`: stub log — thay bằng `KafkaTemplate` khi có cluster.

## Sự kiện → thông báo in-app

| Event | Người nhận |
|-------|------------|
| `OrderCheckoutCreatedEvent` | Học viên, Mentor |
| `OrderPaidEvent` | Học viên |
| `OrderPaymentFailedEvent` | Học viên |
| `BookingCreatedEvent` | Học viên, Mentor |
| `BookingCompletedEvent` | Học viên, Mentor |
| `BookingCanceledEvent` | Bên còn lại |
| `SessionCanceledEvent` | Bên còn lại |
| `MentorApplicationSubmittedEvent` | User nộp đơn + **tất cả ADMIN** |
| `MentorRequestApprovedEvent` | User |
| `MentorRequestRejectedEvent` | User |
| `ProgressReportSubmittedEvent` | Mentor |
| `ProgressReportReviewedEvent` | Học viên |

`PaymentSucceededEvent` — audit/webhook, không trùng notification với `OrderPaidEvent`.

## Realtime (Web + Mobile)

Sau khi lưu DB, `NotificationDeliveryService` gọi `StompNotificationRealtimePublisher`:

- Topic: `/topic/users/{userId}/notifications`
- Envelope: `{ eventType: "NEW_NOTIFICATION", payload: NotificationResponse }`
- Endpoint STOMP: `/ws/chat` (SockJS trên web, raw WebSocket trên mobile)
- Auth: JWT query `?token=` hoặc header `Authorization: Bearer …` lúc CONNECT

Web dashboard: `NotificationBell` + `useNotificationRealtime` (SockJS).  
Mobile: `notificationRealtimeService` (native WS) + `notificationStore.prependRealtime`.

## Thêm event mới

1. Tạo record `implements DomainEvent` trong `common.event`.
2. `eventPublisher.publish(...)` sau khi commit transaction.
3. Thêm nhánh trong `DomainNotificationHandler` nếu cần in-app/push.
