package otp.dao.impl;

import otp.config.DatabaseManager;
import otp.dao.OtpConfigDao;
import otp.model.OtpConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;

public class OtpConfigDaoImpl implements OtpConfigDao {
    private static final Logger logger = LoggerFactory.getLogger(OtpConfigDaoImpl.class);

    private static final String SELECT_CONFIG_SQL =
            "SELECT id, length, ttl_seconds FROM otp_config LIMIT 1";
    private static final String UPDATE_CONFIG_SQL =
            "UPDATE otp_config SET length = ?, ttl_seconds = ? WHERE id = ?";
    private static final String INSERT_DEFAULT_SQL =
            "INSERT INTO otp_config (length, ttl_seconds) VALUES (?, ?)";

    @Override
    public OtpConfig getConfig() {
        logger.debug("Получение конфигурации OTP из базы данных");
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_CONFIG_SQL);
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {
                OtpConfig cfg = new OtpConfig();
                cfg.setId(rs.getLong("id"));
                cfg.setLength(rs.getInt("length"));
                cfg.setTtlSeconds(rs.getInt("ttl_seconds"));
                logger.info("Загружена конфигурация OTP из базы данных: {}", cfg);
                return cfg;
            }
        } catch (SQLException e) {
            logger.error("Ошибка при загрузке конфигурации OTP: {}", e.getMessage(), e);
            throw new RuntimeException("Ошибка при загрузке конфигурации OTP из базы данных", e);
        }
        logger.warn("Конфигурация OTP не найдена в базе данных");
        return null;
    }

    @Override
    public void updateConfig(OtpConfig config) {
        logger.debug("Обновление конфигурации OTP в базе данных: {}", config);
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(UPDATE_CONFIG_SQL)) {

            ps.setInt(1, config.getLength());
            ps.setInt(2, config.getTtlSeconds());
            ps.setLong(3, config.getId());
            int affected = ps.executeUpdate();
            logger.info("Обновлена конфигурация OTP (id={}): length={}, ttlSeconds={} (затронуто {} строк)",
                    config.getId(), config.getLength(), config.getTtlSeconds(), affected);
        } catch (SQLException e) {
            logger.error("Ошибка при обновлении конфигурации OTP [{}]: {}", config, e.getMessage(), e);
            throw new RuntimeException("Ошибка при обновлении конфигурации OTP в базе данных", e);
        }
    }

    @Override
    public void initDefaultConfigIfEmpty() {
        logger.debug("Инициализация конфигурации OTP значениями по умолчанию, если она отсутствует");
        OtpConfig existing = getConfig();
        if (existing != null) {
            logger.info("Конфигурация OTP уже инициализирована: {}", existing);
            return;
        }
        int defaultLength = 6;
        int defaultTtl = 300;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(INSERT_DEFAULT_SQL, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, defaultLength);
            ps.setInt(2, defaultTtl);
            int affected = ps.executeUpdate();
            if (affected == 0) {
                throw new SQLException("Ошибка при вставке конфигурации OTP по умолчанию, не затронуто ни одной строки.");
            }
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    long newId = keys.getLong(1);
                    logger.info("Инициализирована конфигурация OTP по умолчанию (id={}, length={}, ttlSeconds={})",
                            newId, defaultLength, defaultTtl);
                }
            }
        } catch (SQLException e) {
            logger.error("Ошибка при инициализации конфигурации OTP по умолчанию: {}", e.getMessage(), e);
            throw new RuntimeException("Ошибка при инициализации конфигурации OTP по умолчанию", e);
        }
    }
}

