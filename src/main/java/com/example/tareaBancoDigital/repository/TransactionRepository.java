package com.example.tareaBancoDigital.repository;

import com.example.tareaBancoDigital.model.Transaction;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface TransactionRepository extends ReactiveCrudRepository<Transaction, String> {
    Flux<Transaction> findByAccountId(String accountId);

    @Transactional
    Mono<Void> deleteAllByAccountId(String accountId);
}
