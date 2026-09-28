package com.ridelink.payment.repository;

import com.ridelink.payment.domain.Payment;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentRepository extends MongoRepository<Payment, String> {

    Optional<Payment> findByRideId(String rideId);

    Optional<Payment> findByFareId(String fareId);

    boolean existsByRideId(String rideId);
}
