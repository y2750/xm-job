package com.example.common.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 灵活的 LocalDateTime 反序列化器
 * 支持 yyyy-MM-dd 和 yyyy-MM-dd HH:mm:ss 两种格式
 */
public class FlexibleLocalDateTimeDeserializer extends StdDeserializer<LocalDateTime> {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public FlexibleLocalDateTimeDeserializer() {
        super(LocalDateTime.class);
    }

    @Override
    public LocalDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String dateString = p.getText();
        if (dateString == null || dateString.trim().isEmpty()) {
            return null;
        }
        
        dateString = dateString.trim();
        
        // 尝试解析 yyyy-MM-dd HH:mm:ss 格式
        try {
            return LocalDateTime.parse(dateString, DATETIME_FORMATTER);
        } catch (Exception e) {
            // 如果失败，尝试解析 yyyy-MM-dd 格式，并设置为当天的 23:59:59
            try {
                LocalDate date = LocalDate.parse(dateString, DATE_FORMATTER);
                return date.atTime(23, 59, 59);
            } catch (Exception e2) {
                // 如果都失败，尝试 ISO 格式（带 T 的格式）
                try {
                    return LocalDateTime.parse(dateString);
                } catch (Exception e3) {
                    throw new IOException("无法解析日期格式: " + dateString + 
                        "，支持的格式: yyyy-MM-dd 或 yyyy-MM-dd HH:mm:ss", e3);
                }
            }
        }
    }
}

