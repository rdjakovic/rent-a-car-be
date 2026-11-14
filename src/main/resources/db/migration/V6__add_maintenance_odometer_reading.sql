-- Odometer reading recorded when a maintenance job is completed (in the fleet's mileage unit, see app_settings)
ALTER TABLE maintenance ADD COLUMN odometer_reading INTEGER;
ALTER TABLE maintenance ADD CONSTRAINT chk_maintenance_odometer_not_negative CHECK (odometer_reading >= 0);
