-- Ejecuta este script UNA sola vez (psql, pgAdmin o la consola de base de datos de IntelliJ)
-- con un usuario administrador de PostgreSQL (normalmente "postgres").
-- Solo crea la base vacía: las TABLAS las crea Hibernate solo (spring.jpa.hibernate.ddl-auto=update)
-- y los 12 animales de ejemplo los inserta la clase DataSeeder la primera vez que arranca la app.
CREATE DATABASE oceano_db
    WITH ENCODING 'UTF8'
    TEMPLATE template0;
