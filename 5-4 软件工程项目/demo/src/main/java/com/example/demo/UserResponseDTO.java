package com.example.demo;

public class UserResponseDTO {
    private Long userId;
    private String username;
    private String token;
    private Integer totalDays;
    private String avatarUrl;
    private String securityQuestion;

    public UserResponseDTO(User user) {
        this.userId = user.getId();
        this.username = user.getUsername();
        this.totalDays = user.getTotalDays();
        this.avatarUrl = user.getAvatarUrl();
        this.securityQuestion = user.getSecurityQuestion();
    }

    public UserResponseDTO(User user, String token) {
        this.userId = user.getId();
        this.username = user.getUsername();
        this.token = token;
        this.totalDays = user.getTotalDays();
        this.avatarUrl = user.getAvatarUrl();
        this.securityQuestion = user.getSecurityQuestion();
    }

    // Getters and Setters
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public Integer getTotalDays() { return totalDays; }
    public void setTotalDays(Integer totalDays) { this.totalDays = totalDays; }

    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

    public String getSecurityQuestion() { return securityQuestion; }
    public void setSecurityQuestion(String securityQuestion) { this.securityQuestion = securityQuestion; }
}