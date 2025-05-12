package otp.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class PasswordEncoder {
    private static final Logger logger = LoggerFactory.getLogger(PasswordEncoder.class);

    private PasswordEncoder() {}

    public static String hash(String rawPassword) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(rawPassword.getBytes(StandardCharsets.UTF_8));
            String hashedPassword = bytesToHex(hashBytes);
            logger.debug("Пароль успешно захэширован");
            return hashedPassword;
        } catch (NoSuchAlgorithmException e) {
            logger.error("Не удалось получить алгоритм SHA-256 для хеширования пароля", e);
            throw new IllegalStateException("Алгоритм SHA-256 недоступен", e);
        }
    }

    public static boolean matches(String rawPassword, String storedHash) {
        if (storedHash == null || rawPassword == null) {
            logger.warn("Один из паролей (сырой или хэш) равен null, сравнение невозможно");
            return false;
        }

        String hashedPassword = hash(rawPassword);
        boolean match = hashedPassword.equalsIgnoreCase(storedHash);
        if (match) {
            logger.debug("Пароли совпадают");
        } else {
            logger.debug("Пароли не совпадают");
        }
        return match;
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}

