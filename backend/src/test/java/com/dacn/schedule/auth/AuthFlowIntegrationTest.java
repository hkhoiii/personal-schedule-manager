package com.dacn.schedule.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Kiểm tra toàn luồng qua HTTP với ứng dụng và PostgreSQL thật — đúng tiêu chí hoàn thành:
 * "gọi GET /users/me không có token bị 401, có token trả đúng thông tin".
 * Chạy trong transaction và rollback, không để lại dữ liệu.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void register_login_me_refresh_logout() throws Exception {
        // 1. Không có token: 401
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));

        // 2. Đăng ký
        mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"Flow.Test@Gmail.com\",\"password\":\"matkhau123\",\"fullName\":\"Luồng Kiểm Thử\"}"))
                .andExpect(status().isCreated());

        // 3. Đăng nhập
        String loginBody = mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"flow.test@gmail.com\",\"password\":\"matkhau123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andReturn().getResponse().getContentAsString();
        String accessToken = JsonPath.read(loginBody, "$.accessToken");
        String refreshToken = JsonPath.read(loginBody, "$.refreshToken");

        // 4. Có token: trả đúng thông tin người vừa đăng ký
        mockMvc.perform(get("/api/v1/users/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("flow.test@gmail.com"))
                .andExpect(jsonPath("$.fullName").value("Luồng Kiểm Thử"))
                .andExpect(jsonPath("$.roles[0]").value("ROLE_USER"));

        // 5. Làm mới: token mới vẫn dùng được
        String refreshBody = mockMvc.perform(post("/api/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String newAccessToken = JsonPath.read(refreshBody, "$.accessToken");
        String newRefreshToken = JsonPath.read(refreshBody, "$.refreshToken");
        mockMvc.perform(get("/api/v1/users/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + newAccessToken))
                .andExpect(status().isOk());

        // 6. Đăng xuất rồi làm mới lại: 401 TOKEN_EXPIRED
        mockMvc.perform(post("/api/v1/auth/logout").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + newRefreshToken + "\"}"))
                .andExpect(status().isNoContent());
        mockMvc.perform(post("/api/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + newRefreshToken + "\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("TOKEN_EXPIRED"));
    }

    @Test
    void login_wrongPassword_returns401BadCredentials() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"khong.ton.tai@gmail.com\",\"password\":\"matkhau123\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("BAD_CREDENTIALS"));
    }
}
