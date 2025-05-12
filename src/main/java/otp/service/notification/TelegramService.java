package otp.service.notification;

import org.apache.http.client.utils.URIBuilder;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Properties;

public class TelegramService implements NotificationService {
    private static final Logger logger = LoggerFactory.getLogger(TelegramService.class);

    private final String apiBaseUrl;
    private final String token;
    private final String defaultChatId;

    public TelegramService() {
        Properties props = loadConfig();
        this.apiBaseUrl = props.getProperty("telegram.apiUrl");
        this.token = props.getProperty("telegram.token");
        this.defaultChatId = props.getProperty("telegram.chatId");

        logger.info("Сервис уведомлений Telegram инициализирован.  API Base URL: {}, Default Chat ID: {}", apiBaseUrl, defaultChatId);
    }

    private Properties loadConfig() {
        logger.debug("Загрузка конфигурации Telegram из telegram.properties");
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("telegram.properties")) {
            if (is == null) {
                logger.error("Файл telegram.properties не найден");
                throw new IllegalStateException("telegram.properties not found in classpath");
            }
            Properties props = new Properties();
            props.load(is);
            logger.debug("Конфигурация Telegram успешно загружена");
            return props;
        } catch (Exception e) {
            logger.error("Ошибка при загрузке telegram.properties", e);
            throw new RuntimeException("Не удалось загрузить конфигурацию Telegram", e);
        }
    }

    @Override
    public void sendCode(String recipientChatId, String code) {
        String chatId = (recipientChatId == null || recipientChatId.isBlank())
                ? defaultChatId
                : recipientChatId;
        String text = "Ваш одноразовый код подтверждения: " + code;

        logger.debug("Отправка OTP кода {} через Telegram в чат ID: {}", code, chatId);

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            URI uri = new URIBuilder(apiBaseUrl + token + "/sendMessage")
                    .addParameter("chat_id", chatId)
                    .addParameter("text", text)
                    .build();

            HttpGet request = new HttpGet(uri);
            logger.debug("Выполнение запроса Telegram API: {}", uri);

            try (CloseableHttpResponse response = httpClient.execute(request)) {
                int status = response.getStatusLine().getStatusCode();
                logger.debug("Получен ответ от Telegram API. Статус код: {}", status);
                if (status != 200) {
                    logger.error("Ошибка Telegram API.  Статус код: {}", status);
                    throw new RuntimeException("Telegram API returned " + status);
                }
                logger.info("OTP код отправлен через Telegram в чат ID {}", chatId);
            }
        } catch (URISyntaxException e) {
            logger.error("Неверный URI для Telegram API", e);
            throw new RuntimeException("Неверный URI Telegram API", e);
        } catch (Exception e) {
            logger.error("Не удалось отправить сообщение Telegram в чат ID: {}", chatId, e);
            throw new RuntimeException("Ошибка при отправке Telegram", e);
        }
    }
}

