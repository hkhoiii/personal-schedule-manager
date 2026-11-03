/**
 * Module ai: Trợ lý AI (Gemini): tách công việc từ câu nói tự nhiên, phân rã mục tiêu lớn,
 * nhận xét thói quen làm việc.
 *
 * <p>Endpoint (tiền tố {@code /api/v1}):
 * <ul>
 *   <li>{@code POST /ai/parse-tasks}</li>
 *   <li>{@code POST /ai/breakdown-task}</li>
 *   <li>{@code GET /ai/insights}</li>
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
 *   <li>breakdown-task chỉ trả danh sách công việc con đề xuất; người dùng xác nhận rồi mới lưu
 *       qua POST /tasks/{id}/subtasks</li>
 *   <li>insights: lấy số liệu bằng service của module analytics (không đọc thẳng bảng nhatkynangsuat),
 *       gửi Gemini, lưu nhận xét vào cột NhanXetAI của ngày hôm nay (cũng qua analytics) và dùng lại trong ngày,
 *       chỉ gọi Gemini lần đầu trong ngày</li>
 *   <li>Lỗi: AI_PARSE_FAILED, AI_UNAVAILABLE, RATE_LIMITED</li>
 * </ul>
 *
 * <p>Cấu trúc bên trong (tạo thư mục con khi cần): {@code controller}, {@code service},
 * {@code repository}, {@code entity}, {@code dto}. Module không truy cập trực tiếp repository của module khác;
 * gọi qua service của module đó.
 */
package com.dacn.schedule.ai;
