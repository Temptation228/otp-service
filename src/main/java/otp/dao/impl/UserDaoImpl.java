package otp.dao.impl;

import otp.config.DatabaseManager;
import otp.dao.UserDao;
import otp.model.User;
import otp.model.UserRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserDaoImpl implements UserDao {
    private static final Logger logger = LoggerFactory.getLogger(UserDaoImpl.class);

    private static final String INSERT_SQL =
            "INSERT INTO users (username, password_hash, role) VALUES (?, ?, ?)";
    private static final String SELECT_BY_USERNAME_SQL =
            "SELECT id, username, password_hash, role FROM users WHERE username = ?";
    private static final String SELECT_BY_ID_SQL =
            "SELECT id, username, password_hash, role FROM users WHERE id = ?";
    private static final String SELECT_ALL_USERS_SQL =
            "SELECT id, username, password_hash, role FROM users WHERE role <> 'ADMIN'";
    private static final String SELECT_ADMIN_EXISTS_SQL =
            "SELECT 1 FROM users WHERE role = 'ADMIN' LIMIT 1";
    private static final String DELETE_USER_SQL =
            "DELETE FROM users WHERE id = ?";

    @Override
    public void create(User user) {
        logger.debug("Создание пользователя с именем: {}", user.getUsername());
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getPasswordHash());
            ps.setString(3, user.getRole().name());
            int affectedRows = ps.executeUpdate();

            if (affectedRows == 0) {
                logger.warn("Создание пользователя {} не удалось, ни одна строка не затронута.", user.getUsername());
                throw new SQLException("Ошибка при создании пользователя, не затронуто ни одной строки.");
            }

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    user.setId(keys.getLong(1));
                    logger.debug("Сгенерированный ID пользователя {} : {}", user.getUsername(), user.getId());
                } else {
                    logger.warn("Не удалось получить сгенерированный ID пользователя {}", user.getUsername());
                }
            }
            logger.info("Пользователь {} успешно создан", user.getUsername());

        } catch (SQLException e) {
            logger.error("Ошибка при создании пользователя {}: {}", user.getUsername(), e.getMessage());
            throw new RuntimeException("Ошибка при создании пользователя в базе данных", e);
        }
    }

    @Override
    public User findByUsername(String username) {
        logger.debug("Поиск пользователя по имени: {}", username);

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_BY_USERNAME_SQL)) {

            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    User user = mapRow(rs);
                    logger.info("Пользователь {} найден", username);
                    return user;
                } else {
                    logger.info("Пользователь {} не найден", username);
                }
            }

        } catch (SQLException e) {
            logger.error("Ошибка при поиске пользователя {}: {}", username, e.getMessage());
            throw new RuntimeException("Ошибка при поиске пользователя в базе данных", e);
        }

        return null;
    }

    @Override
    public User findById(Long id) {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_BY_ID_SQL)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    User user = mapRow(rs);
                    logger.info("Found user by id {}: {}", id, user);
                    return user;
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding user by id [{}]: {}", id, e.getMessage(), e);
            throw new RuntimeException(e);
        }
        return null;
    }

    @Override
    public List<User> findAllUsersWithoutAdmins() {
        logger.debug("Получение списка всех пользователей, кроме администраторов...");

        List<User> users = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_ALL_USERS_SQL);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                users.add(mapRow(rs));
            }

            logger.info("Найдено {} пользователей, не являющихся администраторами.", users.size());

        } catch (SQLException e) {
            logger.error("Ошибка при получении списка пользователей, не являющихся администраторами: {}", e.getMessage());
            throw new RuntimeException("Ошибка при получении списка пользователей, не являющихся администраторами", e);
        }

        return users;
    }

    @Override
    public boolean adminExists() {
        logger.debug("Проверка, существует ли администратор...");

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_ADMIN_EXISTS_SQL);
             ResultSet rs = ps.executeQuery()) {

            boolean exists = rs.next();
            logger.info("Администратор существует: {}", exists ? "Да" : "Нет");
            return exists;

        } catch (SQLException e) {
            logger.error("Ошибка при проверке существования администратора: {}", e.getMessage());
            throw new RuntimeException("Ошибка при проверке существования администратора", e);
        }
    }

    @Override
    public void delete(Long userId) {
        logger.debug("Удаление пользователя с ID: {}...", userId);

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(DELETE_USER_SQL)) {

            ps.setLong(1, userId);
            int affectedRows = ps.executeUpdate();

            logger.info("Пользователь с ID {} удален. Затронуто {} строк.", userId, affectedRows);

        } catch (SQLException e) {
            logger.error("Ошибка при удалении пользователя с ID [{}]: {}", userId, e.getMessage());
            throw new RuntimeException("Ошибка при удалении пользователя", e);
        }
    }

    private User mapRow(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getLong("id"));
        user.setUsername(rs.getString("username"));
        user.setPasswordHash(rs.getString("password_hash"));
        user.setRole(UserRole.valueOf(rs.getString("role")));
        return user;
    }
}

