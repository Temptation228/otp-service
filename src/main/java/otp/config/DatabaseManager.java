package otp.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class DatabaseManager {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseManager.class);

    private static final String PROPS_FILE = "application.properties";
    private static String url;
    private static String user;
    private static String password;

    static {
        logger.debug("Инициализация параметров подключения к базе данных");
        try (InputStream is = DatabaseManager.class
                .getClassLoader()
                .getResourceAsStream(PROPS_FILE)) {
            if (is == null) {
                String errorMessage = "Не найден файл " + PROPS_FILE + " в classpath";
                logger.error(errorMessage);
                throw new ExceptionInInitializerError(errorMessage);
            }
            Properties props = new Properties();
            props.load(is);
            url = props.getProperty("db.url");
            user = props.getProperty("db.user");
            password = props.getProperty("db.password");

            logger.debug("Параметры подключения к базе данных успешно загружены из {}: URL={}, User={}", PROPS_FILE, url, user);

        } catch (IOException e) {
            String errorMessage = "Ошибка загрузки параметров БД из " + PROPS_FILE + ": " + e.getMessage();
            logger.error(errorMessage, e);
            throw new ExceptionInInitializerError(errorMessage);
        }
    }

    public static Connection getConnection() throws SQLException {
        logger.debug("Получение соединения с базой данных");
        try {
            Connection conn = DriverManager.getConnection(url, user, password);
            logger.debug("Соединение с базой данных успешно установлено");
            return conn;
        } catch (SQLException e) {
            logger.error("Ошибка при получении соединения с базой данных: {}", e.getMessage());
            throw e;
        }
    }

    public static void close(AutoCloseable... resources) {
        logger.debug("Закрытие ресурсов базы данных");
        for (AutoCloseable r : resources) {
            if (r != null) {
                try {
                    r.close();
                    logger.debug("Ресурс {} успешно закрыт", r.getClass().getName());
                } catch (Exception e) {
                    logger.warn("Ошибка при закрытии ресурса {}: {}", r.getClass().getName(), e.getMessage());
                }
            }
        }
    }
}
