package com.dacn.schedule.common.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Định dạng lỗi chung của MỌI endpoint (đã chốt ở Ngày 5).
 *
 * <p>Frontend / Android dựa vào {@code code} để chọn màn hình lỗi, {@code message} để hiển thị,
 * {@code details} để tô đỏ từng ô nhập. {@code details} chỉ xuất hiện khi lỗi gắn với từng trường
 * (ví dụ {@code VALIDATION_FAILED}).
 */
@Schema(name = "ApiError", description = "Định dạng lỗi chung của mọi endpoint")
public record ApiError(
        @Schema(description = "Thời điểm xảy ra lỗi (múi giờ máy chủ)", example = "2026-10-27T01:20:00+07:00")
        OffsetDateTime timestamp,
        @Schema(description = "Mã trạng thái HTTP", example = "422")
        int status,
        @Schema(description = "Mã lỗi nghiệp vụ, ổn định để client xử lý", example = "AI_PARSE_FAILED")
        String code,
        @Schema(description = "Thông điệp tiếng Việt hiển thị cho người dùng",
                example = "Câu nói chưa đủ rõ hoặc dịch vụ AI đang bận.")
        String message,
        @Schema(description = "Đường dẫn của yêu cầu gây lỗi", example = "/api/v1/ai/parse-tasks")
        String path,
        @Schema(description = "Lỗi theo từng trường; chỉ có khi lỗi gắn với trường cụ thể")
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        List<ApiFieldError> details) {
}
