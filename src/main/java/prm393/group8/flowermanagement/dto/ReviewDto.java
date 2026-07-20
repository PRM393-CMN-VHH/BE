package prm393.group8.flowermanagement.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class ReviewDto {
    private int reviewId;
    private int userId;
    private String userName;
    private int rating;
    private String comment;
    private LocalDateTime createdAt;
}
