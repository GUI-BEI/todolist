package com.example.demo;

import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class TaskController {

    private final Server server;

    public TaskController(Server server) {
        this.server = server;
    }

    // ========== 任务相关 ==========

    @PostMapping("/addTask")
    public Result<EventItem> addTask(@RequestBody EventItem task,
                                     @RequestHeader("Authorization") String token) {
        // 验证 token 获取 userId
        Result<User> authResult = server.verifyToken(token.replace("Bearer ", ""));
        if (authResult.getCode() != 200) {
            return Result.fail(authResult.getCode(), authResult.getMessage());
        }
        Long userId = authResult.getData().getId();
        return server.addTask(task, userId);
    }

    @GetMapping("/getTasks")
    public Result<List<EventItem>> getTasks(@RequestHeader("Authorization") String token) {
        Result<User> authResult = server.verifyToken(token.replace("Bearer ", ""));
        if (authResult.getCode() != 200) {
            return Result.fail(authResult.getCode(), authResult.getMessage());
        }
        Long userId = authResult.getData().getId();
        return server.getTasks(userId);
    }

    @GetMapping("/getFilteredTasks")
    public Result<List<EventItem>> getFilteredTasks(
            @RequestHeader("Authorization") String token,
            @RequestParam(required = false) Integer priority,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {

        Result<User> authResult = server.verifyToken(token.replace("Bearer ", ""));
        if (authResult.getCode() != 200) {
            return Result.fail(authResult.getCode(), authResult.getMessage());
        }
        Long userId = authResult.getData().getId();

        LocalDate start = startDate != null ? LocalDate.parse(startDate) : null;
        LocalDate end = endDate != null ? LocalDate.parse(endDate) : null;
        return server.getTasksWithFilter(userId, priority, keyword, start, end);
    }

    @PutMapping("/updateTask/{id}")
    public Result<EventItem> updateTask(@PathVariable Long id,
                                        @RequestBody EventItem updateTask,
                                        @RequestHeader("Authorization") String token) {
        Result<User> authResult = server.verifyToken(token.replace("Bearer ", ""));
        if (authResult.getCode() != 200) {
            return Result.fail(authResult.getCode(), authResult.getMessage());
        }
        Long userId = authResult.getData().getId();
        return server.updateTask(id, updateTask, userId);
    }

    @DeleteMapping("/deleteTask/{id}")
    public Result<Void> deleteTask(@PathVariable Long id,
                                   @RequestHeader("Authorization") String token) {
        Result<User> authResult = server.verifyToken(token.replace("Bearer ", ""));
        if (authResult.getCode() != 200) {
            return Result.fail(authResult.getCode(), authResult.getMessage());
        }
        Long userId = authResult.getData().getId();
        return server.deleteTask(id, userId);
    }

    @PostMapping("/updateTaskTime")
    public Result<EventItem> updateTaskTime(@RequestBody Map<String, String> request,
                                            @RequestHeader("Authorization") String token) {
        Result<User> authResult = server.verifyToken(token.replace("Bearer ", ""));
        if (authResult.getCode() != 200) {
            return Result.fail(authResult.getCode(), authResult.getMessage());
        }
        Long userId = authResult.getData().getId();

        Long id = Long.valueOf(request.get("id"));
        LocalDate start = request.get("start") != null ? LocalDate.parse(request.get("start")) : null;
        LocalDate end = request.get("end") != null ? LocalDate.parse(request.get("end")) : null;
        return server.updateTaskTime(id, start, end, userId);
    }

    // ========== 用户相关 ==========

    @PostMapping("/login")
    public Result<LoginResponseDTO> login(@RequestBody LoginRequestDTO request) {
        return server.login(request.getUsername(), request.getPassword());
    }

    @PostMapping("/register")
    public Result<User> register(@RequestBody LoginRequestDTO request) {
        return server.register(request.getUsername(), request.getPassword());
    }

    @GetMapping("/user/info")
    public Result<User> getUserInfo(@RequestHeader("Authorization") String token) {
        return server.verifyToken(token.replace("Bearer ", ""));
    }

    @PostMapping("/user/update")
    public Result<User> updateUser(@RequestBody Map<String, String> request,
                                   @RequestHeader("Authorization") String token) {
        Result<User> authResult = server.verifyToken(token.replace("Bearer ", ""));
        if (authResult.getCode() != 200) {
            return Result.fail(authResult.getCode(), authResult.getMessage());
        }
        Long userId = authResult.getData().getId();
        return server.updateUser(userId, request.get("username"), request.get("password"));
    }

    // ========== 签到相关 ==========

    @GetMapping("/sign/status")
    public Result<SignStatusDTO> getSignStatus(@RequestHeader("Authorization") String token) {
        Result<User> authResult = server.verifyToken(token.replace("Bearer ", ""));
        if (authResult.getCode() != 200) {
            return Result.fail(authResult.getCode(), authResult.getMessage());
        }
        Long userId = authResult.getData().getId();
        return server.getSignStatus(userId);
    }

    @PostMapping("/sign")
    public Result<SignStatusDTO> signIn(@RequestHeader("Authorization") String token) {
        Result<User> authResult = server.verifyToken(token.replace("Bearer ", ""));
        if (authResult.getCode() != 200) {
            return Result.fail(authResult.getCode(), authResult.getMessage());
        }
        Long userId = authResult.getData().getId();
        return server.signIn(userId);
    }

    // ========== Token 验证接口 ==========

    @GetMapping("/verify")
    public Result<User> verifyToken(@RequestHeader("Authorization") String token) {
        return server.verifyToken(token.replace("Bearer ", ""));
    }
}