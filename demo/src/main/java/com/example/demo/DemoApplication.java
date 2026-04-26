package com.example.demo;

 
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

// 主应用类，Spring Boot 的入口
@SpringBootApplication
public class DemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }


    // 数据库连接测试
    @Bean
public CommandLineRunner testDatabase(EventItemRepository repository) {
    return args -> {
        // 延迟 1 秒，等 Hibernate 建完表
        Thread.sleep(1000);
        
        System.out.println("========== 开始测试数据库连接 ==========");
        try {
            long count = repository.count();
            System.out.println("✅ 数据库连接成功！当前共有 " + count + " 条记录");
        } catch (Exception e) {
            System.out.println("❌ 数据库连接失败！错误信息：");
            e.printStackTrace();
        }
        System.out.println("========== 测试结束 ==========");
    };
   }
}