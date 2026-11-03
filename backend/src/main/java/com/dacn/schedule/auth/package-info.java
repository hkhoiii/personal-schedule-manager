/**
 * Module auth: Xác thực: đăng ký, đăng nhập, làm mới và thu hồi token (JWT).
 *
 * <p>Endpoint (tiền tố {@code /api/v1}):
 * <ul>
 *   <li>{@code POST /auth/register}</li>
 *   <li>{@code POST /auth/login}</li>
 *   <li>{@code POST /auth/refresh}</li>
 *   <li>{@code POST /auth/logout}</li>
 * </ul>
 *
 * <p>Bảng dữ liệu:
 * <ul>
 *   <li>vaitro</li>
 *   <li>vaitronguoidung (đọc người dùng từ module user)</li>
 *   <li>tokenlammoi (refresh token)</li>
 * </ul>
 *
 * <p>Cần lưu ý khi cài đặt:
 * <ul>
 *   <li>Bộ lọc JWT (JwtAuthenticationFilter), JwtService</li>
 *   <li>CustomUserDetailsService đọc bảng nguoidung (thay bean placeholderUserDetailsService trong SecurityConfig)</li>
 *   <li>Access token: JWT ký bằng JWT_SECRET, sống ngắn (khoảng 15 phút), KHÔNG lưu DB</li>
 *   <li>Refresh token: chuỗi ngẫu nhiên (SecureRandom), sống khoảng 7 ngày; chỉ lưu SHA-256 vào cột MaBamToken</li>
 *   <li>refresh: token hợp lệ khi ThuHoiLuc IS NULL và HetHanLuc &gt; hiện tại; ghi ThuHoiLuc cho token cũ
 *       rồi cấp cặp token mới (rotation). Token sai / hết hạn / đã thu hồi: lỗi TOKEN_EXPIRED</li>
 *   <li>logout: ghi ThuHoiLuc cho refresh token gửi lên; access token cũ tự hết hạn sau vài phút</li>
 *   <li>Đổi mật khẩu (module user): thu hồi toàn bộ refresh token của người dùng</li>
 * </ul>
 *
 * <p>Cấu trúc bên trong (tạo thư mục con khi cần): {@code controller}, {@code service},
 * {@code repository}, {@code entity}, {@code dto}. Module không truy cập trực tiếp repository của module khác;
 * gọi qua service của module đó.
 */
package com.dacn.schedule.auth;
