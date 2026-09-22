-- Move from single-time slots to working intervals + track each reservation's
-- own duration. A slot is now derived on the fly: for a service of D minutes,
-- generate start times at 20-minute boundaries inside an interval, and drop
-- the ones that would overlap an existing ACTIVE reservation.

CREATE TABLE IF NOT EXISTS availability_intervals (
    id         BIGSERIAL PRIMARY KEY,
    day_id     BIGINT      NOT NULL REFERENCES availability_days(id) ON DELETE CASCADE,
    start_time TIME        NOT NULL,
    end_time   TIME        NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (day_id, start_time),
    CHECK (end_time > start_time)
);
CREATE INDEX IF NOT EXISTS ix_intervals_day ON availability_intervals(day_id);

-- Backfill legacy slots (if the table still exists in this environment) as
-- 60-min intervals, deduplicated via the unique index.
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables
               WHERE table_schema = current_schema() AND table_name = 'availability_slots') THEN
        INSERT INTO availability_intervals (day_id, start_time, end_time)
        SELECT day_id, slot_time, (slot_time + interval '60 minutes')::time
        FROM availability_slots
        ON CONFLICT (day_id, start_time) DO NOTHING;
        DROP TABLE availability_slots;
    END IF;
END$$;

-- Snapshot each reservation's duration so admin-side changes to a service's
-- duration don't rewrite past bookings.
ALTER TABLE reservations
    ADD COLUMN IF NOT EXISTS duration_min INT NOT NULL DEFAULT 60;

UPDATE reservations r
SET duration_min = COALESCE(s.duration_min, 60)
FROM services s
WHERE s.id = r.service_id
  AND r.duration_min = 60;

-- Round any existing durations up to the nearest multiple of 20 BEFORE
-- adding the check constraint, otherwise legacy 45/90/etc. rows break it.
UPDATE services
   SET duration_min = ((duration_min + 19) / 20) * 20
 WHERE duration_min % 20 <> 0;

UPDATE reservations
   SET duration_min = ((duration_min + 19) / 20) * 20
 WHERE duration_min % 20 <> 0;

ALTER TABLE services
    DROP CONSTRAINT IF EXISTS chk_services_duration_step;
ALTER TABLE services
    ADD CONSTRAINT chk_services_duration_step
    CHECK (duration_min > 0 AND duration_min % 20 = 0);
