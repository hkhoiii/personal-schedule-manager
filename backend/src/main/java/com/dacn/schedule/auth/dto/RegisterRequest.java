package com.dacn.schedule.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Dữ liệu đăng ký tài khoản.
 *
 * <p>Quy tắc mật khẩu: 8 đến 72 ký tự, có ít nhất một chữ cái và một chữ số. Giới hạn 72 là giới hạn của
 * BCrypt (tính theo byte); với mật khẩu có ký tự tiếng Việt, {@code AuthService} kiểm tra thêm số byte.
 */
@Schema(name = "RegisterRequest", description = "Thông tin đăng ký tài khoản")
public record RegisterRequest(

        @Schema(example = "minhanh@gmail.com")
        @NotBlank(message = "Email không được để trống")
        @Size(max = 120, message = "Email tối đa 120 ký tự")
        // @Email mặc định chấp nhận "a@b" (không có tên miền cấp cao); regexp yêu cầu thêm dấu chấm sau @
        @Email(regexp = "^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$", message = "Email không đúng định dạng")
        String email,

        @Schema(description = "8-72 ký tự, có ít nhất một chữ cái và một chữ số", example = "matkhau123")
        @NotBlank(message = "Mật khẩu không được để trống")
        @Size(min = 8, max = 72, message = "Mật khẩu phải từ 8 đến 72 ký tự")
        @Pattern(regexp = "^(?=.*\\p{L})(?=.*\\d).*$", message = "Mật khẩu phải có ít nhất một chữ cái và một chữ số")
        String password,

        @Schema(description = "Họ và tên hiển thị", example = "Nguyễn Minh Anh")
        @NotBlank(message = "Họ và tên không được để trống")
        @Size(max = 100, message = "Họ và tên tối đa 100 ký tự")
        String fullName) {

    /** Không in mật khẩu ra log nếu lỡ log cả đối tượng. */
    @Override
    public String toString() {
        return "RegisterRequest[email=" + email + ", fullName=" + fullName + ", password=***]";
    }
}
