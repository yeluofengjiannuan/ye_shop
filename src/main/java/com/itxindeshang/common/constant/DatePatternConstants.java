package com.itxindeshang.common.constant;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class DatePatternConstants {
    public static final String DATE_TIME_FORM = "yyyy-MM-dd HH:mm:ss";
    // 标准日期时间（带秒）：yyyy-MM-dd HH:mm:ss
    public static final DateTimeFormatter NORMAL_DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    // 标准日期：yyyy-MM-dd
    public static final DateTimeFormatter NORMAL_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    // 系统默认时区（显式指定，避免依赖服务器环境）
    public static final ZoneId SYSTEM_ZONE_ID = ZoneId.of("Asia/Shanghai");
}
