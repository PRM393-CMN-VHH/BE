package prm393.group8.flowermanagement.repository;

import prm393.group8.flowermanagement.entity.StoreLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StoreLocationRepository extends JpaRepository<StoreLocation, Integer> {
}
