-- Roles become STAFF < MANAGER < ADMIN (rights per role: config/security/RolePermissions).
-- EMPLOYEE is renamed to STAFF (same people). CUSTOMER is retired with the public registration.
UPDATE roles SET name = 'STAFF', description = 'Counter staff: customers, reservations, scheduling maintenance'
WHERE name = 'EMPLOYEE';

INSERT INTO roles (name, description) VALUES
('MANAGER', 'Manager: staff rights plus the fleet and running maintenance');

UPDATE roles SET description = 'Administrator: everything, including branches and settings'
WHERE name = 'ADMIN';

-- Users who were only customers have no rights left: disable them
UPDATE users SET enabled = false
WHERE id IN (SELECT ur.user_id FROM user_roles ur JOIN roles r ON r.id = ur.role_id WHERE r.name = 'CUSTOMER')
  AND id NOT IN (SELECT ur.user_id FROM user_roles ur JOIN roles r ON r.id = ur.role_id WHERE r.name <> 'CUSTOMER');

DELETE FROM user_roles WHERE role_id = (SELECT id FROM roles WHERE name = 'CUSTOMER');
DELETE FROM roles WHERE name = 'CUSTOMER';
