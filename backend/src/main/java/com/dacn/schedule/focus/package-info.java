/**
 * Module focus: Phiên tập trung (Pomodoro) gắn với công việc.
 *
 * <p>Endpoint (tiền tố {@code /api/v1}):
 * <ul>
 *   <li>{@code POST /tasks/{id}/start}</li>
 *   <li>{@code POST /tasks/{id}/complete}</li>
 * </ul>
 *
 * <p>Bảng dữ liệu:
 * <ul>
 *   <li>phientaptrung</li>
 * </ul>
 *
 * <p>Cần lưu ý khi cài đặt:
 * <ul>
 *   <li>Controller map vào /tasks/{id}/... nhưng nghiệp vụ nằm ở module này</li>
 *   <li>Hoàn thành phiên cập nhật số phút thực tế cho thống kê</li>
 * </ul>
 *
 * <p>Cấu trúc bên trong (tạo thư mục con khi cần): {@code controller}, {@code service},
 * {@code repository}, {@code entity}, {@code dto}. Module không truy cập trực tiếp repository của module khác;
 * gọi qua service của module đó.
 */
package com.dacn.schedule.focus;
