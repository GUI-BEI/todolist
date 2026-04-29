package com.example.demo;

 
import org.apache.catalina.Server;
import org.springframework.web.bind.annotation.*;

import java.util.List;
// 控制器层（Controller），负责处理HTTP请求，调用Service层进行业务逻辑处理，并返回结果
@RestController
@RequestMapping("/api")
class TaskController {
    private final EventItemServer taskService;

    public TaskController(EventItemServer taskService) {
        this.taskService = taskService;
    }

    private String getUser(String token) {
        return token;
    }

    @GetMapping("/tasks")
    public Result<List<EventItem>> getTasks(@RequestHeader("Authorization") String token) {
        return taskService.getTasks(getUser(token));
    }

    @PostMapping("/tasks")
    public Result<EventItem> addTask(@RequestBody EventItem task, @RequestHeader("Authorization") String token) {
        return taskService.addTask(getUser(token), task);
    }

    @PutMapping("/tasks/{id}")
    public Result<EventItem> updateTask(@PathVariable Long id, @RequestBody EventItem task, @RequestHeader("Authorization") String token) {
        return taskService.updateTask(getUser(token), id, task);
    }

    @DeleteMapping("/tasks/{id}")
    public Result<Void> deleteTask(@PathVariable Long id, @RequestHeader("Authorization") String token) {
        return taskService.deleteTask(getUser(token), id);
    }
}

@RestController
@RequestMapping("")
class UserController{
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public Result<User> register(@RequestParam String username, @RequestParam String password) {
        return userService.register(username, password);
    }

    @PostMapping("/login")
    public Result<String> login(@RequestParam String username, @RequestParam String password) {
        return userService.login(username, password);
    }
}