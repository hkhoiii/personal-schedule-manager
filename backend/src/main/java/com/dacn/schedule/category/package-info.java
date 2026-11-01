/**
 * Module category: Nhóm công việc (danh mục): màu, mức độ quan trọng, khung giờ ưu tiên.
 *
 * <p>Endpoint (tiền tố {@code /api/v1}):
 * <ul>
 *   <li>{@code GET /categories}</li>
 *   <li>{@code POST /categories}</li>
 *   <li>{@code PUT /categories/{id}}</li>
 *   <li>{@code DELETE /categories/{id}}</li>
 * </ul>
 *
 * <p>Bảng dữ liệu:
 * <ul>
 *   <li>danhmuc</li>
 *   <li>the</li>
 * </ul>
 *
 * <p>Cần lưu ý khi cài đặt:
 * <ul>
 *   <li>Lỗi: CATEGORY_IN_USE khi xóa nhóm còn công việc</li>
 * </ul>
 *
 * <p>Cấu trúc bên trong (tạo thư mục con khi cần): {@code controller}, {@code service},
 * {@code repository}, {@code entity}, {@code dto}. Module không truy cập trực tiếp repository của module khác;
 * gọi qua service của module đó.
 */
package com.dacn.schedule.category;
