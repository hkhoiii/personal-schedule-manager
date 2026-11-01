/**
 * Module notification: Nhắc nhở và thông báo trong ứng dụng.
 *
 * <p>Endpoint (tiền tố {@code /api/v1}):
 * <ul>
 *   <li>{@code GET /notifications}</li>
 *   <li>{@code PATCH /notifications/{id}/read}</li>
 *   <li>{@code PATCH /notifications/read-all}</li>
 *   <li>{@code WebSocket /ws/notifications}</li>
 * </ul>
 *
 * <p>Bảng dữ liệu:
 * <ul>
 *   <li>nhacnho</li>
 *   <li>thongbao</li>
 * </ul>
 *
 * <p>Cần lưu ý khi cài đặt:
 * <ul>
 *   <li>Bộ lập lịch gửi nhắc trước N phút, tôn trọng khung không làm phiền</li>
 * </ul>
 *
 * <p>Cấu trúc bên trong (tạo thư mục con khi cần): {@code controller}, {@code service},
 * {@code repository}, {@code entity}, {@code dto}. Module không truy cập trực tiếp repository của module khác;
 * gọi qua service của module đó.
 */
package com.dacn.schedule.notification;
