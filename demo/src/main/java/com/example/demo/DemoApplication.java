package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.config.Task;
import org.springframework.web.bind.annotation.*;

import javax.xml.transform.Result;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;

@SpringBootApplication
@RestController // 加上这个注解，让这个类变成接口控制器
//删掉了crossorigin的全局注释，添加了bean注释保证万无一失
public class DemoApplication {
    private final List<EventItemDto> tasks = new CopyOnWriteArrayList<>();

    public class Result<T> {
      private Integer code;
      private String message;
      private T data;
      public Result(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
       }
       public Integer getCode() { return code; }
       public String getMessage() { return message; }
       public T getData() { return data; }
    }
    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }
//新添加的注释，相当于显式提醒放行
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**")
                        .allowedOrigins("http://127.0.0.1:5500", "http://localhost:5500")
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                        .allowedHeaders("*")
                        .allowCredentials(true);
            }
        };
    }

    @PostMapping("/api/addTask")
    public Result<String> addTask(@RequestBody EventItemDto task) {
        if (task.getTitle() == null || task.getTitle().trim().isEmpty()) {
            return new Result<String>(400, "任务标题不能为空", null);
        }
        tasks.add(task);
        return new Result<String>(200, "任务添加成功", null);
    }
    
    @GetMapping("/api/getTasks")
    public Result<List<EventItemDto>> getTasks() {
        if(tasks.isEmpty()) {
            return new Result<List<EventItemDto>>(404, "没有任务", null);
        }
        return new Result<List<EventItemDto>>(200, "获取任务列表成功", tasks);
    }
}

