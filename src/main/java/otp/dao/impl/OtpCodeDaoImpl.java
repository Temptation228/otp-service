package otp.dao.impl;

import otp.config.DatabaseManager;
import otp.dao.OtpCodeDao;
import otp.model.OtpCode;
import otp.model.OtpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class OtpCodeDaoImpl implements OtpCodeDao {
    private static final Logger logger = LoggerFactory.getLogger(OtpCodeDaoImpl.class);

    private static final String INSERT_SQL =
            "INSERT INTO otp_codes (user_id, operation_id, code, status, created_at) VALUES (?, ?, ?, ?, ?)";
    private static final String SELECT_BY_CODE_SQL =
            "SELECT id, user_id, operation_id, code, status, created_at FROM otp_codes WHERE code = ?";
    private static final String SELECT_BY_USER_SQL =
            "SELECT id, user_id, operation_id, code, status, created_at FROM otp_codes WHERE user_id = ?";
    private static final String UPDATE_MARK_USED_SQL =
            "UPDATE otp_codes SET status = 'USED' WHERE id = ?";
    private static final String UPDATE_MARK_EXPIRED_SQL =
            "UPDATE otp_codes SET status = 'EXPIRED' WHERE status = 'ACTIVE' AND created_at < ?";
    private static final String DELETE_BY_USER_SQL =
            "DELETE FROM otp_codes WHERE user_id = ?";

    @Override
    public void save(OtpCode code) {
        logger.debug("Сохранение OTP кода: {}", code.getCode());

        if (code.getCreatedAt() == null) {
            code.setCreatedAt(LocalDateTime.now());
            logger.debug("Время создания OTP кода установлено на текущее время: {}", code.getCreatedAt());
        }

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {

            ps.setLong(1, code.getUserId());
            if (code.getOperationId() != null) {
                ps.setString(2, code.getOperationId());
                logger.debug("operation_id установлен в: {}", code.getOperationId());
            } else {
                ps.setNull(2, Types.VARCHAR);
                logger.debug("operation_id установлен в NULL");
            }

            ps.setString(3, code.getCode());
            ps.setString(4, code.getStatus().name());
            ps.setTimestamp(5, Timestamp.valueOf(code.getCreatedAt()));

            int affectedRows = ps.executeUpdate();

            if (affectedRows == 0) {
                logger.warn("Сохранение OTP кода {} не удалось, ни одна строка не затронута.", code.getCode());
                throw new SQLException("Ошибка при сохранении OTP кода, не затронуто ни одной строки.");
            }

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    code.setId(keys.getLong(1));
                    logger.debug("Сгенерированный ID для OTP кода {}: {}", code.getCode(), code.getId());
                } else {
                    logger.warn("Не удалось получить сгенерированный ID для OTP кода {}.", code.getCode());
                }
            }

            logger.info("OTP код {} успешно сохранен", code.getCode());

        } catch (SQLException e) {
            logger.error("Ошибка при сохранении OTP кода [{}]: {}", code.getCode(), e.getMessage());
            throw new RuntimeException("Ошибка при сохранении OTP кода", e);
        }
    }

    @Override
    public OtpCode findByCode(String code) {
        logger.debug("Поиск OTP кода по коду: {}", code);

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_BY_CODE_SQL)) {

            ps.setString(1, code);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    OtpCode found = mapRow(rs);
                    logger.info("OTP код {} найден", code);
                    return found;
                } else {
                    logger.info("OTP код {} не найден", code);
                }
            }

        } catch (SQLException e) {
            logger.error("Ошибка при поиске OTP кода [{}]: {}", code, e.getMessage());
            throw new RuntimeException("Ошибка при поиске OTP кода в базе данных", e);
        }
        return null;
    }

    @Override
    public List<OtpCode> findAllByUser(Long userId) {
        logger.debug("Поиск всех OTP кодов для пользователя с ID: {}", userId);

        List<OtpCode> list = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_BY_USER_SQL)) {

            ps.setLong(1, userId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }

            logger.info("Найдено {} OTP кодов для пользователя с ID {}", list.size(), userId);

        } catch (SQLException e) {
            logger.error("Ошибка при поиске OTP кодов для пользователя с ID [{}]: {}", userId, e.getMessage());
            throw new RuntimeException("Ошибка при поиске OTP кодов для пользователя", e);
        }

        return list;
    }

    @Override
    public void markAsUsed(Long id) {
        logger.debug("Пометка OTP кода с ID {} как ИСПОЛЬЗОВАННЫЙ", id);

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(UPDATE_MARK_USED_SQL)) {

            ps.setLong(1, id);
            int affectedRows = ps.executeUpdate();

            logger.info("OTP код с ID {} помечен как ИСПОЛЬЗОВАННЫЙ (затронуто {} строк)", id, affectedRows);

        } catch (SQLException e) {
            logger.error("Ошибка при пометке OTP кода с ID [{}] как ИСПОЛЬЗОВАННЫЙ: {}", id, e.getMessage());
            throw new RuntimeException("Ошибка при пометке OTP кода как использованного", e);
        }
    }

    @Override
    public void markAsExpiredOlderThan(Duration ttl) {
        logger.debug("Пометка OTP кодов как УСТАРЕВШИЕ, старше чем {} секунд", ttl.getSeconds());

        LocalDateTime threshold = LocalDateTime.now().minus(ttl);

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(UPDATE_MARK_EXPIRED_SQL)) {

            ps.setTimestamp(1, Timestamp.valueOf(threshold));
            int affectedRows = ps.executeUpdate();

            logger.info("Помечено {} OTP кодов как УСТАРЕВШИЕ, старше чем {}", affectedRows, threshold);

        } catch (SQLException e) {
            logger.error("Ошибка при пометке устаревших OTP кодов, старше чем {}: {}", threshold, e.getMessage());
            throw new RuntimeException("Ошибка при пометке устаревших OTP кодов", e);
        }
    }

    @Override
    public void deleteAllByUserId(Long userId) {
        logger.debug("Удаление всех OTP кодов для пользователя с ID: {}", userId);

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(DELETE_BY_USER_SQL)) {

            ps.setLong(1, userId);
            int affectedRows = ps.executeUpdate();

            logger.info("Удалено {} OTP кодов для пользователя с ID {}", affectedRows, userId);

        } catch (SQLException e) {
            logger.error("Ошибка при удалении OTP кодов для пользователя с ID [{}]: {}", userId, e.getMessage());
            throw new RuntimeException("Ошибка при удалении OTP кодов для пользователя", e);
        }
    }

    private OtpCode mapRow(ResultSet rs) throws SQLException {
        OtpCode code = new OtpCode();
        code.setId(rs.getLong("id"));
        code.setUserId(rs.getLong("user_id"));
        String op = rs.getString("operation_id");
        code.setOperationId(op != null ? op : null);
        code.setCode(rs.getString("code"));
        code.setStatus(OtpStatus.valueOf(rs.getString("status")));
        Timestamp ts = rs.getTimestamp("created_at");
        code.setCreatedAt(ts != null ? ts.toLocalDateTime() : null);
        return code;
    }
}

