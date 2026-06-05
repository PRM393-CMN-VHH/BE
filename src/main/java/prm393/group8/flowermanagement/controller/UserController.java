package prm393.group8.flowermanagement.controller;

import prm393.group8.flowermanagement.entity.Role;
import prm393.group8.flowermanagement.entity.User;
import prm393.group8.flowermanagement.service.RoleServiceImpl;
import prm393.group8.flowermanagement.service.UserServiceImpl;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@CrossOrigin(origins = "*")
public class UserController {

    private final UserServiceImpl userService;
    private final RoleServiceImpl roleService;

    public UserController(
            UserServiceImpl userService,
            RoleServiceImpl roleService
    ) {
        this.userService = userService;
        this.roleService = roleService;
    }

    @GetMapping("/register")
    public ResponseEntity<?> registerPage() {
        return ResponseEntity.ok(Map.of("message", "Please use POST /register to sign up."));
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerPost(
            @Valid @RequestBody(required = false) User userBody,
            @ModelAttribute User userParam,
            BindingResult bindingResult,
            HttpSession session
    ) {
        User user = userBody != null ? userBody : userParam;

        if (bindingResult.hasErrors() && userBody != null) {
            return ResponseEntity.badRequest().body(bindingResult.getAllErrors());
        }

        if (user.getPhoneNumber() == null || user.getPhoneNumber().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Phone number is required"));
        }

        if (userService.getByPhoneNumber(user.getPhoneNumber()) != null) {
            return ResponseEntity.badRequest().body(Map.of("phoneNumberExist", "Phone number is already in use"));
        }

        // Get role and check if it exists
        Role userRole = roleService.getByRoleId(2);
        if (userRole == null) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Default role not found. Please contact administrator."));
        }

        user.setStatus(true);
        user.setRole(userRole);
        userService.addUser(user);

        session.setAttribute("account", user);

        return ResponseEntity.ok(user);
    }

    @GetMapping("/login")
    public ResponseEntity<?> loginPage() {
        return ResponseEntity.ok(Map.of("message", "Please use POST /login with email and password."));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody(required = false) Map<String, String> credentials,
                                  @RequestParam(value = "email", required = false) String emailParam,
                                  @RequestParam(value = "password", required = false) String passwordParam,
                                  HttpSession session) {
        String email = credentials != null ? credentials.get("email") : emailParam;
        String password = credentials != null ? credentials.get("password") : passwordParam;

        if (email == null || password == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email and password are required"));
        }

        User user = userService.getUserByEmailAndPassword(email, password);

        if (user != null) {
            // Nếu là admin chuyển về trang dashboard
            if (user.getRole().getRoleName().equalsIgnoreCase("admin")) {
                session.setAttribute("adminInfo", user);
                return ResponseEntity.ok(Map.of("role", "admin", "redirect", "/admin/dashboard", "user", user));
            }
            if (!user.isStatus()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Tài khoản này đã bị vô hiệu hóa"));
            }
            // Lưu user entity trực tiếp vào session
            session.setAttribute("account", user);

            return ResponseEntity.ok(Map.of("role", "user", "redirect", "/home", "user", user));
        } else {
            return ResponseEntity.status(401).body(Map.of("error", "Email hoặc mật khẩu không đúng"));
        }
    }

    @GetMapping("/logout")
    public ResponseEntity<?> logoutGet(HttpSession session) {
        session.invalidate();
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logoutPost(HttpSession session) {
        session.invalidate();
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }

    @GetMapping("/api/users/me")
    public ResponseEntity<?> getCurrentUser(HttpSession session) {
        User user = (User) session.getAttribute("account");
        if (user == null) {
            user = (User) session.getAttribute("adminInfo");
        }
        if (user != null) {
            return ResponseEntity.ok(user);
        }
        return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
    }
}
