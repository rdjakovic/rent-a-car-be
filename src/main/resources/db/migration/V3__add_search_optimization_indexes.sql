-- Migration: Add database indexes for reservation search optimization
-- This migration creates indexes to optimize the multi-field search functionality
-- implemented in ReservationRepository.findBySearchTerm()

-- Note: H2 database doesn't support functional indexes with LOWER() function
-- These indexes will still provide performance benefits for the search operations
-- In production PostgreSQL, functional indexes with LOWER() should be used

-- Note: Some indexes already exist from V1 migration:
-- - idx_customers_email (already exists)
-- - idx_reservations_customer (already exists)

-- 1. Composite index on customer search fields (first_name, last_name, email)
-- This optimizes searches across customer name and email fields
CREATE INDEX idx_customers_search_fields ON customers(first_name, last_name, email);

-- 2. Individual indexes on customer fields for partial matches
-- These support individual field searches and LIKE operations
CREATE INDEX idx_customers_first_name ON customers(first_name);
CREATE INDEX idx_customers_last_name ON customers(last_name);
-- idx_customers_email already exists from V1 migration
CREATE INDEX idx_customers_phone ON customers(phone);

-- 3. Indexes on car search fields (make, model)
-- These optimize searches across car details
CREATE INDEX idx_cars_make ON cars(make);
CREATE INDEX idx_cars_model ON cars(model);
CREATE INDEX idx_cars_make_model ON cars(make, model);

-- 4. Index on branch name field
-- This optimizes searches across pickup and dropoff branch names
CREATE INDEX idx_branches_name ON branches(name);

-- 5. Index on reservation created_at for ordering
-- This optimizes the ORDER BY clause in search results
CREATE INDEX idx_reservations_created_at_desc ON reservations(created_at DESC);

-- 6. Composite indexes for JOIN optimization
-- These optimize the JOINs used in the search query
-- idx_reservations_customer already exists from V1 migration
CREATE INDEX idx_reservations_customer_created ON reservations(customer_id, created_at DESC);
CREATE INDEX idx_reservations_car_created ON reservations(car_id, created_at DESC);
CREATE INDEX idx_reservations_pickup_branch_created ON reservations(pickup_branch_id, created_at DESC);
CREATE INDEX idx_reservations_dropoff_branch_created ON reservations(dropoff_branch_id, created_at DESC);

-- Add comments documenting the purpose of these indexes
COMMENT ON INDEX idx_customers_search_fields IS 'Composite index for multi-field customer search optimization';
COMMENT ON INDEX idx_customers_first_name IS 'Index for first name searches';
COMMENT ON INDEX idx_customers_last_name IS 'Index for last name searches';
COMMENT ON INDEX idx_customers_phone IS 'Index for phone number searches';
COMMENT ON INDEX idx_cars_make IS 'Index for car make searches';
COMMENT ON INDEX idx_cars_model IS 'Index for car model searches';
COMMENT ON INDEX idx_cars_make_model IS 'Composite index for car make and model searches';
COMMENT ON INDEX idx_branches_name IS 'Index for branch name searches';
COMMENT ON INDEX idx_reservations_created_at_desc IS 'Index for reservation ordering by creation date';
COMMENT ON INDEX idx_reservations_customer_created IS 'Composite index for customer-reservation JOINs with ordering';
COMMENT ON INDEX idx_reservations_car_created IS 'Composite index for car-reservation JOINs with ordering';
COMMENT ON INDEX idx_reservations_pickup_branch_created IS 'Composite index for pickup branch-reservation JOINs with ordering';
COMMENT ON INDEX idx_reservations_dropoff_branch_created IS 'Composite index for dropoff branch-reservation JOINs with ordering';