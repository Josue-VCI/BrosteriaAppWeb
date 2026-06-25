-- Script SQL Semilla de Base de Datos y Simulación Matemática de 100 Pedidos
-- Base de datos: PostgreSQL
-- Ubicación: backend/src/main/resources/data.sql

-- 1. Insertar Roles por Defecto
INSERT INTO roles (id, name) VALUES 
(1, 'ADMIN'),
(2, 'CAJERO'),
(3, 'COCINERO')
ON CONFLICT (id) DO NOTHING;

-- 2. Insertar Usuarios del Personal (2 Administradores y 3 Cajeros de Atención con BCrypt)
INSERT INTO users (id, name, email, password_hash, role_id, created_at) VALUES
(1, 'Josue Espinoza (Admin 1)', 'admin@brosteria.com', '$2a$10$mDZ0jfgebisarO9aLiK4Q.Xd8A7xVnRDCToqdZj/cpWqUkAUQjg4G', 1, NOW()),
(2, 'Carlos Cajero (Cajero 1)', 'cajero@brosteria.com', '$2a$10$.feCm2bJRkdWwJNo74SSM.5NQHWA0mINlKzznSvgP27nfy0O0Cb1S', 2, NOW()),
(3, 'Administrador 2', 'admin2@brosteria.com', '$2a$10$mDZ0jfgebisarO9aLiK4Q.Xd8A7xVnRDCToqdZj/cpWqUkAUQjg4G', 1, NOW()),
(4, 'Atención 2', 'cajero2@brosteria.com', '$2a$10$.feCm2bJRkdWwJNo74SSM.5NQHWA0mINlKzznSvgP27nfy0O0Cb1S', 2, NOW()),
(5, 'Atención 3', 'cajero3@brosteria.com', '$2a$10$.feCm2bJRkdWwJNo74SSM.5NQHWA0mINlKzznSvgP27nfy0O0Cb1S', 2, NOW())
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

-- 5. Simulación Masiva condicional (3,600 pedidos, ~2.5 MB de volumen de datos, 40 pedidos/día)

-- A. Limpiar tablas si el total de pedidos no coincide con la meta de 3,600 para asegurar re-inicialización
DELETE FROM detalle_pedidos WHERE (SELECT COUNT(*) FROM pedidos) <> 3600;
DELETE FROM pedidos WHERE (SELECT COUNT(*) FROM pedidos) <> 3600;
DELETE FROM clientes WHERE (SELECT COUNT(*) FROM clientes) > 1500;

-- B. Generar 200 Clientes con correos ficticios/inválidos si no se ha inicializado la base de datos
INSERT INTO clientes (id, name, email, phone, address, total_orders, total_spent, points, created_at)
SELECT 
  i,
  CASE (i % 10)
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
  END || ' (' || i || ')',
  'cliente' || i || '@brosteria-invalid.local',
  '9' || CAST(10000000 + (i * 439) % 89999999 AS VARCHAR),
  'Av. Principal Mz ' || CHR(65 + (i % 26)) || ' Lote ' || ((i % 15) + 1) || ', ' || 
    CASE (i % 5)
        WHEN 0 THEN 'Surquillo'
        WHEN 1 THEN 'Miraflores'
        WHEN 2 THEN 'San Borja'
        WHEN 3 THEN 'San Isidro'
        ELSE 'Santiago de Surco'
    END,
  0,
  0.0,
  0,
  NOW() - (i * INTERVAL '2 hours')
FROM generate_series(1, 200) AS i
WHERE NOT EXISTS (SELECT 1 FROM clientes WHERE email LIKE '%@brosteria-invalid.local' LIMIT 1);

-- C. Generar 3600 Pedidos distribuidos en 90 días si no existen
INSERT INTO pedidos (id, customer_name, customer_phone, customer_address, delivery_cost, type, payment_method, total, status, order_date, cliente_id)
SELECT 
  s.id,
  c.name,
  c.phone,
  CASE WHEN s.id % 4 = 3 THEN 'Retiro en local' ELSE c.address END,
  CASE WHEN s.id % 4 = 3 THEN 0.00 ELSE 5.00 END,
  CASE WHEN s.id % 4 = 3 THEN 'PICKUP' ELSE 'DELIVERY' END,
  CASE (s.id % 4) WHEN 0 THEN 'YAPE' WHEN 1 THEN 'PLIN' WHEN 2 THEN 'TARJETA' ELSE 'EFECTIVO' END,
  -- Total = subtotal1 (qty * price1) + subtotal2 (price2 si id % 5 in (1,3)) + delivery_cost
  ( ((s.id % 3) + 1) * p1.price ) + 
  ( CASE WHEN s.id % 5 IN (1, 3) THEN p2.price ELSE 0.0 END ) + 
  ( CASE WHEN s.id % 4 = 3 THEN 0.00 ELSE 5.00 END ),
  CASE 
    WHEN s.id <= 50 THEN
      CASE (s.id % 10)
        WHEN 0 THEN 'PENDIENTE'
        WHEN 1 THEN 'PREPARANDO'
        ELSE 'ENVIADO'
      END
    ELSE
      CASE WHEN (s.id % 20) = 0 THEN 'CANCELADO' ELSE 'ENTREGADO' END
  END,
  (NOW() - (s.id * INTERVAL '36 minutes')) -- 3600 pedidos distribuidos en 90 días (3600 * 36 min = 129600 min = 90 días)
    + CASE (s.id % 3)
        WHEN 0 THEN INTERVAL '0 hours'
        WHEN 1 THEN INTERVAL '4 hours'
        ELSE INTERVAL '-2 hours'
      END,
  c.id
FROM generate_series(1, 3600) AS s(id)
JOIN clientes c ON c.id = ((s.id % 200) + 1)
JOIN productos p1 ON p1.id = ((s.id % 27) + 1)
JOIN productos p2 ON p2.id = (((s.id + 11) % 27) + 1)
WHERE c.email LIKE '%@brosteria-invalid.local'
  AND NOT EXISTS (
    SELECT 1 
    FROM pedidos p_exist 
    JOIN clientes c_exist ON p_exist.cliente_id = c_exist.id 
    WHERE c_exist.email LIKE '%@brosteria-invalid.local' 
    LIMIT 1
  );

-- D. Generar DetallePedidos (Detalle 1: para todos los pedidos seeded)
INSERT INTO detalle_pedidos (pedido_id, producto_id, quantity, subtotal, creams)
SELECT 
  p.id,
  p_cat.id,
  ((p.id % 3) + 1), 
  ((p.id % 3) + 1) * p_cat.price,
  CASE 
    WHEN p_cat.category = 'BEBIDAS' THEN NULL 
    ELSE 'Mayonesa, Ají de la casa' 
  END
FROM pedidos p
JOIN productos p_cat ON p_cat.id = ((p.id % 27) + 1)
JOIN clientes c ON c.id = p.cliente_id
WHERE c.email LIKE '%@brosteria-invalid.local'
  AND NOT EXISTS (
    SELECT 1 
    FROM detalle_pedidos dp 
    WHERE dp.pedido_id = p.id 
      AND dp.producto_id = p_cat.id 
    LIMIT 1
  );

-- E. Generar DetallePedidos (Detalle 2: para el 40% de los pedidos seeded)
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
JOIN clientes c ON c.id = p.cliente_id
WHERE p.id % 5 IN (1, 3)
  AND c.email LIKE '%@brosteria-invalid.local'
  AND NOT EXISTS (
    SELECT 1 
    FROM detalle_pedidos dp 
    WHERE dp.pedido_id = p.id 
      AND dp.producto_id = p_cat.id 
    LIMIT 1
  );

-- F. Actualizar estadísticas acumulativas de Clientes usando un solo UPDATE agrupado (muy rápido)
UPDATE clientes c
SET total_orders = sub.cnt,
    total_spent = sub.spent,
    points = CAST(sub.spent / 10 AS INTEGER)
FROM (
    SELECT p.cliente_id, 
           COUNT(*) as cnt, 
           SUM(p.total) as spent
    FROM pedidos p
    JOIN clientes cl ON cl.id = p.cliente_id
    WHERE p.status = 'ENTREGADO'
      AND cl.email LIKE '%@brosteria-invalid.local'
    GROUP BY p.cliente_id
) sub
WHERE c.id = sub.cliente_id
  AND c.email LIKE '%@brosteria-invalid.local'
  AND c.total_orders = 0;
      
-- G. Ajustar secuencias de ID de PostgreSQL
SELECT setval(pg_get_serial_sequence('users', 'id'), COALESCE(MAX(id), 1)) FROM users;
SELECT setval(pg_get_serial_sequence('clientes', 'id'), COALESCE(MAX(id), 1)) FROM clientes;
SELECT setval(pg_get_serial_sequence('pedidos', 'id'), COALESCE(MAX(id), 1)) FROM pedidos;
SELECT setval(pg_get_serial_sequence('detalle_pedidos', 'id'), COALESCE(MAX(id), 1)) FROM detalle_pedidos;
