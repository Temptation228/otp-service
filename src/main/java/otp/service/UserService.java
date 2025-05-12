package otp.service;

import otp.dao.UserDao;
import otp.model.User;
import otp.model.UserRole;
import otp.util.PasswordEncoder;
import otp.util.TokenManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class UserService {
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    private final UserDao userDao;

    public UserService(UserDao userDao) {
        this.userDao = userDao;
    }

    public void register(String username, String password, UserRole role) {
        logger.debug("Попытка регистрации пользователя: {}", username);

        if (userDao.findByUsername(username) != null) {
            logger.warn("Попытка регистрации с существующим именем пользователя: {}", username);
            throw new IllegalArgumentException("Имя пользователя уже существует");
        }
        if (role == UserRole.ADMIN && adminExists()) {
            logger.warn("Попытка регистрации второго администратора: {}", username);
            throw new IllegalStateException("Администратор уже существует");
        }

        String hashed = PasswordEncoder.hash(password);
        User user = new User(null, username, hashed, role);
        userDao.create(user);
        logger.info("Зарегистрирован новый пользователь: {} с ролью {}", username, role);
    }
    public boolean adminExists() {
        List<User> users = userDao.findAllUsersWithoutAdmins();
        boolean isEmpty = users.isEmpty();
        logger.debug("Проверка существования администратора.  Результат: {}", isEmpty ? "Администратор не найден" : "Администратор существует");
        return isEmpty;
    }
    public String login(String username, String password) {
        logger.debug("Попытка входа пользователя: {}", username);
        User user = userDao.findByUsername(username);
        if (user == null) {
            logger.warn("Вход не удался: пользователь не найден {}", username);
            throw new IllegalArgumentException("Неверное имя пользователя или пароль");
        }
        if (!PasswordEncoder.matches(password, user.getPasswordHash())) {
            logger.warn("Вход не удался: неверный пароль для {}", username);
            throw new IllegalArgumentException("Неверное имя пользователя или пароль");
        }
        String token = TokenManager.generateToken(user);
        logger.info("Пользователь {} вошел в систему, сгенерирован токен", username);
        return token;
    }

    public User findById(Long id) {
        logger.debug("Поиск пользователя по ID: {}", id);
        User user = userDao.findById(id);
        if (user != null) {
            logger.debug("Пользователь с ID {} найден", id);
        } else {
            logger.debug("Пользователь с ID {} не найден", id);
        }
        return user;
    }

    public List<User> findAllWithoutAdmins() {
        logger.debug("Поиск всех пользователей, кроме администраторов");
        List<User> users = userDao.findAllUsersWithoutAdmins();
        logger.debug("Найдено {} пользователей, кроме администраторов", users.size());
        return users;
    }

    public void deleteUser(Long id) {
        logger.debug("Удаление пользователя с ID: {}", id);
        userDao.delete(id);
        logger.info("Удален пользователь с id {}", id);
    }
}
