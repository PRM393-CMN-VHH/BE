package prm393.group8.flowermanagement.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Entity
@Table(name = "products")
@NoArgsConstructor
@AllArgsConstructor
@Data
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_id", updatable = false)
    private int productId;

    @NotBlank(message = "Tên sản phẩm không được để trống")
    @Column(name = "product_name", length = 150, nullable = false)
    private String productName;

    @NotBlank(message = "Mô tả không được để trống")
    @Column(length = 255)
    private String description;

    @Min(value = 100, message = "Giá sản phẩm phải lớn hơn 100 VND")
    @Column(nullable = false)
    private double price;

    @Min(value = 0, message = "Số lượng trong kho không được nhỏ hơn 0")
    @Column(nullable = false)
    private int stock;

    @Column(name = "image_url", nullable = false)
    private String imageUrl;

    @ManyToOne
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @OneToMany(mappedBy = "product")
    @JsonIgnore
    private List<OrderDetail> orderDetails;

    public Product(String productName, String description, double price, int stock, String imageUrl, Category category) {
        this.productName = productName;
        this.description = description;
        this.price = price;
        this.stock = stock;
        this.imageUrl = imageUrl;
        this.category = category;
    }
}
