package com.storm.tools;

import jdk.jfr.Description;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.context.i18n.LocaleContextHolder;

import java.time.LocalDateTime;

public class testTimeTools {

    @Tool(description = "在用户的时区获取当前的时间")
    public String getCurrentTime(){

        return LocalDateTime.now().atZone(LocaleContextHolder.getTimeZone().toZoneId()).toString();

    }
}
