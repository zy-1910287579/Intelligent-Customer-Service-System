package com.storm.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class MvcConfiguration implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        //表示的是对服务端那些路径允许跨域
        registry.addMapping("/**")
                //允许哪些 源（Origin） 访问。
                //"*" 表示允许所有源，即任何域名、任何端口的页面都能请求你的 API
                .allowedOrigins("*")
                //允许的 HTTP 方法。
                //列出了常用的 RESTful 方法以及 OPTIONS（预检请求必须允许）。
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("Content-Disposition");

    }
}
