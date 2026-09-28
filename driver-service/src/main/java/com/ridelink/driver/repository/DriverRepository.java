package com.ridelink.driver.repository;

import com.ridelink.driver.domain.Driver;
import com.ridelink.driver.domain.DriverAvailability;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DriverRepository extends MongoRepository<Driver, String> {
    List<Driver> findByAvailability(DriverAvailability availability);
    List<Driver> findByAvailabilityAndServiceArea(DriverAvailability availability, String serviceArea);
    Optional<Driver> findByAccountId(String accountId);
    boolean existsByAccountId(String accountId);
}
