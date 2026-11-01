/**
 * Module ai: Trợ lý AI (Gemini): tách công việc từ câu nói tự nhiên.
 *
 * <p>Endpoint (tiền tố {@code /api/v1}):
 * <ul>
 *   <li>{@code POST /ai/parse-tasks}</li>
 * </ul>
 *
 * <p>Bảng dữ liệu:
 * <ul>
 *   <li>nhatkyai</li>
 * </ul>
 *
 * <p>Cần lưu ý khi cài đặt:
 * <ul>
 *   <li>Subpackage client.gemini gọi Gemini API, khóa đọc từ biến môi trường GEMINI_API_KEY</li>
 *   <li>AI chỉ đề xuất, không tự ghi vào lịch</li>
 *   <li>Lỗi: AI_PARSE_FAILED, AI_UNAVAILABLE, RATE_LIMITED</li>
 * </ul>
 *
 * <p>Cấu trúc bên trong (tạo thư mục con khi cần): {@code controller}, {@code service},
 * {@code repository}, {@code entity}, {@code dto}. Module không truy cập trực tiếp repository của module khác;
 * gọi qua service của module đó.
 */
package com.dacn.schedule.ai;
