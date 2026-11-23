-- Демонстрационные клиенты и заявки. Учётная запись администратора создаётся приложением из .env.

INSERT INTO clients (id, code, full_name, phone, email, active, created_at, updated_at, version) VALUES
    ('c1000000-0000-4000-8000-000000000001', 'CL-001', 'Анна Смирнова', '+7-900-111-22-33', 'anna@example.com', TRUE, TIMESTAMPTZ '2026-01-10 10:00:00+00', TIMESTAMPTZ '2026-01-10 10:00:00+00', 0),
    ('c1000000-0000-4000-8000-000000000002', 'CL-002', 'Игорь Петров', '+7-900-222-33-44', 'igor@example.com', TRUE, TIMESTAMPTZ '2026-01-10 10:00:00+00', TIMESTAMPTZ '2026-01-10 10:00:00+00', 0),
    ('c1000000-0000-4000-8000-000000000003', 'CL-003', 'Мария Кузнецова', '+7-900-333-44-55', 'maria@example.com', TRUE, TIMESTAMPTZ '2026-01-10 10:00:00+00', TIMESTAMPTZ '2026-01-10 10:00:00+00', 0);

INSERT INTO reservations (
    id, code, animal_name, comment, reservation_date, client_id, status,
    deposit_amount, needs_care, priority, archived, created_at, updated_at, version
) VALUES
    ('a1000000-0000-4000-8000-000000000001', 'RS-001', 'Барсик', 'Нужен осмотр', DATE '2026-02-01',
     'c1000000-0000-4000-8000-000000000001', 'CREATED', 3000.00, TRUE, 'CRITICAL', FALSE,
     TIMESTAMPTZ '2026-02-01 09:00:00+00', TIMESTAMPTZ '2026-02-01 09:00:00+00', 0),
    ('a1000000-0000-4000-8000-000000000002', 'RS-002', 'Рекс', 'Крупная собака', DATE '2026-02-03',
     'c1000000-0000-4000-8000-000000000002', 'CONFIRMED', 25000.00, FALSE, 'LOW', FALSE,
     TIMESTAMPTZ '2026-02-03 09:00:00+00', TIMESTAMPTZ '2026-02-03 09:00:00+00', 0),
    ('a1000000-0000-4000-8000-000000000003', 'RS-003', 'Мурка', 'Забрали домой', DATE '2026-01-20',
     'c1000000-0000-4000-8000-000000000003', 'COMPLETED', 8000.00, FALSE, 'NOT_APPLICABLE', FALSE,
     TIMESTAMPTZ '2026-01-20 09:00:00+00', TIMESTAMPTZ '2026-01-20 09:00:00+00', 0);

INSERT INTO reservation_tag (reservation_id, tag) VALUES
    ('a1000000-0000-4000-8000-000000000001', 'привит'),
    ('a1000000-0000-4000-8000-000000000002', 'крупный');
