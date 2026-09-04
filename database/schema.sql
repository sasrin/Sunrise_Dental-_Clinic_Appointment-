-- Sunrise Dental Clinic Database Schema
-- MySQL Database Setup

CREATE DATABASE IF NOT EXISTS dental_clinic;
USE dental_clinic;

-- Users table for authentication
CREATE TABLE IF NOT EXISTS users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Appointments table
CREATE TABLE IF NOT EXISTS appointments (
    appointment_number VARCHAR(50) PRIMARY KEY,
    patient_name VARCHAR(100) NOT NULL,
    address VARCHAR(200) NOT NULL,
    contact_number VARCHAR(20) NOT NULL,
    dentist_name VARCHAR(100) NOT NULL,
    treatment_type VARCHAR(100) NOT NULL,
    appointment_date DATE NOT NULL,
    appointment_time TIME NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Treatment costs table
CREATE TABLE IF NOT EXISTS treatment_costs (
    treatment_type VARCHAR(100) PRIMARY KEY,
    cost DECIMAL(10, 2) NOT NULL
);

-- Configuration table for consultation fee
CREATE TABLE IF NOT EXISTS config (
    key_name VARCHAR(50) PRIMARY KEY,
    value VARCHAR(100) NOT NULL
);

-- Insert default consultation fee
INSERT INTO config (key_name, value) VALUES ('consultation_fee', '500.00')
ON DUPLICATE KEY UPDATE value = '500.00';

-- Insert common treatment costs
INSERT INTO treatment_costs (treatment_type, cost) VALUES
('Cleaning', '2000.00'),
('Filling', '3500.00'),
('Root Canal', '15000.00'),
('Extraction', '2500.00'),
('Crown', '12000.00'),
('Denture', '18000.00'),
('Braces', '45000.00'),
('Whitening', '8000.00'),
('Checkup', '1500.00'),
('X-Ray', '1000.00')
ON DUPLICATE KEY UPDATE cost = VALUES(cost);
