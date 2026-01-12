-- Create Database
CREATE DATABASE IF NOT EXISTS bank_system;
USE bank_system;

-- =========================
-- Accounts Table
-- =========================
CREATE TABLE accounts (
    id INT AUTO_INCREMENT PRIMARY KEY,
    account_number VARCHAR(20) UNIQUE NOT NULL,
    customer_name VARCHAR(50) NOT NULL,
    pin INT NOT NULL,
    balance DECIMAL(10,2) DEFAULT 0.00,
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- =========================
-- Transactions Table
-- =========================
CREATE TABLE transactions (
    id INT AUTO_INCREMENT PRIMARY KEY,
    account_number VARCHAR(20) NOT NULL,
    transaction_type VARCHAR(20),
    amount DECIMAL(10,2),
    balance_after DECIMAL(10,2),
    transaction_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
