package com.unishare.api.modules.admin.service.impl;

import com.unishare.api.common.constants.BookingStatuses;
import com.unishare.api.common.constants.OrderStatuses;
import com.unishare.api.common.dto.AppException;
import com.unishare.api.common.dto.PageResponse;
import com.unishare.api.modules.admin.dto.AdminBookingResponse;
import com.unishare.api.modules.admin.service.AdminBookingService;
import com.unishare.api.modules.booking.entity.Booking;
import com.unishare.api.modules.booking.entity.BookingSession;
import com.unishare.api.modules.booking.exception.BookingErrorCode;
import com.unishare.api.modules.booking.repository.BookingRepository;
import com.unishare.api.modules.booking.repository.BookingSessionRepository;
import com.unishare.api.modules.order.entity.Order;
import com.unishare.api.modules.order.repository.OrderRepository;
import com.unishare.api.modules.service.entity.ServicePackage;
import com.unishare.api.modules.service.entity.ServicePackageVersion;
import com.unishare.api.modules.service.repository.ServicePackageRepository;
import com.unishare.api.modules.service.repository.ServicePackageVersionRepository;
import com.unishare.api.modules.user.entity.UserProfile;
import com.unishare.api.modules.user.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminBookingServiceImpl implements AdminBookingService {

    private final BookingRepository bookingRepository;
    private final OrderRepository orderRepository;
    private final BookingSessionRepository sessionRepository;
    private final UserProfileRepository userProfileRepository;
    private final ServicePackageRepository packageRepository;
    private final ServicePackageVersionRepository versionRepository;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AdminBookingResponse> list(String status, String q, Pageable pageable) {
        Specification<Booking> spec = (root, query, cb) -> cb.conjunction();
        String feStatus = normalize(status);
        if (feStatus != null) {
            spec = spec.and(bookingStatusSpec(feStatus));
        }
        Page<Booking> page = bookingRepository.findAll(spec, pageable);
        List<AdminBookingResponse> rows = mapBookings(page.getContent());
        if (q != null && !q.isBlank()) {
            String needle = q.trim().toLowerCase();
            rows = rows.stream().filter(r -> matchesQ(r, needle)).toList();
        }
        return PageResponse.<AdminBookingResponse>builder()
                .items(rows)
                .page(page.getNumber())
                .size(page.getSize())
                .total(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public AdminBookingResponse getById(UUID id) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new AppException(BookingErrorCode.BOOKING_NOT_FOUND));
        return mapBookings(List.of(booking)).stream()
                .findFirst()
                .orElseThrow();
    }

    private List<AdminBookingResponse> mapBookings(List<Booking> bookings) {
        if (bookings.isEmpty()) {
            return List.of();
        }
        Set<UUID> orderIds = bookings.stream().map(Booking::getOrderId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<UUID, Order> orders = orderRepository.findAllById(orderIds).stream()
                .collect(Collectors.toMap(Order::getId, Function.identity()));
        Set<UUID> userIds = new HashSet<>();
        bookings.forEach(b -> {
            userIds.add(b.getBuyerId());
            userIds.add(b.getMentorId());
        });
        Map<UUID, UserProfile> profiles = userProfileRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(UserProfile::getUserId, Function.identity()));
        Set<UUID> packageIds = bookings.stream().map(Booking::getPackageId).collect(Collectors.toSet());
        Map<UUID, ServicePackage> packages = packageRepository.findAllById(packageIds).stream()
                .collect(Collectors.toMap(ServicePackage::getId, Function.identity()));
        Set<UUID> versionIds = orders.values().stream().map(Order::getServiceId).collect(Collectors.toSet());
        Map<UUID, ServicePackageVersion> versions = versionRepository.findAllById(versionIds).stream()
                .collect(Collectors.toMap(ServicePackageVersion::getId, Function.identity()));

        List<AdminBookingResponse> result = new ArrayList<>();
        for (Booking b : bookings) {
            Order order = b.getOrderId() != null ? orders.get(b.getOrderId()) : null;
            List<BookingSession> sessions = sessionRepository.findByBookingIdOrderByScheduledAtAsc(b.getId());
            Instant scheduledAt = sessions.isEmpty()
                    ? (b.getCreatedAt() != null ? b.getCreatedAt() : Instant.now())
                    : sessions.get(0).getScheduledAt();
            int durationMin = 60;
            if (order != null) {
                ServicePackageVersion v = versions.get(order.getServiceId());
                if (v != null && v.getDuration() != null) {
                    durationMin = v.getDuration() * 60;
                }
            }
            ServicePackage pkg = packages.get(b.getPackageId());
            UserProfile buyer = profiles.get(b.getBuyerId());
            UserProfile mentor = profiles.get(b.getMentorId());
            BigDecimal amount = order != null ? order.getTotalAmount() : BigDecimal.ZERO;
            result.add(AdminBookingResponse.builder()
                    .id(b.getId())
                    .code("BK-" + b.getId().toString().substring(0, 8).toUpperCase())
                    .learnerId(b.getBuyerId())
                    .learnerName(buyer != null ? buyer.getDisplayName() : "Học viên")
                    .mentorId(b.getMentorId())
                    .mentorName(mentor != null ? mentor.getDisplayName() : "Mentor")
                    .scheduledAt(scheduledAt)
                    .durationMin(durationMin)
                    .status(mapStatus(b, order))
                    .packageTitle(pkg != null ? pkg.getName() : "Gói dịch vụ")
                    .amountVnd(amount)
                    .createdAt(b.getCreatedAt())
                    .build());
        }
        return result;
    }

    private boolean matchesQ(AdminBookingResponse r, String needle) {
        return r.getCode().toLowerCase().contains(needle)
                || r.getLearnerName().toLowerCase().contains(needle)
                || r.getMentorName().toLowerCase().contains(needle)
                || r.getPackageTitle().toLowerCase().contains(needle);
    }

    private String mapStatus(Booking b, Order order) {
        String st = b.getStatus() != null ? b.getStatus() : BookingStatuses.PENDING;
        if (BookingStatuses.PENDING.equals(st)
                && order != null
                && OrderStatuses.PENDING_PAYMENT.equalsIgnoreCase(order.getStatus())) {
            return "pending_payment";
        }
        return switch (st) {
            case BookingStatuses.SCHEDULED -> "confirmed";
            case BookingStatuses.IN_PROGRESS -> "in_progress";
            case BookingStatuses.COMPLETED -> "completed";
            case BookingStatuses.CANCELED -> "cancelled_by_user";
            case BookingStatuses.REFUNDED -> "cancelled_by_user";
            default -> "pending_payment";
        };
    }

    private Specification<Booking> bookingStatusSpec(String feStatus) {
        return (root, query, cb) -> {
            String be = switch (feStatus) {
                case "pending_payment" -> BookingStatuses.PENDING;
                case "confirmed" -> BookingStatuses.SCHEDULED;
                case "in_progress" -> BookingStatuses.IN_PROGRESS;
                case "completed" -> BookingStatuses.COMPLETED;
                case "cancelled_by_user", "cancelled_by_mentor", "no_show" -> BookingStatuses.CANCELED;
                default -> null;
            };
            return be == null ? cb.conjunction() : cb.equal(root.get("status"), be);
        };
    }

    private String normalize(String raw) {
        return raw == null || raw.isBlank() || "all".equalsIgnoreCase(raw) ? null : raw.trim().toLowerCase();
    }
}
