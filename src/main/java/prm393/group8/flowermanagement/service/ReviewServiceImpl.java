package prm393.group8.flowermanagement.service;

import prm393.group8.flowermanagement.dto.ReviewDto;
import prm393.group8.flowermanagement.entity.Order;
import prm393.group8.flowermanagement.entity.OrderDetail;
import prm393.group8.flowermanagement.entity.Product;
import prm393.group8.flowermanagement.entity.Review;
import prm393.group8.flowermanagement.entity.User;
import prm393.group8.flowermanagement.repository.OrderDetailRepository;
import prm393.group8.flowermanagement.repository.OrderRepository;
import prm393.group8.flowermanagement.repository.ProductRepository;
import prm393.group8.flowermanagement.repository.ReviewRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final OrderRepository orderRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final ProductRepository productRepository;

    public ReviewServiceImpl(ReviewRepository reviewRepository,
                              OrderRepository orderRepository,
                              OrderDetailRepository orderDetailRepository,
                              ProductRepository productRepository) {
        this.reviewRepository = reviewRepository;
        this.orderRepository = orderRepository;
        this.orderDetailRepository = orderDetailRepository;
        this.productRepository = productRepository;
    }

    @Override
    public List<ReviewDto> getReviewsForProduct(int productId) {
        return reviewRepository.findByProduct_ProductIdOrderByCreatedAtDesc(productId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    public double getAverageRating(int productId) {
        Double avg = reviewRepository.findAverageRatingByProductId(productId);
        return avg == null ? 0.0 : avg;
    }

    @Override
    public long getReviewCount(int productId) {
        return reviewRepository.countByProduct_ProductId(productId);
    }

    @Override
    public boolean hasUserReviewed(User user, int productId) {
        return reviewRepository.findByProduct_ProductIdAndUser_UserId(productId, user.getUserId()).isPresent();
    }

    @Override
    public boolean canUserReview(User user, int productId) {
        if (hasUserReviewed(user, productId)) return false;
        return hasCompletedPurchase(user.getUserId(), productId);
    }

    private boolean hasCompletedPurchase(int userId, int productId) {
        List<Order> completedOrders = orderRepository.findByUser_UserIdAndOrderStatusIgnoreCase(userId, "COMPLETED");
        for (Order order : completedOrders) {
            List<OrderDetail> details = orderDetailRepository.findByOrder_OrderId(order.getOrderId());
            if (details.stream().anyMatch(d -> d.getProduct().getProductId() == productId)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public ReviewDto createReview(User user, int productId, int rating, String comment) {
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Đánh giá phải từ 1 đến 5 sao");
        }
        if (!canUserReview(user, productId)) {
            throw new IllegalStateException(
                    hasUserReviewed(user, productId)
                            ? "Bạn đã đánh giá sản phẩm này rồi"
                            : "Bạn cần xác nhận đã nhận hàng của một đơn có sản phẩm này trước khi đánh giá"
            );
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm"));

        Review review = new Review();
        review.setProduct(product);
        review.setUser(user);
        review.setRating(rating);
        review.setComment(comment == null ? "" : comment.trim());
        review.setCreatedAt(LocalDateTime.now());

        return toDto(reviewRepository.save(review));
    }

    private ReviewDto toDto(Review review) {
        ReviewDto dto = new ReviewDto();
        dto.setReviewId(review.getReviewId());
        dto.setUserId(review.getUser().getUserId());
        dto.setUserName(review.getUser().getFullName());
        dto.setRating(review.getRating());
        dto.setComment(review.getComment());
        dto.setCreatedAt(review.getCreatedAt());
        return dto;
    }
}
