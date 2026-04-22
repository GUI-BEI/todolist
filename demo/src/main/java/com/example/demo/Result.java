package com.example.demo;
// 统一结果封装类，包含状态码、消息和数据
public class Result<T> {
    private Integer code;
    private String message;
    private T data;
    public Result(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
       }

    public static <T> Result<T> success(T data) {
        return new Result<>(200, "操作成功", data);
    }

    public static <T> Result<T> fail(Integer code, String message) {
        return new Result<>(code, message, null);
    }
      
    public Integer getCode() { return code; }
    public String getMessage() { return message; }
    public T getData() { return data; }
}
