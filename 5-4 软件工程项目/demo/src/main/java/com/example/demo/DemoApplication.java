package com.example.demo;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class DemoApplication {
    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }

    // 【核心修改】注入 UserRepository，统计用户表记录数
    @Bean
    public CommandLineRunner testDatabase(UserRepository userRepository) {
        return args -> {
            // 延迟 1 秒，等 Hibernate 建完表
            Thread.sleep(1000);

            System.out.println("========== 开始测试数据库连接 ==========");
            try {
                long userCount = userRepository.count(); // 直接用 JpaRepository 自带的 count()
                System.out.println(" 数据库连接成功！当前共有 " + userCount + " 个用户");
            } catch (Exception e) {
                System.out.println(" 数据库连接失败！错误信息：");
                e.printStackTrace();
            }
            System.out.println("========== 测试结束 ==========");
        };
    }
}