package otp.controllers;

import com.sun.net.httpserver.HttpExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import otp.dao.impl.OtpCodeDaoImpl;
import otp.dao.impl.OtpConfigDaoImpl;
import otp.dao.impl.UserDaoImpl;
import otp.model.User;
import otp.service.AdminService;
import otp.util.JsonUtil;
import otp.util.HttpUtils;

import java.io.IOException;
import java.net.URI;
import java.util.List;

public class AdminController {
    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);

    private final AdminService adminService = new AdminService(
            new OtpConfigDaoImpl(),
            new UserDaoImpl(),
            new OtpCodeDaoImpl()
    );

    public void updateOtpConfig(HttpExchange exchange) throws IOException {
        logger.debug("Вызов метода updateOtpConfig");

        if (!"PATCH".equalsIgnoreCase(exchange.getRequestMethod())) {
            logger.warn("Неверный метод запроса: {}. Ожидается PATCH", exchange.getRequestMethod());
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
            ConfigRequest req = JsonUtil.fromJson(exchange.getRequestBody(), ConfigRequest.class);
            logger.debug("Получен запрос ConfigRequest: length={}, ttlSeconds={}", req.length, req.ttlSeconds);

            adminService.updateOtpConfig(req.length, req.ttlSeconds);
            HttpUtils.sendEmptyResponse(exchange, 204);
            logger.info("Конфигурация OTP успешно обновлена: length={}, ttlSeconds={}", req.length, req.ttlSeconds);

        } catch (IllegalArgumentException e) {
            logger.warn("Ошибка в запросе: {}", e.getMessage());
            HttpUtils.sendError(exchange, 400, e.getMessage());

        } catch (Exception e) {
            logger.error("Внутренняя ошибка сервера при обновлении конфигурации OTP", e);
            HttpUtils.sendError(exchange, 500, "Internal server error");
        }
    }

    public void listUsers(HttpExchange exchange) throws IOException {
        logger.debug("Вызов метода listUsers");

        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            logger.warn("Неверный метод запроса: {}. Ожидается GET", exchange.getRequestMethod());
            HttpUtils.sendError(exchange, 405, "Method Not Allowed");
            return;
        }

        try {
            List<User> users = adminService.getAllUsersWithoutAdmins();
            String json = JsonUtil.toJson(users);
            HttpUtils.sendJsonResponse(exchange, 200, json);
            logger.info("Список пользователей успешно получен. Количество пользователей: {}", users.size());

        } catch (Exception e) {
            logger.error("Внутренняя ошибка сервера при получении списка пользователей", e);
            HttpUtils.sendError(exchange, 500, "Internal server error");
        }
    }

    public void deleteUser(HttpExchange exchange) throws IOException {
        logger.debug("Вызов метода deleteUser");

        if (!"DELETE".equalsIgnoreCase(exchange.getRequestMethod())) {
            logger.warn("Неверный метод запроса: {}. Ожидается DELETE", exchange.getRequestMethod());
            HttpUtils.sendError(exchange, 405, "Method Not Allowed");
            return;
        }

        try {
            URI uri = exchange.getRequestURI();
            String[] segments = uri.getPath().split("/");
            Long id = Long.valueOf(segments[segments.length - 1]);
            logger.debug("Получен ID пользователя для удаления: {}", id);

            adminService.deleteUserAndCodes(id);
            HttpUtils.sendEmptyResponse(exchange, 204);
            logger.info("Пользователь с ID {} и связанные с ним коды успешно удалены", id);

        } catch (NumberFormatException e) {
            logger.warn("Неверный формат ID пользователя");
            HttpUtils.sendError(exchange, 400, "Invalid user ID");

        } catch (IllegalArgumentException e) {
            logger.warn("Пользователь не найден: {}", e.getMessage());
            HttpUtils.sendError(exchange, 404, e.getMessage());

        } catch (Exception e) {
            logger.error("Внутренняя ошибка сервера при удалении пользователя", e);
            HttpUtils.sendError(exchange, 500, "Internal server error");
        }
    }

    private static class ConfigRequest {
        public int length;
        public int ttlSeconds;
    }
}
