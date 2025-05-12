package otp.main;

import com.sun.net.httpserver.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import otp.controllers.Dispatcher;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.util.Properties;

public class Application {

    private static final Logger logger = LoggerFactory.getLogger(Application.class);

    public static void main(String[] args) {
        try {
            Properties config = new Properties();
            try (InputStream is = Application.class.getClassLoader()
                    .getResourceAsStream("application.properties")) {
                if (is != null) {
                    config.load(is);
                    logger.debug("Файл application.properties успешно загружен");
                } else {
                    logger.warn("Файл application.properties не найден, используются значения по умолчанию");
                }
            }

            int port = Integer.parseInt(config.getProperty("server.port", "8080"));
            logger.info("Запуск сервера на порту: {}", port);

            HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

            Dispatcher dispatcher = new Dispatcher();
            dispatcher.registerRoutes(server);

            server.start();
            logger.info("Сервер запущен на http://localhost:{}", port);

        } catch (IOException e) {
            logger.error("Не удалось запустить сервер: {}", e.getMessage());
            logger.error("Ошибка при запуске сервера", e);
            System.exit(1);
        }
    }
}
