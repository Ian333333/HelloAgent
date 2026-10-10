package com.example.l4j.tools;

import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Locale;

/**
 * 日期时间工具。
 */
@Component
public class DateTimeTools {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Tool("获取当前的日期和时间，格式为 yyyy-MM-dd HH:mm:ss")
    public String getCurrentDateTime() {
        return LocalDateTime.now().format(FORMATTER);
    }

    @Tool("获取今天是星期几")
    public String getDayOfWeek() {
        return LocalDateTime.now().getDayOfWeek()
                .getDisplayName(TextStyle.FULL, Locale.CHINA);
    }
}
