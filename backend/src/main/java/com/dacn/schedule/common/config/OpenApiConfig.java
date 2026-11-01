package com.dacn.schedule.common.config;

import com.dacn.schedule.common.api.ApiError;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import java.util.Map;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cấu hình Swagger / OpenAPI (springdoc).
 *
 * <ul>
 *   <li>Giao diện: {@code /swagger-ui.html}; tài liệu JSON: {@code /v3/api-docs}.</li>
 *   <li>Nút "Authorize" nhận JWT dạng Bearer. Mặc định MỌI endpoint yêu cầu token; endpoint công khai
 *       (đăng nhập, đăng ký...) gắn {@code @SecurityRequirements} rỗng để bỏ ổ khóa.</li>
 *   <li>Tự thêm các phản hồi lỗi chung (400, 401, 403, 500) trỏ tới schema {@link ApiError} cho mọi endpoint,
 *       nên không phải khai báo lặp lại trong từng controller.</li>
 * </ul>
 */
@Configuration
public class OpenApiConfig {

    public static final String BEARER_SCHEME = "bearerAuth";

    private static final String ERROR_SCHEMA_REF = "#/components/schemas/ApiError";

    @Bean
    public OpenAPI scheduleOpenApi(@Value("${app.version:0.0.1-SNAPSHOT}") String version) {
        return new OpenAPI()
                .info(new Info()
                        .title("Nhịp - API quản lý & tự động lập lịch cá nhân")
                        .version(version)
                        .description("""
                                REST API của hệ thống Nhịp: công việc, lịch, trợ lý AI, thông báo, thống kê.
                                Mọi lỗi trả về cùng một định dạng `ApiError` (xem schema bên dưới).
                                Bấm **Authorize** và dán JWT (không kèm chữ "Bearer") để thử các endpoint cần đăng nhập."""))
                .components(new Components()
                        .addSecuritySchemes(BEARER_SCHEME, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Access token nhận được từ POST /auth/login")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME));
    }

    /** Thêm schema {@code ApiError} và các phản hồi lỗi chung vào mọi endpoint. */
    @Bean
    public OpenApiCustomizer apiErrorResponsesCustomizer() {
        return this::addCommonErrorResponses;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void addCommonErrorResponses(OpenAPI openApi) {
        if (openApi.getComponents() == null) {
            openApi.setComponents(new Components());
        }
        Components components = openApi.getComponents();

        // Đăng ký schema ApiError (và ApiFieldError) nếu chưa có controller nào tham chiếu tới.
        Map<String, Schema> errorSchemas = ModelConverters.getInstance().readAll(ApiError.class);
        errorSchemas.forEach((name, schema) -> {
            if (components.getSchemas() == null || !components.getSchemas().containsKey(name)) {
                components.addSchemas(name, schema);
            }
        });

        if (openApi.getPaths() == null) {
            return;
        }
        openApi.getPaths().values().forEach(pathItem -> pathItem.readOperations().forEach(operation -> {
            ApiResponses responses = operation.getResponses();
            if (responses == null) {
                responses = new ApiResponses();
                operation.setResponses(responses);
            }
            if (hasInput(operation)) {
                addIfAbsent(responses, "400", "Dữ liệu gửi lên không hợp lệ (VALIDATION_FAILED)");
            }
            if (!isPublic(operation)) {
                addIfAbsent(responses, "401", "Chưa đăng nhập hoặc token hết hạn (UNAUTHENTICATED, TOKEN_EXPIRED)");
                addIfAbsent(responses, "403", "Không đủ quyền (FORBIDDEN)");
            }
            addIfAbsent(responses, "500", "Lỗi máy chủ không dự đoán trước (INTERNAL_ERROR)");
        }));
    }

    /** Endpoint công khai là endpoint khai báo danh sách yêu cầu bảo mật rỗng ({@code @SecurityRequirements}). */
    private boolean isPublic(Operation operation) {
        return operation.getSecurity() != null && operation.getSecurity().isEmpty();
    }

    private boolean hasInput(Operation operation) {
        return operation.getRequestBody() != null
                || (operation.getParameters() != null && !operation.getParameters().isEmpty());
    }

    @SuppressWarnings("rawtypes")
    private void addIfAbsent(ApiResponses responses, String status, String description) {
        if (responses.containsKey(status)) {
            return;
        }
        Schema<?> errorRef = new Schema<>().$ref(ERROR_SCHEMA_REF);
        io.swagger.v3.oas.models.media.MediaType json = new io.swagger.v3.oas.models.media.MediaType();
        json.setSchema(errorRef);
        responses.addApiResponse(status, new ApiResponse()
                .description(description)
                .content(new Content().addMediaType("application/json", json)));
    }
}
