package otp.service.notification;

import org.smpp.TCPIPConnection;
import org.smpp.Session;
import org.smpp.pdu.BindResponse;
import org.smpp.pdu.BindTransmitter;
import org.smpp.pdu.SubmitSM;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class SmsService implements NotificationService {
    private static final Logger logger = LoggerFactory.getLogger(SmsService.class);

    private final String host;
    private final int port;
    private final String systemId;
    private final String password;
    private final String systemType;
    private final String sourceAddr;

    public SmsService() {
        Properties props = loadConfig();
        this.host = props.getProperty("smpp.host");
        this.port = Integer.parseInt(props.getProperty("smpp.port"));
        this.systemId = props.getProperty("smpp.system_id");
        this.password = props.getProperty("smpp.password");
        this.systemType = props.getProperty("smpp.system_type");
        this.sourceAddr = props.getProperty("smpp.source_addr");

        logger.info("Сервис уведомлений по SMS инициализирован. Хост: {}, Порт: {}, System ID: {}, Source Address: {}",
                host, port, systemId, sourceAddr);
    }

    private Properties loadConfig() {
        logger.debug("Загрузка конфигурации SMS из sms.properties");
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("sms.properties")) {
            if (is == null) {
                logger.error("Файл sms.properties не найден");
                throw new IllegalStateException("sms.properties not found");
            }
            Properties props = new Properties();
            props.load(is);
            logger.debug("Конфигурация SMS успешно загружена");
            return props;
        } catch (IOException e) {
            logger.error("Ошибка при загрузке sms.properties", e);
            throw new RuntimeException("Не удалось загрузить конфигурацию SMS", e);
        }
    }

    @Override
    public void sendCode(String recipientPhone, String code) {
        logger.debug("Отправка OTP кода {} по SMS на номер {}", code, recipientPhone);
        TCPIPConnection connection = null;
        Session session = null;
        try {
            connection = new TCPIPConnection(host, port);
            session = new Session(connection);

            BindTransmitter bindReq = new BindTransmitter();
            bindReq.setSystemId(systemId);
            bindReq.setPassword(password);
            bindReq.setSystemType(systemType);
            bindReq.setInterfaceVersion((byte) 0x34);
            bindReq.setAddressRange(sourceAddr);

            logger.debug("Отправка запроса BindTransmitter");
            BindResponse bindResp = session.bind(bindReq);
            if (bindResp.getCommandStatus() != 0) {
                logger.error("SMPP bind не удался. Код ошибки: {}", bindResp.getCommandStatus());
                throw new RuntimeException("SMPP bind failed: " + bindResp.getCommandStatus());
            }
            logger.debug("BindTransmitter успешно выполнен");

            SubmitSM submit = new SubmitSM();
            submit.setSourceAddr(sourceAddr);
            submit.setDestAddr(recipientPhone);
            submit.setShortMessage("Ваш OTP код: " + code);
            logger.debug("Отправка запроса SubmitSM");
            session.submit(submit);

            logger.info("OTP отправлен по SMS на номер {}", recipientPhone);
        } catch (Exception e) {
            logger.error("Не удалось отправить SMS на номер {}", recipientPhone, e);
            throw new RuntimeException("Ошибка при отправке SMS", e);
        } finally {
            if (session != null) try {
                logger.debug("Выполнение unbind сессии");
                session.unbind();
            } catch (Exception ignored) {
                logger.warn("Ошибка при выполнении unbind сессии", ignored);
            }
            if (connection != null) try {
                logger.debug("Закрытие соединения");
                connection.close();
            } catch (IOException ignored) {
                logger.warn("Ошибка при закрытии соединения", ignored);
            }
        }
    }
}