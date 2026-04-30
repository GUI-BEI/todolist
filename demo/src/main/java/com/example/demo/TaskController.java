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

    // ========== 任务相关（保持不变）==========
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
        LocalDateTime start = startDate != null ? LocalDateTime.parse(startDate) : null;
        LocalDateTime end = endDate != null ? LocalDateTime.parse(endDate) : null;
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

        System.out.println("接收到更新请求，type = " + updateTask.getType());

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
        String startStr = request.get("start");
        String endStr = request.get("end");
        LocalDateTime start = startStr != null ? parseDateTime(startStr) : null;
        LocalDateTime end = endStr != null ? parseDateTime(endStr) : null;
        return server.updateTaskTime(id, start, end, userId);
    }

    private LocalDateTime parseDateTime(String dateTimeStr) {
        if (dateTimeStr == null) return null;
        if (dateTimeStr.contains(" ")) {
            return LocalDateTime.parse(dateTimeStr.replace(" ", "T"));
        }
        return LocalDateTime.parse(dateTimeStr);
    }

    // ========== 用户相关（统一返回 UserResponseDTO）==========
    @PostMapping("/login")
    public Result<UserResponseDTO> login(@RequestBody LoginRequestDTO request) {
        return server.login(request.getUsername(), request.getPassword());
    }

    @PostMapping("/register")
    public Result<UserResponseDTO> register(@RequestBody LoginRequestDTO request) {
        return server.register(request.getUsername(), request.getPassword());
    }

    @GetMapping("/user/info")
    public Result<UserResponseDTO> getUserInfo(@RequestHeader("Authorization") String token) {
        Result<Long> authResult = server.verifyTokenAndGetId(token.replace("Bearer ", ""));
        if (authResult.getCode() != 200) {
            return Result.fail(authResult.getCode(), authResult.getMessage());
        }
        Long userId = authResult.getData();
        return server.getUserInfo(userId);
    }

    @PostMapping("/user/update")
    public Result<UserResponseDTO> updateUser(@RequestBody Map<String, String> request,
                                               @RequestHeader("Authorization") String token) {
        Result<Long> authResult = server.verifyTokenAndGetId(token.replace("Bearer ", ""));
        if (authResult.getCode() != 200) {
            return Result.fail(authResult.getCode(), authResult.getMessage());
        }
        Long userId = authResult.getData();
        return server.updateUser(userId, request.get("username"), request.get("password"));
    }

    // ========== 签到相关（保持不变）==========
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
    public Result<UserResponseDTO> verifyToken(@RequestHeader("Authorization") String token) {
        return server.verifyToken(token.replace("Bearer ", ""));
    }

    // ========== 密保相关 ==========

    // 设置密保问题（需要登录）
    @PostMapping("/user/security")
    public Result<Void> setSecurityQuestion(@RequestBody SecurityQuestionDTO request,
                                            @RequestHeader("Authorization") String token) {
        Result<Long> authResult = server.verifyTokenAndGetId(token.replace("Bearer ", ""));
        if (authResult.getCode() != 200) {
            return Result.fail(authResult.getCode(), authResult.getMessage());
        }
        Long userId = authResult.getData();
        return server.setSecurityQuestion(userId, request.getSecurityQuestion(), request.getSecurityAnswer());
    }

    // 获取密保问题（不需要登录）
    @GetMapping("/user/security/question")
    public Result<String> getSecurityQuestion(@RequestParam String username) {
        return server.getSecurityQuestion(username);
    }

    // 重置密码（不需要登录）
    @PostMapping("/user/reset-password")
    public Result<Void> resetPassword(@RequestBody ResetPasswordDTO request) {
        return server.resetPassword(request.getUsername(), request.getSecurityAnswer(), request.getNewPassword());
    }

    // ========== 标签管理相关 ==========

    @GetMapping("/tags")
    public Result<List<TagDTO>> getTags(@RequestHeader("Authorization") String token) {
        Result<Long> authResult = server.verifyTokenAndGetId(token.replace("Bearer ", ""));
        if (authResult.getCode() != 200) {
            return Result.fail(authResult.getCode(), authResult.getMessage());
        }
        Long userId = authResult.getData();
        return server.getTags(userId);
    }

    @PostMapping("/tags")
    public Result<TagDTO> addTag(@RequestBody Map<String, String> request,
                                 @RequestHeader("Authorization") String token) {
        Result<Long> authResult = server.verifyTokenAndGetId(token.replace("Bearer ", ""));
        if (authResult.getCode() != 200) {
            return Result.fail(authResult.getCode(), authResult.getMessage());
        }
        Long userId = authResult.getData();
        String tagName = request.get("tagName");
        return server.addTag(userId, tagName);
    }

    @DeleteMapping("/tags/{tagName}")
    public Result<Void> deleteTag(@PathVariable String tagName,
                                  @RequestHeader("Authorization") String token) {
        Result<Long> authResult = server.verifyTokenAndGetId(token.replace("Bearer ", ""));
        if (authResult.getCode() != 200) {
            return Result.fail(authResult.getCode(), authResult.getMessage());
        }
        Long userId = authResult.getData();
        return server.deleteTag(userId, tagName);
    }
}