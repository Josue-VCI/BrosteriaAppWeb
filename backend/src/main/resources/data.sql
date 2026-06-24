-- Script SQL Semilla de Base de Datos y Simulación Matemática de 100 Pedidos
-- Base de datos: PostgreSQL
-- Ubicación: backend/src/main/resources/data.sql

-- 1. Insertar Roles por Defecto
INSERT INTO roles (id, name) VALUES 
(1, 'ADMIN'),
(2, 'CAJERO'),
(3, 'COCINERO')
ON CONFLICT (id) DO NOTHING;

-- 2. Insertar Usuarios del Personal (Claves hasheadas con BCrypt para 'admin123' y 'cajero123')
INSERT INTO users (id, name, email, password_hash, role_id, created_at) VALUES
(1, 'Josue Espinoza (Admin)', 'admin@brosteria.com', '$2a$10$8.uXF3I.u4uS3mK666wOuep9qYyKqK4m/qB8uA1uS85rY22S6.w0G', 1, NOW()),
(2, 'Carlos Cajero', 'cajero@brosteria.com', '$2a$10$8.uXF3I.u4uS3mK666wOuep9qYyKqK4m/qB8uA1uS85rY22S6.w0G', 2, NOW())
ON CONFLICT (id) DO NOTHING;

-- 3. Insertar Catálogo de Productos Reales de La Brostería
INSERT INTO productos (id, name, description, price, category, image_url, active) VALUES
-- Combos Mundialistas
(1, 'Combo El Hincha ⚽', '1 Pieza de pollo broster + Papas fritas + Chicha helada o gaseosa personal', 15.00, 'COMBOS', 'images/combo_pecho.png', true),
(2, 'Combo La Jugada Familiar 🏆', '3 Piezas de pollo broster + 2 Salchipapas personales + Gaseosa de 1.5 Litros', 45.00, 'COMBOS', 'images/combo_familiar.png', true),
(3, 'Combo Gol de Media Cancha 🥅', '2 Piezas de pollo broster + Papas fritas + 1 gaseosa personal', 25.00, 'COMBOS', 'images/combo_pecho.png', true),
(4, 'Combo Tiempo Extra ⏱️', '4 Piezas de pollo broster + Papas familiares + Ensalada + Gaseosa 1.5L', 55.00, 'COMBOS', 'images/combo_familiar.png', true),
-- Clásicos de Siempre
(5, 'Combo Pecho Crujiente 🤤', '1 Pecho broster crocante + Porción generosa de papas fritas', 13.00, 'CLASICOS', 'images/combo_pecho.png', true),
(6, 'Combo Pierna Jugosa 🍗', '1 Pierna broster jugosa + Porción de papas fritas', 11.00, 'CLASICOS', 'images/combo_pierna.png', true),
(7, 'Combo Ala Dorada 🦴', '1 Ala broster crocante + Porción de papas fritas', 9.00, 'CLASICOS', 'images/combo_pierna.png', true),
(8, '1/4 de Pollo Broster', 'Cuarto de pollo broster (pecho o pierna) + Papas + Ensalada', 18.00, 'CLASICOS', 'images/combo_pecho.png', true),
(9, '1/2 Pollo Broster', 'Medio pollo broster (2 piezas) + Papas + Ensalada + Cremas', 32.00, 'CLASICOS', 'images/combo_familiar.png', true),
(10, 'Pollo Entero Broster', 'Pollo entero broster (4 piezas) + Papas familiares + Ensalada', 55.00, 'CLASICOS', 'images/combo_familiar.png', true),
-- Salchipapas
(11, 'Salchipapa Personal 🍟', 'Papas fritas crocantes + Hot dog en rodajas + Cremas', 8.00, 'SALCHIPAPAS', 'images/salchipapas.png', true),
(12, 'Salchipapa Familiar Especial 🍟', 'Doble porción de papas + Hot dog abundante + Cremas', 15.00, 'SALCHIPAPAS', 'images/salchipapas.png', true),
(13, 'Papas Fritas Solas', 'Porción clásica de papas fritas crujientes', 5.00, 'SALCHIPAPAS', 'images/salchipapas.png', true),
(14, 'Brosteipapa Extrema 🔥', 'Papas fritas + Tiras de pollo broster crujiente encima + Cremas', 12.00, 'SALCHIPAPAS', 'images/salchipapas.png', true),
-- Hamburguesas
(15, 'Hamburguesa Clásica', 'Carne smash simple + Lechuga + Tomate + Cremas', 10.00, 'BURGERS', 'images/hamburguesa.png', true),
(16, 'Hamburguesa Broster 🍔', 'Filete de pollo broster + Lechuga + Tomate + Cremas de la casa', 12.00, 'BURGERS', 'images/hamburguesa.png', true),
(17, 'Hamburguesa Doble Smash', 'Doble carne smash + Doble queso cheddar + Tocino', 15.00, 'BURGERS', 'images/hamburguesa.png', true),
-- Alitas
(18, 'Alitas BBQ (6 unidades)', '6 Alitas bañadas en salsa BBQ premium + Papas fritas', 15.00, 'ALITAS', 'images/alitas.png', true),
(19, 'Alitas Picantes (6 unidades) 🌶️', '6 Alitas en salsa búfalo picante + Papas fritas', 15.00, 'ALITAS', 'images/alitas.png', true),
(20, 'Alitas Broster (6 unidades)', '6 Alitas fritas al estilo broster crujiente + Papas', 14.00, 'ALITAS', 'images/alitas.png', true),
-- Bebidas
(21, 'Chicha Morada Helada', 'Chicha morada natural hecha en casa de 500ml', 3.00, 'BEBIDAS', 'images/bebidas.png', true),
(22, 'Gaseosa Personal', 'Gaseosa Inka Cola o Coca Cola helada en botella', 3.00, 'BEBIDAS', 'images/bebidas.png', true),
(23, 'Gaseosa 1.5 Litros', 'Gaseosa Inka Cola o Coca Cola de litro y medio helada', 8.00, 'BEBIDAS', 'images/bebidas.png', true),
(24, 'Limonada Frozen', 'Limonada helada tipo frappe de 500ml', 5.00, 'BEBIDAS', 'images/bebidas.png', true),
-- Extras
(25, 'Porción Extra de Papas', 'Porción adicional de papas fritas', 3.00, 'EXTRAS', 'images/salchipapas.png', true),
(26, 'Ensalada Extra', 'Ensalada fresca de lechuga, tomate y pepino', 2.00, 'EXTRAS', 'images/salchipapas.png', true),
(27, 'Porción de Arroz Blanco', 'Porción de arroz blanco bien graneado', 3.00, 'EXTRAS', 'images/salchipapas.png', true)
ON CONFLICT (id) DO NOTHING;

-- Ajustar secuencias de productos
SELECT setval(pg_get_serial_sequence('productos', 'id'), coalesce(max(id), 1)) FROM productos;

-- 4. Insertar Insumos de Inventario Base
INSERT INTO insumos (id, name, quantity, unit, minimum_stock, updated_at) VALUES
(1, 'Pollo trozado fresco', 180.0, 'unidades', 50.0, NOW()),
(2, 'Papas amarillas cortadas', 145.0, 'kg', 35.0, NOW()),
(3, 'Aceite vegetal de cocina', 75.0, 'litros', 20.0, NOW()),
(4, 'Crema Mayonesa (Galón)', 6.0, 'galones', 2.0, NOW()),
(5, 'Ketchup clásico (Galón)', 4.0, 'galones', 1.5, NOW()),
(6, 'Ají de la casa especial', 8.0, 'litros', 3.0, NOW()),
(7, 'Cajas de cartón combo', 350.0, 'unidades', 100.0, NOW()),
(8, 'Envases de gaseosa descartable', 120.0, 'unidades', 40.0, NOW())
ON CONFLICT (id) DO NOTHING;

SELECT setval(pg_get_serial_sequence('insumos', 'id'), coalesce(max(id), 1)) FROM insumos;

-- 5. Bloque PL/pgSQL para Simulación Masiva (~50 MB de datos variados)
DO $$
DECLARE
    v_order_count INTEGER;
    i INTEGER;
    v_cust_name VARCHAR;
    v_cust_phone VARCHAR;
    v_cust_address VARCHAR;
    v_distrito VARCHAR;
BEGIN
    SELECT COUNT(*) INTO v_order_count FROM pedidos;
    IF v_order_count < 1000 THEN
        -- Limpiar tablas para asegurar carga fresca
        TRUNCATE TABLE detalle_pedidos CASCADE;
        DELETE FROM pedidos;
        DELETE FROM clientes;
        
        -- A. Generar 2000 Clientes con correos ficticios/inválidos
        FOR i IN 1..2000 LOOP
            v_cust_name := CASE (i % 10)
                WHEN 0 THEN 'Carlos'
                WHEN 1 THEN 'María'
                WHEN 2 THEN 'Juan'
                WHEN 3 THEN 'Ana'
                WHEN 4 THEN 'Luis'
                WHEN 5 THEN 'Laura'
                WHEN 6 THEN 'Diego'
                WHEN 7 THEN 'Sofía'
                WHEN 8 THEN 'José'
                ELSE 'Carmen'
            END || ' ' || CASE (i % 8)
                WHEN 0 THEN 'Pérez'
                WHEN 1 THEN 'Gómez'
                WHEN 2 THEN 'Rodríguez'
                WHEN 3 THEN 'Sánchez'
                WHEN 4 THEN 'López'
                WHEN 5 THEN 'Torres'
                WHEN 6 THEN 'Díaz'
                ELSE 'Vargas'
            END || ' (' || i || ')';
            
            v_cust_phone := '9' || CAST(10000000 + (i * 439) % 89999999 AS VARCHAR);
            
            v_distrito := CASE (i % 6)
                WHEN 0 THEN 'Carabayllo'
                WHEN 1 THEN 'Comas'
                WHEN 2 THEN 'Los Olivos'
                WHEN 3 THEN 'Puente Piedra'
                WHEN 4 THEN 'Independencia'
                ELSE 'San Martín de Porres'
            END;
            v_cust_address := 'Av. Principal Mz ' || CHR(65 + (i % 26)) || ' Lote ' || ((i % 15) + 1) || ', ' || v_distrito;
            
            INSERT INTO clientes (id, name, email, phone, address, total_orders, total_spent, points, created_at)
            VALUES (i, v_cust_name, 'cliente' || i || '@brosteria-invalid.local', v_cust_phone, v_cust_address, 0, 0.0, 0, NOW() - (i * INTERVAL '1 hour'));
        END LOOP;
        
        -- B. Generar 50000 Pedidos distribuidos en los últimos 90 días
        INSERT INTO pedidos (id, customer_name, customer_phone, customer_address, delivery_cost, type, payment_method, total, status, order_date, cliente_id)
        SELECT 
          s.id,
          c.name,
          c.phone,
          CASE WHEN s.id % 4 = 3 THEN 'Retiro en local' ELSE c.address END,
          CASE WHEN s.id % 4 = 3 THEN 0.00 ELSE 5.00 END,
          CASE WHEN s.id % 4 = 3 THEN 'PICKUP' ELSE 'DELIVERY' END,
          CASE (s.id % 4) WHEN 0 THEN 'YAPE' WHEN 1 THEN 'PLIN' WHEN 2 THEN 'TARJETA' ELSE 'EFECTIVO' END,
          0.0, -- Total a calcular posteriormente
          CASE (s.id % 15) WHEN 0 THEN 'CANCELADO' WHEN 1 THEN 'PREPARANDO' WHEN 2 THEN 'PENDIENTE' ELSE 'ENTREGADO' END,
          -- Fechas distribuidas, concentradas en horas de comida (12-15h) y cena (18-23h)
          (NOW() - (s.id * INTERVAL '2.5 minutes')) 
            + CASE (s.id % 3)
                WHEN 0 THEN INTERVAL '0 hours'
                WHEN 1 THEN INTERVAL '4 hours'
                ELSE INTERVAL '-2 hours'
              END,
          c.id
        FROM generate_series(1, 50000) AS s(id)
        JOIN clientes c ON c.id = ((s.id % 2000) + 1);

        -- C. Generar DetallePedidos (1 a 2 items por pedido)
        -- Detalle 1 (Todos los pedidos tienen al menos 1 producto)
        INSERT INTO detalle_pedidos (pedido_id, producto_id, quantity, subtotal, creams)
        SELECT 
          p.id,
          p_cat.id,
          ((p.id % 3) + 1), -- Cantidad: 1, 2 o 3
          ((p.id % 3) + 1) * p_cat.price,
          CASE 
            WHEN p_cat.category = 'BEBIDAS' THEN NULL 
            ELSE 'Mayonesa, Ají de la casa' 
          END
        FROM pedidos p
        JOIN productos p_cat ON p_cat.id = ((p.id % 27) + 1);

        -- Detalle 2 (para el 40% de los pedidos)
        INSERT INTO detalle_pedidos (pedido_id, producto_id, quantity, subtotal, creams)
        SELECT 
          p.id,
          p_cat.id,
          1,
          p_cat.price,
          CASE 
            WHEN p_cat.category = 'BEBIDAS' THEN NULL 
            ELSE 'Ketchup' 
          END
        FROM pedidos p
        JOIN productos p_cat ON p_cat.id = (((p.id + 11) % 27) + 1)
        WHERE p.id % 5 IN (1, 3);

        -- D. Actualizar Totales del Pedido sumando delivery + subtotal de detalles
        UPDATE pedidos p
        SET total = COALESCE((SELECT SUM(d.subtotal) FROM detalle_pedidos d WHERE d.pedido_id = p.id), 0) + p.delivery_cost;

        -- E. Actualizar estadísticas acumulativas de Clientes
        UPDATE clientes c
        SET total_orders = COALESCE((SELECT COUNT(*) FROM pedidos p WHERE p.cliente_id = c.id AND p.status = 'ENTREGADO'), 0),
            total_spent = COALESCE((SELECT SUM(p.total) FROM pedidos p WHERE p.cliente_id = c.id AND p.status = 'ENTREGADO'), 0.0),
            points = COALESCE((SELECT CAST(SUM(p.total)/10 AS INTEGER) FROM pedidos p WHERE p.cliente_id = c.id AND p.status = 'ENTREGADO'), 0);
            
        -- F. Ajustar secuencias de ID de PostgreSQL
        PERFORM setval(pg_get_serial_sequence('clientes', 'id'), COALESCE(MAX(id), 1)) FROM clientes;
        PERFORM setval(pg_get_serial_sequence('pedidos', 'id'), COALESCE(MAX(id), 1)) FROM pedidos;
        PERFORM setval(pg_get_serial_sequence('detalle_pedidos', 'id'), COALESCE(MAX(id), 1)) FROM detalle_pedidos;
    END IF;
END $$;
