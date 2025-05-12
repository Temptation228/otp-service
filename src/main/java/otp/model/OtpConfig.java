package otp.model;

import java.util.Objects;

public class OtpConfig {
    private Long id;
    private int length;
    private int ttlSeconds;
    public OtpConfig() {
    }

    public OtpConfig(Long id, int length, int ttlSeconds) {
        this.id = id;
        this.length = length;
        this.ttlSeconds = ttlSeconds;
    }
    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public int getLength() {
        return length;
    }
    public void setLength(int length) {
        this.length = length;
    }
    public int getTtlSeconds() {
        return ttlSeconds;
    }
    public void setTtlSeconds(int ttlSeconds) {
        this.ttlSeconds = ttlSeconds;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        OtpConfig that = (OtpConfig) o;
        return length == that.length
                && ttlSeconds == that.ttlSeconds
                && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, length, ttlSeconds);
    }

    @Override
    public String toString() {
        return "OtpConfig{" +
                "id=" + id +
                ", length=" + length +
                ", ttlSeconds=" + ttlSeconds +
                '}';
    }
}

