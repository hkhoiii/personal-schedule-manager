package com.dacn.schedule.common.api;

/**
 * Hằng số đường dẫn dùng chung cho mọi controller.
 *
 * <p>Mọi controller khai báo đường dẫn dạng {@code ApiPaths.V1 + "/tasks"} để khi cần đổi
 * tiền tố (ví dụ bỏ {@code /v1}) chỉ phải sửa ở một nơi.
 */
public final class ApiPaths {

    /** Tiền tố của toàn bộ REST API (phiên bản 1). */
    public static final String V1 = "/api/v1";

    private ApiPaths() {
    }
}
