package otp.service.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;


public class FileService implements NotificationService {
    private static final Logger logger = LoggerFactory.getLogger(FileService.class);
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public void sendCode(String recipientPath, String code) {
        logger.debug("Запись OTP кода {} в файл {}", code, recipientPath);
        Path path = Paths.get(recipientPath);
        String entry = String.format("%s - OTP: %s%n",
                LocalDateTime.now().format(TIMESTAMP_FORMAT),
                code);
        try {
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }
            Files.write(path, entry.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            logger.info("OTP код записан в файл {}", recipientPath);
        } catch (IOException e) {
            logger.error("Не удалось записать OTP в файл {}", recipientPath, e);
            throw new RuntimeException("Ошибка записи в файл", e);
        }
    }
}

