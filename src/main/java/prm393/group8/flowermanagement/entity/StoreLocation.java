package prm393.group8.flowermanagement.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "store_locations")
@NoArgsConstructor
@AllArgsConstructor
@Data
public class StoreLocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "store_id", updatable = false)
    private int storeId;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, length = 255)
    private String address;

    @Column(nullable = false, length = 20)
    private String phone;

    @Column(nullable = false, length = 50)
    private String hours;

    @Column(nullable = false)
    private double latitude;

    @Column(nullable = false)
    private double longitude;

    public StoreLocation(String name, String address, String phone, String hours, double latitude, double longitude) {
        this.name = name;
        this.address = address;
        this.phone = phone;
        this.hours = hours;
        this.latitude = latitude;
        this.longitude = longitude;
    }
}
