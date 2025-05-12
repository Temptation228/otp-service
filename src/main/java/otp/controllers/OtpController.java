package otp.controllers;

import com.sun.net.httpserver.HttpExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import otp.dao.impl.OtpCodeDaoImpl;
import otp.dao.impl.OtpConfigDaoImpl;
import otp.dao.impl.UserDaoImpl;
import otp.service.OtpService;
import otp.service.notification.NotificationChannel;
import otp.service.notification.NotificationServiceFactory;
import otp.util.JsonUtil;
import otp.util.HttpUtils;

import java.io.IOException;

public class OtpController {
    private static final Logger logger = LoggerFactory.getLogger(OtpController.class);

    private final OtpService otpService = new OtpService(
            new OtpCodeDaoImpl(),
            new OtpConfigDaoImpl(),
            new UserDaoImpl(),
            new NotificationServiceFactory()
    );

    public void generateOtp(HttpExchange exchange) throws IOException {
        logger.debug("Вызов метода generateOtp");

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
            GenerateRequest req = JsonUtil.fromJson(exchange.getRequestBody(), GenerateRequest.class);
            logger.debug("Получен запрос GenerateRequest: userId={}, operationId={}, channel={}",
                    req.userId, req.operationId, req.channel);

            otpService.sendOtpToUser(req.userId, req.operationId,
                    NotificationChannel.valueOf(req.channel));

            HttpUtils.sendEmptyResponse(exchange, 202);
            logger.info("Запрос на генерацию OTP для пользователя {} успешно обработан", req.userId);

        } catch (IllegalArgumentException e) {
            logger.warn("Ошибка в запросе: {}", e.getMessage());
            HttpUtils.sendError(exchange, 400, e.getMessage());

        } catch (Exception e) {
            logger.error("Внутренняя ошибка сервера при обработке запроса на генерацию OTP", e);
            HttpUtils.sendError(exchange, 500, "Internal server error");
        }
    }

    public void validateOtp(HttpExchange exchange) throws IOException {
        logger.debug("Вызов метода validateOtp");

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
            ValidateRequest req = JsonUtil.fromJson(exchange.getRequestBody(), ValidateRequest.class);
            logger.debug("Получен запрос ValidateRequest: code={}", req.code);

            boolean valid = otpService.validateOtp(req.code);
            if (valid) {
                HttpUtils.sendEmptyResponse(exchange, 200);
                logger.info("OTP код {} успешно проверен", req.code);
            } else {
                logger.warn("Неверный или устаревший OTP код: {}", req.code);
                HttpUtils.sendError(exchange, 400, "Invalid or expired code");
            }

        } catch (IllegalArgumentException e) {
            logger.warn("Ошибка в запросе: {}", e.getMessage());
            HttpUtils.sendError(exchange, 400, e.getMessage());

        } catch (Exception e) {
            logger.error("Внутренняя ошибка сервера при проверке OTP кода", e);
            HttpUtils.sendError(exchange, 500, "Internal server error");
        }
    }

    private static class GenerateRequest {
        public Long userId;
        public String operationId;
        public String channel;
    }

    private static class ValidateRequest {
        public String code;
    }
}
