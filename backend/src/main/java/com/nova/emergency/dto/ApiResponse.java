package com.nova.emergency.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;

@Data
public class ApiResponse<T> {
    private T data;
    private boolean success;
    private String code;
    private String message;
    private String timestamp;
    private Meta meta;

    public ApiResponse() {}

    public ApiResponse(T data, boolean success, String code, String message, String timestamp, Meta meta) {
        this.data = data;
        this.success = success;
        this.code = code;
        this.message = message;
        this.timestamp = timestamp;
        this.meta = meta;
    }

    public T getData() { return data; }
    public void setData(T data) { this.data = data; }
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
    public Meta getMeta() { return meta; }
    public void setMeta(Meta meta) { this.meta = meta; }

    @Data
            public static class Meta {
        private Long total;
        private Integer page;
        private Integer pageSize;

        public Meta() {}

        public Meta(Long total, Integer page, Integer pageSize) {
            this.total = total;
            this.page = page;
            this.pageSize = pageSize;
        }

        public Long getTotal() { return total; }
        public void setTotal(Long total) { this.total = total; }
        public Integer getPage() { return page; }
        public void setPage(Integer page) { this.page = page; }
        public Integer getPageSize() { return pageSize; }
        public void setPageSize(Integer pageSize) { this.pageSize = pageSize; }
    }

    public static <T> ApiResponse<T> ok(T data) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setData(data);
        response.setSuccess(true);
        response.setTimestamp(Instant.now().toString());
        return response;
    }

    public static <T> ApiResponse<T> ok(T data, String message) {
        ApiResponse<T> response = ok(data);
        response.setMessage(message);
        return response;
    }

    public static <T> ApiResponse<T> error(String message) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setSuccess(false);
        response.setMessage(message);
        response.setTimestamp(Instant.now().toString());
        return response;
    }

    public static <T> ApiResponse<T> error(String code, String message) {
        ApiResponse<T> response = error(message);
        response.setCode(code);
        return response;
    }
}
