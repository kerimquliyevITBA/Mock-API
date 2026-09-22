-- Legacy 20-min slots got backfilled as consecutive 60-min intervals
-- (10:00-11:00, 11:00-12:00, ...). A service whose duration exceeds any
-- single chunk can't be booked, so merge back-to-back intervals within
-- the same day into a single span.

CREATE TEMP TABLE _merged AS
WITH grouped AS (
    SELECT day_id, start_time, end_time,
           SUM(CASE WHEN start_time = LAG(end_time) OVER (PARTITION BY day_id ORDER BY start_time)
                    THEN 0 ELSE 1 END)
             OVER (PARTITION BY day_id ORDER BY start_time) AS grp
    FROM availability_intervals
)
SELECT day_id, MIN(start_time) AS start_time, MAX(end_time) AS end_time
FROM grouped
GROUP BY day_id, grp;

DELETE FROM availability_intervals;

INSERT INTO availability_intervals (day_id, start_time, end_time)
SELECT day_id, start_time, end_time FROM _merged;

DROP TABLE _merged;
