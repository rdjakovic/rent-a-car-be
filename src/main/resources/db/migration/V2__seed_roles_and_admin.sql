-- Insert roles
INSERT INTO roles (name, description) VALUES 
('ADMIN', 'System administrator with full access'),
('EMPLOYEE', 'Company employee with operational access'),
('CUSTOMER', 'Customer with rental access');

-- Insert admin user
-- Password is 'admin123' encoded with BCrypt
INSERT INTO users (username, email, password, first_name, last_name, phone, enabled) VALUES 
('admin', 'admin@rentacar.com', '$2a$10$8.UnVuG9HHgffUDAlk8qfOuVGkqRzgVymGe07xd00DMxs.AQubh4a', 'System', 'Administrator', '+1-555-0100', true);

-- Assign admin role to admin user
INSERT INTO user_roles (user_id, role_id) VALUES 
((SELECT id FROM users WHERE username = 'admin'), (SELECT id FROM roles WHERE name = 'ADMIN'));

-- Insert sample branches
INSERT INTO branches (name, address, city, country, phone, email, opening_hours) VALUES 
('Downtown Branch', '123 Main Street', 'New York', 'USA', '+1-555-0101', 'downtown@rentacar.com', 'Mon-Fri 8:00-18:00, Sat 9:00-17:00, Sun 10:00-16:00'),
('Airport Branch', 'JFK Airport Terminal 4', 'New York', 'USA', '+1-555-0102', 'airport@rentacar.com', 'Daily 6:00-24:00'),
('West Side Branch', '456 West Avenue', 'Los Angeles', 'USA', '+1-555-0103', 'westside@rentacar.com', 'Mon-Fri 7:30-19:00, Sat-Sun 9:00-17:00');

-- Insert sample employee user and employee record
INSERT INTO users (username, email, password, first_name, last_name, phone, enabled) VALUES 
('john.doe', 'john.doe@rentacar.com', '$2a$10$8.UnVuG9HHgffUDAlk8qfOuVGkqRzgVymGe07xd00DMxs.AQubh4a', 'John', 'Doe', '+1-555-0201', true);

INSERT INTO user_roles (user_id, role_id) VALUES 
((SELECT id FROM users WHERE username = 'john.doe'), (SELECT id FROM roles WHERE name = 'EMPLOYEE'));

INSERT INTO employees (user_id, employee_id, branch_id, position, hire_date, salary, is_active) VALUES 
((SELECT id FROM users WHERE username = 'john.doe'), 'EMP001', 1, 'Branch Manager', '2024-01-15', 55000.00, true);

-- Insert sample cars
INSERT INTO cars (vin, make, model, car_year, category, transmission, fuel_type, seats, mileage, status, daily_price, branch_id, color, license_plate) VALUES
('1HGBH41JXMN109186', 'Toyota', 'Camry', 2023, 'INTERMEDIATE', 'AUTOMATIC', 'GASOLINE', 5, 15000, 'AVAILABLE', 45.00, 1, 'Silver', 'ABC-1234'),
('2T3RFREV9FW123456', 'Honda', 'Civic', 2024, 'COMPACT', 'AUTOMATIC', 'GASOLINE', 5, 8000, 'AVAILABLE', 35.00, 1, 'White', 'DEF-5678'),
('3VWD17AJ8EM234567', 'Chevrolet', 'Suburban', 2023, 'SUV', 'AUTOMATIC', 'GASOLINE', 8, 22000, 'AVAILABLE', 85.00, 2, 'Black', 'GHI-9012'),
('WBAPL5G59F3A45678', 'BMW', 'X5', 2024, 'LUXURY', 'AUTOMATIC', 'GASOLINE', 5, 5000, 'AVAILABLE', 120.00, 3, 'Blue', 'JKL-3456'),
('1FTEW1EP6HF567890', 'Ford', 'Transit', 2023, 'VAN', 'MANUAL', 'DIESEL', 8, 18000, 'AVAILABLE', 65.00, 1, 'White', 'MNO-7890'),
('5YJ3E1EA7KF317001', 'Tesla', 'Model 3', 2022, 'COMPACT', 'AUTOMATIC', 'ELECTRIC', 5, 12000, 'AVAILABLE', 55.00, 2, 'Red', 'TES-3001'),
('WBA3A5C56DF600002', 'BMW', 'Z4', 2023, 'PREMIUM', 'AUTOMATIC', 'GASOLINE', 2, 4000, 'AVAILABLE', 110.00, 3, 'Yellow', 'BMW-Z4002'),
('SALWR2RV1KA799003', 'Land Rover', 'Range Rover', 2024, 'SUV', 'AUTOMATIC', 'DIESEL', 7, 6000, 'AVAILABLE', 130.00, 2, 'Green', 'LRR-9003'),
('JTDKN3DU0A1234004', 'Toyota', 'Prius', 2022, 'ECONOMY', 'AUTOMATIC', 'HYBRID', 5, 9000, 'AVAILABLE', 38.00, 1, 'Blue', 'PRI-4004'),
('1C4RJFBG8FC123005', 'Jeep', 'Grand Cherokee', 2023, 'SUV', 'AUTOMATIC', 'GASOLINE', 5, 14000, 'AVAILABLE', 75.00, 3, 'Gray', 'JGC-5005'),
('WAUZZZ8V6JA123006', 'Audi', 'A3', 2024, 'COMPACT', 'MANUAL', 'GASOLINE', 4, 3000, 'AVAILABLE', 42.00, 2, 'Black', 'AUD-3006'),
('3CZRE4H59BG123007', 'Honda', 'CR-V', 2023, 'SUV', 'AUTOMATIC', 'HYBRID', 5, 11000, 'AVAILABLE', 68.00, 1, 'Silver', 'HCR-7007'),
('WDDZF4JB1JA123008', 'Mercedes-Benz', 'E-Class', 2024, 'LUXURY', 'AUTOMATIC', 'GASOLINE', 5, 2000, 'AVAILABLE', 140.00, 3, 'White', 'MBE-8008');

-- Insert sample customer
INSERT INTO customers (first_name, last_name, email, phone, driver_license_no, date_of_birth, address, city, country, license_expiry_date) VALUES 
('Alice', 'Johnson', 'alice.johnson@email.com', '+1-555-0301', 'DL123456789', '1985-03-15', '789 Oak Street', 'New York', 'USA', '2028-03-15'),
('Bob', 'Smith', 'bob.smith@email.com', '+1-555-0302', 'DL987654321', '1990-07-22', '321 Pine Avenue', 'Los Angeles', 'USA', '2027-07-22'),
('Carlos', 'Martinez', 'carlos.martinez@email.com', '+1-555-0303', 'DL111223344', '1982-11-05', '12 Maple Drive', 'Chicago', 'USA', '2026-11-05'),
('Diana', 'Lee', 'diana.lee@email.com', '+1-555-0304', 'DL555666777', '1995-02-18', '88 Elm Street', 'San Francisco', 'USA', '2029-02-18'),
('Ethan', 'Wong', 'ethan.wong@email.com', '+1-555-0305', 'DL888999000', '1988-08-30', '23 Cedar Lane', 'Seattle', 'USA', '2028-08-30'),
('Fatima', 'Khan', 'fatima.khan@email.com', '+1-555-0306', 'DL222333444', '1993-12-12', '45 Spruce Road', 'Houston', 'USA', '2027-12-12'),
('George', 'Brown', 'george.brown@email.com', '+1-555-0307', 'DL777888999', '1979-06-25', '67 Willow Ave', 'Miami', 'USA', '2026-06-25'),
('Hannah', 'Kim', 'hannah.kim@email.com', '+1-555-0308', 'DL333444555', '1997-09-14', '90 Birch Blvd', 'Boston', 'USA', '2030-09-14'),
('Ivan', 'Petrov', 'ivan.petrov@email.com', '+1-555-0309', 'DL444555666', '1984-04-09', '101 Aspen Ct', 'Denver', 'USA', '2029-04-09'),
('Julia', 'Rossi', 'julia.rossi@email.com', '+1-555-0310', 'DL555666778', '1992-10-21', '202 Poplar St', 'Las Vegas', 'USA', '2027-10-21');

-- Insert sample reservations (all future dates to comply with CHECK constraint)
INSERT INTO reservations (customer_id, car_id, start_date, end_date, pickup_branch_id, dropoff_branch_id, status, total_price, currency, notes) VALUES 
(1, 1, DATEADD('DAY', 7, CURRENT_DATE), DATEADD('DAY', 10, CURRENT_DATE), 1, 1, 'CONFIRMED', 135.00, 'USD', 'Customer requested GPS navigation system'),
(2, 2, DATEADD('DAY', 1, CURRENT_DATE), DATEADD('DAY', 4, CURRENT_DATE), 2, 2, 'CONFIRMED', 105.00, 'USD', 'Business trip rental'),
(3, 3, DATEADD('DAY', 14, CURRENT_DATE), DATEADD('DAY', 21, CURRENT_DATE), 1, 3, 'CONFIRMED', 595.00, 'USD', 'Family vacation - one way rental'),
(4, 4, DATEADD('DAY', 2, CURRENT_DATE), DATEADD('DAY', 5, CURRENT_DATE), 3, 3, 'CONFIRMED', 360.00, 'USD', 'Weekend luxury rental'),
(5, 5, DATEADD('DAY', 3, CURRENT_DATE), DATEADD('DAY', 5, CURRENT_DATE), 1, 1, 'CONFIRMED', 130.00, 'USD', 'Moving assistance'),
(6, 6, DATEADD('DAY', 1, CURRENT_DATE), DATEADD('DAY', 3, CURRENT_DATE), 2, 2, 'CONFIRMED', 110.00, 'USD', 'Electric vehicle test drive'),
(7, 7, DATEADD('DAY', 21, CURRENT_DATE), DATEADD('DAY', 23, CURRENT_DATE), 3, 3, 'PENDING', 220.00, 'USD', 'Special occasion rental'),
(8, 8, DATEADD('DAY', 8, CURRENT_DATE), DATEADD('DAY', 13, CURRENT_DATE), 2, 1, 'CONFIRMED', 650.00, 'USD', 'Corporate executive rental'),
(9, 9, DATEADD('DAY', 5, CURRENT_DATE), DATEADD('DAY', 12, CURRENT_DATE), 1, 1, 'CONFIRMED', 266.00, 'USD', 'Eco-friendly city driving'),
(10, 10, DATEADD('DAY', 28, CURRENT_DATE), DATEADD('DAY', 35, CURRENT_DATE), 3, 2, 'PENDING', 525.00, 'USD', 'Adventure trip rental'),
(1, 11, DATEADD('DAY', 4, CURRENT_DATE), DATEADD('DAY', 6, CURRENT_DATE), 2, 2, 'CONFIRMED', 84.00, 'USD', 'Compact city rental'),
(2, 12, DATEADD('DAY', 10, CURRENT_DATE), DATEADD('DAY', 17, CURRENT_DATE), 1, 1, 'CONFIRMED', 476.00, 'USD', 'Hybrid SUV for road trip'),
(3, 13, DATEADD('DAY', 6, CURRENT_DATE), DATEADD('DAY', 10, CURRENT_DATE), 3, 3, 'CONFIRMED', 560.00, 'USD', 'Luxury business meetings'),
(4, 1, DATEADD('DAY', 35, CURRENT_DATE), DATEADD('DAY', 42, CURRENT_DATE), 1, 2, 'PENDING', 315.00, 'USD', 'Extended business travel'),
(5, 2, DATEADD('DAY', 15, CURRENT_DATE), DATEADD('DAY', 18, CURRENT_DATE), 2, 3, 'CONFIRMED', 105.00, 'USD', 'Airport transfer service'),
(6, 3, DATEADD('DAY', 18, CURRENT_DATE), DATEADD('DAY', 25, CURRENT_DATE), 3, 1, 'CONFIRMED', 595.00, 'USD', 'Cross-country family trip'),
(7, 4, DATEADD('DAY', 9, CURRENT_DATE), DATEADD('DAY', 12, CURRENT_DATE), 1, 1, 'CONFIRMED', 360.00, 'USD', 'Anniversary celebration'),
(8, 5, DATEADD('DAY', 12, CURRENT_DATE), DATEADD('DAY', 19, CURRENT_DATE), 2, 2, 'CONFIRMED', 455.00, 'USD', 'Contractor work vehicle'),
(9, 6, DATEADD('DAY', 11, CURRENT_DATE), DATEADD('DAY', 14, CURRENT_DATE), 3, 3, 'CONFIRMED', 165.00, 'USD', 'Green technology showcase'),
(10, 7, DATEADD('DAY', 25, CURRENT_DATE), DATEADD('DAY', 27, CURRENT_DATE), 1, 1, 'PENDING', 220.00, 'USD', 'Sports car weekend');

-- Insert sample maintenance record
INSERT INTO maintenance (car_id, employee_id, maintenance_type, description, scheduled_date, status, cost, currency, notes) VALUES 
(1, 1, 'ROUTINE', 'Regular 15,000 mile service - oil change, filter replacement, tire rotation', DATEADD('DAY', 30, CURRENT_DATE), 'SCHEDULED', 150.00, 'USD', 'Due for scheduled maintenance');

-- Insert sample payments (only for CONFIRMED reservations)
INSERT INTO payments (reservation_id, amount, currency, status, payment_method, provider, transaction_ref, payment_date) VALUES 
(1, 135.00, 'USD', 'CAPTURED', 'CREDIT_CARD', 'Stripe', 'txn_abc123def456', CURRENT_TIMESTAMP),
(2, 105.00, 'USD', 'CAPTURED', 'CREDIT_CARD', 'PayPal', 'txn_def456ghi789', CURRENT_TIMESTAMP),
(3, 595.00, 'USD', 'CAPTURED', 'DEBIT_CARD', 'Stripe', 'txn_ghi789jkl012', CURRENT_TIMESTAMP),
(4, 360.00, 'USD', 'CAPTURED', 'CREDIT_CARD', 'Square', 'txn_jkl012mno345', CURRENT_TIMESTAMP),
(5, 130.00, 'USD', 'CAPTURED', 'CASH', 'CASH', 'cash_001', CURRENT_TIMESTAMP),
(6, 110.00, 'USD', 'CAPTURED', 'CREDIT_CARD', 'Stripe', 'txn_mno345pqr678', CURRENT_TIMESTAMP),
(8, 650.00, 'USD', 'CAPTURED', 'CREDIT_CARD', 'AmEx', 'txn_pqr678stu901', CURRENT_TIMESTAMP),
(9, 266.00, 'USD', 'CAPTURED', 'DEBIT_CARD', 'PayPal', 'txn_stu901vwx234', CURRENT_TIMESTAMP),
(11, 84.00, 'USD', 'CAPTURED', 'CREDIT_CARD', 'Stripe', 'txn_vwx234yzab567', CURRENT_TIMESTAMP),
(12, 476.00, 'USD', 'CAPTURED', 'CREDIT_CARD', 'Square', 'txn_yzab567cdef890', CURRENT_TIMESTAMP),
(13, 560.00, 'USD', 'CAPTURED', 'CREDIT_CARD', 'Stripe', 'txn_cdef890ghij123', CURRENT_TIMESTAMP),
(15, 105.00, 'USD', 'CAPTURED', 'DEBIT_CARD', 'PayPal', 'txn_ghij123klmn456', CURRENT_TIMESTAMP),
(16, 595.00, 'USD', 'CAPTURED', 'CREDIT_CARD', 'Stripe', 'txn_klmn456opqr789', CURRENT_TIMESTAMP),
(17, 360.00, 'USD', 'CAPTURED', 'CREDIT_CARD', 'Square', 'txn_opqr789stuv012', CURRENT_TIMESTAMP),
(18, 455.00, 'USD', 'CAPTURED', 'CREDIT_CARD', 'AmEx', 'txn_stuv012wxyz345', CURRENT_TIMESTAMP),
(19, 165.00, 'USD', 'CAPTURED', 'CREDIT_CARD', 'Stripe', 'txn_wxyz345abcd678', CURRENT_TIMESTAMP);
