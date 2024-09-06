package com.example.tareaBancoDigital.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("loan")
public class Loan {

    @Id
    private String loanId;
    private Double balance;
    private Double interestRate;
    private String customerId;

    public Loan(String loanId, Double balance, Double interestRate, String customerId) {
        this.loanId = loanId;
        this.balance = balance;
        this.interestRate = interestRate;
        this.customerId = customerId;
    }

    public String getLoanId() {
        return loanId;
    }

    public void setLoanId(String loanId) {
        this.loanId = loanId;
    }

    public Double getBalance() {
        return balance;
    }

    public void setBalance(Double balance) {
        this.balance = balance;
    }

    public Double getInterestRate() {
        return interestRate;
    }

    public void setInterestRate(Double interestRate) {
        this.interestRate = interestRate;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }
}
