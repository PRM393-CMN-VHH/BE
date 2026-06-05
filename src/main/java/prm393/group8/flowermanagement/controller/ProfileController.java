package prm393.group8.flowermanagement.controller;

import prm393.group8.flowermanagement.entity.User;
import prm393.group8.flowermanagement.service.UserServiceImpl;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/profile")
public class ProfileController {

    private final UserServiceImpl userService;

    public ProfileController(UserServiceImpl userService) {
        this.userService = userService;
    }

    // 1. [GET] /profile - Lấy hồ sơ cá nhân
    @GetMapping
    public ResponseEntity<?> profilePage(HttpSession session) {
        User account = (User) session.getAttribute("account");
        if (account == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not logged in"));
        }
        return ResponseEntity.ok(account);
    }

    // 2. [POST] /profile/update - Cập nhật hồ sơ
    @PostMapping("/update")
    public ResponseEntity<?> updateProfile(@Valid @RequestBody(required = false) User accountFormBody,
                                          @ModelAttribute User accountFormParam,
                                          BindingResult result,
                                          HttpSession session) {
        User accountForm = accountFormBody != null ? accountFormBody : accountFormParam;
        
        if (result.hasErrors() && accountFormBody != null) {
            return ResponseEntity.badRequest().body(result.getAllErrors());
        }

        User currentUser = (User) session.getAttribute("account");
        if (currentUser == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not logged in"));
        }

        // Cập nhật dữ liệu
        accountForm.setUserId(currentUser.getUserId()); // Ensure correct ID
        if (accountForm.getPassword() == null || accountForm.getPassword().isEmpty()) {
            accountForm.setPassword(currentUser.getPassword());
        }
        accountForm.setRole(currentUser.getRole());
        accountForm.setStatus(currentUser.isStatus());
        
        userService.updateProfile(accountForm);

        // Lấy lại user đã update từ DB
        User updatedUser = userService.findById(accountForm.getUserId());

        // Cập nhật session
        session.setAttribute("account", updatedUser);

        return ResponseEntity.ok(updatedUser);
    }
}
