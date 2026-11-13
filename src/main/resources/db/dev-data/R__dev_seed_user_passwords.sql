-- Dev/local only (loaded via spring.flyway.locations in application-local.yml / application-dev.yml).
-- The hash seeded by V2 does not match the documented password; reset seed users to 'admin123'.
UPDATE users
SET password = '$2a$10$fKrQ62A6PK1YVQz0GvIAFewBN4SCsy0stg7zhi1kzAh1D9G.oldMi'
WHERE username IN ('admin', 'john.doe');
