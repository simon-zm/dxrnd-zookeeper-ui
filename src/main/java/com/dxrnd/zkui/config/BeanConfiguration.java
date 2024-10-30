package com.dxrnd.zkui.config;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;

import java.io.IOException;
import java.util.Properties;

@Configuration
public class BeanConfiguration {
    @Bean(name = "globalProps")
    public Properties properties() throws IOException {
        ClassPathResource classPathResource = new ClassPathResource("application.properties");
        Properties globalProps = PropertiesLoaderUtils.loadProperties(classPathResource);
        return globalProps;
    }



}
