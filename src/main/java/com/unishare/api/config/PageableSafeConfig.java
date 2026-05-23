package com.unishare.api.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.data.web.SortHandlerMethodArgumentResolver;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Lọc {@link Pageable} sort property không hợp lệ trước khi controller nhận.
 *
 * <p>Khi client (vd Swagger UI mặc định) gửi {@code sort=["string"]}, Spring Data tạo {@link Sort}
 * với property {@code ["string"]} → Hibernate ném {@code InvalidDataAccessApiUsageException}
 * vì không có property nào tên như vậy. Resolver này strip property không match
 * {@code ^[a-zA-Z_][a-zA-Z0-9_.]*$} → fallback {@code Sort.unsorted()}.
 *
 * <p>Custom resolver được add vào {@code addArgumentResolvers} → Spring duyệt trước default
 * {@link PageableHandlerMethodArgumentResolver}.
 */
@Configuration
public class PageableSafeConfig implements WebMvcConfigurer {

    private static final Pattern SAFE_PROPERTY = Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_.]*$");

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        SortHandlerMethodArgumentResolver sortResolver = new SortHandlerMethodArgumentResolver();
        PageableHandlerMethodArgumentResolver delegate = new PageableHandlerMethodArgumentResolver(sortResolver);

        resolvers.add(new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return delegate.supportsParameter(parameter);
            }

            @Override
            public Object resolveArgument(MethodParameter parameter,
                                          ModelAndViewContainer mavContainer,
                                          NativeWebRequest webRequest,
                                          WebDataBinderFactory binderFactory) {
                Pageable pageable = (Pageable) delegate.resolveArgument(parameter, mavContainer, webRequest, binderFactory);
                if (pageable == null || pageable.getSort().isUnsorted()) {
                    return pageable;
                }
                List<Sort.Order> safe = pageable.getSort().stream()
                        .filter(order -> SAFE_PROPERTY.matcher(order.getProperty()).matches())
                        .toList();
                Sort safeSort = safe.isEmpty() ? Sort.unsorted() : Sort.by(safe);
                if (!pageable.isPaged()) {
                    return pageable;
                }
                return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), safeSort);
            }
        });
    }
}
