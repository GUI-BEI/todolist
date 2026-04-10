package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.config.Task;
import org.springframework.web.bind.annotation.*;

import javax.xml.transform.Result;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@SpringBootApplication
@RestController // 加上这个注解，让这个类变成接口控制器
@CrossOrigin(origins = "*")
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

