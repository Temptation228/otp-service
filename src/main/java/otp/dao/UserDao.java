package otp.dao;

import otp.model.User;
import java.util.List;


public interface UserDao {
    void create(User user);
    User findByUsername(String username);
    User findById(Long id);
    List<User> findAllUsersWithoutAdmins();
    boolean adminExists();
    void delete(Long userId);
}
