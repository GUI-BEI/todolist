package com.example.demo;

import org.mindrot.jbcrypt.BCrypt; // 【核心修改2】引入BCrypt
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class Server {

    private final EventItemRepository eventItemRepository;
    private final UserRepository userRepository;

    // 构造方法保持注入
    public Server(EventItemRepository eventItemRepository, UserRepository userRepository) {
        this.eventItemRepository = eventItemRepository;
        this.userRepository = userRepository;
    }

    // ========== 任务相关方法（适配动态表）==========

    // 添加任务
    public Result<EventItem> addTask(EventItem task, Long userId) {
        if (task.getTitle() == null || task.getTitle().trim().isEmpty()) {
            return Result.fail(400, "任务标题不能为空");
        }
        task.setId(null);
        task.setUserId(userId);
        EventItem savedTask = eventItemRepository.save(userId, task);
        return Result.success(savedTask);
    }

    // 获取用户的所有任务
    public Result<List<EventItem>> getTasks(Long userId) {
        List<EventItem> tasks = eventItemRepository.findByUserId(userId);
        return Result.success(tasks);
    }

    // 获取筛选后的任务
    public Result<List<EventItem>> getTasksWithFilter(Long userId, Integer priority,
                                                      String keyword, LocalDate startDate,
                                                      LocalDate endDate) {
        String kw = keyword != null ? keyword.trim().toLowerCase() : null;
        List<EventItem> tasks = eventItemRepository.findByFilter(userId, priority, kw, startDate, endDate);
        return Result.success(tasks);
    }

    // 更新任务
    public Result<EventItem> updateTask(Long id, EventItem updateTask, Long userId) {
        EventItem task = eventItemRepository.findById(userId, id);
        if (task == null) {
            return Result.fail(404, "任务不存在");
        }

        if (updateTask.getTitle() != null) task.setTitle(updateTask.getTitle());
        if (updateTask.getDescription() != null) task.setDescription(updateTask.getDescription());
        if (updateTask.getPriority() != null) task.setPriority(updateTask.getPriority());
        if (updateTask.getStart() != null) task.setStart(updateTask.getStart());
        if (updateTask.getEnd() != null) task.setEnd(updateTask.getEnd());
        if (updateTask.getType() != null) task.setType(updateTask.getType());
        if (updateTask.getCompleted() != null) task.setCompleted(updateTask.getCompleted());

        EventItem saved = eventItemRepository.update(userId, task);
        return Result.success(saved);
    }

    // 删除任务
    public Result<Void> deleteTask(Long id, Long userId) {
        EventItem task = eventItemRepository.findById(userId, id);
        if (task == null) {
            return Result.fail(404, "任务不存在");
        }

        eventItemRepository.deleteById(userId, id);
        return Result.success(null);
    }

    // 单独更新时间
    public Result<EventItem> updateTaskTime(Long id, LocalDate newStart, LocalDate newEnd, Long userId) {
        EventItem task = eventItemRepository.findById(userId, id);
        if (task == null) {
            return Result.fail(404, "任务不存在");
        }

        eventItemRepository.updateTaskTime(userId, id, newStart, newEnd);
        EventItem updatedTask = eventItemRepository.findById(userId, id);
        return Result.success(updatedTask);
    }

    // ========== 用户相关方法（新增：BCrypt密码加密）==========

    // 用户登录（确保 Optional<User> 赋值正确）
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
        if (!BCrypt.checkpw(password, user.getPassword())) {
            return Result.fail(401, "密码错误");
        }

        String token = UUID.randomUUID().toString();
        user.setToken(token);
        userRepository.save(user);

        LoginResponseDTO response = new LoginResponseDTO(user, token);
        return Result.success(response);
    }

    // 用户注册（【核心修改4】BCrypt加密存储密码+事务）
    @Transactional(rollbackFor = Exception.class)
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

        // BCrypt加密密码
        String encryptedPwd = BCrypt.hashpw(password, BCrypt.gensalt());
        User newUser = new User(username, encryptedPwd);
        User savedUser = userRepository.save(newUser);
        
        // 为新用户创建专属任务表
        eventItemRepository.createTaskTableForUser(savedUser.getId());
        
        savedUser.setPassword(null);
        savedUser.setToken(null);
        return Result.success(savedUser);
    }

    // 以下用户相关方法逻辑完全不变
    public Result<User> getUserInfo(Long userId) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            return Result.fail(404, "用户不存在");
        }
        User user = userOpt.get();
        user.setPassword(null);
        return Result.success(user);
    }

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
            // 更新密码时也用BCrypt加密
            user.setPassword(BCrypt.hashpw(newPassword, BCrypt.gensalt()));
        }

        User savedUser = userRepository.save(user);
        savedUser.setPassword(null);
        savedUser.setToken(null);
        return Result.success(savedUser);
    }

    // ========== 签到相关方法（逻辑不变）==========
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