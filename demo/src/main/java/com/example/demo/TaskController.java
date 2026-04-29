package com.example.demo;

import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
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
        Result<Long> authResult = server.verifyTokenAndGetId(token.replace("Bearer ", ""));
        if (authResult.getCode() != 200) {
            return Result.fail(authResult.getCode(), authResult.getMessage());
        }
        Long userId = authResult.getData();
        return server.addTask(task, userId);
    }

    @GetMapping("/getTasks")
    public Result<List<EventItem>> getTasks(@RequestHeader("Authorization") String token) {
        Result<Long> authResult = server.verifyTokenAndGetId(token.replace("Bearer ", ""));
        if (authResult.getCode() != 200) {
            return Result.fail(authResult.getCode(), authResult.getMessage());
        }
        Long userId = authResult.getData();
        return server.getTasks(userId);
    }

    @GetMapping("/getFilteredTasks")
    public Result<List<EventItem>> getFilteredTasks(
            @RequestHeader("Authorization") String token,
            @RequestParam(required = false) Integer priority,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {

        Result<Long> authResult = server.verifyTokenAndGetId(token.replace("Bearer ", ""));
        if (authResult.getCode() != 200) {
            return Result.fail(authResult.getCode(), authResult.getMessage());
        }
        Long userId = authResult.getData();

        // 解析为LocalDateTime（原逻辑正确，无需改）
        LocalDateTime start = startDate != null ? LocalDateTime.parse(startDate) : null;
        LocalDateTime end = endDate != null ? LocalDateTime.parse(endDate) : null;

        // 传递LocalDateTime给Server（Server层已修正参数类型）
        return server.getTasksWithFilter(userId, priority, keyword, start, end);
    }

    @PutMapping("/updateTask/{id}")
    public Result<EventItem> updateTask(@PathVariable Long id,
                                        @RequestBody EventItem updateTask,
                                        @RequestHeader("Authorization") String token) {
        Result<Long> authResult = server.verifyTokenAndGetId(token.replace("Bearer ", ""));
        if (authResult.getCode() != 200) {
            return Result.fail(authResult.getCode(), authResult.getMessage());
        }
        Long userId = authResult.getData();
        return server.updateTask(id, updateTask, userId);
    }

    @DeleteMapping("/deleteTask/{id}")
    public Result<Void> deleteTask(@PathVariable Long id,
                                   @RequestHeader("Authorization") String token) {
        Result<Long> authResult = server.verifyTokenAndGetId(token.replace("Bearer ", ""));
        if (authResult.getCode() != 200) {
            return Result.fail(authResult.getCode(), authResult.getMessage());
        }
        Long userId = authResult.getData();
        return server.deleteTask(id, userId);
    }

    @PostMapping("/updateTaskTime")
    public Result<EventItem> updateTaskTime(@RequestBody Map<String, String> request,
                                            @RequestHeader("Authorization") String token) {
        Result<Long> authResult = server.verifyTokenAndGetId(token.replace("Bearer ", ""));
        if (authResult.getCode() != 200) {
            return Result.fail(authResult.getCode(), authResult.getMessage());
        }
        Long userId = authResult.getData();

        Long id = Long.valueOf(request.get("id"));
        // 解析日期时间字符串为 LocalDateTime
        // 前端传来的格式可能是 "2026-04-30 00:00:00" 或 "2026-04-30T00:00:00"
        String startStr = request.get("start");
        String endStr = request.get("end");

        LocalDateTime start = startStr != null ? parseDateTime(startStr) : null;
        LocalDateTime end = endStr != null ? parseDateTime(endStr) : null;

        return server.updateTaskTime(id, start, end, userId);
    }

    // 辅助方法：解析多种日期时间格式
    private LocalDateTime parseDateTime(String dateTimeStr) {
        if (dateTimeStr == null) return null;
        // 处理 "2026-04-30 00:00:00" 格式
        if (dateTimeStr.contains(" ")) {
            return LocalDateTime.parse(dateTimeStr.replace(" ", "T"));
        }
        // 处理 "2026-04-30T00:00:00" 格式
        return LocalDateTime.parse(dateTimeStr);
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
        Result<Long> authResult = server.verifyTokenAndGetId(token.replace("Bearer ", ""));
        if (authResult.getCode() != 200) {
            return Result.fail(authResult.getCode(), authResult.getMessage());
        }
        Long userId = authResult.getData();
        return server.updateUser(userId, request.get("username"), request.get("password"));
    }

    // ========== 签到相关 ==========

    @GetMapping("/sign/status")
    public Result<SignStatusDTO> getSignStatus(@RequestHeader("Authorization") String token) {
        Result<Long> authResult = server.verifyTokenAndGetId(token.replace("Bearer ", ""));
        if (authResult.getCode() != 200) {
            return Result.fail(authResult.getCode(), authResult.getMessage());
        }
        Long userId = authResult.getData();
        return server.getSignStatus(userId);
    }

    @PostMapping("/sign")
    public Result<SignStatusDTO> signIn(@RequestHeader("Authorization") String token) {
        Result<Long> authResult = server.verifyTokenAndGetId(token.replace("Bearer ", ""));
        if (authResult.getCode() != 200) {
            return Result.fail(authResult.getCode(), authResult.getMessage());
        }
        Long userId = authResult.getData();
        return server.signIn(userId);
    }

    @GetMapping("/verify")
    public Result<User> verifyToken(@RequestHeader("Authorization") String token) {
        return server.verifyToken(token.replace("Bearer ", ""));
    }
}