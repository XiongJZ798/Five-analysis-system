package com._5ganalysisrate.g5rate.dto;

// ApiResponse类用于封装API响应的数据结构
import lombok.Data;

@Data
public class ApiResponse<T> {
    private int code;// 响应码
    private String message;// 响应信息
    private T data;// 响应的数据

    // 静态方法用于创建成功的ApiResponse
    public static <T> ApiResponse<T> success(T data) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setCode(200);
        response.setMessage("success");
        response.setData(data);
        return response;
    }

    /**
     * 生成一个错误的响应对象
     *
     * @param code    错误代码
     * @param message 错误信息
     * @return 包含错误信息的响应对象
     */
    public static <T> ApiResponse<T> error(int code, String message) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setCode(code);
        response.setMessage(message);
        return response;
    }

    /**
     * 生成一个错误的响应对象（使用默认错误代码500）
     *
     * @param message 错误信息
     * @return 包含错误信息的响应对象
     */
    public static <T> ApiResponse<T> error(String message) {
        return error(500, message);
    }
} 