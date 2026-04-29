package com.example.demo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService { // 文件名和类名一致
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private TaskRepository taskRepository;
    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Transactional(rollbackFor = Exception.class)
    public Result<User> register(String username, String password) {
        User existUser = userRepository.findByUsername(username);
        if (existUser != null) {
            return Result.fail(409, "用户名已存在");
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        userRepository.save(user);

        taskRepository.createTable(username);

        user.setPassword(null);
        return Result.success(user);
    }

    public Result<String> login(String username, String password) {
        User user = userRepository.findByUsername(username);
        if (user == null || !passwordEncoder.matches(password, user.getPassword())) {
            return Result.fail(401, "用户名或密码错误");
        }
        return Result.success("登录成功（此处后续可替换为JWT token）");
    }
}