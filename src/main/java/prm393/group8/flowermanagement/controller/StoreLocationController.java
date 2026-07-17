package prm393.group8.flowermanagement.controller;

import prm393.group8.flowermanagement.repository.StoreLocationRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/store-locations")
public class StoreLocationController {

    private final StoreLocationRepository storeLocationRepository;

    public StoreLocationController(StoreLocationRepository storeLocationRepository) {
        this.storeLocationRepository = storeLocationRepository;
    }

    // [GET] /api/store-locations - Danh sách cửa hàng (công khai, không cần đăng nhập)
    @GetMapping("")
    public ResponseEntity<?> getStoreLocations() {
        return ResponseEntity.ok(storeLocationRepository.findAll());
    }
}
