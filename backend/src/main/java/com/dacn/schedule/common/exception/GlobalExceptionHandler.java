package com.dacn.schedule.common.exception;

import com.dacn.schedule.common.api.ApiError;
import com.dacn.schedule.common.api.ApiFieldError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Bắt mọi ngoại lệ và trả về MỘT định dạng lỗi duy nhất ({@link ApiError}).
 *
 * <p>Thứ tự ưu tiên: ngoại lệ nghiệp vụ ({@link ApiException}) → lỗi kiểm tra dữ liệu →
 * lỗi bảo mật → lỗi do Spring MVC tự sinh (404, 405, 415...) → mọi lỗi còn lại (500, không lộ chi tiết).
 *
 * <p>Lỗi xảy ra trong tầng bảo mật (thiếu token, không đủ quyền) cũng đi qua lớp này nhờ
 * {@code RestAuthenticationEntryPoint} và {@code RestAccessDeniedHandler}.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final Clock clock;

    public GlobalExceptionHandler(Clock clock) {
        this.clock = clock;
    }

    // ---------------------------------------------------------------- lỗi nghiệp vụ

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiError> handleApiException(ApiException ex, HttpServletRequest request) {
        ErrorCode code = ex.getErrorCode();
        if (code.getStatus() >= 500) {
            log.error("{} tại {} {}", code, request.getMethod(), request.getRequestURI(), ex);
        } else {
            log.debug("{} tại {} {}: {}", code, request.getMethod(), request.getRequestURI(), ex.getMessage());
        }
        return respond(code.getStatus(), code, ex.getMessage(), ex.getDetails(), request);
    }

    // ---------------------------------------------------------------- kiểm tra dữ liệu

    /** {@code @Valid @RequestBody} không hợp lệ. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleBodyValidation(MethodArgumentNotValidException ex,
                                                         HttpServletRequest request) {
        List<ApiFieldError> details = new ArrayList<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            details.add(new ApiFieldError(fieldError.getField(), fieldError.getDefaultMessage()));
        }
        for (ObjectError globalError : ex.getBindingResult().getGlobalErrors()) {
            details.add(new ApiFieldError(globalError.getObjectName(), globalError.getDefaultMessage()));
        }
        return validationFailed(details, request);
    }

    /** Ràng buộc đặt trực tiếp trên tham số của phương thức controller ({@code @Min}, {@code @NotBlank}...). */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ApiError> handleMethodValidation(HandlerMethodValidationException ex,
                                                           HttpServletRequest request) {
        List<ApiFieldError> details = new ArrayList<>();
        for (ParameterValidationResult result : ex.getParameterValidationResults()) {
            String parameterName = result.getMethodParameter().getParameterName();
            for (MessageSourceResolvable error : result.getResolvableErrors()) {
                String field = error instanceof FieldError fieldError ? fieldError.getField() : parameterName;
                details.add(new ApiFieldError(field != null ? field : "parameter", error.getDefaultMessage()));
            }
        }
        return validationFailed(details, request);
    }

    /** Ràng buộc kiểm tra ở tầng service / tham số ({@code @Validated}). */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraintViolation(ConstraintViolationException ex,
                                                              HttpServletRequest request) {
        List<ApiFieldError> details = new ArrayList<>();
        for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
            String path = violation.getPropertyPath().toString();
            String field = path.substring(path.lastIndexOf('.') + 1);
            details.add(new ApiFieldError(field, violation.getMessage()));
        }
        return validationFailed(details, request);
    }

    /** Tham số URL sai kiểu, ví dụ {@code /tasks?page=abc}. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTypeMismatch(MethodArgumentTypeMismatchException ex,
                                                       HttpServletRequest request) {
        List<ApiFieldError> details = List.of(
                new ApiFieldError(ex.getName(), "Giá trị không đúng định dạng"));
        return validationFailed(details, request);
    }

    /** Thiếu tham số bắt buộc trên URL. */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiError> handleMissingParameter(MissingServletRequestParameterException ex,
                                                           HttpServletRequest request) {
        List<ApiFieldError> details = List.of(
                new ApiFieldError(ex.getParameterName(), "Thiếu tham số bắt buộc"));
        return validationFailed(details, request);
    }

    /** Thân yêu cầu không phải JSON hợp lệ, hoặc sai kiểu dữ liệu (không lộ chi tiết nội bộ). */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadableBody(HttpMessageNotReadableException ex,
                                                         HttpServletRequest request) {
        log.debug("Thân yêu cầu không đọc được tại {} {}: {}",
                request.getMethod(), request.getRequestURI(), ex.getMessage());
        return respond(400, ErrorCode.VALIDATION_FAILED,
                "Nội dung yêu cầu không phải JSON hợp lệ hoặc có trường sai kiểu dữ liệu.", List.of(), request);
    }

    // ---------------------------------------------------------------- bảo mật

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiError> handleBadCredentials(BadCredentialsException ex, HttpServletRequest request) {
        return respond(401, ErrorCode.BAD_CREDENTIALS, null, List.of(), request);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiError> handleAuthentication(AuthenticationException ex, HttpServletRequest request) {
        return respond(401, ErrorCode.UNAUTHENTICATED, null, List.of(), request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        return respond(403, ErrorCode.FORBIDDEN, null, List.of(), request);
    }

    // ---------------------------------------------------------------- còn lại

    /**
     * Lỗi do Spring MVC tự sinh (404 không có đường dẫn, 405, 415, 406...) đều là {@link ErrorResponse};
     * giữ nguyên mã HTTP và header (ví dụ {@code Allow}) nhưng đổi sang định dạng chung.
     * Mọi ngoại lệ khác là lỗi không dự đoán: ghi log đầy đủ nhưng chỉ trả thông điệp chung.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleAnyException(Exception ex, HttpServletRequest request) {
        if (ex instanceof ErrorResponse errorResponse && errorResponse.getStatusCode().is4xxClientError()) {
            int status = errorResponse.getStatusCode().value();
            ApiError body = buildError(status, ErrorCode.fromHttpStatus(status), null, List.of(), request);
            return ResponseEntity.status(status)
                    .headers(errorResponse.getHeaders())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body);
        }
        log.error("Lỗi không xác định tại {} {}", request.getMethod(), request.getRequestURI(), ex);
        return respond(500, ErrorCode.INTERNAL_ERROR, null, List.of(), request);
    }

    // ---------------------------------------------------------------- tiện ích

    private ResponseEntity<ApiError> validationFailed(List<ApiFieldError> details, HttpServletRequest request) {
        return respond(400, ErrorCode.VALIDATION_FAILED, null, details, request);
    }

    private ResponseEntity<ApiError> respond(int status, ErrorCode code, String message,
                                             List<ApiFieldError> details, HttpServletRequest request) {
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .body(buildError(status, code, message, details, request));
    }

    private ApiError buildError(int status, ErrorCode code, String message,
                                List<ApiFieldError> details, HttpServletRequest request) {
        return new ApiError(
                OffsetDateTime.now(clock),
                status,
                code.name(),
                message != null ? message : code.getDefaultMessage(),
                request.getRequestURI(),
                details);
    }
}
