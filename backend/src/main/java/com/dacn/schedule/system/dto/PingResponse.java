package com.dacn.schedule.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;

@Schema(name = "PingResponse", description = "Trạng thái máy chủ")
public record PingResponse(
        @Schema(example = "UP") String status,
        @Schema(description = "Tên ứng dụng", example = "schedule") String application,
        @Schema(description = "Phiên bản ứng dụng", example = "0.0.1-SNAPSHOT") String version,
        @Schema(description = "Giờ máy chủ", example = "2026-10-31T17:00:00+07:00") OffsetDateTime serverTime) {
}
