package otp.controllers;
import com.sun.net.httpserver.HttpExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import otp.dao.impl.UserDaoImpl;
import otp.model.UserRole;
import otp.service.UserService;
import otp.util.JsonUtil;
import otp.util.HttpUtils;

import java.io.IOException;
import java.util.Map;


public class AuthController {
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final UserService userService = new UserService(new UserDaoImpl());

    public void handleRegister(HttpExchange exchange) throws IOException {
        logger.debug("Вызов метода handleRegister");

        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            logger.warn("Неверный метод запроса: {}. Ожидается POST", exchange.getRequestMethod());
            HttpUtils.sendError(exchange, 405, "Method Not Allowed");
            return;
        }

        String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
        if (contentType == null || !contentType.contains("application/json")) {
            logger.warn("Неверный Content-Type: {}. Ожидается application/json", contentType);
            HttpUtils.sendError(exchange, 415, "Content-Type must be application/json");
            return;
        }

        try {
            RegisterRequest req = JsonUtil.fromJson(exchange.getRequestBody(), RegisterRequest.class);
            logger.debug("Получен запрос RegisterRequest: username={}, role={}", req.username, req.role);

            if ("ADMIN".equals(req.role) && userService.adminExists()) {
                logger.warn("Попытка регистрации второго администратора");
                HttpUtils.sendError(exchange, 409, "Admin already exists");
                return;
            }

            userService.register(req.username, req.password, UserRole.valueOf(req.role));
            HttpUtils.sendEmptyResponse(exchange, 201);
            logger.info("Пользователь {} успешно зарегистрирован с ролью {}", req.username, req.role);

        } catch (IllegalArgumentException | IllegalStateException e) {
            logger.warn("Ошибка при регистрации: {}", e.getMessage());
            HttpUtils.sendError(exchange, 409, e.getMessage());

        } catch (Exception e) {
            logger.error("Внутренняя ошибка сервера при обработке запроса на регистрацию", e);
            HttpUtils.sendError(exchange, 500, "Internal server error");
        }
    }

    public void handleLogin(HttpExchange exchange) throws IOException {
        logger.debug("Вызов метода handleLogin");

        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            logger.warn("Неверный метод запроса: {}. Ожидается POST", exchange.getRequestMethod());
            HttpUtils.sendError(exchange, 405, "Method Not Allowed");
            return;
        }

        String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
        if (contentType == null || !contentType.contains("application/json")) {
            logger.warn("Неверный Content-Type: {}. Ожидается application/json", contentType);
            HttpUtils.sendError(exchange, 415, "Content-Type must be application/json");
            return;
        }

        try {
            LoginRequest req = JsonUtil.fromJson(exchange.getRequestBody(), LoginRequest.class);
            logger.debug("Получен запрос LoginRequest: username={}", req.username);

            String token = userService.login(req.username, req.password);
            if (token == null) {
                logger.warn("Неверное имя пользователя или пароль");
                HttpUtils.sendError(exchange, 401, "Unauthorized");
                return;
            }

            String json = JsonUtil.toJson(Map.of("token", token));
            HttpUtils.sendJsonResponse(exchange, 200, json);
            logger.info("Пользователь {} успешно вошел в систему", req.username);

        } catch (IllegalArgumentException e) {
            logger.warn("Ошибка при входе в систему: {}", e.getMessage());
            HttpUtils.sendError(exchange, 401, e.getMessage());

        } catch (Exception e) {
            logger.error("Внутренняя ошибка сервера при обработке запроса на вход", e);
            HttpUtils.sendError(exchange, 500, "Internal server error");
        }
    }

    private static class RegisterRequest {
        public String username;
        public String password;
        public String role;
    }

    private static class LoginRequest {
        public String username;
        public String password;
    }
}
