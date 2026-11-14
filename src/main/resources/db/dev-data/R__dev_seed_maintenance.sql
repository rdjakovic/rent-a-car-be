-- Dev/local only (loaded via spring.flyway.locations in application-local.yml / application-dev.yml).
-- Sample maintenance jobs in every state so the maintenance pages have something to show.
-- Each insert is skipped if its row already exists, so re-running the script does not duplicate data.

-- In progress: the car is in the shop
INSERT INTO maintenance (car_id, employee_id, maintenance_type, description, scheduled_date, status, cost, currency, notes)
SELECT c.id, 1, 'REPAIR', 'Front brake pads and rotors replacement', CURRENT_DATE - INTERVAL '1' DAY, 'IN_PROGRESS', 320.00, 'USD',
       'Squealing brakes reported by the last renter'
FROM cars c
WHERE c.license_plate = 'JGC-5005'
  AND NOT EXISTS (SELECT 1 FROM maintenance m WHERE m.description = 'Front brake pads and rotors replacement');

UPDATE cars SET status = 'MAINTENANCE'
WHERE id IN (SELECT car_id FROM maintenance WHERE status = 'IN_PROGRESS');

-- Overdue: scheduled in the past, never started
INSERT INTO maintenance (car_id, maintenance_type, description, scheduled_date, status, cost, currency)
SELECT c.id, 'INSPECTION', 'Annual safety inspection', CURRENT_DATE - INTERVAL '2' DAY, 'SCHEDULED', 60.00, 'USD'
FROM cars c
WHERE c.license_plate = 'PRI-4004'
  AND NOT EXISTS (SELECT 1 FROM maintenance m WHERE m.description = 'Annual safety inspection');

-- Due today
INSERT INTO maintenance (car_id, maintenance_type, description, scheduled_date, status, cost, currency)
SELECT c.id, 'ROUTINE', 'Tire rotation and cabin air filter', CURRENT_DATE, 'SCHEDULED', 90.00, 'USD'
FROM cars c
WHERE c.license_plate = 'TES-3001'
  AND NOT EXISTS (SELECT 1 FROM maintenance m WHERE m.description = 'Tire rotation and cabin air filter');

-- Open safety recall
INSERT INTO maintenance (car_id, maintenance_type, description, scheduled_date, status, currency, notes)
SELECT c.id, 'RECALL', 'Manufacturer safety recall: airbag inflator replacement', CURRENT_DATE + INTERVAL '3' DAY, 'SCHEDULED', 'USD',
       'Repair is free of charge at the dealer'
FROM cars c
WHERE c.license_plate = 'LRR-9003'
  AND NOT EXISTS (SELECT 1 FROM maintenance m WHERE m.description = 'Manufacturer safety recall: airbag inflator replacement');

-- Service history
INSERT INTO maintenance (car_id, employee_id, maintenance_type, description, scheduled_date, completed_date, status, cost, currency, odometer_reading, notes)
SELECT c.id, 1, 'ROUTINE', 'Oil and filter change', CURRENT_DATE - INTERVAL '60' DAY, CURRENT_DATE - INTERVAL '60' DAY, 'COMPLETED', 129.90, 'USD', 14200,
       'Synthetic 0W-20'
FROM cars c
WHERE c.license_plate = 'ABC-1234'
  AND NOT EXISTS (SELECT 1 FROM maintenance m WHERE m.description = 'Oil and filter change');

INSERT INTO maintenance (car_id, maintenance_type, description, scheduled_date, completed_date, status, cost, currency, odometer_reading)
SELECT c.id, 'ROUTINE', 'Major service: oil, filters, brake fluid', CURRENT_DATE - INTERVAL '45' DAY, CURRENT_DATE - INTERVAL '44' DAY, 'COMPLETED', 189.50, 'USD', 21500
FROM cars c
WHERE c.license_plate = 'GHI-9012'
  AND NOT EXISTS (SELECT 1 FROM maintenance m WHERE m.description = 'Major service: oil, filters, brake fluid');

INSERT INTO maintenance (car_id, maintenance_type, description, scheduled_date, completed_date, status, cost, currency, odometer_reading, notes)
SELECT c.id, 'REPAIR', 'Windshield chip repair', CURRENT_DATE - INTERVAL '20' DAY, CURRENT_DATE - INTERVAL '20' DAY, 'COMPLETED', 85.00, 'USD', 4800,
       'Damage found at return inspection'
FROM cars c
WHERE c.license_plate = 'JKL-3456'
  AND NOT EXISTS (SELECT 1 FROM maintenance m WHERE m.description = 'Windshield chip repair');

INSERT INTO maintenance (car_id, maintenance_type, description, scheduled_date, status, currency, notes)
SELECT c.id, 'INSPECTION', 'Pre-winter inspection', CURRENT_DATE - INTERVAL '10' DAY, 'CANCELLED', 'USD',
       'Done together with the next routine service instead'
FROM cars c
WHERE c.license_plate = 'MNO-7890'
  AND NOT EXISTS (SELECT 1 FROM maintenance m WHERE m.description = 'Pre-winter inspection');

UPDATE cars SET last_service_date = CURRENT_DATE - INTERVAL '60' DAY WHERE license_plate = 'ABC-1234' AND last_service_date IS NULL;
UPDATE cars SET last_service_date = CURRENT_DATE - INTERVAL '44' DAY WHERE license_plate = 'GHI-9012' AND last_service_date IS NULL;
UPDATE cars SET last_service_date = CURRENT_DATE - INTERVAL '20' DAY WHERE license_plate = 'JKL-3456' AND last_service_date IS NULL;

-- Cars coming due for service (no job scheduled yet)
UPDATE cars SET next_service_date = CURRENT_DATE - INTERVAL '3' DAY WHERE license_plate = 'BMW-Z4002' AND next_service_date IS NULL;
UPDATE cars SET next_service_date = CURRENT_DATE + INTERVAL '5' DAY WHERE license_plate = 'MBE-8008' AND next_service_date IS NULL;
UPDATE cars SET next_service_date = CURRENT_DATE + INTERVAL '10' DAY WHERE license_plate = 'AUD-3006' AND next_service_date IS NULL;
