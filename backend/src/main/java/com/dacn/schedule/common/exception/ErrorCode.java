package com.dacn.schedule.common.exception;

/**
 * Danh sách mã lỗi nghiệp vụ. Khớp với bảng mã lỗi trong tài liệu thiết kế (Ngày 5).
 *
 * <p>Mỗi mã gắn với một mã HTTP và một thông điệp tiếng Việt mặc định. Khi cần thông điệp cụ thể hơn,
 * truyền vào {@link ApiException} (ví dụ "Không tìm thấy công việc với id 5").
 *
 * <p>Hai mã {@link #METHOD_NOT_ALLOWED} và {@link #UNSUPPORTED_MEDIA_TYPE} là bổ sung so với bảng
 * ban đầu, để trả đúng định dạng lỗi cho yêu cầu sai phương thức / sai kiểu nội dung.
 */
public enum ErrorCode {

    VALIDATION_FAILED(400, "Dữ liệu gửi lên không hợp lệ."),
    WRONG_PASSWORD(400, "Mật khẩu hiện tại không đúng."),

    BAD_CREDENTIALS(401, "Email hoặc mật khẩu không đúng."),
    TOKEN_EXPIRED(401, "Phiên đăng nhập đã hết hạn, vui lòng đăng nhập lại."),
    UNAUTHENTICATED(401, "Bạn cần đăng nhập để thực hiện thao tác này."),

    FORBIDDEN(403, "Bạn không có quyền thực hiện thao tác này."),
    NOT_FOUND(404, "Không tìm thấy dữ liệu yêu cầu."),
    METHOD_NOT_ALLOWED(405, "Phương thức HTTP không được hỗ trợ cho đường dẫn này."),
    UNSUPPORTED_MEDIA_TYPE(415, "Kiểu nội dung gửi lên không được hỗ trợ."),

    EMAIL_ALREADY_USED(409, "Email này đã được đăng ký."),
    DEPENDENCY_CYCLE(409, "Các phụ thuộc giữa công việc tạo thành vòng lặp."),
    SCHEDULE_CONFLICT(409, "Khối lịch bị trùng giờ với lịch đang có hoặc lịch cố định."),
    CATEGORY_IN_USE(409, "Nhóm vẫn còn công việc nên không thể xóa."),

    AI_PARSE_FAILED(422, "Câu nói chưa đủ rõ hoặc dịch vụ AI đang bận. Lịch của bạn vẫn được giữ nguyên."),
    NO_SLOT_AVAILABLE(422, "Không tìm được khung giờ phù hợp."),

    RATE_LIMITED(429, "Bạn gọi trợ lý AI quá nhiều lần, vui lòng thử lại sau."),

    INTERNAL_ERROR(500, "Đã có lỗi xảy ra phía máy chủ. Vui lòng thử lại sau."),
    AI_UNAVAILABLE(503, "Trợ lý AI tạm thời không khả dụng. Lịch của bạn vẫn được giữ nguyên.");

    private final int status;
    private final String defaultMessage;

    ErrorCode(int status, String defaultMessage) {
        this.status = status;
        this.defaultMessage = defaultMessage;
    }

    /** Mã trạng thái HTTP tương ứng. */
    public int getStatus() {
        return status;
    }

    /** Thông điệp tiếng Việt mặc định. */
    public String getDefaultMessage() {
        return defaultMessage;
    }

    /**
     * Quy đổi mã HTTP của các lỗi do framework tự sinh (404, 405, 415...) sang mã lỗi nghiệp vụ.
     * Các mã 4xx chưa liệt kê được xem là dữ liệu gửi lên không hợp lệ.
     */
    public static ErrorCode fromHttpStatus(int httpStatus) {
        return switch (httpStatus) {
            case 401 -> UNAUTHENTICATED;
            case 403 -> FORBIDDEN;
            case 404 -> NOT_FOUND;
            case 405 -> METHOD_NOT_ALLOWED;
            case 415 -> UNSUPPORTED_MEDIA_TYPE;
            case 429 -> RATE_LIMITED;
            default -> httpStatus >= 400 && httpStatus < 500 ? VALIDATION_FAILED : INTERNAL_ERROR;
        };
    }
}
