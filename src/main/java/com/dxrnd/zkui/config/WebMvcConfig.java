package com.dxrnd.zkui.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * @Author: simon.zhang
 * @Date: 2024/10/30
 * @decrisption
 */

/**
 * MVC配置
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        /**
         * 注册静态文件路径 配置外部的资源要使用file声明&#xff0c;配置jar包内部的使用classpath声明
         */
        // 首页
        registry.addResourceHandler("/css/**").addResourceLocations("classpath:/static/webapp/css/");
//        // 对应的 index.js
        registry.addResourceHandler("/js/**").addResourceLocations("classpath:/static/webapp/js/");
//        // 其余静态文件的路径解析
        registry.addResourceHandler("/images/**").addResourceLocations("classpath:/static/webapp/images/");

        registry.addResourceHandler("/html/**").addResourceLocations("classpath:/static/webapp/html/");
        registry.addResourceHandler("/fonts/**").addResourceLocations("classpath:/static/webapp/fonts/");
    }
}
