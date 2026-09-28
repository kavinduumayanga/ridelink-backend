package com.ridelink.driver.repository;

import com.ridelink.driver.domain.Vehicle;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VehicleRepository extends MongoRepository<Vehicle, String> {
    Optional<Vehicle> findByDriverId(String driverId);
    boolean existsByLicensePlate(String licensePlate);
    boolean existsByDriverId(String driverId);
}
