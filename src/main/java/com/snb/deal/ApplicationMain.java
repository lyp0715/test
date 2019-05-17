package com.snb.deal;

import com.alibaba.dubbo.config.spring.context.annotation.EnableDubbo;
import org.mybatis.spring.annotation.MapperScan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ImportResource;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {"com.snb.deal.*", "com.jianlc.event", "com.jianlc.tc"
        , "com.jianlc.schedule.*", "com.snb.third.*"})
@ImportResource(locations = {"classpath*:spring/spring-*.xml"})
@MapperScan(basePackages = {"com.snb.deal.mapper", "com.jianlc.event.persistence.mapper"})
@EnableScheduling
@EnableAutoConfiguration
@EnableDubbo
public class ApplicationMain {

    private static Logger logger = LoggerFactory.getLogger(ApplicationMain.class);

    public static void main(String[] args) {

        SpringApplication app = new SpringApplication(ApplicationMain.class);
        ConfigurableApplicationContext context = app.run(args);
        logger.info("started: {}", context);
    }
}
