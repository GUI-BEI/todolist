package com.example.demo;

import org.mindrot.jbcrypt.BCrypt;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.Optional;

@SpringBootApplication
public class DemoApplication {
    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }

    @Bean
    public CommandLineRunner initData(UserRepository userRepository,
                                      EventItemRepository eventItemRepository) {
        return args -> {
            Thread.sleep(1000);

            System.out.println("========== 开始初始化数据 ==========");

            // 1. 测试数据库连接
            try {
                long userCount = userRepository.count();
                System.out.println("数据库连接成功！当前共有 " + userCount + " 个用户");
            } catch (Exception e) {
                System.out.println("数据库连接失败！错误信息：");
                e.printStackTrace();
            }

            // 2. 创建游客账号
            try {
                String visitorUsername = "visitor";
                String visitorPassword = "visitor123";

                Optional<User> existingVisitor = userRepository.findByUsername(visitorUsername);

                if (existingVisitor.isEmpty()) {
                    // 创建游客用户
                    User visitor = new User();
                    visitor.setUsername(visitorUsername);
                    // 使用 BCrypt 加密密码
                    visitor.setPassword(BCrypt.hashpw(visitorPassword, BCrypt.gensalt()));
                    visitor.setTotalDays(0);

                    User savedVisitor = userRepository.save(visitor);
                    System.out.println("游客账号创建成功！");
                    System.out.println("   用户名: " + visitorUsername);
                    System.out.println("   密码: " + visitorPassword);

                    // 为游客创建任务表
                    eventItemRepository.createTaskTableForUser(savedVisitor.getId());
                    System.out.println("游客任务表创建成功！");
                } else {
                    System.out.println("游客账号已存在，跳过创建");
                }
            } catch (Exception e) {
                System.out.println("创建游客账号失败：");
                e.printStackTrace();
            }

            System.out.println("========== 初始化完成 ==========");
        };
    }
}