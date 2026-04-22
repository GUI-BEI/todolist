package com.example.demo;

 
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@SpringBootApplication
public class DemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }

    // CORS 配置（优化：使用 allowedOriginPatterns）
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**")
                        .allowedOriginPatterns("http://127.0.0.1:5500", "http://localhost:5500")
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                        .allowedHeaders("*")
                        .allowCredentials(true)
                        .maxAge(3600); // 预检请求缓存时间，减少OPTIONS请求
            }
        };
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