package com.bank;

import com.bank.dao.AccountDAO;
import com.bank.model.Account;
import java.math.BigDecimal;
import java.util.Scanner;

public class MainMenu {
    private static Scanner scanner = new Scanner(System.in);
    private static AccountDAO accountDAO = new AccountDAO();
    private static Account loggedInAccount = null;

    public static void main(String[] args) {
        System.out.println("=== Bank Management System ===");
        while (true) {
            if (loggedInAccount == null) {
                showWelcomeMenu();
            } else {
                showCustomerMenu();
            }
        }
    }

    private static void showWelcomeMenu() {
        System.out.println("\n1. Create Account");
        System.out.println("2. Login");
        System.out.println("3. Exit");
        System.out.print("Choose: ");
        int choice = scanner.nextInt();
        scanner.nextLine();

        switch (choice) {
            case 1 -> createAccount();
            case 2 -> login();
            case 3 -> {
                System.out.println("Thank you for using the system.");
                System.exit(0);
            }
            default -> System.out.println("Invalid option");
        }
    }

    private static void createAccount() {
        System.out.print("Enter Account Number: ");
        String accNum = scanner.nextLine();
        System.out.print("Enter Customer Name: ");
        String name = scanner.nextLine();
        System.out.print("Set 4-digit PIN: ");
        String pin = scanner.nextLine();

        Account newAcc = new Account(accNum, name, pin, BigDecimal.ZERO);
        if (accountDAO.createAccount(newAcc)) {
            System.out.println("✓ Account created successfully!");
        } else {
            System.out.println("✗ Failed to create account. It may already exist.");
        }
    }

    private static void login() {
        System.out.print("Account Number: ");
        String accNum = scanner.nextLine();
        System.out.print("PIN: ");
        String pin = scanner.nextLine();

        loggedInAccount = accountDAO.findByAccountNumber(accNum, pin);
        if (loggedInAccount != null) {
            System.out.println("✓ Login successful. Welcome, " + loggedInAccount.getCustomerName());
        } else {
            System.out.println("✗ Invalid account number or PIN.");
        }
    }

    private static void showCustomerMenu() {
        System.out.println("\n=== Customer Menu (" + loggedInAccount.getAccountNumber() + ") ===");
        System.out.println("1. Check Balance");
        System.out.println("2. Deposit");
        System.out.println("3. Withdraw");
        System.out.println("4. ⭐ Transfer Funds");  // NEW!
        System.out.println("5. Transaction History");
        System.out.println("6. Logout");
        System.out.print("Choose: ");
        int choice = scanner.nextInt();
        scanner.nextLine();

        switch (choice) {
            case 1 -> checkBalance();
            case 2 -> deposit();
            case 3 -> withdraw();
            case 4 -> transferFunds();  // NEW!
            case 5 -> accountDAO.showTransactionHistory(loggedInAccount.getAccountNumber());
            case 6 -> {
                loggedInAccount = null;
                System.out.println("Logged out.");
            }
            default -> System.out.println("Invalid option.");
        }
    }

    private static void checkBalance() {
        System.out.println("Current Balance: ₹" + loggedInAccount.getBalance());
    }

    private static void deposit() {
        System.out.print("Enter amount to deposit: ₹");
        try {
            BigDecimal amount = new BigDecimal(scanner.nextLine());
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                System.out.println("Amount must be positive.");
                return;
            }
            if (accountDAO.deposit(loggedInAccount.getAccountNumber(), amount)) {
                loggedInAccount = accountDAO.findByAccountNumber(loggedInAccount.getAccountNumber(), loggedInAccount.getPin());
                System.out.println("✓ Deposit successful. New balance: ₹" + loggedInAccount.getBalance());
            } else {
                System.out.println("✗ Deposit failed.");
            }
        } catch (NumberFormatException e) {
            System.out.println("Invalid amount format.");
        }
    }

    private static void withdraw() {
        System.out.print("Enter amount to withdraw: ₹");
        try {
            BigDecimal amount = new BigDecimal(scanner.nextLine());
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                System.out.println("Amount must be positive.");
                return;
            }
            if (accountDAO.withdraw(loggedInAccount.getAccountNumber(), amount)) {
                loggedInAccount = accountDAO.findByAccountNumber(loggedInAccount.getAccountNumber(), loggedInAccount.getPin());
                System.out.println("✓ Withdrawal successful. New balance: ₹" + loggedInAccount.getBalance());
            } else {
                System.out.println("✗ Withdrawal failed - Insufficient funds.");
            }
        } catch (NumberFormatException e) {
            System.out.println("Invalid amount format.");
        }
    }

    // ⭐ NEW: Transfer Funds Method
    private static void transferFunds() {
        System.out.print("Enter recipient Account Number: ");
        String toAccount = scanner.nextLine();

        System.out.print("Enter transfer amount: ₹");
        try {
            BigDecimal amount = new BigDecimal(scanner.nextLine());
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                System.out.println("Amount must be positive.");
                return;
            }

            if (accountDAO.transfer(loggedInAccount.getAccountNumber(), toAccount, amount)) {
                loggedInAccount = accountDAO.findByAccountNumber(loggedInAccount.getAccountNumber(), loggedInAccount.getPin());
                System.out.println("✓ Transfer successful! New balance: ₹" + loggedInAccount.getBalance());
            } else {
                System.out.println("✗ Transfer failed - Invalid recipient or insufficient funds.");
            }
        } catch (NumberFormatException e) {
            System.out.println("Invalid amount format.");
        }
    }
}
