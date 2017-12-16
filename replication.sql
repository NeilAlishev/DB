-- DB - db_practice7
-- WITHOUT MATERIALIZED VIEW

CREATE TABLE original (
    id SERIAL PRIMARY KEY,
    name varchar,
    some_bool boolean
);

CREATE TABLE log (
    modified_id integer, -- what row was modified
    table_name varchar, -- what table was modified
    action varchar, -- INSERT, UPDATE or DELETE
    operation_timestamp timestamp, -- when this happened
    status boolean -- true, if ETL successfully wrote to the replica table
);

CREATE TABLE replica (
    id SERIAL PRIMARY KEY,
    name varchar,
    some_bool boolean
);

-- create After trigger to write to the log table.
CREATE OR REPLACE FUNCTION log_trigger()
RETURNS TRIGGER
AS $$
BEGIN
    if (TG_OP = 'DELETE') then
        EXECUTE 'INSERT INTO log VALUES ($1, $2, $3, $4, $5)' using OLD.id, 'original', TG_OP, now(), false;
    else
        EXECUTE 'INSERT INTO log VALUES ($1, $2, $3, $4, $5)' using NEW.id, 'original', TG_OP, now(), false;
    end if;
RETURN NULL;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER log_trigger
after insert OR update OR delete on original
for each row execute PROCEDURE log_trigger();

-- DB - db_practice8
-- WITH MATERIALIZED VIEW

CREATE EXTENSION dblink;

CREATE TABLE original (
    id SERIAL PRIMARY KEY,
    name varchar,
    some_bool boolean
);

CREATE MATERIALIZED VIEW mat_view AS select * FROM
    dblink('dbname=db_practice8 port=5432 host=127.0.0.1 user=neil password=',
        'select * from original') AS original(id integer, name varchar, some_bool boolean);

-- Вызываем это, когда хотим получить свежие данные об оригинальной таблице
REFRESH MATERIALIZED CONCURRENTLY VIEW mat_view;