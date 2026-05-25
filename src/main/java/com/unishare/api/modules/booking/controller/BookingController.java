package com.unishare.api.modules.booking.controller;

import com.unishare.api.common.dto.ApiResponse;
import com.unishare.api.config.OpenApiConfig;
import com.unishare.api.infrastructure.security.CustomUserPrincipal;
import com.unishare.api.modules.booking.dto.*;
import com.unishare.api.modules.booking.service.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
@SecurityRequirement(name = OpenApiConfig.BEARER_JWT)
@Tag(name = "Bookings")
public class BookingController {

        private final BookingService bookingService;

        @Operation(summary = "Booking của tôi (mentee/buyer)")
        @PreAuthorize("hasAuthority(T(com.unishare.api.common.constants.Capabilities).VIEW_BOOKING)")
        @GetMapping("/me/buyer")
        public ResponseEntity<ApiResponse<List<BookingResponse>>> myBookingsAsBuyer(
                        @AuthenticationPrincipal CustomUserPrincipal principal) {
                return ResponseEntity.ok(ApiResponse.<List<BookingResponse>>build()
                                .withData(bookingService.listForBuyer(principal.getUserId())));
        }

        @Operation(summary = "Booking của tôi (mentor)")
        @PreAuthorize("hasAuthority(T(com.unishare.api.common.constants.Capabilities).VIEW_OWN_BOOKINGS)")
        @GetMapping("/me/mentor")
        public ResponseEntity<ApiResponse<List<BookingResponse>>> myBookingsAsMentor(
                        @AuthenticationPrincipal CustomUserPrincipal principal) {
                return ResponseEntity.ok(ApiResponse.<List<BookingResponse>>build()
                                .withData(bookingService.listForMentor(principal.getUserId())));
        }

        @Operation(summary = "Chi tiết booking")
        @PreAuthorize("hasAuthority(T(com.unishare.api.common.constants.Capabilities).VIEW_BOOKING)")
        @GetMapping("/{id}")
        public ResponseEntity<ApiResponse<BookingResponse>> get(
                        @AuthenticationPrincipal CustomUserPrincipal principal,
                        @PathVariable("id") UUID id) {
                return ResponseEntity.ok(ApiResponse.<BookingResponse>build()
                                .withData(bookingService.getById(id, principal.getUserId())));
        }

        @Operation(summary = "Cập nhật phiên (session)")
        @PreAuthorize("hasAuthority(T(com.unishare.api.common.constants.Capabilities).MANAGE_SESSIONS)")
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

        @Operation(summary = "Thêm minh chứng buổi học")
        @PreAuthorize("hasAuthority(T(com.unishare.api.common.constants.Capabilities).MANAGE_SESSIONS)")
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
        @PreAuthorize("hasAuthority(T(com.unishare.api.common.constants.Capabilities).VIEW_BOOKING)")
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
        @PreAuthorize("hasAuthority(T(com.unishare.api.common.constants.Capabilities).MANAGE_SESSIONS)")
        @PostMapping("/{bookingId}/sessions/{sessionId}/complete")
        public ResponseEntity<ApiResponse<BookingSessionResponse>> completeSession(
                        @AuthenticationPrincipal CustomUserPrincipal principal,
                        @PathVariable("bookingId") UUID bookingId,
                        @PathVariable("sessionId") UUID sessionId) {
                return ResponseEntity.ok(ApiResponse.<BookingSessionResponse>build()
                                .withData(bookingService.completeSession(bookingId, sessionId, principal.getUserId()))
                                .withMessage("Hoan thanh buoi hoc thanh cong"));
        }
}
