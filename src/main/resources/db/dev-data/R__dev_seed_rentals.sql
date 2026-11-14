-- Dev/local only (loaded via spring.flyway.locations in application-local.yml / application-dev.yml).
-- Rentals that are out right now, so pick-up/return and RENTED cars can be tried out.
-- Each insert is skipped if its row already exists, so re-running the script does not duplicate data.

-- On rental, due back in two days
INSERT INTO reservations (customer_id, car_id, start_date, end_date, pickup_branch_id, dropoff_branch_id, status,
                          total_price, currency, notes, mileage_out, picked_up_at)
SELECT cu.id, c.id, CURRENT_DATE - INTERVAL '2' DAY, CURRENT_DATE + INTERVAL '2' DAY, c.branch_id, c.branch_id, 'ACTIVE',
       c.daily_price * 4, 'USD', 'Dev seed: on rental', c.mileage, CURRENT_TIMESTAMP - INTERVAL '2' DAY
FROM cars c, customers cu
WHERE c.license_plate = 'AUD-3006'
  AND cu.email = 'diana.lee@email.com'
  AND NOT EXISTS (SELECT 1 FROM reservations r WHERE r.notes = 'Dev seed: on rental');

-- Overdue: should have come back yesterday
INSERT INTO reservations (customer_id, car_id, start_date, end_date, pickup_branch_id, dropoff_branch_id, status,
                          total_price, currency, notes, mileage_out, picked_up_at)
SELECT cu.id, c.id, CURRENT_DATE - INTERVAL '6' DAY, CURRENT_DATE - INTERVAL '1' DAY, c.branch_id, c.branch_id, 'ACTIVE',
       c.daily_price * 5, 'USD', 'Dev seed: overdue return', c.mileage, CURRENT_TIMESTAMP - INTERVAL '6' DAY
FROM cars c, customers cu
WHERE c.license_plate = 'HCR-7007'
  AND cu.email = 'ethan.wong@email.com'
  AND NOT EXISTS (SELECT 1 FROM reservations r WHERE r.notes = 'Dev seed: overdue return');

UPDATE cars SET status = 'RENTED'
WHERE id IN (SELECT car_id FROM reservations WHERE status = 'ACTIVE');
