package com.dxrnd.zkui;

import com.dxrnd.zkui.dao.Dao;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

import java.util.Date;
import java.util.Properties;

@SpringBootApplication
public class ZkuiApplication {
    private final static org.slf4j.Logger logger = LoggerFactory.getLogger(ZkuiApplication.class);

    public static void main(String[] args){
        logger.debug("Starting ZKUI!");
        ApplicationContext run = SpringApplication.run(ZkuiApplication.class, args);
        Properties globalProps = run.getBean("globalProps",Properties.class);
        globalProps.setProperty("uptime", new Date().toString());
        new Dao(globalProps).checkNCreate();
    }

}
