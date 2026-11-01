package com.dacn.schedule.system;

import com.dacn.schedule.common.api.ApiPaths;
import com.dacn.schedule.system.dto.PingResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Clock;
import java.time.OffsetDateTime;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint kiểm tra máy chủ còn sống. Dùng để: kiểm tra nhanh sau khi chạy, để frontend / Android
 * kiểm tra kết nối, và làm mẫu cho cách viết controller trong dự án (đường dẫn, Swagger, không cần token).
 */
@RestController
@RequestMapping(ApiPaths.V1 + "/system")
@Tag(name = "Hệ thống", description = "Kiểm tra trạng thái máy chủ")
public class SystemController {

    private final Clock clock;
    private final String applicationName;
    private final String version;

    public SystemController(Clock clock,
                            @Value("${spring.application.name}") String applicationName,
                            @Value("${app.version:0.0.1-SNAPSHOT}") String version) {
        this.clock = clock;
        this.applicationName = applicationName;
        this.version = version;
    }

    @GetMapping("/ping")
    @SecurityRequirements
    @Operation(summary = "Kiểm tra máy chủ", description = "Không cần đăng nhập. Trả về tên, phiên bản và giờ máy chủ.")
    public PingResponse ping() {
        return new PingResponse("UP", applicationName, version, OffsetDateTime.now(clock));
    }
}
