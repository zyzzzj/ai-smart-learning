package com.ailearn;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * 基于AI大模型的智能学习系统 - 启动类。
 */
@SpringBootApplication
@MapperScan("com.ailearn.mapper")
@EnableTransactionManagement
@EnableCaching
@EnableScheduling
@EnableAsync
public class AiLearningApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiLearningApplication.class, args);
        System.out.println("""

                ==================================================
                  AI 智能学习系统已启动
                  接口文档: http://localhost:8082/swagger-ui.html
                ==================================================
                """);
    }
}
