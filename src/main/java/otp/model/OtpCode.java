package otp.model;

import java.time.LocalDateTime;
import java.util.Objects;

public class OtpCode {
    private Long id;
    private Long userId;
    private String operationId;
    private String code;
    private OtpStatus status;
    private LocalDateTime createdAt;

    public OtpCode() {
    }
    public OtpCode(Long id,
                   Long userId,
                   String operationId,
                   String code,
                   OtpStatus status,
                   LocalDateTime createdAt) {
        this.id = id;
        this.userId = userId;
        this.operationId = operationId;
        this.code = code;
        this.status = status;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getOperationId() {
        return operationId;
    }

    public void setOperationId(String operationId) {
        this.operationId = operationId;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public OtpStatus getStatus() {
        return status;
    }

    public void setStatus(OtpStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        OtpCode otpCode = (OtpCode) o;
        return Objects.equals(id, otpCode.id)
                && Objects.equals(userId, otpCode.userId)
                && Objects.equals(operationId, otpCode.operationId)
                && Objects.equals(code, otpCode.code)
                && status == otpCode.status
                && Objects.equals(createdAt, otpCode.createdAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, userId, operationId, code, status, createdAt);
    }

    @Override
    public String toString() {
        return "OtpCode{" +
                "id=" + id +
                ", userId=" + userId +
                ", operationId='" + operationId + '\'' +
                ", code='" + code + '\'' +
                ", status=" + status +
                ", createdAt=" + createdAt +
                '}';
    }
}

