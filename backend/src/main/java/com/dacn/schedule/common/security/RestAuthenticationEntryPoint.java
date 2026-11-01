package com.dacn.schedule.common.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerExceptionResolver;

/**
 * Được gọi khi người dùng CHƯA đăng nhập (thiếu / sai token) mà truy cập endpoint cần xác thực.
 *
 * <p>Thay vì tự viết JSON, chuyển ngoại lệ cho {@code GlobalExceptionHandler} xử lý để lỗi bảo mật
 * có cùng định dạng với mọi lỗi khác (mã {@code UNAUTHENTICATED}, HTTP 401).
 *
 * <p>Khi viết bộ lọc JWT (tuần sau), muốn trả {@code TOKEN_EXPIRED} thì gọi
 * {@code resolver.resolveException(request, response, null, new ApiException(ErrorCode.TOKEN_EXPIRED))}.
 */
@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final HandlerExceptionResolver resolver;

    public RestAuthenticationEntryPoint(@Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver) {
        this.resolver = resolver;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) {
        resolver.resolveException(request, response, null, authException);
    }
}
