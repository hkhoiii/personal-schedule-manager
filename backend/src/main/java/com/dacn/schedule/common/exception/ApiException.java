package com.dacn.schedule.common.exception;

import com.dacn.schedule.common.api.ApiFieldError;
import java.util.List;

/**
 * Ngoại lệ nghiệp vụ: service ném ra khi gặp lỗi đã dự đoán trước. {@link GlobalExceptionHandler}
 * sẽ chuyển nó thành phản hồi JSON theo định dạng {@code ApiError}.
 *
 * <pre>{@code
 * throw new ApiException(ErrorCode.EMAIL_ALREADY_USED);
 * throw new ApiException(ErrorCode.DEPENDENCY_CYCLE, "Việc A và việc B phụ thuộc vòng nhau.");
 * }</pre>
 */
public class ApiException extends RuntimeException {

    private final ErrorCode errorCode;
    private final transient List<ApiFieldError> details;

    public ApiException(ErrorCode errorCode) {
        this(errorCode, null, List.of(), null);
    }

    public ApiException(ErrorCode errorCode, String message) {
        this(errorCode, message, List.of(), null);
    }

    public ApiException(ErrorCode errorCode, String message, List<ApiFieldError> details) {
        this(errorCode, message, details, null);
    }

    public ApiException(ErrorCode errorCode, String message, Throwable cause) {
        this(errorCode, message, List.of(), cause);
    }

    private ApiException(ErrorCode errorCode, String message, List<ApiFieldError> details, Throwable cause) {
        super(message != null ? message : errorCode.getDefaultMessage(), cause);
        this.errorCode = errorCode;
        this.details = details != null ? List.copyOf(details) : List.of();
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public List<ApiFieldError> getDetails() {
        return details;
    }
}
