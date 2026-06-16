-- ============================================================
--  SportZone — Fix contraseñas a SHA-256
--  Contraseña de todos los usuarios: password123
--  SHA-256("password123") = ef92b778bafe771e89245b89ecbc08a44a4e166c06659911881f383d4473e94f
-- ============================================================

USE sportzone_db;

SET SQL_SAFE_UPDATES = 0;

UPDATE usuarios
SET contrasena_hash = 'ef92b778bafe771e89245b89ecbc08a44a4e166c06659911881f383d4473e94f';

SET SQL_SAFE_UPDATES = 1;

-- Verificar que se actualizaron
SELECT id_usuario, nombre_completo, email, contrasena_hash FROM usuarios;
