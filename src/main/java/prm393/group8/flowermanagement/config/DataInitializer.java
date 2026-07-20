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
    private final StoreLocationRepository storeLocationRepository;

    public DataInitializer(
            ProductRepository productRepository,
            CategoryRepository categoryRepository,
            RoleRepository roleRepository,
            UserRepository userRepository,
            StoreLocationRepository storeLocationRepository
    ) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.storeLocationRepository = storeLocationRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        // Categories used to be flower types (Hoa Hồng, Hoa Lan, ...); this app now
        // browses by gift occasion instead (Sinh nhật, Tỏ tình, ...). Runs on every
        // startup but is a no-op once already migrated, so it's safe on old DBs.
        migrateFlowerTypeCategoriesToOccasions();

        // Store locations được seed riêng để DB cũ (đã có roles) vẫn nhận được dữ liệu cửa hàng
        if (storeLocationRepository.count() == 0) {
            storeLocationRepository.saveAll(List.of(
                    new StoreLocation(
                            "Tiệm Hoa Xinh - Quận 1",
                            "456 Hai Bà Trưng, Quận 1, TP.HCM",
                            "0909 789 000",
                            "07:00 - 20:00",
                            10.7876,
                            106.6948
                    ),
                    new StoreLocation(
                            "Tiệm Hoa Xinh - Quận 3",
                            "123 Nguyễn Đình Chiểu, Quận 3, TP.HCM",
                            "0909 789 001",
                            "08:00 - 21:00",
                            10.7785,
                            106.6882
                    )
            ));
            System.out.println("Seeded store locations.");
        }

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

        // 3. Categories — browse by gift occasion, not flower type
        Category tinhYeu = new Category("Tỏ tình");
        Category sinhNhat = new Category("Sinh nhật");
        Category chucMung = new Category("Chúc mừng");
        Category khaiTruong = new Category("Khai trương");
        Category kyNiem = new Category("Kỷ niệm");
        Category totNghiep = new Category("Tốt nghiệp");
        Category triAn = new Category("Tri ân");
        Category ngayLe = new Category("Ngày lễ");

        categoryRepository.saveAll(List.of(
                tinhYeu, sinhNhat, chucMung, khaiTruong, kyNiem, totNghiep, triAn, ngayLe
        ));

        // 4. Products (All products are bouquets)
        Product roseRed = new Product(
                "Bó Hồng Đỏ Yêu Thương",
                productDescription(
                        "Bó Hồng Đỏ Yêu Thương là bó hoa mang sắc đỏ lãng mạn, phù hợp để gửi lời yêu thương chân thành và tạo điểm nhấn nổi bật trong những dịp đặc biệt.",
                        List.of(
                                "12 hoa hồng đỏ Đà Lạt",
                                "Hoa baby trắng",
                                "Lá phụ xanh",
                                "Giấy gói kem cao cấp",
                                "Nơ lụa"
                        ),
                        "Lãng mạn, nổi bật, tinh tế.",
                        List.of("Sinh nhật", "Kỷ niệm", "Tỏ tình", "Ngày lễ", "Chúc mừng"),
                        "Bó hoa tượng trưng cho tình yêu nồng nàn, sự trân trọng và những cảm xúc chân thành dành cho người nhận.",
                        "50-55 cm",
                        "35-40 cm"
                ),
                320000,
                30,
                "https://7fgarden.com/wp-content/uploads/2024/05/bo-hoa-hong-ecuador.webp",
                tinhYeu
        );

        Product roseEcuador = new Product(
                "Bó Hồng Ecuador Dịu Dàng",
                productDescription(
                        "Bó Hồng Ecuador Dịu Dàng là bó hoa mang phong cách Hàn Quốc với tông hồng pastel nhẹ nhàng, phù hợp để gửi lời yêu thương và những lời chúc ý nghĩa đến người nhận.",
                        List.of(
                                "10 hoa hồng Ecuador hồng pastel",
                                "Hoa baby trắng",
                                "Lá bạc",
                                "Giấy gói kem cao cấp",
                                "Ruy băng satin"
                        ),
                        "Thanh lịch, hiện đại, tinh tế.",
                        List.of("Sinh nhật", "Kỷ niệm", "Tỏ tình", "Ngày lễ", "Chúc mừng"),
                        "Bó hoa tượng trưng cho tình yêu chân thành, sự quan tâm và những lời chúc tốt đẹp dành cho người nhận.",
                        "50-55 cm",
                        "35-40 cm"
                ),
                650000,
                20,
                "https://lamantfloral.com/wp-content/uploads/2024/09/z5841224342864_67e1d9e1bcd573c149874d57a9e222e5.jpg",
                sinhNhat
        );

        Product orchidWhite = new Product(
                "Bó Lan Trắng Thanh Nhã",
                productDescription(
                        "Bó Lan Trắng Thanh Nhã là thiết kế hoa trang nhã với sắc trắng tinh khôi, thích hợp để gửi lời chúc mừng sang trọng và lịch sự.",
                        List.of(
                                "Hoa lan trắng",
                                "Hoa phụ trắng",
                                "Lá xanh trang trí",
                                "Giấy gói trắng cao cấp",
                                "Ruy băng satin"
                        ),
                        "Trang nhã, sang trọng, nhẹ nhàng.",
                        List.of("Chúc mừng", "Khai trương", "Sinh nhật", "Tri ân", "Ngày lễ"),
                        "Bó hoa tượng trưng cho sự tinh khiết, lời chúc tốt lành và sự trân trọng dành cho người nhận.",
                        "55-60 cm",
                        "35-40 cm"
                ),
                480000,
                18,
                "https://hoathangtu.com/wp-content/uploads/2024/10/IMG_1033.jpg",
                chucMung
        );

        Product lilyYellow = new Product(
                "Bó Ly Vàng Rạng Rỡ",
                productDescription(
                        "Bó Ly Vàng Rạng Rỡ mang sắc vàng ấm áp cùng hương thơm dịu nhẹ, phù hợp để gửi lời chúc may mắn và niềm vui đến người nhận.",
                        List.of(
                                "Hoa ly vàng",
                                "Hoa baby trắng",
                                "Lá bạc",
                                "Giấy gói tone sáng",
                                "Ruy băng trang trí"
                        ),
                        "Rực rỡ, ấm áp, thanh lịch.",
                        List.of("Sinh nhật", "Chúc mừng", "Khai trương", "Tốt nghiệp", "Ngày lễ"),
                        "Bó hoa tượng trưng cho niềm vui, sự thịnh vượng và những khởi đầu tốt đẹp.",
                        "55-65 cm",
                        "40-45 cm"
                ),
                390000,
                24,
                "https://hoatuoingocvan.com/storage/products/kBnGBXx49tRet8dPlTr4s8W4nxutPr52qUxl4Wry.jpg",
                khaiTruong
        );

        Product tulipRed = new Product(
                "Bó Tulip Đỏ Kiêu Sa",
                productDescription(
                        "Bó Tulip Đỏ Kiêu Sa là bó hoa nhập khẩu với sắc đỏ cuốn hút, thiết kế tối giản nhưng sang trọng theo phong cách Hàn Quốc.",
                        List.of(
                                "10 hoa tulip đỏ nhập khẩu",
                                "Lá phụ xanh",
                                "Giấy gói Hàn Quốc",
                                "Giấy lót cao cấp",
                                "Ruy băng satin"
                        ),
                        "Tối giản, sang trọng, kiêu sa.",
                        List.of("Tỏ tình", "Kỷ niệm", "Sinh nhật", "Ngày lễ", "Chúc mừng"),
                        "Bó hoa tượng trưng cho tình yêu tinh tế, sự ngưỡng mộ và những cảm xúc sâu sắc.",
                        "40-45 cm",
                        "28-32 cm"
                ),
                720000,
                16,
                "https://vuonhoatuoi.vn/wp-content/uploads/2026/04/bo-hoa-tulip-do-5.webp",
                kyNiem
        );

        Product sunflowerSingle = new Product(
                "Bó Hướng Dương Nắng Mai",
                productDescription(
                        "Bó Hướng Dương Nắng Mai mang sắc vàng tươi sáng, phù hợp để gửi lời chúc thành công, lạc quan và tràn đầy năng lượng.",
                        List.of(
                                "5 hoa hướng dương",
                                "Hoa thạch thảo trắng",
                                "Lá phụ xanh",
                                "Giấy gói tone sáng",
                                "Ruy băng trang trí"
                        ),
                        "Tươi sáng, trẻ trung, năng động.",
                        List.of("Tốt nghiệp", "Khai trương", "Sinh nhật", "Chúc mừng", "Ngày lễ"),
                        "Bó hoa tượng trưng cho niềm tin, sự lạc quan và lời chúc luôn hướng về những điều tốt đẹp.",
                        "55-60 cm",
                        "35-40 cm"
                ),
                280000,
                35,
                "https://img.mayflower.vn/2019/08/bo-hoa-huong-duong.jpg",
                totNghiep
        );

        // Giá khuyến mãi cho một vài sản phẩm (hiển thị "giá khuyến mãi nếu có")
        roseRed.setPromoPrice(280000.0);
        sunflowerSingle.setPromoPrice(240000.0);

        productRepository.saveAll(List.of(
                roseRed, roseEcuador, orchidWhite, lilyYellow, tulipRed, sunflowerSingle
        ));

        System.out.println("Flower Shop database initialized successfully!");
    }

    // One-time migration for pre-existing databases: categories were flower types
    // (Hoa Hồng, Hoa Lan, Hoa Ly, Hoa Tulip, Hoa Hướng Dương); rename them in place
    // to gift occasions, add the remaining occasion categories, and move products
    // that shared a flower-type category into their own distinct occasion.
    private void migrateFlowerTypeCategoriesToOccasions() {
        boolean alreadyMigrated = categoryRepository.findAll().stream()
                .anyMatch(c -> "Sinh nhật".equals(c.getCategoryName()));
        if (alreadyMigrated) return;

        var byOldName = categoryRepository.findAll().stream()
                .collect(java.util.stream.Collectors.toMap(Category::getCategoryName, c -> c));

        Category rose = byOldName.get("Hoa Hồng");
        Category orchid = byOldName.get("Hoa Lan");
        Category lily = byOldName.get("Hoa Ly");
        Category tulip = byOldName.get("Hoa Tulip");
        Category sunflower = byOldName.get("Hoa Hướng Dương");
        if (rose == null || orchid == null || lily == null || tulip == null || sunflower == null) {
            // Not the flower-type schema we expect (e.g. a fresh DB already using
            // occasion categories, or categories were customized) — leave it alone.
            return;
        }

        System.out.println("Migrating categories from flower type to gift occasion...");

        rose.setCategoryName("Tỏ tình");
        orchid.setCategoryName("Chúc mừng");
        lily.setCategoryName("Khai trương");
        tulip.setCategoryName("Kỷ niệm");
        sunflower.setCategoryName("Tốt nghiệp");
        categoryRepository.saveAll(List.of(rose, orchid, lily, tulip, sunflower));

        Category sinhNhat = categoryRepository.save(new Category("Sinh nhật"));
        categoryRepository.save(new Category("Tri ân"));
        categoryRepository.save(new Category("Ngày lễ"));

        // "Hoa Hồng" used to cover two products; keep one under its new "Tỏ tình"
        // category and move the other to "Sinh nhật" so they aren't identical tags.
        productRepository.findAll().stream()
                .filter(p -> "Bó Hồng Ecuador Dịu Dàng".equals(p.getProductName()))
                .findFirst()
                .ifPresent(p -> {
                    p.setCategory(sinhNhat);
                    productRepository.save(p);
                });

        System.out.println("Category migration done.");
    }

    private static String productDescription(
            String intro,
            List<String> components,
            String style,
            List<String> occasions,
            String meaning,
            String height,
            String diameter
    ) {
        return """
                **Mô tả sản phẩm**

                %s

                **Thành phần**
                %s

                **Phong cách**
                %s

                **Phù hợp cho**
                %s

                **Ý nghĩa**
                %s

                **Kích thước**
                - Chiều cao: %s
                - Đường kính: %s

                **Lưu ý**
                - Hoa là sản phẩm tự nhiên nên màu sắc và kích thước có thể chênh lệch nhẹ.
                - Trong trường hợp một số loại hoa không có theo mùa, shop sẽ thay thế bằng loại hoa tương đương về màu sắc và giá trị, đồng thời vẫn giữ nguyên phong cách thiết kế.
                """.formatted(
                intro,
                bulletList(components),
                style,
                bulletList(occasions),
                meaning,
                height,
                diameter
        );
    }

    private static String bulletList(List<String> items) {
        return "- " + String.join("\n- ", items);
    }
}
