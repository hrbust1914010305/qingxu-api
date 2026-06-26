package com.qingxu.qingxuapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class QingxuApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(QingxuApiApplication.class, args);
    }

}
