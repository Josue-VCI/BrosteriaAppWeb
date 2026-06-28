-- Ejecutar una sola vez en el SQL Editor de Supabase.
-- Es transaccional, idempotente y no modifica IDs ni relaciones.
BEGIN;

UPDATE productos AS p
SET name = clean.name,
    description = clean.description
FROM (VALUES
    (1, 'Combo El Hincha', '1 Pieza de pollo broster + Papas fritas + Chicha helada o gaseosa personal'),
    (2, 'Combo La Jugada Familiar', '3 Piezas de pollo broster + 2 Salchipapas personales + Gaseosa de 1.5 Litros'),
    (3, 'Combo Gol de Media Cancha', '2 Piezas de pollo broster + Papas fritas + 1 gaseosa personal'),
    (4, 'Combo Tiempo Extra', '4 Piezas de pollo broster + Papas familiares + Ensalada + Gaseosa 1.5L'),
    (5, 'Combo Pecho Crujiente', '1 Pecho broster crocante + Porcion generosa de papas fritas'),
    (6, 'Combo Pierna Jugosa', '1 Pierna broster jugosa + Porcion de papas fritas'),
    (7, 'Combo Ala Dorada', '1 Ala broster crocante + Porcion de papas fritas'),
    (8, '1/4 de Pollo Broster', 'Cuarto de pollo broster (pecho o pierna) + Papas + Ensalada'),
    (9, '1/2 Pollo Broster', 'Medio pollo broster (2 piezas) + Papas + Ensalada + Cremas'),
    (10, 'Pollo Entero Broster', 'Pollo entero broster (4 piezas) + Papas familiares + Ensalada'),
    (11, 'Salchipapa Personal', 'Papas fritas crocantes + Hot dog en rodajas + Cremas'),
    (12, 'Salchipapa Familiar Especial', 'Doble porcion de papas + Hot dog abundante + Cremas'),
    (13, 'Papas Fritas Solas', 'Porcion clasica de papas fritas crujientes'),
    (14, 'Brosteipapa Extrema', 'Papas fritas + Tiras de pollo broster crujiente encima + Cremas'),
    (15, 'Hamburguesa Clasica', 'Carne smash simple + Lechuga + Tomate + Cremas'),
    (16, 'Hamburguesa Broster', 'Filete de pollo broster + Lechuga + Tomate + Cremas de la casa'),
    (17, 'Hamburguesa Doble Smash', 'Doble carne smash + Doble queso cheddar + Tocino'),
    (18, 'Alitas BBQ (6 unidades)', '6 Alitas banadas en salsa BBQ premium + Papas fritas'),
    (19, 'Alitas Picantes (6 unidades)', '6 Alitas en salsa bufalo picante + Papas fritas'),
    (20, 'Alitas Broster (6 unidades)', '6 Alitas fritas al estilo broster crujiente + Papas'),
    (21, 'Chicha Morada Helada', 'Chicha morada natural hecha en casa de 500ml'),
    (22, 'Gaseosa Personal', 'Gaseosa Inka Cola o Coca Cola helada en botella'),
    (23, 'Gaseosa 1.5 Litros', 'Gaseosa Inka Cola o Coca Cola de litro y medio helada'),
    (24, 'Limonada Frozen', 'Limonada helada tipo frappe de 500ml'),
    (25, 'Porcion Extra de Papas', 'Porcion adicional de papas fritas'),
    (26, 'Ensalada Extra', 'Ensalada fresca de lechuga, tomate y pepino'),
    (27, 'Porcion de Arroz Blanco', 'Porcion de arroz blanco bien graneado'),
    (28, 'Promo Duo Crujiente', '1/4 de Pollo Broster Pecho + 1 Salchipapa Personal + 2 Vasos de Chicha Morada Helada'),
    (29, 'Mega Balde Brosterero', '10 Piezas crujientes de Broster + Papas fritas gigantes + Ensalada familiar + Gaseosa 1.5L + Cremas'),
    (30, 'Promo Burger Lover', '2 Hamburguesas Broster + Porcion de Papas Fritas Mediana + 2 Gaseosas Personales')
) AS clean(id, name, description)
WHERE p.id = clean.id;

UPDATE insumos AS i
SET name = clean.name
FROM (VALUES
    (1, 'Pollo trozado fresco'),
    (2, 'Papas amarillas cortadas'),
    (3, 'Aceite vegetal de cocina'),
    (4, 'Crema Mayonesa (Galon)'),
    (5, 'Ketchup clasico (Galon)'),
    (6, 'Aji de la casa especial'),
    (7, 'Cajas de carton combo'),
    (8, 'Envases de gaseosa descartable')
) AS clean(id, name)
WHERE i.id = clean.id;

COMMIT;
