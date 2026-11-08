package com.dacn.schedule.common.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dacn.schedule.auth.security.JwtService;
import com.dacn.schedule.common.config.ClockConfig;
import com.dacn.schedule.common.config.SecurityConfig;
import com.dacn.schedule.common.exception.GlobalExceptionHandler;
import com.dacn.schedule.system.SystemController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Kiểm tra cấu hình bảo mật khung: endpoint công khai mở, endpoint khác trả lỗi JSON chung
 * (không phải trang đăng nhập HTML hay thân rỗng), CORS cho frontend Vite.
 */
@WebMvcTest(controllers = SystemController.class)
@Import({SecurityConfig.class, JwtService.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class,
        GlobalExceptionHandler.class, ClockConfig.class})
class SecurityErrorFormatTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void ping_isPublic() throws Exception {
        mockMvc.perform(get("/api/v1/system/ping"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.application").exists())
                .andExpect(jsonPath("$.serverTime").exists());
    }

    @Test
    void protectedEndpointWithoutToken_returnsUnauthenticatedInCommonFormat() throws Exception {
        mockMvc.perform(get("/api/v1/tasks"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"))
                .andExpect(jsonPath("$.path").value("/api/v1/tasks"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @WithMockUser
    void authenticatedRequestToMissingEndpoint_returnsNotFoundInCommonFormat() throws Exception {
        mockMvc.perform(get("/api/v1/chua-co-endpoint-nay"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void corsPreflightFromFrontend_isAllowed() throws Exception {
        mockMvc.perform(options("/api/v1/tasks")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "authorization,content-type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }
}
