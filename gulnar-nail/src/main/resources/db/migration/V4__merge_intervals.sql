-- Legacy 20-min slots got backfilled as consecutive 60-min intervals
-- (10:00-11:00, 11:00-12:00, ...). A service whose duration exceeds any
-- single chunk can't be booked, so collapse a day's intervals into their
-- (min start, max end) span whenever the day has more than one interval.
--
-- Safe on installs that already have a single 10:00-19:00 span (untouched),
-- and safe on the freshly seeded 10:00-19:00 (also untouched).

CREATE TEMP TABLE _bounds ON COMMIT DROP AS
SELECT day_id, MIN(start_time) AS start_time, MAX(end_time) AS end_time
FROM availability_intervals
GROUP BY day_id
HAVING COUNT(*) > 1;

DELETE FROM availability_intervals
WHERE day_id IN (SELECT day_id FROM _bounds);

INSERT INTO availability_intervals (day_id, start_time, end_time)
SELECT day_id, start_time, end_time FROM _bounds;
