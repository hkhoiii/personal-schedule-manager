# Backend - Nhịp (Spring Boot 4, Java 21)

## Chạy

1. Bật Docker Desktop. Ở thư mục gốc dự án, tạo file `.env` từ `.env.example` (lần đầu), đặt `DB_PASSWORD`
   và `JWT_SECRET` (tạo bằng `openssl rand -base64 48` trong Git Bash; giá trị mẫu trong `.env.example` sẽ bị từ chối).
2. Trong thư mục `backend` chạy `mvn spring-boot:run`
   (hoặc `..\tools\apache-maven-3.9.6\bin\mvn.cmd spring-boot:run` trên Windows).
   Ứng dụng tự chạy `docker compose` để bật PostgreSQL (cổng 5433). Phải chạy lệnh từ thư mục `backend`
   (đường dẫn `../docker-compose.yml` tính từ đây). Tắt ứng dụng thì PostgreSQL vẫn chạy tiếp.

Ứng dụng tự đọc file `.env` ở thư mục gốc (`spring.config.import` trong `application.properties`), nên **không cần**
tự đặt biến môi trường. Biến môi trường thật của hệ điều hành (nếu có) được ưu tiên hơn `.env`.
Thiếu hoặc sai `JWT_SECRET` thì ứng dụng dừng ngay lúc khởi động và báo rõ lý do.

Lần đầu bật, PostgreSQL tự chạy `database/init_db_quanlycongviec.sql`. Script này **chỉ chạy khi volume dữ liệu còn trống**.
Nếu database của bạn được tạo trước khi có bảng `tokenlammoi`, chạy bản vá (ở thư mục gốc, không mất dữ liệu):

```
docker exec -i psm-postgres sh -c 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB"' < database/patch_01_tokenlammoi.sql
```

Muốn xóa sạch và tạo lại database từ đầu: `docker compose down -v` rồi chạy lại ứng dụng (**mất toàn bộ dữ liệu**).

Kiểm tra:

| Địa chỉ | Ý nghĩa |
| --- | --- |
| http://localhost:8080/api/v1/system/ping | Máy chủ còn sống (không cần token) |
| http://localhost:8080/swagger-ui.html | Giao diện Swagger, thử endpoint trực tiếp |
| http://localhost:8080/v3/api-docs | Tài liệu OpenAPI dạng JSON (dùng để sinh client sau này) |

Mọi endpoint khác cần access token (xem mục Xác thực bên dưới).

## Test

```
mvn test -Dtest="GlobalExceptionHandlerTest,SecurityErrorFormatTest"   # không cần PostgreSQL
mvn test                                                               # gồm cả ScheduleApplicationTests (cần PostgreSQL đang chạy)
```

Khi chạy test, Spring Boot **không** tự bật docker compose, nên `mvn test` đầy đủ cần PostgreSQL đang chạy
(`docker compose up -d` ở thư mục gốc). Mật khẩu DB và `JWT_SECRET` vẫn được đọc tự động từ `.env`.

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

## Xác thực (JWT)

| Endpoint | Ý nghĩa |
| --- | --- |
| `POST /api/v1/auth/register` | Đăng ký (201) |
| `POST /api/v1/auth/login` | Trả `accessToken` (JWT, 15 phút) + `refreshToken` (7 ngày) |
| `POST /api/v1/auth/refresh` | Đổi refresh token lấy cặp mới; token cũ bị thu hồi (dùng lại token cũ: thu hồi mọi phiên) |
| `POST /api/v1/auth/logout` | Thu hồi refresh token (204) |
| `GET /api/v1/users/me` | Thông tin tài khoản đang đăng nhập (cần token) |

- Mở không cần token: `/api/v1/auth/**`, Swagger, `/api/v1/system/**`. Mọi đường dẫn khác (kể cả `/actuator/**`) cần token.
- Lỗi: thiếu token `401 UNAUTHENTICATED`; token hết hạn `401 TOKEN_EXPIRED` (client gọi `/auth/refresh`);
  token giả `401 UNAUTHENTICATED` "Token không hợp lệ"; thiếu quyền `403 FORBIDDEN`. Tất cả cùng định dạng `ApiError`.
- Lấy người dùng hiện tại trong controller: tham số `@AuthenticationPrincipal AuthenticatedUser currentUser`
  (`common.security`), dùng `currentUser.userId()`. Giới hạn theo vai trò: `@PreAuthorize("hasRole('ADMIN')")`.
- Thành phần: `JwtService`, `JwtAuthenticationFilter`, `AccountUserDetailsService` (`auth/security`),
  `AuthService`, `RefreshTokenService` (`auth/service`), `SecurityConfig` (`common/config`).
- Khi test, khóa JWT lấy từ `src/test/resources/config/application.properties` (khóa cố định chỉ dùng cho test).
- Thử bằng Postman: import `postman/personal-schedule-manager.postman_collection.json` (ở thư mục gốc) rồi chạy
  từ trên xuống; đăng nhập xong biến `{{token}}` tự được điền.
