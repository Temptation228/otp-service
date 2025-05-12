package otp.service;

import otp.dao.OtpConfigDao;
import otp.dao.OtpCodeDao;
import otp.dao.UserDao;
import otp.model.OtpConfig;
import otp.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class AdminService {
    private static final Logger logger = LoggerFactory.getLogger(AdminService.class);
    private final OtpConfigDao configDao;
    private final UserDao userDao;
    private final OtpCodeDao codeDao;

    public AdminService(OtpConfigDao configDao, UserDao userDao, OtpCodeDao codeDao) {
        this.configDao = configDao;
        this.userDao = userDao;
        this.codeDao = codeDao;
    }

    public void updateOtpConfig(int length, int ttlSeconds) {
        logger.debug("Попытка обновить конфигурацию OTP: length={}, ttlSeconds={}", length, ttlSeconds);
        OtpConfig cfg = new OtpConfig(1L, length, ttlSeconds);
        configDao.updateConfig(cfg);
        logger.info("Конфигурация OTP обновлена: length={}, ttlSeconds={}", length, ttlSeconds);
    }

    public List<User> getAllUsersWithoutAdmins() {
        logger.debug("Получение списка всех пользователей, кроме администраторов");
        List<User> users = userDao.findAllUsersWithoutAdmins();
        logger.debug("Найдено {} пользователей, не являющихся администраторами", users.size());
        return users;
    }

    public void deleteUserAndCodes(Long userId) {
        logger.debug("Удаление пользователя с ID {} и связанных с ним кодов OTP", userId);
        codeDao.deleteAllByUserId(userId);
        userDao.delete(userId);
        logger.info("Удален пользователь {} и его коды OTP", userId);
    }
}


