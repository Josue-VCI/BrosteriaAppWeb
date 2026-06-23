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

-- 5. Insertar Clientes Frecuentes (Incluyendo correos de prueba reales para masivos)
INSERT INTO clientes (id, name, email, phone, address, total_orders, total_spent, points, created_at) VALUES
(1, 'Renzo Alva', 'renzoalva@mail.com', '960373441', 'Av. Universitaria 1420, Carabayllo', 0, 0.0, 0, NOW() - INTERVAL '80 days'),
(2, 'Milagros Sánchez', 'milasanchez@mail.com', '987654321', 'Calle Los Jazmines 432, Carabayllo', 0, 0.0, 0, NOW() - INTERVAL '75 days'),
(3, 'Juan Carlos D.', 'juancarlosd@look.com', '955432110', 'Jr. Junín 105, Carabayllo', 0, 0.0, 0, NOW() - INTERVAL '70 days'),
(4, 'Sofia Cárdenas', 'sofia.cardenas@mail.com', '998877665', 'Av. Túpac Amaru 4500, Carabayllo', 0, 0.0, 0, NOW() - INTERVAL '60 days'),
(5, 'Pedro López', 'pedrolopez@mail.com', '933221100', 'Asoc. Las Margaritas Mz D Lte 5, Carabayllo', 0, 0.0, 0, NOW() - INTERVAL '50 days'),
(6, 'Elena Mendoza', 'elenamendoza@mail.com', '944887722', 'Jr. Arequipa 512, Comas', 0, 0.0, 0, NOW() - INTERVAL '40 days'),
(7, 'Diego Torres', 'diegotorres@mail.com', '912345678', 'Av. San Felipe 890, Carabayllo', 0, 0.0, 0, NOW() - INTERVAL '35 days'),
(8, 'Patricia Ruiz', 'patruiz@mail.com', '977654312', 'Calle Primavera 212, Carabayllo', 0, 0.0, 0, NOW() - INTERVAL '30 days'),
(9, 'Gustavo Rojas', 'gustavorojas@mail.com', '988123456', 'Av. Lomas de Carabayllo 120, Carabayllo', 0, 0.0, 0, NOW() - INTERVAL '25 days'),
(10, 'Lucia Fernandez', 'luciafernandez@mail.com', '955778899', 'Urb. Santa Isabel Calle 4, Carabayllo', 0, 0.0, 0, NOW() - INTERVAL '20 days'),
(11, 'Josue Becerra (Pruebas)', 'josuebecerrav@gmail.com', '960373551', 'Carabayllo, Lima', 0, 0.0, 0, NOW() - INTERVAL '15 days'),
(12, 'UPC Mail (Pruebas)', 'u20231f683@upc.edu.pe', '960373552', 'Santiago de Surco, Lima', 0, 0.0, 0, NOW() - INTERVAL '10 days')
ON CONFLICT (id) DO NOTHING;

SELECT setval(pg_get_serial_sequence('clientes', 'id'), coalesce(max(id), 1)) FROM clientes;

-- 6. Insertar 100 Pedidos Simulados con alias explícito para la columna de generate_series
INSERT INTO pedidos (id, customer_name, customer_phone, customer_address, delivery_cost, type, payment_method, total, status, order_date)
SELECT 
  s.id,
  CASE 
    WHEN s.id % 12 = 0 THEN 'Renzo Alva'
    WHEN s.id % 12 = 1 THEN 'Milagros Sánchez'
    WHEN s.id % 12 = 2 THEN 'Juan Carlos D.'
    WHEN s.id % 12 = 3 THEN 'Sofia Cárdenas'
    WHEN s.id % 12 = 4 THEN 'Pedro López'
    WHEN s.id % 12 = 5 THEN 'Elena Mendoza'
    WHEN s.id % 12 = 6 THEN 'Diego Torres'
    WHEN s.id % 12 = 7 THEN 'Patricia Ruiz'
    WHEN s.id % 12 = 8 THEN 'Gustavo Rojas'
    WHEN s.id % 12 = 9 THEN 'Lucia Fernandez'
    WHEN s.id % 12 = 10 THEN 'Josue Becerra (Pruebas)'
    ELSE 'UPC Mail (Pruebas)'
  END,
  '9' || CAST(10000000 + FLOOR(random() * 89999999) AS VARCHAR),
  CASE WHEN s.id % 3 = 0 THEN 'Av. Universitaria 1420, Carabayllo' WHEN s.id % 3 = 1 THEN 'Calle Los Jazmines 432, Carabayllo' ELSE 'Retiro en local' END,
  CASE WHEN s.id % 3 <> 2 THEN 5.00 ELSE 0.00 END,
  CASE WHEN s.id % 3 <> 2 THEN 'DELIVERY' ELSE 'PICKUP' END,
  CASE WHEN s.id % 3 = 0 THEN 'YAPE' WHEN s.id % 3 = 1 THEN 'PLIN' ELSE 'EFECTIVO' END,
  0.0, -- Se actualizará luego
  CASE WHEN s.id % 12 = 0 THEN 'CANCELADO' WHEN s.id % 12 = 1 THEN 'PREPARANDO' ELSE 'ENTREGADO' END,
  NOW() - (s.id * INTERVAL '18 hours')
FROM generate_series(1, 100) AS s(id)
ON CONFLICT (id) DO NOTHING;

-- 7. Insertar Detalles de Pedido (Primer item)
INSERT INTO detalle_pedidos (pedido_id, producto_id, quantity, subtotal, creams)
SELECT 
  p.id,
  ((p.id % 27) + 1)::BIGINT,
  1,
  (SELECT price FROM productos WHERE id = ((p.id % 27) + 1)::BIGINT),
  'Mayonesa, Ají de la casa'
FROM pedidos p
ON CONFLICT DO NOTHING;

-- Segundo item para pedidos impares
INSERT INTO detalle_pedidos (pedido_id, producto_id, quantity, subtotal, creams)
SELECT 
  p.id,
  (((p.id + 5) % 27) + 1)::BIGINT,
  1,
  (SELECT price FROM productos WHERE id = (((p.id + 5) % 27) + 1)::BIGINT),
  'Ketchup'
FROM pedidos p
WHERE p.id % 2 = 1
ON CONFLICT DO NOTHING;

-- 8. Actualizar Totales del Pedido sumando delivery + subtotal de detalles
UPDATE pedidos p
SET total = COALESCE((SELECT SUM(d.subtotal) FROM detalle_pedidos d WHERE d.pedido_id = p.id), 0) + p.delivery_cost;

-- 9. Actualizar estadísticas de Clientes
UPDATE clientes c
SET total_orders = COALESCE((SELECT COUNT(*) FROM pedidos p WHERE p.cliente_id = c.id AND p.status = 'ENTREGADO'), 0),
    total_spent = COALESCE((SELECT SUM(p.total) FROM pedidos p WHERE p.cliente_id = c.id AND p.status = 'ENTREGADO'), 0.0),
    points = COALESCE((SELECT CAST(SUM(p.total)/10 AS INTEGER) FROM pedidos p WHERE p.cliente_id = c.id AND p.status = 'ENTREGADO'), 0);

-- Ajustar secuencias de ID de pedidos en PostgreSQL
SELECT setval(pg_get_serial_sequence('pedidos', 'id'), coalesce(max(id), 1)) FROM pedidos;
SELECT setval(pg_get_serial_sequence('detalle_pedidos', 'id'), coalesce(max(id), 1)) FROM detalle_pedidos;
