package com.bank.model;

import java.math.BigDecimal;

public class Account {
    private String accountNumber;
    private String customerName;
    private String pin;
    private BigDecimal balance;

    public Account(String accountNumber, String customerName, String pin, BigDecimal balance) {
        this.accountNumber = accountNumber;
        this.customerName = customerName;
        this.pin = pin;
        this.balance = balance;
    }

    // Getters
    public String getAccountNumber() {
        return accountNumber;
    }

    public String getCustomerName() {
        return customerName;
    }

    public String getPin() {
        return pin;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    // Setters
    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }
}
