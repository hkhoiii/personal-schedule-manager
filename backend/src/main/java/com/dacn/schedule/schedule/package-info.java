/**
 * Module schedule: Lịch (khối thời gian) và thuật toán xếp lịch tự động.
 *
 * <p>Endpoint (tiền tố {@code /api/v1}):
 * <ul>
 *   <li>{@code GET /schedule?from=&to=}</li>
 *   <li>{@code GET /schedule/summary}</li>
 *   <li>{@code PATCH /schedule/blocks/{id}}</li>
 *   <li>{@code POST /schedule/generate}</li>
 *   <li>{@code POST /schedule/apply}</li>
 *   <li>{@code POST /schedule/reschedule}</li>
 * </ul>
 *
 * <p>Bảng dữ liệu:
 * <ul>
 *   <li>khoithoigian</li>
 * </ul>
 *
 * <p>Cần lưu ý khi cài đặt:
 * <ul>
 *   <li>Subpackage algorithm: Priority Score, tìm khung giờ trống, khóa khối cố định</li>
 *   <li>generate chỉ TẠO ĐỀ XUẤT, chỉ apply mới ghi vào lịch</li>
 *   <li>Lỗi: SCHEDULE_CONFLICT, NO_SLOT_AVAILABLE</li>
 * </ul>
 *
 * <p>Cấu trúc bên trong (tạo thư mục con khi cần): {@code controller}, {@code service},
 * {@code repository}, {@code entity}, {@code dto}. Module không truy cập trực tiếp repository của module khác;
 * gọi qua service của module đó.
 */
package com.dacn.schedule.schedule;
