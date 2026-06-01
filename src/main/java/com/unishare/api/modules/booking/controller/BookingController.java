package com.unishare.api.modules.booking.controller;

import com.unishare.api.common.dto.ApiResponse;
import com.unishare.api.common.dto.PageResponse;
import com.unishare.api.config.OpenApiConfig;
import com.unishare.api.infrastructure.security.CustomUserPrincipal;
import com.unishare.api.modules.booking.dto.*;
import com.unishare.api.modules.booking.service.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
@SecurityRequirement(name = OpenApiConfig.BEARER_JWT)
@Tag(name = "Bookings")
public class BookingController {

        private final BookingService bookingService;

        @Operation(summary = "Booking của tôi (mentee/buyer)")
        @PreAuthorize("hasAnyRole('USER', 'MENTOR', 'ADMIN')")
        @GetMapping("/me/buyer")
        public ResponseEntity<ApiResponse<PageResponse<BookingResponse>>> myBookingsAsBuyer(
                        @AuthenticationPrincipal CustomUserPrincipal principal,
                        @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
                return ResponseEntity.ok(ApiResponse.<PageResponse<BookingResponse>>build()
                                .withData(bookingService.listForBuyer(principal.getUserId(), pageable)));
        }

        @Operation(summary = "Buổi học sắp tới gần nhất (học viên)")
        @PreAuthorize("hasAnyRole('USER', 'MENTOR', 'ADMIN')")
        @GetMapping("/me/buyer/next-session")
        public ResponseEntity<ApiResponse<NextUpcomingSessionResponse>> nextSessionAsBuyer(
                        @AuthenticationPrincipal CustomUserPrincipal principal) {
                return ResponseEntity.ok(ApiResponse.<NextUpcomingSessionResponse>build()
                                .withData(bookingService.getNextUpcomingSessionForBuyer(principal.getUserId())));
        }

        @Operation(summary = "Booking của tôi (mentor)")
        @PreAuthorize("hasRole('MENTOR')")
        @GetMapping("/me/mentor")
        public ResponseEntity<ApiResponse<PageResponse<BookingResponse>>> myBookingsAsMentor(
                        @AuthenticationPrincipal CustomUserPrincipal principal,
                        @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
                return ResponseEntity.ok(ApiResponse.<PageResponse<BookingResponse>>build()
                                .withData(bookingService.listForMentor(principal.getUserId(), pageable)));
        }

        @Operation(summary = "Buổi dạy sắp tới gần nhất (mentor)")
        @PreAuthorize("hasRole('MENTOR')")
        @GetMapping("/me/mentor/next-session")
        public ResponseEntity<ApiResponse<NextUpcomingSessionResponse>> nextSessionAsMentor(
                        @AuthenticationPrincipal CustomUserPrincipal principal) {
                return ResponseEntity.ok(ApiResponse.<NextUpcomingSessionResponse>build()
                                .withData(bookingService.getNextUpcomingSessionForMentor(principal.getUserId())));
        }

        @Operation(summary = "Chi tiết booking")
        @PreAuthorize("hasAnyRole('USER', 'MENTOR', 'ADMIN')")
        @GetMapping("/{id}")
        public ResponseEntity<ApiResponse<BookingResponse>> get(
                        @AuthenticationPrincipal CustomUserPrincipal principal,
                        @PathVariable("id") UUID id) {
                return ResponseEntity.ok(ApiResponse.<BookingResponse>build()
                                .withData(bookingService.getById(id, principal.getUserId())));
        }

        @Operation(summary = "Cập nhật phiên (session)")
        @PreAuthorize("hasRole('MENTOR')")
        @PatchMapping("/{bookingId}/sessions/{sessionId}")
        public ResponseEntity<ApiResponse<BookingSessionResponse>> updateSession(
                        @AuthenticationPrincipal CustomUserPrincipal principal,
                        @PathVariable("bookingId") UUID bookingId,
                        @PathVariable("sessionId") UUID sessionId,
                        @RequestBody UpdateSessionRequest request) {
                return ResponseEntity.ok(ApiResponse.<BookingSessionResponse>build()
                                .withData(bookingService.updateSession(bookingId, sessionId, principal.getUserId(),
                                                request)));
        }

        @Operation(summary = "Tạo buổi học mới cho Booking (Mentor)")
        @PreAuthorize("hasRole('MENTOR')")
        @PostMapping("/{bookingId}/sessions")
        public ResponseEntity<ApiResponse<BookingSessionResponse>> createSession(
                        @AuthenticationPrincipal CustomUserPrincipal principal,
                        @PathVariable("bookingId") UUID bookingId,
                        @Valid @RequestBody CreateSessionRequest request) {
                return ResponseEntity.ok(ApiResponse.<BookingSessionResponse>build()
                                .withData(bookingService.createSession(bookingId, principal.getUserId(), request)));
        }

        @Operation(summary = "Thêm minh chứng buổi học")
        @PreAuthorize("hasAnyRole('USER', 'MENTOR', 'ADMIN')")
        @PostMapping("/{bookingId}/sessions/{sessionId}/evidences")
        public ResponseEntity<ApiResponse<EvidenceResponse>> addEvidence(
                        @AuthenticationPrincipal CustomUserPrincipal principal,
                        @PathVariable("bookingId") UUID bookingId,
                        @PathVariable("sessionId") UUID sessionId,
                        @Valid @RequestBody AddEvidenceRequest request) {
                return ResponseEntity.ok(ApiResponse.<EvidenceResponse>build()
                                .withData(bookingService.addEvidence(bookingId, sessionId, principal.getUserId(),
                                                request)));
        }

        @Operation(summary = "Hủy booking")
        @PreAuthorize("hasAnyRole('USER', 'MENTOR', 'ADMIN')")
        @PostMapping("/{bookingId}/cancel")
        public ResponseEntity<ApiResponse<BookingResponse>> cancelBooking(
                        @AuthenticationPrincipal CustomUserPrincipal principal,
                        @PathVariable("bookingId") UUID bookingId,
                        @Valid @RequestBody CancelBookingRequest request) {
                return ResponseEntity.ok(ApiResponse.<BookingResponse>build()
                                .withData(bookingService.cancelBooking(bookingId, principal.getUserId(), request))
                                .withMessage("Huy booking thanh cong"));
        }

        @Operation(summary = "Hoàn thành buổi học")
        @PreAuthorize("hasRole('MENTOR')")
        @PostMapping("/{bookingId}/sessions/{sessionId}/complete")
        public ResponseEntity<ApiResponse<BookingSessionResponse>> completeSession(
                        @AuthenticationPrincipal CustomUserPrincipal principal,
                        @PathVariable("bookingId") UUID bookingId,
                        @PathVariable("sessionId") UUID sessionId) {
                return ResponseEntity.ok(ApiResponse.<BookingSessionResponse>build()
                                .withData(bookingService.completeSession(bookingId, sessionId, principal.getUserId()))
                                .withMessage("Hoan thanh buoi hoc thanh cong"));
        }

        @Operation(summary = "Xác nhận hoàn thành buổi học (mentee/mentor)")
        @PreAuthorize("hasAnyRole('USER', 'MENTOR', 'ADMIN')")
        @PostMapping("/{bookingId}/sessions/{sessionId}/confirm-completion")
        public ResponseEntity<ApiResponse<BookingSessionResponse>> confirmSessionCompletion(
                        @AuthenticationPrincipal CustomUserPrincipal principal,
                        @PathVariable("bookingId") UUID bookingId,
                        @PathVariable("sessionId") UUID sessionId,
                        @Valid @RequestBody ConfirmSessionCompletionRequest request) {
                return ResponseEntity.ok(ApiResponse.<BookingSessionResponse>build()
                                .withData(bookingService.confirmSessionCompletion(
                                                bookingId, sessionId, principal.getUserId(), request))
                                .withMessage("Da ghi nhan xac nhan buoi hoc"));
        }

        @Operation(summary = "Cập nhật tiến trình gói dịch vụ (mentor)")
        @PreAuthorize("hasRole('MENTOR')")
        @PatchMapping("/{bookingId}/progress")
        public ResponseEntity<ApiResponse<BookingResponse>> updateProgress(
                        @AuthenticationPrincipal CustomUserPrincipal principal,
                        @PathVariable("bookingId") UUID bookingId,
                        @RequestBody java.util.Map<String, Integer> body) {
                int progressPercent = body.getOrDefault("progressPercent", 0);
                return ResponseEntity.ok(ApiResponse.<BookingResponse>build()
                                .withData(bookingService.updateProgress(bookingId, principal.getUserId(), progressPercent))
                                .withMessage("Cap nhat tien trinh thanh cong"));
        }
}
