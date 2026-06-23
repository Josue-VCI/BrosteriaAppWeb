-- Script SQL Semilla de Base de Datos y Simulación Matemática de 100 Pedidos
-- Base de datos: PostgreSQL
-- Ubicación sugerida en el Backend: backend/src/main/resources/data.sql

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

-- Adjust sequence for PostgreSQL if IDs are set manually
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

-- 5. Insertar Clientes Frecuentes
INSERT INTO clientes (id, name, email, phone, address, total_orders, total_spent, points, created_at) VALUES
(1, 'Renzo Alva', 'renzoalva@gmail.com', '960373441', 'Av. Universitaria 1420, Carabayllo', 0, 0.0, 0, NOW() - INTERVAL '80 days'),
(2, 'Milagros Sánchez', 'milasanchez@gmail.com', '987654321', 'Calle Los Jazmines 432, Carabayllo', 0, 0.0, 0, NOW() - INTERVAL '75 days'),
(3, 'Juan Carlos D.', 'juancarlosd@outlook.com', '955432110', 'Jr. Junín 105, Carabayllo', 0, 0.0, 0, NOW() - INTERVAL '70 days'),
(4, 'Sofia Cárdenas', 'sofia.cardenas@gmail.com', '998877665', 'Av. Túpac Amaru 4500, Carabayllo', 0, 0.0, 0, NOW() - INTERVAL '60 days'),
(5, 'Pedro López', 'pedrolopez@gmail.com', '933221100', 'Asoc. Las Margaritas Mz D Lte 5, Carabayllo', 0, 0.0, 0, NOW() - INTERVAL '50 days'),
(6, 'Elena Mendoza', 'elenamendoza@gmail.com', '944887722', 'Jr. Arequipa 512, Comas', 0, 0.0, 0, NOW() - INTERVAL '40 days'),
(7, 'Diego Torres', 'diegotorres@gmail.com', '912345678', 'Av. San Felipe 890, Carabayllo', 0, 0.0, 0, NOW() - INTERVAL '35 days'),
(8, 'Patricia Ruiz', 'patruiz@gmail.com', '977654312', 'Calle Primavera 212, Carabayllo', 0, 0.0, 0, NOW() - INTERVAL '30 days'),
(9, 'Gustavo Rojas', 'gustavorojas@gmail.com', '988123456', 'Av. Lomas de Carabayllo 120, Carabayllo', 0, 0.0, 0, NOW() - INTERVAL '25 days'),
(10, 'Lucia Fernandez', 'luciafernandez@gmail.com', '955778899', 'Urb. Santa Isabel Calle 4, Carabayllo', 0, 0.0, 0, NOW() - INTERVAL '20 days')
ON CONFLICT (id) DO NOTHING;

SELECT setval(pg_get_serial_sequence('clientes', 'id'), coalesce(max(id), 1)) FROM clientes;

-- 6. Procedimiento PL/pgSQL para generar 100 pedidos simulados consistentes
DO $$
DECLARE
    i INT := 1;
    v_pedido_id INT;
    v_cliente_id INT;
    v_cliente_name VARCHAR(100);
    v_cliente_phone VARCHAR(20);
    v_cliente_address VARCHAR(200);
    
    v_prod_1 INT;
    v_price_1 DOUBLE PRECISION;
    v_prod_2 INT;
    v_price_2 DOUBLE PRECISION;
    v_qty_1 INT;
    v_qty_2 INT;
    
    v_delivery_cost DOUBLE PRECISION;
    v_subtotal DOUBLE PRECISION;
    v_total DOUBLE PRECISION;
    v_type VARCHAR(20);
    v_payment_method VARCHAR(20);
    v_status VARCHAR(20);
    v_order_date TIMESTAMP;
    v_creams VARCHAR(200);
    
    v_creams_list VARCHAR[] := ARRAY['Mayonesa', 'Ketchup', 'Ají de la casa', 'Tártara Premium', 'Salsa Golf', 'Mostaza'];
BEGIN
    -- Limpiar pedidos antiguos para evitar duplicados en pruebas
    DELETE FROM detalle_pedidos;
    DELETE FROM pedidos;

    WHILE i <= 100 LOOP
        -- 1. Determinar Fecha del pedido (distribuidas en los últimos 90 días)
        v_order_date := NOW() - (random() * INTERVAL '90 days');
        
        -- 2. Determinar Cliente (50% clientes registrados, 50% anónimos/caja directa)
        IF random() < 0.6 THEN
            v_cliente_id := (random() * 9)::INT + 1; -- IDs del 1 al 10
            SELECT name, phone, address INTO v_cliente_name, v_cliente_phone, v_cliente_address FROM clientes WHERE id = v_cliente_id;
        ELSE
            v_cliente_id := NULL;
            v_cliente_name := 'Cliente Al Paso #' || i;
            v_cliente_phone := '9' || (10000000 + (random() * 89999999)::INT)::VARCHAR;
            v_cliente_address := '';
        END IF;

        -- 3. Tipo de pedido (Delivery o Pickup)
        IF v_cliente_address <> '' AND random() < 0.7 THEN
            v_type := 'DELIVERY';
            v_delivery_cost := 5.00;
        ELSE
            v_type := 'PICKUP';
            v_delivery_cost := 0.00;
            v_cliente_address := 'Retiro en local';
        END IF;

        -- 4. Método de Pago (55% Yape, 25% Plin, 20% Efectivo)
        IF random() < 0.55 THEN
            v_payment_method := 'YAPE';
        ELSIF random() < 0.80 THEN
            v_payment_method := 'PLIN';
        ELSE
            v_payment_method := 'EFECTIVO';
        END IF;

        -- 5. Estado de Pedido (90% Entregado, 7% Cancelado, 3% Preparando)
        IF random() < 0.90 THEN
            v_status := 'ENTREGADO';
        ELSIF random() < 0.97 THEN
            v_status := 'CANCELADO';
        ELSE
            v_status := 'PREPARANDO';
        END IF;

        -- 6. Seleccionar 2 productos al azar para el pedido
        v_prod_1 := (random() * 26)::INT + 1; -- IDs del 1 al 27
        SELECT price INTO v_price_1 FROM productos WHERE id = v_prod_1;
        v_qty_1 := (random() * 2)::INT + 1; -- Cantidad de 1 o 2

        IF random() < 0.4 THEN
            v_prod_2 := (random() * 26)::INT + 1;
            SELECT price INTO v_price_2 FROM productos WHERE id = v_prod_2;
            v_qty_2 := (random() * 1)::INT + 1; -- Cantidad de 1 o 2
        ELSE
            v_prod_2 := NULL;
            v_price_2 := 0.0;
            v_qty_2 := 0;
        END IF;

        -- Calcular Totales
        v_subtotal := (v_price_1 * v_qty_1) + (v_price_2 * v_qty_2);
        v_total := v_subtotal + v_delivery_cost;

        -- 7. Insertar el Pedido
        INSERT INTO pedidos (customer_name, customer_phone, customer_address, delivery_cost, type, payment_method, total, status, cliente_id, order_date)
        VALUES (v_cliente_name, v_cliente_phone, v_cliente_address, v_delivery_cost, v_type, v_payment_method, v_total, v_status, v_cliente_id, v_order_date)
        RETURNING id INTO v_pedido_id;

        -- 8. Insertar Detalles del Pedido
        -- Selección aleatoria de 3 cremas
        v_creams := v_creams_list[(random() * 5)::INT + 1] || ', ' || v_creams_list[(random() * 5)::INT + 1];

        INSERT INTO detalle_pedidos (pedido_id, producto_id, quantity, subtotal, creams)
        VALUES (v_pedido_id, v_prod_1, v_qty_1, (v_price_1 * v_qty_1), v_creams);

        IF v_prod_2 IS NOT NULL THEN
            INSERT INTO detalle_pedidos (pedido_id, producto_id, quantity, subtotal, creams)
            VALUES (v_pedido_id, v_prod_2, v_qty_2, (v_price_2 * v_qty_2), v_creams);
        END IF;

        -- 9. Actualizar estadísticas de clientes si el cliente está registrado y el pedido se entregó
        IF v_cliente_id IS NOT NULL AND v_status = 'ENTREGADO' THEN
            UPDATE clientes 
            SET total_orders = total_orders + 1,
                total_spent = total_spent + v_total,
                points = points + (v_total::INT / 10) -- 1 punto por cada S/. 10
            WHERE id = v_cliente_id;
        END IF;

        i := i + 1;
    END LOOP;
END $$;
