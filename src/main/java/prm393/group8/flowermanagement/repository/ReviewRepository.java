package prm393.group8.flowermanagement.repository;

import prm393.group8.flowermanagement.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Integer> {
    List<Review> findByProduct_ProductIdOrderByCreatedAtDesc(int productId);
    Optional<Review> findByProduct_ProductIdAndUser_UserId(int productId, int userId);
    long countByProduct_ProductId(int productId);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.product.productId = :productId")
    Double findAverageRatingByProductId(@Param("productId") int productId);
}
