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
('1FTEW1EP6HF567890', 'Ford', 'Transit', 2023, 'VAN', 'MANUAL', 'DIESEL', 8, 18000, 'AVAILABLE', 65.00, 1, 'White', 'MNO-7890');

-- Insert sample customer
INSERT INTO customers (first_name, last_name, email, phone, driver_license_no, date_of_birth, address, city, country, license_expiry_date) VALUES 
('Alice', 'Johnson', 'alice.johnson@email.com', '+1-555-0301', 'DL123456789', '1985-03-15', '789 Oak Street', 'New York', 'USA', '2028-03-15'),
('Bob', 'Smith', 'bob.smith@email.com', '+1-555-0302', 'DL987654321', '1990-07-22', '321 Pine Avenue', 'Los Angeles', 'USA', '2027-07-22');

-- Insert sample reservation
INSERT INTO reservations (customer_id, car_id, start_date, end_date, pickup_branch_id, dropoff_branch_id, status, total_price, currency, notes) VALUES 
(1, 1, DATEADD('DAY', 7, CURRENT_DATE), DATEADD('DAY', 10, CURRENT_DATE), 1, 1, 'CONFIRMED', 135.00, 'USD', 'Customer requested GPS navigation system');

-- Insert sample maintenance record
INSERT INTO maintenance (car_id, employee_id, maintenance_type, description, scheduled_date, status, cost, currency, notes) VALUES 
(1, 1, 'ROUTINE', 'Regular 15,000 mile service - oil change, filter replacement, tire rotation', DATEADD('DAY', 30, CURRENT_DATE), 'SCHEDULED', 150.00, 'USD', 'Due for scheduled maintenance');

-- Insert sample payment
INSERT INTO payments (reservation_id, amount, currency, status, payment_method, provider, transaction_ref, payment_date) VALUES 
(1, 135.00, 'USD', 'CAPTURED', 'CREDIT_CARD', 'Stripe', 'txn_abc123def456', CURRENT_TIMESTAMP);
