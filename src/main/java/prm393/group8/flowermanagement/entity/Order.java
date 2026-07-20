package prm393.group8.flowermanagement.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "orders")
@NoArgsConstructor
@AllArgsConstructor
@Data
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id", updatable = false)
    private int orderId;

    @Column(name = "total_price", nullable = false)
    private double totalPrice;

    // columnDefinition default backfills existing rows when Hibernate ALTERs
    // this column in (they'd otherwise violate NOT NULL with no psql on hand
    // to backfill manually).
    @Column(name = "shipping_fee", nullable = false, columnDefinition = "double precision default 0")
    private double shippingFee;

    @Column(name = "order_status", length = 20, nullable = false)
    private String orderStatus;

    @Column(name = "payment_status", length = 20, nullable = false)
    private String paymentStatus;

    @Column(name = "payment_method", length = 50, nullable = false)
    private String paymentMethod;

    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;


}
