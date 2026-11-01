# Backend - Nhịp (Spring Boot 4, Java 21)

## Chạy

1. Bật PostgreSQL: ở thư mục gốc dự án chạy `docker compose up -d` (cổng 5433, đọc cấu hình từ file `.env`).
2. Đặt biến môi trường `DB_PASSWORD` (cùng giá trị với `.env`). `DB_NAME`, `DB_USERNAME`, `DB_PORT` có giá trị mặc định
   trong `application.properties`.
3. Trong thư mục `backend` chạy `mvn spring-boot:run`
   (hoặc `..\tools\apache-maven-3.9.6\bin\mvn.cmd spring-boot:run` trên Windows).

Kiểm tra:

| Địa chỉ | Ý nghĩa |
| --- | --- |
| http://localhost:8080/api/v1/system/ping | Máy chủ còn sống (không cần token) |
| http://localhost:8080/swagger-ui.html | Giao diện Swagger, thử endpoint trực tiếp |
| http://localhost:8080/v3/api-docs | Tài liệu OpenAPI dạng JSON (dùng để sinh client sau này) |

Mọi endpoint khác hiện trả `401 UNAUTHENTICATED` cho tới khi làm xong phần Xác thực (JWT).

## Test

```
mvn test -Dtest="GlobalExceptionHandlerTest,SecurityErrorFormatTest"   # không cần PostgreSQL
mvn test                                                               # gồm cả ScheduleApplicationTests (cần PostgreSQL đang chạy)
```

## Cấu trúc package (theo module)

```
com.dacn.schedule
├── ScheduleApplication
├── common                     # dùng chung, không chứa nghiệp vụ
│   ├── api                    # ApiError, ApiFieldError, ApiPaths
│   ├── config                 # SecurityConfig, OpenApiConfig, ClockConfig
│   ├── exception              # ErrorCode, ApiException, NotFoundException, GlobalExceptionHandler
│   └── security               # RestAuthenticationEntryPoint, RestAccessDeniedHandler
├── system                     # /system/ping
├── auth                       # đăng ký, đăng nhập, JWT
├── user                       # tài khoản, thiết lập cá nhân
├── category                   # nhóm công việc
├── task                       # công việc, công việc con, phụ thuộc (Kahn)
├── schedule                   # lịch, thuật toán xếp lịch
├── focus                      # phiên tập trung (Pomodoro)
├── ai                         # trợ lý AI (Gemini)
├── notification               # nhắc nhở, thông báo, WebSocket
└── analytics                  # thống kê
```

Mỗi module (trừ `common`) hiện chỉ có `package-info.java` ghi rõ endpoint, bảng dữ liệu và lưu ý cài đặt của module đó.
Khi viết mã thật, tạo các thư mục con `controller`, `service`, `repository`, `entity`, `dto` bên trong module.
Module không gọi repository của module khác; gọi qua service.

## Viết một endpoint mới

1. Controller đặt trong module tương ứng, đường dẫn dạng `@RequestMapping(ApiPaths.V1 + "/tasks")`.
2. Gắn `@Tag(name = "...")` cho controller và `@Operation(summary = "...")` cho từng phương thức để Swagger hiển thị tiếng Việt.
3. Endpoint công khai (không cần token) thêm `@SecurityRequirements` rỗng và thêm đường dẫn vào `PUBLIC_PATHS` trong `SecurityConfig`.
4. Ràng buộc dữ liệu đặt trên DTO và **ghi `message` tiếng Việt**, ví dụ `@NotBlank(message = "Không được để trống")`.
   Mặc định Bean Validation trả tiếng Anh.
5. Không tự bắt lỗi trong controller. Cứ ném ngoại lệ, `GlobalExceptionHandler` sẽ trả đúng định dạng.

## Báo lỗi

```java
throw new NotFoundException("công việc", id);                                  // 404 NOT_FOUND
throw new ApiException(ErrorCode.EMAIL_ALREADY_USED);                          // 409
throw new ApiException(ErrorCode.DEPENDENCY_CYCLE, "Việc A và B phụ thuộc vòng nhau.");
```

Phản hồi lỗi luôn có dạng:

```json
{
  "timestamp": "2026-10-31T17:00:00+07:00",
  "status": 400,
  "code": "VALIDATION_FAILED",
  "message": "Dữ liệu gửi lên không hợp lệ.",
  "path": "/api/v1/tasks",
  "details": [ { "field": "title", "message": "Không được để trống" } ]
}
```

`details` chỉ có khi lỗi gắn với từng trường. Lỗi 500 không bao giờ lộ chi tiết nội bộ (chi tiết nằm trong log).
Danh sách mã lỗi xem `ErrorCode.java`.

## Việc cần làm ở phần Xác thực (module `auth`)

- Viết `JwtAuthenticationFilter` và đăng ký vào `SecurityConfig` (trước `UsernamePasswordAuthenticationFilter`).
- Token hết hạn: gọi `resolver.resolveException(request, response, null, new ApiException(ErrorCode.TOKEN_EXPIRED))`.
- Viết `CustomUserDetailsService` đọc bảng `nguoidung` và **xóa** bean `placeholderUserDetailsService` trong `SecurityConfig`.
