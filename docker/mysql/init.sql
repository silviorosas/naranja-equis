-- Script de inicialización para MySQL
-- Crea las bases de datos necesarias para los microservicios

CREATE DATABASE IF NOT EXISTS auth_db;
CREATE DATABASE IF NOT EXISTS wallet_db;
CREATE DATABASE IF NOT EXISTS transaction_db;

-- Otorgar permisos al usuario
GRANT ALL PRIVILEGES ON auth_db.* TO 'naranjax_user'@'%';
GRANT ALL PRIVILEGES ON wallet_db.* TO 'naranjax_user'@'%';
GRANT ALL PRIVILEGES ON transaction_db.* TO 'naranjax_user'@'%';

FLUSH PRIVILEGES;

-- Info
SELECT 'Bases de datos creadas exitosamente' AS mensaje;
