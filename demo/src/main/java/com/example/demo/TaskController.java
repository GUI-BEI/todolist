package com.example.demo;

 
import org.springframework.web.bind.annotation.*;

import java.util.List;
// 控制器层（Controller），负责处理HTTP请求，调用Service层进行业务逻辑处理，并返回结果
@RestController
@RequestMapping("/api")
public class TaskController {

    // 构造器注入（Spring 推荐）
    private final Server server;
    public TaskController(Server server) {
        this.server = server;
    }

    // 添加任务（持久化到数据库）
    @PostMapping("/addTask")
    public Result<EventItem> addTask(@RequestBody EventItem task) {
        return server.addTask(task);
    }

    // 获取所有任务（从数据库查询）
    @GetMapping("/getTasks")
    public Result<List<EventItem>> getTasks() {
        return server.getTasks();
    }

    // 新增：修改任务
    @PutMapping("/updateTask/{id}")
    public Result<EventItem> updateTask(@PathVariable Long id, @RequestBody EventItem updateTask) {
        return server.updateTask(id, updateTask);
    }

    // 新增：删除任务
    @DeleteMapping("/deleteTask/{id}")
    public Result<Void> deleteTask(@PathVariable Long id) {
        return server.deleteTask(id);
    }
}