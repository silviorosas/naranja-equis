-- Datos de ejemplo para testing (opcional)
-- Este script se puede ejecutar manualmente después de levantar los servicios

USE auth_db;

-- Ejemplo de usuario (la contraseña debería ser hasheada en la aplicación)
-- Password: "password123" hasheado con BCrypt
INSERT INTO users (email, password_hash, phone, document_type, document_number, first_name, last_name, status, two_fa_enabled, created_at, updated_at)
VALUES 
('juan.perez@example.com', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5GyYIR.KeO8g.', '+541112345678', 'DNI', '12345678', 'Juan', 'Pérez', 'ACTIVE', false, NOW(), NOW()),
('maria.garcia@example.com', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5GyYIR.KeO8g.', '+541187654321', 'DNI', '87654321', 'María', 'García', 'ACTIVE', false, NOW(), NOW());

-- Roles
INSERT INTO roles (name, description)
VALUES 
('USER', 'Usuario estándar'),
('ADMIN', 'Administrador del sistema'),
('MERCHANT', 'Comercio');

-- Asignar rol USER a los usuarios
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id 
FROM users u, roles r 
WHERE u.email IN ('juan.perez@example.com', 'maria.garcia@example.com') 
  AND r.name = 'USER';

USE wallet_db;

-- Billeteras de ejemplo
INSERT INTO wallets (user_id, balance, currency, cvu, alias, status, daily_limit, monthly_limit, created_at, updated_at)
VALUES 
(1, 10000.00, 'ARS', '0000003100089134567891', 'juan.perez.wallet', 'ACTIVE', 50000.00, 500000.00, NOW(), NOW()),
(2, 5000.00, 'ARS', '0000003100089187654321', 'maria.garcia.wallet', 'ACTIVE', 50000.00, 500000.00, NOW(), NOW());

USE transaction_db;

-- Transacción de ejemplo
INSERT INTO transactions (id, type, from_wallet_id, to_wallet_id, amount, currency, status, description, created_at, updated_at, completed_at)
VALUES 
(UUID(), 'TRANSFER', 1, 2, 1000.00, 'ARS', 'COMPLETED', 'Pago de prueba', NOW(), NOW(), NOW());

SELECT 'Datos de ejemplo insertados correctamente' AS mensaje;
