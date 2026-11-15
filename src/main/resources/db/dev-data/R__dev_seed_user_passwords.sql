-- Dev/local only (loaded via spring.flyway.locations in application-local.yml / application-dev.yml).
-- The hash seeded by V2 does not match the documented password; reset seed users to 'admin123'.
UPDATE users
SET password = '$2a$10$fKrQ62A6PK1YVQz0GvIAFewBN4SCsy0stg7zhi1kzAh1D9G.oldMi'
WHERE username IN ('admin', 'john.doe');

-- A manager to try the MANAGER rights (password 'admin123'); idempotent, as this file reruns when it changes
INSERT INTO users (username, email, password, first_name, last_name, phone, enabled)
SELECT 'maria.manager', 'maria.manager@rentacar.com', '$2a$10$fKrQ62A6PK1YVQz0GvIAFewBN4SCsy0stg7zhi1kzAh1D9G.oldMi',
       'Maria', 'Manager', '+1-555-0202', true
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'maria.manager');

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u, roles r
WHERE u.username = 'maria.manager' AND r.name = 'MANAGER'
  AND NOT EXISTS (SELECT 1 FROM user_roles ur WHERE ur.user_id = u.id AND ur.role_id = r.id);

INSERT INTO employees (user_id, employee_id, branch_id, position, hire_date, is_active)
SELECT u.id, 'EMP002', 1, 'Fleet Manager', DATE '2024-03-01', true FROM users u
WHERE u.username = 'maria.manager' AND NOT EXISTS (SELECT 1 FROM employees e WHERE e.user_id = u.id);
