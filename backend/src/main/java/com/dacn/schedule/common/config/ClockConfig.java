package com.dacn.schedule.common.config;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cung cấp {@link Clock} dùng chung. Mọi nơi cần "bây giờ" (timestamp lỗi, tính hạn chót,
 * thuật toán lập lịch...) nên tiêm {@code Clock} thay vì gọi {@code OffsetDateTime.now()} trực tiếp,
 * để test có thể cố định thời gian.
 *
 * <p>Múi giờ lấy từ {@code app.timezone} (mặc định Asia/Ho_Chi_Minh). Dữ liệu trong DB vẫn lưu UTC.
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock(@Value("${app.timezone:Asia/Ho_Chi_Minh}") String timezone) {
        return Clock.system(ZoneId.of(timezone));
    }
}
