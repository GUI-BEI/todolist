package com.example.demo;

import org.mindrot.jbcrypt.BCrypt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
public class Server {
    private final EventItemRepository eventItemRepository;
    private final UserRepository userRepository;
    private final TaskTagRepository taskTagRepository;
    private final TaskAttachmentRepository attachmentRepository;

    public Server(EventItemRepository eventItemRepository,
                  UserRepository userRepository,
                  TaskTagRepository taskTagRepository,
                  TaskAttachmentRepository attachmentRepository) {
        this.eventItemRepository = eventItemRepository;
        this.userRepository = userRepository;
        this.taskTagRepository = taskTagRepository;
        this.attachmentRepository = attachmentRepository;
    }

    // ========== 任务相关方法（保持不变）==========
    public Result<EventItem> addTask(EventItem task, Long userId) {
        if (task.getTitle() == null || task.getTitle().trim().isEmpty()) {
            return Result.fail(400, "任务标题不能为空");
        }
        task.setId(null);
        task.setUserId(userId);
        EventItem savedTask = eventItemRepository.save(userId, task);
        return Result.success(savedTask);
    }

    public Result<List<EventItem>> getTasks(Long userId) {
        List<EventItem> tasks = eventItemRepository.findByUserId(userId);  // 已在Repository中排序
        return Result.success(tasks);
    }

    public Result<List<EventItem>> getTasksWithFilter(Long userId, Integer priority,
                                                      String keyword, LocalDateTime startDate,
                                                      LocalDateTime endDate) {
        String kw = keyword != null ? keyword.trim().toLowerCase() : null;
        List<EventItem> tasks = eventItemRepository.findByFilter(userId, priority, kw, startDate, endDate);
        return Result.success(tasks);
    }

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

    public Result<Void> deleteTask(Long id, Long userId) {
        EventItem task = eventItemRepository.findById(userId, id);
        if (task == null) {
            return Result.fail(404, "任务不存在");
        }

        // 删除任务的所有附件文件
        List<TaskAttachment> attachments = attachmentRepository.findByTaskId(id);
        for (TaskAttachment attachment : attachments) {
            String filePath = AttachmentConfig.getAttachmentPath() + attachment.getFilePath();
            File file = new File(filePath);
            if (file.exists()) {
                file.delete();
            }
        }

        // 删除附件记录
        attachmentRepository.deleteByTaskId(id);

        // 删除任务
        eventItemRepository.deleteById(userId, id);
        return Result.success(null);
    }

    public Result<EventItem> updateTaskTime(Long id, LocalDateTime newStart, LocalDateTime newEnd, Long userId) {
        EventItem task = eventItemRepository.findById(userId, id);
        if (task == null) {
            return Result.fail(404, "任务不存在");
        }
        eventItemRepository.updateTaskTime(userId, id, newStart, newEnd);
        EventItem updatedTask = eventItemRepository.findById(userId, id);
        return Result.success(updatedTask);
    }

    // ========== 用户相关方法（统一使用 UserResponseDTO）==========
    public Result<UserResponseDTO> login(String username, String password) {
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
        return Result.success(new UserResponseDTO(user, token));
    }

    @Transactional(rollbackFor = Exception.class)
    public Result<UserResponseDTO> register(String username, String password) {
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
        String encryptedPwd = BCrypt.hashpw(password, BCrypt.gensalt());
        User newUser = new User(username, encryptedPwd);
        User savedUser = userRepository.save(newUser);

        CompletableFuture.runAsync(() -> {
            try {
                eventItemRepository.createTaskTableForUser(savedUser.getId());
            } catch (Exception e) {
                System.out.println("建表失败：" + e.getMessage());
            }
        });

        return Result.success(new UserResponseDTO(savedUser));
    }

    public Result<UserResponseDTO> getUserInfo(Long userId) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            return Result.fail(404, "用户不存在");
        }
        User user = userOpt.get();
        return Result.success(new UserResponseDTO(user));
    }

    public Result<UserResponseDTO> verifyToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            return Result.fail(401, "未提供token");
        }
        Optional<User> userOpt = userRepository.findByToken(token);
        if (userOpt.isEmpty()) {
            return Result.fail(401, "token无效或已过期");
        }
        User user = userOpt.get();
        return Result.success(new UserResponseDTO(user));
    }

    public Result<Long> verifyTokenAndGetId(String token) {
        if (token == null || token.trim().isEmpty()) {
            return Result.fail(401, "未提供token");
        }
        Optional<User> userOpt = userRepository.findByToken(token);
        if (userOpt.isEmpty()) {
            return Result.fail(401, "token无效或已过期");
        }
        return Result.success(userOpt.get().getId());
    }

    public Result<UserResponseDTO> updateUser(Long userId, String newUsername, String newPassword) {
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
            user.setPassword(BCrypt.hashpw(newPassword, BCrypt.gensalt()));
        }
        User savedUser = userRepository.save(user);
        return Result.success(new UserResponseDTO(savedUser));
    }

    // ========== 签到相关方法（保持不变）==========
    public Result<SignStatusDTO> getSignStatus(Long userId) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            System.out.println(" 获取签到状态失败：用户ID " + userId + " 不存在");
            return Result.fail(404, "用户不存在");
        }
        User user = userOpt.get();
        LocalDate today = LocalDate.now();
        boolean signedToday = user.getLastSignDate() != null &&
                user.getLastSignDate().toLocalDate().equals(today);
        Integer totalDays = user.getTotalDays() == null ? 0 : user.getTotalDays();
        return Result.success(new SignStatusDTO(totalDays, signedToday));
    }

    @Transactional(rollbackFor = Exception.class)
    public Result<SignStatusDTO> signIn(Long userId) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            System.out.println(" 签到失败：用户ID " + userId + " 不存在");
            return Result.fail(404, "用户不存在");
        }
        User user = userOpt.get();
        LocalDate today = LocalDate.now();

        if (user.getLastSignDate() != null) {
            LocalDate lastSignDate = user.getLastSignDate().toLocalDate();
            if (lastSignDate.equals(today)) {
                System.out.println(" 用户ID " + userId + " 今日已签到");
                return Result.fail(400, "今日已经签到过了");
            }
        }

        int newTotalDays;
        if (user.getLastSignDate() != null) {
            LocalDate lastSignDate = user.getLastSignDate().toLocalDate();
            if (lastSignDate.plusDays(1).equals(today)) {
                newTotalDays = (user.getTotalDays() == null ? 0 : user.getTotalDays()) + 1;
            } else {
                newTotalDays = 1;
            }
        } else {
            newTotalDays = 1;
        }

        user.setTotalDays(newTotalDays);
        user.setLastSignDate(LocalDateTime.now());

        User savedUser;
        try {
            savedUser = userRepository.save(user);
            System.out.println(" 用户ID " + userId + " 签到成功，总天数：" + newTotalDays);
        } catch (Exception e) {
            System.out.println(" 用户ID " + userId + " 签到保存失败：" + e.getMessage());
            e.printStackTrace();
            return Result.fail(500, "签到失败：服务器保存数据出错");
        }

        return Result.success(new SignStatusDTO(savedUser.getTotalDays(), true));
    }

    // 设置密保问题和答案
    public Result<Void> setSecurityQuestion(Long userId, String question, String answer) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            return Result.fail(404, "用户不存在");
        }

        User user = userOpt.get();

        if (question == null || question.trim().isEmpty()) {
            return Result.fail(400, "密保问题不能为空");
        }
        if (answer == null || answer.trim().isEmpty()) {
            return Result.fail(400, "密保答案不能为空");
        }

        user.setSecurityQuestion(question);
        user.setSecurityAnswer(answer);
        userRepository.save(user);

        return Result.success(null);
    }

    // 获取用户的密保问题（用于忘记密码）
    public Result<String> getSecurityQuestion(String username) {
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            return Result.fail(404, "用户名不存在");
        }

        User user = userOpt.get();
        if (user.getSecurityQuestion() == null || user.getSecurityQuestion().isEmpty()) {
            return Result.fail(404, "该用户未设置密保问题");
        }

        return Result.success(user.getSecurityQuestion());
    }

    // 验证密保答案并重置密码
    public Result<Void> resetPassword(String username, String answer, String newPassword) {
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            return Result.fail(404, "用户名不存在");
        }

        User user = userOpt.get();

        if (user.getSecurityAnswer() == null) {
            return Result.fail(400, "该用户未设置密保问题");
        }

        if (!user.getSecurityAnswer().equals(answer)) {
            return Result.fail(401, "密保答案错误");
        }

        if (newPassword == null || newPassword.length() < 6) {
            return Result.fail(400, "新密码长度至少6位");
        }

        // 使用 BCrypt 加密新密码
        String hashedPassword = BCrypt.hashpw(newPassword, BCrypt.gensalt());
        user.setPassword(hashedPassword);
        userRepository.save(user);

        return Result.success(null);
    }

    // ========== 标签管理相关方法 ==========

    // 获取用户的所有标签
    public Result<List<TagDTO>> getTags(Long userId) {
        List<TaskTag> tags = taskTagRepository.findByUserIdOrderBySortOrderAsc(userId);
        List<TagDTO> tagDTOs = tags.stream().map(TagDTO::new).collect(Collectors.toList());
        return Result.success(tagDTOs);
    }

    // 添加标签
    public Result<TagDTO> addTag(Long userId, String tagName) {
        if (tagName == null || tagName.trim().isEmpty()) {
            return Result.fail(400, "标签名不能为空");
        }

        if (taskTagRepository.existsByUserIdAndTagName(userId, tagName)) {
            return Result.fail(409, "标签已存在");
        }

        TaskTag tag = new TaskTag(userId, tagName);
        TaskTag savedTag = taskTagRepository.save(tag);
        return Result.success(new TagDTO(savedTag));
    }

    // 删除标签
    public Result<Void> deleteTag(Long userId, String tagName) {
        if (tagName == null || tagName.trim().isEmpty()) {
            return Result.fail(400, "标签名不能为空");
        }

        taskTagRepository.deleteByUserIdAndTagName(userId, tagName);
        return Result.success(null);
    }

    // 上传头像
    public Result<String> uploadAvatar(Long userId, MultipartFile file) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            return Result.fail(404, "用户不存在");
        }

        User user = userOpt.get();

        // 检查文件类型
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return Result.fail(400, "只能上传图片文件");
        }

        // 检查文件大小（限制2MB）
        if (file.getSize() > 2 * 1024 * 1024) {
            return Result.fail(400, "图片大小不能超过2MB");
        }

        try {
            // 确保目录存在
            File avatarDir = new File(FileUploadConfig.getAvatarPath());
            if (!avatarDir.exists()) {
                avatarDir.mkdirs();
            }

            // 生成唯一文件名
            String extension = getFileExtension(file.getOriginalFilename());
            String fileName = userId + "_" + UUID.randomUUID().toString() + extension;
            String filePath = FileUploadConfig.getAvatarPath() + fileName;

            // 删除旧头像
            if (user.getAvatarUrl() != null && !user.getAvatarUrl().isEmpty()) {
                String oldFileName = user.getAvatarUrl().substring(user.getAvatarUrl().lastIndexOf("/") + 1);
                File oldFile = new File(FileUploadConfig.getAvatarPath() + oldFileName);
                if (oldFile.exists()) {
                    oldFile.delete();
                }
            }

            // 保存新头像
            Path path = Paths.get(filePath);
            Files.write(path, file.getBytes());

            // 保存头像URL（相对路径，用于前端访问）
            String avatarUrl = "/avatars/" + fileName;
            user.setAvatarUrl(avatarUrl);
            userRepository.save(user);

            System.out.println("保存的头像URL: " + avatarUrl);

            return Result.success(avatarUrl);

        } catch (IOException e) {
            e.printStackTrace();
            return Result.fail(500, "头像保存失败");
        }
    }

    // 获取文件扩展名
    private String getFileExtension(String filename) {
        if (filename == null || filename.lastIndexOf(".") == -1) {
            return ".jpg";
        }
        return filename.substring(filename.lastIndexOf("."));
    }

    // 上传附件
    public Result<AttachmentDTO> uploadAttachment(Long taskId, Long userId, MultipartFile file) {
        // 检查任务是否存在
        EventItem task = eventItemRepository.findById(userId, taskId);
        if (task == null) {
            return Result.fail(404, "任务不存在");
        }

        // 检查文件大小（限制10MB）
        if (file.getSize() > 10 * 1024 * 1024) {
            return Result.fail(400, "文件大小不能超过10MB");
        }

        try {
            // 确保目录存在
            File attachmentDir = new File(AttachmentConfig.getAttachmentPath());
            if (!attachmentDir.exists()) {
                attachmentDir.mkdirs();
            }

            // 生成唯一文件名
            String originalFileName = file.getOriginalFilename();
            String extension = "";
            if (originalFileName != null && originalFileName.lastIndexOf(".") > 0) {
                extension = originalFileName.substring(originalFileName.lastIndexOf("."));
            }
            String savedFileName = UUID.randomUUID().toString() + extension;
            String filePath = AttachmentConfig.getAttachmentPath() + savedFileName;

            // 保存文件
            Path path = Paths.get(filePath);
            Files.write(path, file.getBytes());

            // 保存附件记录
            TaskAttachment attachment = new TaskAttachment();
            attachment.setTaskId(taskId);
            attachment.setUserId(userId);
            attachment.setFileName(originalFileName);
            attachment.setFilePath(savedFileName);
            attachment.setFileSize(file.getSize());
            attachment.setFileType(file.getContentType());
            attachment.setUploadTime(LocalDateTime.now());

            TaskAttachment saved = attachmentRepository.save(attachment);

            return Result.success(new AttachmentDTO(saved));

        } catch (IOException e) {
            e.printStackTrace();
            return Result.fail(500, "文件上传失败");
        }
    }

    // 获取任务的所有附件
    public Result<List<AttachmentDTO>> getAttachments(Long taskId, Long userId) {
        EventItem task = eventItemRepository.findById(userId, taskId);
        if (task == null) {
            return Result.fail(404, "任务不存在");
        }

        List<TaskAttachment> attachments = attachmentRepository.findByTaskId(taskId);
        List<AttachmentDTO> dtos = attachments.stream()
                .map(AttachmentDTO::new)
                .collect(Collectors.toList());

        return Result.success(dtos);
    }

    // 删除附件
    public Result<Void> deleteAttachment(Long attachmentId, Long userId) {
        Optional<TaskAttachment> opt = attachmentRepository.findById(attachmentId);
        if (opt.isEmpty()) {
            return Result.fail(404, "附件不存在");
        }

        TaskAttachment attachment = opt.get();
        if (!attachment.getUserId().equals(userId)) {
            return Result.fail(403, "无权删除此附件");
        }

        // 删除文件
        String filePath = AttachmentConfig.getAttachmentPath() + attachment.getFilePath();
        File file = new File(filePath);
        if (file.exists()) {
            file.delete();
        }

        // 删除记录
        attachmentRepository.deleteById(attachmentId);

        return Result.success(null);
    }
}