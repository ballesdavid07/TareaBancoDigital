package com.example.tareaBancoDigital.service;

import com.example.tareaBancoDigital.dto.CreateAccountRequest;
import com.example.tareaBancoDigital.dto.TransferRequest;
import com.example.tareaBancoDigital.dto.UpdateAccountRequest;
import com.example.tareaBancoDigital.exception.BusinessException;
import com.example.tareaBancoDigital.model.Account;
import com.example.tareaBancoDigital.model.CustomerProfile;
import com.example.tareaBancoDigital.model.Loan;
import com.example.tareaBancoDigital.model.Transaction;
import com.example.tareaBancoDigital.repository.AccountRepository;
import com.example.tareaBancoDigital.repository.CustomerProfileRepository;
import com.example.tareaBancoDigital.repository.LoanRepository;
import com.example.tareaBancoDigital.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class BankService {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private CustomerProfileRepository customerProfileRepository;
    @Autowired
    private LoanRepository loanRepository;

    private final Sinks.Many<String> sink = Sinks.many().multicast().onBackpressureBuffer();

    public Mono<Double> getBalance(String accountId) {
        // Caso de uso: Consultar el saldo actual de una cuenta bancaria. Sino hay balance se debe tener un valor de 0.0
        return this.getTransactions(accountId)
                .switchIfEmpty(Mono.error(new BusinessException("La cuenta no existe.")))
                .map(Transaction::getAmount)
                .reduce(0.0, Double::sum); // Implementar la lógica de consulta aquí
    }

    public Mono<String> transferMoney(TransferRequest request) {
        // Caso de uso: Transferir dinero de una cuenta a otra. Hacer llamado de otro flujo simulando el llamado
        return this.getBalance(request.getFromAccount())
                .filter(balance -> balance >= request.getAmount())
                .switchIfEmpty(Mono.error(new BusinessException("La cuenta no tiene saldo suficiente para transferir")))
                .flatMap(balance -> {
                    Transaction transaction = new Transaction(request.getFromAccount() + request.getToAccount(), request.getFromAccount(), -request.getAmount());
                    return this.transactionRepository.save(transaction)
                            .flatMap(saved -> {
                                Transaction transactionTarget = new Transaction(request.getToAccount() + request.getFromAccount(), request.getToAccount(), request.getAmount());
                                return this.transactionRepository.save(transactionTarget);
                            });
                })
                .map(x -> "Transacción realizada correctamente.");
    }

    public Flux<Transaction> getTransactions(String accountId) {
        // Caso de uso: Consultar el historial de transacciones de una cuenta bancaria.
        /*List<Transaction> transactions = Arrays.asList(
                new Transaction("1", accountId, 200.00),
                new Transaction("2", accountId, -150.00),
                new Transaction("3", accountId, 300.00)
        );*/
        return this.transactionRepository.findByAccountId(accountId);
        /*return Flux.fromIterable(transactions)
                .filter(tx -> tx.getAccountId().equals(accountId));*/
    }

    public Mono<String> createAccount(CreateAccountRequest request) {
        // Caso de uso: Crear una nueva cuenta bancaria con un saldo inicial.
        Account account = new Account(request.getAccountId(), "Cuenta nueva");
        Transaction transaction = new Transaction(request.getAccountId(), request.getInitialBalance());
        return this.accountRepository.save(account)
                .flatMap(account1 -> this.transactionRepository.save(transaction)
                        .map(tx -> {
                            CustomerProfile customerProfile = new CustomerProfile(tx.getTransactionId() + "_name", tx.getTransactionId() + "@gmail.com", tx.getAccountId());
                            return this.customerProfileRepository.save(customerProfile);
                        }))
                .map(x -> "Cuenta creada.");
    }

    @Transactional
    public Mono<String> closeAccount(String accountId) {
        // Caso de uso: Cerrar una cuenta bancaria especificada. Verificar que la ceunta exista y si no existe debe retornar un error controlado
        return this.accountRepository.findById(accountId)
                .switchIfEmpty(Mono.error(new BusinessException("La cuenta no existe")))
                .map(account -> this.accountRepository.delete(account))
                .map(x -> this.transactionRepository.deleteAllByAccountId(accountId).subscribe())
                .map(x -> "Cuenta eliminada.");
    }

    public Mono<String> updateAccount(UpdateAccountRequest request) {
        // Caso de uso: Actualizar la información de una cuenta bancaria especificada. Verificar que la ceunta exista y si no existe debe retornar un error controlado
        return this.accountRepository.findById(request.getAccountId())
                .switchIfEmpty(Mono.error(new BusinessException("La cuenta no existe.")))
                .flatMap(acc -> {
                    Account account = new Account(request.getAccountId(), request.getNewData());
                    acc.setData(request.getNewData());
                    return this.accountRepository.save(acc);
                })
                .map(account -> "Cuenta actualizada.");
    }

    public Mono<CustomerProfile> getCustomerProfile(String accountId) {
        // Caso de uso: Consultar el perfil del cliente que posee la cuenta bancaria. Obtener los valores por cada uno de los flujos y si no existe alguno debe presentar un error
        //Con Monos
        Mono<String> customerIdMono = Mono.just("12345");
        Mono<String> nameMono = Mono.just("John Doe");
        Mono<String> emailMono = Mono.just("john.doe@example.com");

        return this.customerProfileRepository.findByAccountId(accountId)
                .switchIfEmpty(Mono.error(new BusinessException("El cliente no existe.")))
                .next()
                .flatMap(customerProfile -> {
                    Mono<String> ci = customerIdMono.filter(c -> c.equals(customerProfile.getCustomerId())).switchIfEmpty(Mono.error(new BusinessException("El customerId no existe")));
                    Mono<String> n = nameMono.filter(c -> c.equals(customerProfile.getName())).switchIfEmpty(Mono.error(new BusinessException("El nombre no existe")));
                    Mono<String> e = emailMono.filter(c -> c.equals(customerProfile.getEmail())).switchIfEmpty(Mono.error(new BusinessException("El email no existe")));
                    return Mono.zip(ci, n, e)
                            .flatMap(objects -> {
                                CustomerProfile customerP = new CustomerProfile(objects.getT1(), objects.getT2(), objects.getT3());
                                return Mono.just(customerP);
                            });
                });

        //Con base de datos
        /*return this.customerProfileRepository.findByAccountId(accountId)
                .switchIfEmpty(Mono.error(new BusinessException("El cliente no existe.")))
                .next();*/
    }

    public Flux<Loan> getActiveLoans(String customerId) {
        // Caso de uso: Consultar todos los préstamos activos asociados al cliente especificado.
        /*List<Loan> loans = Arrays.asList(
                new Loan("loan1", 5000.00, 0.05),
                new Loan("loan2", 10000.00, 0.04)
        );*/

        return this.customerProfileRepository.findById(customerId)
                .switchIfEmpty(Mono.error(new BusinessException("El cliente no existe.")))
                .flatMapMany(customerProfile ->
                        this.loanRepository.findByCustomerId(customerProfile.getCustomerId())
                                .switchIfEmpty(Mono.error(new BusinessException("El cliente no tiene prestamos.")))
                );
    }

    public Flux<Double> simulateInterest(String accountId) {
        // Caso de uso: Simular el interés compuesto en una cuenta bancaria. Sacar un rago de 10 años y aplicar la siguiente formula = principal * Math.pow(1 + rate, year)

        double principal = 1000.00;
        double rate = 0.05;

        return Flux.range(1, 10)
                .flatMap(year -> getBalance(accountId)
                        .map(balance -> principal * Math.pow(1 + rate, year))
                );
    }

    public Mono<String> getLoanStatus(String loanId) {
        // Caso de uso: Consultar el estado de un préstamo. se debe tener un flujo balanceMono y interestRateMono. Imprimir con el formato siguiente el resultado   "Loan ID: %s, Balance: %.2f, Interest Rate: %.2f%%"
        //Con Monos
        List<Loan> loans = Arrays.asList(
                new Loan("loan1", 5000.00, 0.05, "any"),
                new Loan("loan2", 10000.00, 0.04, "any2")
        );
        Mono<Double> balanceMono = Mono.just(2434.00);
        Mono<Double> interestRateMono = Mono.just(0.05);

        return loans.stream()
                .filter(loan -> loan.getLoanId().equals(loanId))
                .findFirst()
                .map(loan -> Mono.zip(balanceMono, interestRateMono, (bm, ir) ->
                        String.format("Loan ID: %s, Balance: %.2f, Interest Rate: %.2f%%", loan.getLoanId(), bm, ir)))
                .orElse(Mono.error(new BusinessException("Prestamo no encontrado")));


        //Con base de datos
        /*return this.loanRepository.findById(loanId)
                .map(loan -> String.format("Loan ID: %s, Balance: %.2f, Interest Rate: %.2f%%", loan.getLoanId(), loan.getBalance(), loan.getInterestRate()));*/

    }


}
