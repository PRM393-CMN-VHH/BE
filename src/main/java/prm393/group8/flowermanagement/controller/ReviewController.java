package prm393.group8.flowermanagement.controller;

import prm393.group8.flowermanagement.dto.ReviewDto;
import prm393.group8.flowermanagement.entity.User;
import prm393.group8.flowermanagement.service.ReviewService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/products/{productId}/reviews")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    // [GET] /api/products/{productId}/reviews - Public: danh sách đánh giá + điểm trung bình
    @GetMapping
    public ResponseEntity<?> getReviews(@PathVariable("productId") int productId, HttpSession session) {
        Map<String, Object> response = new java.util.HashMap<>();
        response.put("reviews", reviewService.getReviewsForProduct(productId));
        response.put("averageRating", reviewService.getAverageRating(productId));
        response.put("reviewCount", reviewService.getReviewCount(productId));

        User account = currentUser(session);
        if (account != null) {
            response.put("canReview", reviewService.canUserReview(account, productId));
            response.put("alreadyReviewed", reviewService.hasUserReviewed(account, productId));
        }
        return ResponseEntity.ok(response);
    }

    // [POST] /api/products/{productId}/reviews - Khách hàng đã xác nhận nhận hàng mới được đánh giá
    @PostMapping
    public ResponseEntity<?> createReview(
            @PathVariable("productId") int productId,
            @RequestBody Map<String, Object> body,
            HttpSession session) {
        User account = currentUser(session);
        if (account == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not logged in"));
        }

        Object ratingRaw = body.get("rating");
        if (ratingRaw == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Thiếu số sao đánh giá"));
        }
        int rating = Integer.parseInt(ratingRaw.toString());
        String comment = body.get("comment") != null ? body.get("comment").toString() : "";

        try {
            ReviewDto dto = reviewService.createReview(account, productId, rating, comment);
            return ResponseEntity.ok(dto);
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    private User currentUser(HttpSession session) {
        User user = (User) session.getAttribute("account");
        if (user == null) user = (User) session.getAttribute("adminInfo");
        return user;
    }
}
