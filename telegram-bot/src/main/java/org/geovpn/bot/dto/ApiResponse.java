package org.geovpn.bot.dto;
import lombok.Data;

@Data
public class ApiResponse<T> {
    private boolean success;
    private String message;
    private T data;
    private ErrorResponse error;

    public boolean isSuccess() { return success; }

    @Data
    public static class ErrorResponse {
        private String code;
        private String message;
    }
}