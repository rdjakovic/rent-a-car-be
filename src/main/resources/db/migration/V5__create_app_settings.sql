-- Application-wide settings (key/value), editable by admins via /api/v1/settings
CREATE TABLE app_settings (
    setting_key VARCHAR(100) PRIMARY KEY,
    setting_value VARCHAR(255) NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Unit the fleet's mileage is recorded in (label only, no conversion)
INSERT INTO app_settings (setting_key, setting_value) VALUES ('mileage_unit', 'KILOMETERS');
