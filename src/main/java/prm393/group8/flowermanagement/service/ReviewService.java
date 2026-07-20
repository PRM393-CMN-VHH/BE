package prm393.group8.flowermanagement.service;

import prm393.group8.flowermanagement.dto.ReviewDto;
import prm393.group8.flowermanagement.entity.User;

import java.util.List;

public interface ReviewService {
    List<ReviewDto> getReviewsForProduct(int productId);
    double getAverageRating(int productId);
    long getReviewCount(int productId);

    // Eligible = has a COMPLETED order containing this product and hasn't reviewed it yet.
    boolean canUserReview(User user, int productId);
    boolean hasUserReviewed(User user, int productId);

    ReviewDto createReview(User user, int productId, int rating, String comment);
}
