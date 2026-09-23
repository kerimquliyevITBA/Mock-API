-- Move back to single-time slots. Admin adds discrete times (18:15,
-- 19:00, ...), customer picks from that list, service duration is
-- informational only (no interval overlap math).
--
-- Also wipes every ACTIVE reservation and every working interval on
-- request, so the new booking flow starts clean. Services and past
-- reservation history stay put.

DELETE FROM reservations WHERE status = 'ACTIVE';
DELETE FROM availability_intervals;
DROP TABLE IF EXISTS availability_intervals;

CREATE TABLE IF NOT EXISTS availability_slots (
    id         BIGSERIAL PRIMARY KEY,
    day_id     BIGINT      NOT NULL REFERENCES availability_days(id) ON DELETE CASCADE,
    slot_time  TIME        NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (day_id, slot_time)
);
CREATE INDEX IF NOT EXISTS ix_slots_day ON availability_slots(day_id);
