package com.unishare.api.modules.booking.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.unishare.api.common.constants.BookingStatuses;
import com.unishare.api.common.constants.SessionStatuses;
import com.unishare.api.config.GlobalExceptionHandler;
import com.unishare.api.infrastructure.security.CustomUserPrincipal;
import com.unishare.api.modules.booking.dto.BookingResponse;
import com.unishare.api.modules.booking.dto.BookingSessionResponse;
import com.unishare.api.modules.booking.dto.UpdateSessionRequest;
import com.unishare.api.modules.booking.service.BookingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class BookingControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private BookingService bookingService;

    @InjectMocks
    private BookingController bookingController;

    private UUID userId;
    private CustomUserPrincipal principal;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        principal = new CustomUserPrincipal(
                userId,
                "testuser@gmail.com",
                "hashedpassword",
                List.of("USER"),
                List.of("VIEW_BOOKING", "MANAGE_SESSIONS"),
                true
        );

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        HandlerMethodArgumentResolver principalResolver = new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return parameter.getParameterType().equals(CustomUserPrincipal.class);
            }

            @Override
            public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                          NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
                return principal;
            }
        };

        mockMvc = MockMvcBuilders.standaloneSetup(bookingController)
                .setCustomArgumentResolvers(principalResolver, new PageableHandlerMethodArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /api/v1/bookings/me/buyer - Trả về danh sách booking của buyer")
    void myBookingsAsBuyer_ReturnsSuccess() throws Exception {
        BookingResponse response = BookingResponse.builder()
                .id(UUID.randomUUID())
                .status(BookingStatuses.PENDING)
                .build();

        com.unishare.api.common.dto.PageResponse<BookingResponse> page =
                com.unishare.api.common.dto.PageResponse.<BookingResponse>builder()
                        .items(List.of(response))
                        .page(0)
                        .size(20)
                        .total(1)
                        .totalPages(1)
                        .build();
        when(bookingService.listForBuyer(eq(userId), any())).thenReturn(page);

        mockMvc.perform(get("/api/v1/bookings/me/buyer"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].status").value(BookingStatuses.PENDING))
                .andExpect(jsonPath("$.isSuccess").value(true));
    }

    @Test
    @DisplayName("GET /api/v1/bookings/{id} - Trả về chi tiết booking")
    void getBookingById_ReturnsSuccess() throws Exception {
        UUID bookingId = UUID.randomUUID();
        BookingResponse response = BookingResponse.builder()
                .id(bookingId)
                .status(BookingStatuses.IN_PROGRESS)
                .build();

        when(bookingService.getById(bookingId, userId)).thenReturn(response);

        mockMvc.perform(get("/api/v1/bookings/{id}", bookingId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(bookingId.toString()))
                .andExpect(jsonPath("$.data.status").value(BookingStatuses.IN_PROGRESS));
    }

    @Test
    @DisplayName("PATCH /api/v1/bookings/{bookingId}/sessions/{sessionId} - Cập nhật session thành công")
    void updateSession_ReturnsSuccess() throws Exception {
        UUID bookingId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();

        UpdateSessionRequest request = new UpdateSessionRequest();
        request.setStatus(SessionStatuses.COMPLETED);

        BookingSessionResponse response = BookingSessionResponse.builder()
                .id(sessionId)
                .status(SessionStatuses.COMPLETED)
                .build();

        when(bookingService.updateSession(eq(bookingId), eq(sessionId), eq(userId), any(UpdateSessionRequest.class)))
                .thenReturn(response);

        mockMvc.perform(patch("/api/v1/bookings/{bookingId}/sessions/{sessionId}", bookingId, sessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value(SessionStatuses.COMPLETED))
                .andExpect(jsonPath("$.isSuccess").value(true));
    }
}
