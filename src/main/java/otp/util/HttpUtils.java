package otp.util;

import com.sun.net.httpserver.HttpExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class HttpUtils {

    private static final Logger logger = LoggerFactory.getLogger(HttpUtils.class);

    public static void sendJsonResponse(HttpExchange exch, int status, String json) throws IOException {
        try {
            exch.getResponseHeaders().set("Content-Type", "application/json");
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            exch.sendResponseHeaders(status, bytes.length);
            try (OutputStream os = exch.getResponseBody()) {
                os.write(bytes);
            }
            logger.debug("Отправлен JSON ответ. Статус: {}, Длина: {}, Путь: {}", status, bytes.length, exch.getRequestURI().getPath());
        } catch (IOException e) {
            logger.error("Ошибка при отправке JSON ответа. Статус: {}, Путь: {}", status, exch.getRequestURI().getPath(), e);
            throw e;
        }
    }

    public static void sendEmptyResponse(HttpExchange exch, int status) throws IOException {
        try {
            exch.sendResponseHeaders(status, -1);
            logger.debug("Отправлен пустой ответ. Статус: {}, Путь: {}", status, exch.getRequestURI().getPath());
        } catch (IOException e) {
            logger.error("Ошибка при отправке пустого ответа. Статус: {}, Путь: {}", status, exch.getRequestURI().getPath(), e);
            throw e;
        }
    }

    public static void sendError(HttpExchange exch, int status, String message) throws IOException {
        try {
            String errorJson = String.format("{\"error\":\"%s\"}", message);
            sendJsonResponse(exch, status, errorJson);
            logger.warn("Отправлен ответ об ошибке. Статус: {}, Сообщение: {}, Путь: {}", status, message, exch.getRequestURI().getPath());
        } catch (IOException e) {
            logger.error("Ошибка при отправке ответа об ошибке. Статус: {}, Сообщение: {}, Путь: {}", status, message, exch.getRequestURI().getPath(), e);
            throw e;
        }
    }
}


