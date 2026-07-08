package com.qingxu.qingxuapi;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
@MapperScan("com.qingxu.qingxuapi.infrastructure.persistence.mapper")
public class QingxuApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(QingxuApiApplication.class, args);
    }

}
