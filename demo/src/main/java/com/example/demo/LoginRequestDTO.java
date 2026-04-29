package com.example.demo;

public class LoginRequestDTO {
    private String username;
    private String password;

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}

// 登录响应对象
class LoginResponseDTO {
    private Long userId;
    private String username;
    private String token;
    private Integer totalDays;

    public LoginResponseDTO(User user, String token) {
        this.userId = user.getId();
        this.username = user.getUsername();
        this.token = token;
        this.totalDays = user.getTotalDays();
    }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public Integer getTotalDays() { return totalDays; }
    public void setTotalDays(Integer totalDays) { this.totalDays = totalDays; }
}