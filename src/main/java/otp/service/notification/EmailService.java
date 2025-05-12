package otp.service.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.mail.*;
import javax.mail.internet.*;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class EmailService implements NotificationService {
    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    private final Session session;
    private final String fromAddress;

    public EmailService() {
        Properties props = loadConfig();
        this.fromAddress = props.getProperty("email.from");
        this.session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(
                        props.getProperty("email.username"),
                        props.getProperty("email.password")
                );
            }
        });
        logger.info("Сервис уведомлений по Email инициализирован. Адрес отправителя: {}", fromAddress);
    }

    private Properties loadConfig() {
        logger.debug("Загрузка конфигурации email из email.properties");
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("email.properties")) {
            if (is == null) {
                logger.error("Файл email.properties не найден в classpath");
                throw new IllegalStateException("email.properties not found in classpath");
            }
            Properties props = new Properties();
            props.load(is);
            logger.debug("Конфигурация email успешно загружена");
            return props;
        } catch (IOException e) {
            logger.error("Не удалось загрузить email.properties", e);
            throw new RuntimeException("Could not load email configuration", e);
        }
    }

    @Override
    public void sendCode(String recipientEmail, String code) {
        logger.debug("Отправка OTP кода {} по Email на адрес {}", code, recipientEmail);
        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(fromAddress));
            message.setRecipient(Message.RecipientType.TO, new InternetAddress(recipientEmail));
            message.setSubject("Ваш OTP Код");
            message.setText("Ваш одноразовый код подтверждения: " + code);

            Transport.send(message);
            logger.info("OTP код отправлен по Email на адрес {}", recipientEmail);
        } catch (MessagingException e) {
            logger.error("Не удалось отправить OTP email на адрес {}", recipientEmail, e);
            throw new RuntimeException("Email sending failed", e);
        }
    }
}

