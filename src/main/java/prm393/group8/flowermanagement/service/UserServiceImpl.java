package prm393.group8.flowermanagement.service;

import prm393.group8.flowermanagement.entity.User;
import prm393.group8.flowermanagement.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    public UserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public User getByPhoneNumber(String phoneNumber) {
        return userRepository.findByPhoneNumber(phoneNumber);
    }

    @Override
    public User getByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Override
    public void addUser(User user) {
        userRepository.save(user);
    }

    @Override
    public User getUserByEmailAndPassword(String email, String password) {
        return userRepository.findByEmailAndPassword(email, password);
    }

    // Cập nhật hồ sơ (chỉ sửa 3 trường được phép)
    @Override
    @Transactional
    public void updateProfile(User user) {
        User existing = userRepository.findById(user.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng"));

        // Cập nhật các trường cho phép thay đổi
        existing.setFullName(user.getFullName());
        existing.setPhoneNumber(user.getPhoneNumber());
        existing.setAddress(user.getAddress());

        // Lưu lại
        userRepository.save(existing);
    }

    @Override
    public User findById(int userId) {
        return userRepository.findById(userId).orElse(null);
    }

    @Override
    public List<User> getAllUser() {
        return userRepository.findAll();
    }

    @Override
    public Page<User> getPaginatedUsers(int pageNo, int pageSize) {
        Pageable pageable = PageRequest.of(pageNo - 1, pageSize);
        return userRepository.findAll(pageable);
    }

    @Override
    public Page<User> searchPaginatedUsersByFullName(String fullName, int pageNo, int pageSize) {
        Pageable pageable = PageRequest.of(pageNo - 1, pageSize);
        return userRepository.findUsersByFullNameContainingIgnoreCase(fullName, pageable);
    }

    @Override
    public void saveUser(User user) {
        userRepository.save(user);
    }
}
