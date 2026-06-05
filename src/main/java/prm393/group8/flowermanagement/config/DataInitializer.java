package prm393.group8.flowermanagement.config;

import prm393.group8.flowermanagement.entity.*;
import prm393.group8.flowermanagement.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final ProductComboItemRepository productComboItemRepository;

    public DataInitializer(
            ProductRepository productRepository,
            CategoryRepository categoryRepository,
            RoleRepository roleRepository,
            UserRepository userRepository,
            ProductComboItemRepository productComboItemRepository
    ) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.productComboItemRepository = productComboItemRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        // Only initialize if database is empty
        if (roleRepository.count() > 0) {
            System.out.println("Database already initialized. Skipping initialization.");
            return;
        }

        System.out.println("Initializing database with Flower Shop data...");

        // 1. Roles
        Role adminRole = new Role("admin");
        Role userRole = new Role("user");
        roleRepository.save(adminRole);
        roleRepository.save(userRole);

        // 2. Users
        User admin = new User("Quản trị viên Tiệm Hoa", "0991111111", "123 Đường Láng, Hà Nội", "admin@gmail.com", "12345678");
        admin.setRole(adminRole);
        admin.setStatus(true);
        userRepository.save(admin);

        User normalUser = new User("Nguyễn Văn A", "0992222222", "456 Nguyễn Trãi, Hà Nội", "user@gmail.com", "12345678");
        normalUser.setRole(userRole);
        normalUser.setStatus(true);
        userRepository.save(normalUser);

        // 3. Categories
        Category rose = new Category("Hoa Hồng");
        Category orchid = new Category("Hoa Lan");
        Category lily = new Category("Hoa Ly");
        Category tulip = new Category("Hoa Tulip");
        Category sunflower = new Category("Hoa Hướng Dương");
        Category comboMix = new Category("Bó Hoa / Combo");

        categoryRepository.saveAll(List.of(rose, orchid, lily, tulip, sunflower, comboMix));

        // 4. Products (Flowers)
        // Roses
        Product roseRed = new Product(
                "Hoa hồng đỏ Đà Lạt",
                "Hoa hồng đỏ tươi Đà Lạt, biểu tượng tình yêu nồng cháy. Cành dài 60cm.",
                15000,
                150,
                "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?q=80&w=600&auto=format&fit=crop",
                rose
        );

        Product roseEcuador = new Product(
                "Hoa hồng Ecuador Hồng",
                "Hoa hồng Ecuador màu hồng phấn nhập khẩu, bông to, độ bền cao từ 7-10 ngày.",
                45000,
                80,
                "https://images.unsplash.com/photo-1533616688419-b7a585564566?q=80&w=600&auto=format&fit=crop",
                rose
        );

        // Orchids
        Product orchidWhite = new Product(
                "Lan Hồ Điệp Trắng",
                "Chậu lan hồ điệp trắng sang trọng, phù hợp làm quà tặng khai trương, chúc mừng.",
                250000,
                20,
                "https://images.unsplash.com/photo-1525310072745-f49212b5ac6d?q=80&w=600&auto=format&fit=crop",
                orchid
        );

        // Lilies
        Product lilyYellow = new Product(
                "Hoa Ly Vàng Thơm",
                "Hoa ly vàng thơm ngát, 3-5 tai bông mỗi cành, nở to và giữ hương lâu.",
                35000,
                100,
                "https://images.unsplash.com/photo-1508784932223-3b37c47e87ae?q=80&w=600&auto=format&fit=crop",
                lily
        );

        // Tulips
        Product tulipRed = new Product(
                "Hoa Tulip Đỏ Nhập Hà Lan",
                "Hoa tulip đỏ rực rỡ nhập khẩu trực tiếp từ Hà Lan.",
                50000,
                50,
                "https://images.unsplash.com/photo-1520763185298-1b434c919102?q=80&w=600&auto=format&fit=crop",
                tulip
        );

        // Sunflowers
        Product sunflowerSingle = new Product(
                "Hoa Hướng Dương Single",
                "Hoa hướng dương bông đơn to rực rỡ mang năng lượng tích cực.",
                20000,
                120,
                "https://images.unsplash.com/photo-1597848212624-a19eb35e2651?q=80&w=600&auto=format&fit=crop",
                sunflower
        );

        // Combos (Bó hoa thiết kế)
        Product comboLove = new Product(
                "Bó Hoa Tình Yêu ngọt ngào",
                "Bó hoa gồm 19 bông hồng đỏ Đà Lạt kết hợp cùng hoa baby trắng trang trí.",
                350000,
                15,
                "https://images.unsplash.com/photo-1561181286-d3fee7d55364?q=80&w=600&auto=format&fit=crop",
                comboMix
        );

        Product comboGraduation = new Product(
                "Bó Hoa Hướng Dương Tốt Nghiệp",
                "Bó hoa gồm 3 bông hướng dương kết hợp cùng hoa thạch thảo và lá bạc.",
                180000,
                30,
                "https://images.unsplash.com/photo-1591886960571-74d43a9d4166?q=80&w=600&auto=format&fit=crop",
                comboMix
        );

        productRepository.saveAll(List.of(
                roseRed, roseEcuador, orchidWhite, lilyYellow, tulipRed, sunflowerSingle, comboLove, comboGraduation
        ));

        // 5. Product Combo Items (If comboLove is made of roseRed)
        ProductComboItem item1 = new ProductComboItem(comboLove, roseRed, 19);
        ProductComboItem item2 = new ProductComboItem(comboGraduation, sunflowerSingle, 3);
        productComboItemRepository.saveAll(List.of(item1, item2));

        System.out.println("Flower Shop database initialized successfully!");
    }
}
