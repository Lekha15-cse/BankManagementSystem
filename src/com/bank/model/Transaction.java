package com.bank.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Transaction {
    private String accountNumber;
    private String transactionType;
    private BigDecimal amount;
    private LocalDateTime date;

    public Transaction(String accountNumber, String transactionType, BigDecimal amount, LocalDateTime date) {
        this.accountNumber = accountNumber;
        this.transactionType = transactionType;
        this.amount = amount;
        this.date = date;
    }

    // Getters and setters can be added as needed
}
