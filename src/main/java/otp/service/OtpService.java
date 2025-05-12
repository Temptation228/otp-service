package otp.service;

import otp.dao.OtpCodeDao;
import otp.dao.OtpConfigDao;
import otp.dao.UserDao;
import otp.model.OtpCode;
import otp.model.OtpConfig;
import otp.model.OtpStatus;
import otp.model.User;
import otp.service.notification.NotificationChannel;
import otp.service.notification.NotificationService;
import otp.service.notification.NotificationServiceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

public class OtpService {
    private static final Logger logger = LoggerFactory.getLogger(OtpService.class);
    private static final SecureRandom random = new SecureRandom();

    private final OtpCodeDao otpCodeDao;
    private final OtpConfigDao otpConfigDao;
    private final UserDao userDao;
    private final NotificationServiceFactory notificationFactory;

    public OtpService(OtpCodeDao otpCodeDao,
                      OtpConfigDao otpConfigDao,
                      UserDao userDao,
                      NotificationServiceFactory notificationFactory) {
        this.otpCodeDao = otpCodeDao;
        this.otpConfigDao = otpConfigDao;
        this.userDao = userDao;
        this.notificationFactory = notificationFactory;
    }
    public String generateOtp(Long userId, String operationId) {
        logger.debug("Генерация OTP для пользователя {} и операции {}", userId, operationId);

        OtpConfig config = otpConfigDao.getConfig();
        int length = config.getLength();

        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(random.nextInt(10));
        }
        String code = sb.toString();

        OtpCode otp = new OtpCode(
                null,
                userId,
                operationId,
                code,
                OtpStatus.ACTIVE,
                LocalDateTime.now()
        );
        otpCodeDao.save(otp);
        logger.info("Сгенерирован OTP {} для пользователя {} и операции {}", code, userId, operationId);
        return code;
    }
    public OtpConfig getConfig() {
        logger.debug("Получение конфигурации OTP");
        OtpConfig config = otpConfigDao.getConfig();
        logger.debug("Конфигурация OTP: {}", config);
        return config;
    }
    public void sendOtpToUser(Long userId, String operationId, NotificationChannel channel) {
        logger.debug("Отправка OTP пользователю {} по каналу {}", userId, channel);
        String code = generateOtp(userId, operationId);
        User user = userDao.findById(userId);
        if (user == null) {
            logger.error("sendOtpToUser: пользователь не найден, id={}", userId);
            throw new IllegalArgumentException("Пользователь не найден");
        }

        String recipient = user.getUsername();
        NotificationService svc = notificationFactory.getService(channel);
        svc.sendCode(recipient, code);
        logger.info("Отправлен OTP код для пользователя {} по каналу {}", userId, channel);
    }
    public boolean validateOtp(String inputCode) {
        logger.debug("Валидация OTP кода: {}", inputCode);
        OtpCode otp = otpCodeDao.findByCode(inputCode);
        if (otp == null) {
            logger.warn("validateOtp: код не найден {}", inputCode);
            return false;
        }
        if (otp.getStatus() != OtpStatus.ACTIVE) {
            logger.warn("validateOtp: код {} не активен (status={})", inputCode, otp.getStatus());
            return false;
        }
        OtpConfig config = otpConfigDao.getConfig();
        LocalDateTime expiry = otp.getCreatedAt().plusSeconds(config.getTtlSeconds());
        if (LocalDateTime.now().isAfter(expiry)) {
            otpCodeDao.markAsExpiredOlderThan(Duration.ofSeconds(config.getTtlSeconds()));
            logger.warn("validateOtp: код {} истек {}", inputCode, expiry);
            return false;
        }

        otpCodeDao.markAsUsed(otp.getId());
        logger.info("validateOtp: код {} подтвержден и помечен как ИСПОЛЬЗОВАННЫЙ", inputCode);
        return true;
    }
    public void markExpiredOtps() {
        logger.debug("Пометка устаревших OTP");
        OtpConfig config = otpConfigDao.getConfig();
        Duration ttl = Duration.ofSeconds(config.getTtlSeconds());
        otpCodeDao.markAsExpiredOlderThan(ttl);
        logger.info("markExpiredOtps: устаревшие коды старше {} секунд помечены", config.getTtlSeconds());
    }
}

