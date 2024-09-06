package com.example.tareaBancoDigital.repository;

import com.example.tareaBancoDigital.model.Loan;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface LoanRepository extends ReactiveCrudRepository<Loan, String> {
    Flux<Loan> findByCustomerId(String customerId);
}
