-- Sunrise Dental Clinic Sample Data
-- DEVELOPMENT/TEST DATA ONLY
-- DO NOT USE IN PRODUCTION

USE dental_clinic;

-- Insert sample user account
-- Username: sasrin
-- Password: sasrin123 (hashed using SHA-256 with salt)
INSERT INTO users (username, password_hash) VALUES
('sasrin', 'xXoIsx0qZ5V+lg50fUw5Rd97D2BMyljDc2oPP8h7uyUv7FpSbJw0PMzFMhaCpnSA');

-- Insert sample appointments for testing
INSERT INTO appointments (appointment_number, patient_name, address, contact_number, dentist_name, treatment_type, appointment_date, appointment_time) VALUES
('APT001', 'John Smith', '123 Main Street, Colombo', '0771234567', 'Dr. Perera', 'Cleaning', '2026-09-10', '09:00:00'),
('APT002', 'Mary Johnson', '456 Lake Road, Colombo', '0779876543', 'Dr. Silva', 'Filling', '2026-09-11', '10:30:00'),
('APT003', 'Kamal Fernando', '789 Hill View, Kandy', '0712345678', 'Dr. Perera', 'Root Canal', '2026-09-12', '14:00:00'),
('APT004', 'Nimali Rathnayake', '321 Ocean Drive, Galle', '0756789012', 'Dr. Silva', 'Extraction', '2026-09-13', '11:00:00'),
('APT005', 'Sunil Wijesinghe', '654 Garden Lane, Colombo', '0723456789', 'Dr. Perera', 'Checkup', '2026-09-14', '08:30:00');
