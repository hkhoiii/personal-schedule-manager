package com.dacn.schedule.common.exception;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dacn.schedule.common.api.ApiFieldError;
import com.dacn.schedule.common.config.ClockConfig;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Kiểm tra {@link GlobalExceptionHandler} trả đúng định dạng lỗi chung cho từng loại ngoại lệ.
 * Chỉ nạp tầng web (không cần PostgreSQL). Tầng bảo mật được mở hoàn toàn để chỉ kiểm tra bộ xử lý lỗi.
 */
@WebMvcTest(controllers = GlobalExceptionHandlerTest.ErrorTestController.class)
@Import({GlobalExceptionHandler.class, ClockConfig.class,
        GlobalExceptionHandlerTest.PermitAllSecurity.class,
        GlobalExceptionHandlerTest.ErrorTestController.class})
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void notFoundException_returnsCommonFormat() throws Exception {
        mockMvc.perform(get("/test-errors/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Không tìm thấy công việc với id 5."))
                .andExpect(jsonPath("$.path").value("/test-errors/not-found"))
                .andExpect(jsonPath("$.timestamp").value(matchesPattern("\\d{4}-\\d{2}-\\d{2}T.*")))
                .andExpect(jsonPath("$.details").doesNotExist());
    }

    @Test
    void apiExceptionWithDetails_keepsCodeStatusAndDetails() throws Exception {
        mockMvc.perform(get("/test-errors/conflict"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DEPENDENCY_CYCLE"))
                .andExpect(jsonPath("$.message").value("Việc A và việc B phụ thuộc vòng nhau."))
                .andExpect(jsonPath("$.details[0].field").value("dependsOn"));
    }

    @Test
    void invalidBody_returnsValidationFailedWithFieldDetails() throws Exception {
        mockMvc.perform(post("/test-errors/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"\",\"estimateMinutes\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.details.length()").value(2))
                .andExpect(jsonPath("$.details[?(@.field=='title')].message").value("Không được để trống"))
                .andExpect(jsonPath("$.details[?(@.field=='estimateMinutes')].message").value("Tối thiểu 1 phút"));
    }

    @Test
    void malformedJson_returnsValidationFailedWithoutInternalDetails() throws Exception {
        mockMvc.perform(post("/test-errors/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{khong phai json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.message").value(containsString("JSON")))
                .andExpect(content().string(not(containsString("JsonParseException"))));
    }

    @Test
    void wrongParameterType_returnsFieldDetail() throws Exception {
        mockMvc.perform(get("/test-errors/number").param("page", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.details[0].field").value("page"));
    }

    @Test
    void missingParameter_returnsFieldDetail() throws Exception {
        mockMvc.perform(get("/test-errors/number"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.details[0].field").value("page"))
                .andExpect(jsonPath("$.details[0].message").value("Thiếu tham số bắt buộc"));
    }

    @Test
    void unexpectedException_returnsGenericInternalErrorWithoutLeakingDetails() throws Exception {
        mockMvc.perform(get("/test-errors/boom"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(content().string(not(containsString("chi tiết nội bộ"))));
    }

    @Test
    void accessDeniedThrownFromController_returnsForbidden() throws Exception {
        mockMvc.perform(get("/test-errors/forbidden"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void wrongHttpMethod_returnsMethodNotAllowedAndKeepsAllowHeader() throws Exception {
        mockMvc.perform(post("/test-errors/not-found"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(header().exists("Allow"))
                .andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"));
    }

    @Test
    void unsupportedMediaType_returnsUnsupportedMediaType() throws Exception {
        mockMvc.perform(post("/test-errors/validate")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("hello"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.code").value("UNSUPPORTED_MEDIA_TYPE"));
    }

    @Test
    void unknownPath_returnsNotFoundInCommonFormat() throws Exception {
        mockMvc.perform(get("/duong-dan-khong-ton-tai"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.path").value("/duong-dan-khong-ton-tai"));
    }

    // ------------------------------------------------------------------ hỗ trợ kiểm thử

    /** Mở toàn bộ để test chỉ tập trung vào bộ xử lý lỗi. */
    @TestConfiguration
    static class PermitAllSecurity {
        @Bean
        SecurityFilterChain permitAllFilterChain(HttpSecurity http) throws Exception {
            http.csrf(csrf -> csrf.disable())
                    .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
            return http.build();
        }
    }

    /** Controller chỉ dùng trong test, cố ý ném từng loại lỗi. */
    @RestController
    @RequestMapping("/test-errors")
    static class ErrorTestController {

        record NewTask(
                @NotBlank(message = "Không được để trống") String title,
                @Min(value = 1, message = "Tối thiểu 1 phút") int estimateMinutes) {
        }

        @GetMapping("/not-found")
        void notFound() {
            throw new NotFoundException("công việc", 5);
        }

        @GetMapping("/conflict")
        void conflict() {
            throw new ApiException(ErrorCode.DEPENDENCY_CYCLE, "Việc A và việc B phụ thuộc vòng nhau.",
                    List.of(new ApiFieldError("dependsOn", "Tạo thành vòng lặp")));
        }

        @PostMapping("/validate")
        void validate(@Valid @RequestBody NewTask body) {
        }

        @GetMapping("/number")
        void number(@RequestParam int page) {
        }

        @GetMapping("/boom")
        void boom() {
            throw new IllegalStateException("chi tiết nội bộ bí mật");
        }

        @GetMapping("/forbidden")
        void forbidden() {
            throw new AccessDeniedException("không đủ quyền");
        }
    }
}
