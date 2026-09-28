package com.ridelink.payment.repository;

import com.ridelink.payment.domain.Fare;
import com.ridelink.payment.domain.FareType;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FareRepository extends MongoRepository<Fare, String> {

    List<Fare> findByRideId(String rideId);

    Optional<Fare> findByRideIdAndFareType(String rideId, FareType fareType);

    boolean existsByRideIdAndFareType(String rideId, FareType fareType);
}
