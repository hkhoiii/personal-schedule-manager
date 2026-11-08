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
 * có cùng định dạng với mọi lỗi khác (HTTP 401).
 *
 * <p>Nếu bộ lọc JWT đã gặp lỗi token (hết hạn, sai chữ ký...), nó lưu {@code ApiException} vào thuộc tính
 * {@link #TOKEN_ERROR_ATTRIBUTE} của request; khi đó trả đúng lỗi đó ({@code TOKEN_EXPIRED} hoặc
 * {@code UNAUTHENTICATED} "Token không hợp lệ") để client biết nên gọi /auth/refresh hay đăng nhập lại.
 * Không có token thì trả {@code UNAUTHENTICATED}.
 */
@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    /** Tên thuộc tính request chứa lỗi token do bộ lọc JWT ghi lại. */
    public static final String TOKEN_ERROR_ATTRIBUTE = RestAuthenticationEntryPoint.class.getName() + ".TOKEN_ERROR";

    private final HandlerExceptionResolver resolver;

    public RestAuthenticationEntryPoint(@Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver) {
        this.resolver = resolver;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) {
        Exception error = request.getAttribute(TOKEN_ERROR_ATTRIBUTE) instanceof Exception tokenError
                ? tokenError
                : authException;
        resolver.resolveException(request, response, null, error);
    }
}
