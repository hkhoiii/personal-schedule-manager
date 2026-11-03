-- ==============================================================================
-- BẢN VÁ 01: Thêm bảng TOKENLAMMOI (refresh token) vào database ĐANG CÓ DỮ LIỆU
-- Chỉ cần chạy nếu database được tạo trước khi init_db_quanlycongviec.sql có bảng này.
-- Database tạo mới từ init_db_quanlycongviec.sql đã có sẵn bảng, KHÔNG cần chạy file này.
-- Chạy lại nhiều lần vẫn an toàn (IF NOT EXISTS), không xóa dữ liệu nào.
--
-- Cách chạy (ở thư mục gốc dự án, Docker đang bật):
--   docker exec -i psm-postgres sh -c 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB"' < database/patch_01_tokenlammoi.sql
-- ==============================================================================

CREATE TABLE IF NOT EXISTS tokenlammoi (
    "id" BIGSERIAL PRIMARY KEY,
    "NguoiDungId" BIGINT NOT NULL REFERENCES nguoidung("id") ON DELETE CASCADE,
    "MaBamToken" VARCHAR(64) NOT NULL UNIQUE,
    "HetHanLuc" TIMESTAMPTZ NOT NULL,
    "ThuHoiLuc" TIMESTAMPTZ,
    "ThietBi" VARCHAR(255),
    "NgayTao" TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_tokenlammoi_nguoidung ON tokenlammoi("NguoiDungId");
