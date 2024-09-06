package com.example.tareaBancoDigital.repository;

import com.example.tareaBancoDigital.model.CustomerProfile;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface CustomerProfileRepository extends ReactiveCrudRepository<CustomerProfile, String> {
    Flux<CustomerProfile> findByAccountId(String accountId);
}
