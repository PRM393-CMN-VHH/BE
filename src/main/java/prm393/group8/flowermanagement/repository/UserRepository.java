package prm393.group8.flowermanagement.repository;

import prm393.group8.flowermanagement.entity.Product;
import prm393.group8.flowermanagement.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {
    public User findByPhoneNumber(String phoneNumber);
    public User findByEmail(String email);
    public User findByEmailAndPassword(String email, String password);
    public Page<User> findUsersByFullNameContainingIgnoreCase(String fullName, Pageable pageable);
    public List<User> findByRole_RoleNameIgnoreCase(String roleName);
}
