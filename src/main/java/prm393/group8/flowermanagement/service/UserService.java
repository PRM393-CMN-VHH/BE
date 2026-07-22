package prm393.group8.flowermanagement.service;

import prm393.group8.flowermanagement.entity.User;
import org.springframework.data.domain.Page;

import java.util.List;

public interface UserService {
    User getByPhoneNumber(String phoneNumber);
    User getByEmail(String email);
    void addUser(User user);
    User getUserByEmailAndPassword(String email, String password);
    void updateProfile(User user);
    User findById(int userId);
    List<User> getAllUser();
    Page<User> getPaginatedUsers(int pageNo, int pageSize);
    Page<User> searchPaginatedUsersByFullName(String fullName, int pageNo, int pageSize);
    void saveUser(User user);
}
