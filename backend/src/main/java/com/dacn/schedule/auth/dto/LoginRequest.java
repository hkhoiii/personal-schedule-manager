package com.dacn.schedule.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Dữ liệu đăng nhập. Chỉ kiểm tra không rỗng / không quá dài; KHÔNG áp quy tắc độ mạnh mật khẩu ở đây
 * (sai thì trả BAD_CREDENTIALS như mọi mật khẩu sai khác).
 */
@Schema(name = "LoginRequest", description = "Thông tin đăng nhập")
public record LoginRequest(

        @Schema(example = "minhanh@gmail.com")
        @NotBlank(message = "Email không được để trống")
        @Size(max = 120, message = "Email tối đa 120 ký tự")
        String email,

        @Schema(example = "matkhau123")
        @NotBlank(message = "Mật khẩu không được để trống")
        @Size(max = 72, message = "Mật khẩu tối đa 72 ký tự")
        String password) {

    /** Không in mật khẩu ra log nếu lỡ log cả đối tượng. */
    @Override
    public String toString() {
        return "LoginRequest[email=" + email + ", password=***]";
    }
}
