/**
 * Module task: Công việc, công việc con và quan hệ phụ thuộc.
 *
 * <p>Endpoint (tiền tố {@code /api/v1}):
 * <ul>
 *   <li>{@code GET /tasks}</li>
 *   <li>{@code GET /tasks/summary}</li>
 *   <li>{@code POST /tasks}</li>
 *   <li>{@code GET /tasks/{id}}</li>
 *   <li>{@code PUT /tasks/{id}}</li>
 *   <li>{@code PATCH /tasks/{id}/status}</li>
 *   <li>{@code DELETE /tasks/{id}}</li>
 *   <li>{@code POST /tasks/{id}/subtasks}</li>
 *   <li>{@code POST /tasks/batch}</li>
 * </ul>
 *
 * <p>Bảng dữ liệu:
 * <ul>
 *   <li>congviec</li>
 *   <li>congviecthe</li>
 *   <li>congvieccon</li>
 *   <li>congviecphuthuoc</li>
 * </ul>
 *
 * <p>Cần lưu ý khi cài đặt:
 * <ul>
 *   <li>Phát hiện vòng phụ thuộc bằng thuật toán Kahn, lỗi DEPENDENCY_CYCLE</li>
 *   <li>Kiểm tra mọi truy vấn lọc theo người dùng hiện tại</li>
 *   <li>GET /tasks/{id} trả kèm công việc con, thẻ và danh sách phụ thuộc (cho màn hình chi tiết)</li>
 *   <li>Công việc của người khác: trả 404 NOT_FOUND (không trả 403) để không lộ id nào đang tồn tại</li>
 * </ul>
 *
 * <p>Cấu trúc bên trong (tạo thư mục con khi cần): {@code controller}, {@code service},
 * {@code repository}, {@code entity}, {@code dto}. Module không truy cập trực tiếp repository của module khác;
 * gọi qua service của module đó.
 */
package com.dacn.schedule.task;
