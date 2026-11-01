/**
 * Module user: Người dùng và thiết lập cá nhân.
 *
 * <p>Endpoint (tiền tố {@code /api/v1}):
 * <ul>
 *   <li>{@code GET /users/me}</li>
 *   <li>{@code PUT /users/me}</li>
 *   <li>{@code PUT /users/me/password}</li>
 *   <li>{@code GET /preferences}</li>
 *   <li>{@code PUT /preferences}</li>
 * </ul>
 *
 * <p>Bảng dữ liệu:
 * <ul>
 *   <li>nguoidung</li>
 *   <li>caidatnguoidung</li>
 *   <li>khonggianlamviec</li>
 * </ul>
 *
 * <p>Cần lưu ý khi cài đặt:
 * <ul>
 *   <li>Entity NguoiDung dùng chung cho module auth</li>
 *   <li>Lỗi: WRONG_PASSWORD khi đổi mật khẩu sai</li>
 * </ul>
 *
 * <p>Cấu trúc bên trong (tạo thư mục con khi cần): {@code controller}, {@code service},
 * {@code repository}, {@code entity}, {@code dto}. Module không truy cập trực tiếp repository của module khác;
 * gọi qua service của module đó.
 */
package com.dacn.schedule.user;
