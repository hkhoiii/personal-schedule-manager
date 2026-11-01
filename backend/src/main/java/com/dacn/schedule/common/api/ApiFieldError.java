package com.dacn.schedule.common.api;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Một lỗi gắn với một trường cụ thể của yêu cầu (dùng trong {@link ApiError#details()}).
 * Giao diện dùng {@code field} để tô đỏ đúng ô nhập.
 */
@Schema(name = "ApiFieldError", description = "Lỗi của một trường dữ liệu")
public record ApiFieldError(
        @Schema(description = "Tên trường (hoặc tên tham số) bị lỗi", example = "title")
        String field,
        @Schema(description = "Mô tả lỗi để hiển thị cho người dùng", example = "Không được để trống")
        String message) {
}
