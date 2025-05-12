package otp.controllers;

import com.sun.net.httpserver.Filter;
import com.sun.net.httpserver.HttpExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import otp.model.User;
import otp.model.UserRole;
import otp.util.HttpUtils;
import otp.util.TokenManager;

import java.io.IOException;


public class AuthFilter extends Filter {
    private static final Logger logger = LoggerFactory.getLogger(AuthFilter.class);

    private final UserRole requiredRole;

    public AuthFilter(UserRole requiredRole) {
        this.requiredRole = requiredRole;
    }

    @Override
    public String description() {
        return "Аутентификация и проверка роли";
    }

    @Override
    public void doFilter(HttpExchange exchange, Chain chain) throws IOException {
        logger.debug("Выполнение фильтра аутентификации для роли {}", requiredRole);

        String authHeader = exchange.getRequestHeaders().getFirst("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            logger.warn("Отсутствует или неверный заголовок Authorization: {}", authHeader);
            HttpUtils.sendError(exchange, 401, "Missing or invalid Authorization header");
            return;
        }

        String token = authHeader.substring(7);
        logger.debug("Извлечен токен: {}", token);

        User user = TokenManager.getUser(token);
        if (user == null) {
            logger.warn("Неверный или устаревший токен: {}", token);
            HttpUtils.sendError(exchange, 401, "Invalid or expired token");
            return;
        }

        logger.debug("Пользователь {} успешно аутентифицирован", user.getUsername());

        if (user.getRole().ordinal() < requiredRole.ordinal()) {
            logger.warn("У пользователя {} недостаточно прав для доступа к ресурсу. Требуется роль: {}", user.getUsername(), requiredRole);
            HttpUtils.sendError(exchange, 403, "Forbidden");
            return;
        }

        exchange.setAttribute("user", user);
        logger.debug("Доступ разрешен для пользователя {} с ролью {}", user.getUsername(), user.getRole());
        chain.doFilter(exchange);
    }
}
