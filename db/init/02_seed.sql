-- Rutas de ejemplo para arrancar (borralas o pausalas desde la API).
-- Las fechas son relativas al día de creación de la base para que nunca
-- queden en el pasado al levantar el proyecto por primera vez.
INSERT INTO routes (origin, destination, departure_date, return_date, currency, target_price_min, target_price_max, notes)
VALUES
    ('EZE', 'MAD', CURRENT_DATE + 120, CURRENT_DATE + 140, 'USD',  900, 1100, 'Ejemplo: ida y vuelta a Madrid'),
    ('EZE', 'MIA', CURRENT_DATE + 75,  CURRENT_DATE + 85,  'USD',  550,  700, 'Ejemplo: ida y vuelta a Miami'),
    ('AEP', 'BRC', CURRENT_DATE + 45,  NULL,               'USD',   60,   90, 'Ejemplo: solo ida a Bariloche');
