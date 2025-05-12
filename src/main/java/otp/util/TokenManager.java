package otp.util;

import otp.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;


public final class TokenManager {
    private static final Logger logger = LoggerFactory.getLogger(TokenManager.class);

    private static final Map<String, TokenInfo> tokens = new ConcurrentHashMap<>();

    private static final long TTL_MINUTES = 30;

    private TokenManager() {}

    public static String generateToken(User user) {
        String token = UUID.randomUUID().toString();
        Instant expiry = Instant.now().plus(TTL_MINUTES, ChronoUnit.MINUTES);
        tokens.put(token, new TokenInfo(user, expiry));
        logger.info("Сгенерирован токен {} для пользователя {} (действителен до {})", token, user.getUsername(), expiry);
        return token;
    }
    public static boolean validate(String token) {
        TokenInfo info = tokens.get(token);
        if (info == null) {
            logger.warn("Ошибка валидации токена: токен не найден");
            return false;
        }
        if (Instant.now().isAfter(info.expiry)) {
            tokens.remove(token);
            logger.warn("Токен {} истек {} и был удален из хранилища", token, info.expiry);
            return false;
        }
        logger.debug("Токен {} успешно прошел валидацию", token);
        return true;
    }
    public static User getUser(String token) {
        if (!validate(token)) {
            logger.debug("Невозможно получить пользователя по токену {}, так как токен не валиден", token);
            return null;
        }
        User user = tokens.get(token).user;
        logger.debug("По токену {} получен пользователь {}", token, user.getUsername());
        return user;
    }
    public static void revoke(String token) {
        if (tokens.remove(token) != null) {
            logger.info("Токен {} отозван", token);
        } else {
            logger.warn("Попытка отозвать токен {}, который не существует", token);
        }
    }
    private static class TokenInfo {
        final User user;
        final Instant expiry;

        TokenInfo(User user, Instant expiry) {
            this.user = user;
            this.expiry = expiry;
        }
    }
}
