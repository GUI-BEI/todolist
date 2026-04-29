package com.example.demo;

import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class Server {

    private final EventItemRepository repository;
    private final UserRepository userRepository;

    public Server(EventItemRepository repository, UserRepository userRepository) {
        this.repository = repository;
        this.userRepository = userRepository;
    }

    // ========== 任务相关方法（带用户隔离）==========

    // 添加任务
    public Result<EventItem> addTask(EventItem task, Long userId) {
        if (task.getTitle() == null || task.getTitle().trim().isEmpty()) {
            return Result.fail(400, "任务标题不能为空");
        }
        task.setId(null);
        task.setUserId(userId);
        EventItem savedTask = repository.save(task);
        return Result.success(savedTask);
    }

    // 获取用户的所有任务
    public Result<List<EventItem>> getTasks(Long userId) {
        List<EventItem> tasks = repository.findByUserId(userId);
        return Result.success(tasks);
    }

    // 获取筛选后的任务
    public Result<List<EventItem>> getTasksWithFilter(Long userId, Integer priority,
                                                      String keyword, LocalDate startDate,
                                                      LocalDate endDate) {
        List<EventItem> tasks = repository.findByUserId(userId);

        // 按优先级筛选
        if (priority != null) {
            tasks = tasks.stream()
                    .filter(t -> priority.equals(t.getPriority()))
                    .collect(Collectors.toList());
        }

        // 按关键词筛选
        if (keyword != null && !keyword.trim().isEmpty()) {
            String kw = keyword.trim().toLowerCase();
            tasks = tasks.stream()
                    .filter(t -> (t.getTitle() != null && t.getTitle().toLowerCase().contains(kw)) ||
                            (t.getDescription() != null && t.getDescription().toLowerCase().contains(kw)))
                    .collect(Collectors.toList());
        }

        // 按日期范围筛选
        if (startDate != null && endDate != null) {
            tasks = tasks.stream()
                    .filter(t -> !(t.getEnd().isBefore(startDate) || t.getStart().isAfter(endDate)))
                    .collect(Collectors.toList());
        }

        return Result.success(tasks);
    }

    // 更新任务
    public Result<EventItem> updateTask(Long id, EventItem updateTask, Long userId) {
        Optional<EventItem> taskOpt = repository.findById(id);
        if (taskOpt.isEmpty()) {
            return Result.fail(404, "任务不存在");
        }

        EventItem task = taskOpt.get();
        if (!task.getUserId().equals(userId)) {
            return Result.fail(403, "无权修改此任务");
        }

        if (updateTask.getTitle() != null) task.setTitle(updateTask.getTitle());
        if (updateTask.getDescription() != null) task.setDescription(updateTask.getDescription());
        if (updateTask.getPriority() != null) task.setPriority(updateTask.getPriority());
        if (updateTask.getStart() != null) task.setStart(updateTask.getStart());
        if (updateTask.getEnd() != null) task.setEnd(updateTask.getEnd());
        if (updateTask.getType() != null) task.setType(updateTask.getType());
        if (updateTask.getCompleted() != null) task.setCompleted(updateTask.getCompleted());

        EventItem saved = repository.save(task);
        return Result.success(saved);
    }

    // 删除任务
    public Result<Void> deleteTask(Long id, Long userId) {
        Optional<EventItem> taskOpt = repository.findById(id);
        if (taskOpt.isEmpty()) {
            return Result.fail(404, "任务不存在");
        }

        EventItem task = taskOpt.get();
        if (!task.getUserId().equals(userId)) {
            return Result.fail(403, "无权删除此任务");
        }

        repository.deleteById(id);
        return Result.success(null);
    }

    // 单独更新时间
    public Result<EventItem> updateTaskTime(Long id, LocalDate newStart, LocalDate newEnd, Long userId) {
        Optional<EventItem> taskOpt = repository.findById(id);
        if (taskOpt.isEmpty()) {
            return Result.fail(404, "任务不存在");
        }

        EventItem task = taskOpt.get();
        if (!task.getUserId().equals(userId)) {
            return Result.fail(403, "无权修改此任务");
        }

        if (newStart != null) task.setStart(newStart);
        if (newEnd != null) task.setEnd(newEnd);

        EventItem saved = repository.save(task);
        return Result.success(saved);
    }

    // ========== 用户相关方法 ==========

    // 用户登录
    public Result<LoginResponseDTO> login(String username, String password) {
        if (username == null || username.trim().isEmpty()) {
            return Result.fail(400, "用户名不能为空");
        }
        if (password == null || password.trim().isEmpty()) {
            return Result.fail(400, "密码不能为空");
        }

        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            return Result.fail(401, "用户名不存在");
        }

        User user = userOpt.get();
        if (!user.getPassword().equals(password)) {
            return Result.fail(401, "密码错误");
        }

        // 生成简单 token
        String token = UUID.randomUUID().toString();
        user.setToken(token);
        userRepository.save(user);

        LoginResponseDTO response = new LoginResponseDTO(user, token);
        return Result.success(response);
    }

    // 用户注册
    public Result<User> register(String username, String password) {
        if (username == null || username.trim().isEmpty()) {
            return Result.fail(400, "用户名不能为空");
        }
        if (password == null || password.trim().isEmpty()) {
            return Result.fail(400, "密码不能为空");
        }
        if (password.length() < 6) {
            return Result.fail(400, "密码长度至少6位");
        }

        if (userRepository.existsByUsername(username)) {
            return Result.fail(409, "用户名已存在");
        }

        User newUser = new User(username, password);
        User savedUser = userRepository.save(newUser);
        savedUser.setPassword(null);
        savedUser.setToken(null);
        return Result.success(savedUser);
    }

    // 获取用户信息
    public Result<User> getUserInfo(Long userId) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            return Result.fail(404, "用户不存在");
        }
        User user = userOpt.get();
        user.setPassword(null);
        user.setToken(null);
        return Result.success(user);
    }

    // 验证 token
    public Result<User> verifyToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            return Result.fail(401, "未提供token");
        }

        Optional<User> userOpt = userRepository.findByToken(token);
        if (userOpt.isEmpty()) {
            return Result.fail(401, "token无效或已过期");
        }

        User user = userOpt.get();
        user.setPassword(null);
        return Result.success(user);
    }

    // 更新用户信息
    public Result<User> updateUser(Long userId, String newUsername, String newPassword) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            return Result.fail(404, "用户不存在");
        }

        User user = userOpt.get();

        if (newUsername != null && !newUsername.equals(user.getUsername())) {
            if (userRepository.existsByUsername(newUsername)) {
                return Result.fail(409, "用户名已存在");
            }
            user.setUsername(newUsername);
        }

        if (newPassword != null && !newPassword.trim().isEmpty()) {
            if (newPassword.length() < 6) {
                return Result.fail(400, "密码长度至少6位");
            }
            user.setPassword(newPassword);
        }

        User savedUser = userRepository.save(user);
        savedUser.setPassword(null);
        savedUser.setToken(null);
        return Result.success(savedUser);
    }

    // ========== 签到相关方法 ==========

    // 获取签到状态
    public Result<SignStatusDTO> getSignStatus(Long userId) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            return Result.fail(404, "用户不存在");
        }

        User user = userOpt.get();
        LocalDate today = LocalDate.now();
        boolean signedToday = user.getLastSignDate() != null && user.getLastSignDate().equals(today);

        return Result.success(new SignStatusDTO(user.getTotalDays(), signedToday));
    }

    // 执行签到
    public Result<SignStatusDTO> signIn(Long userId) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            return Result.fail(404, "用户不存在");
        }

        User user = userOpt.get();
        LocalDate today = LocalDate.now();

        if (user.getLastSignDate() != null && user.getLastSignDate().equals(today)) {
            return Result.fail(400, "今日已经签到过了");
        }

        if (user.getLastSignDate() != null && user.getLastSignDate().plusDays(1).equals(today)) {
            user.setTotalDays(user.getTotalDays() + 1);
        } else {
            user.setTotalDays(1);
        }

        user.setLastSignDate(today);
        User savedUser = userRepository.save(user);

        return Result.success(new SignStatusDTO(savedUser.getTotalDays(), true));
    }
}