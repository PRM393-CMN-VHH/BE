package prm393.group8.flowermanagement.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "product_combo_item")
@NoArgsConstructor
@AllArgsConstructor
@Data
public class ProductComboItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne
    @JoinColumn(name = "combo_id")
    private Product combo; // sản phẩm cha (combo)

    @ManyToOne
    @JoinColumn(name = "component_id")
    private Product component; // sản phẩm con

    private int quantity; // số lượng mỗi sản phẩm con trong combo

    public ProductComboItem(Product combo, Product component, int quantity) {
        this.combo = combo;
        this.component = component;
        this.quantity = quantity;
    }
}
