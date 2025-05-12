package otp.dao;

import otp.model.OtpCode;
import java.time.Duration;
import java.util.List;


public interface OtpCodeDao {
    void save(OtpCode code);
    OtpCode findByCode(String code);
    List<OtpCode> findAllByUser(Long userId);
    void markAsUsed(Long id);
    void markAsExpiredOlderThan(Duration ttl);
    void deleteAllByUserId(Long userId);
}

