CREATE TABLE master (
    id SERIAL PRIMARY KEY,
    type_info integer NOT NULL,
    name varchar NOT NULL
);

CREATE INDEX ON master(type_info);

CREATE OR REPLACE FUNCTION partition_trigger()
RETURNS TRIGGER
AS $$
BEGIN
    -- create new partition
    EXECUTE format('CREATE TABLE %I(like master including all) inherits (master);', 'child_' || NEW.type_info);

    -- create check on created partition
    EXECUTE format ('ALTER TABLE %I ADD CONSTRAINT partition_check CHECK (type_info = %s);', 'child_' || NEW.type_info, NEW.type_info);

    -- insert value into the partition
    EXECUTE format('INSERT INTO %I(type_info, name) VALUES ($1,$2)', 'child_' || NEW.type_info) using NEW.type_info, NEW.name;
RETURN NULL;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER partition_trigger
before insert on master
for each row execute PROCEDURE partition_trigger();

-- After inserting 15 rows into the master table:

-- db_practice6=# \d
--              List of relations
--  Schema |     Name      |   Type   | Owner
-- --------+---------------+----------+-------
--  public | child_1       | table    | neil
--  public | child_10      | table    | neil
--  public | child_11      | table    | neil
--  public | child_12      | table    | neil
--  public | child_13      | table    | neil
--  public | child_14      | table    | neil
--  public | child_15      | table    | neil
--  public | child_2       | table    | neil
--  public | child_3       | table    | neil
--  public | child_4       | table    | neil
--  public | child_5       | table    | neil
--  public | child_6       | table    | neil
--  public | child_7       | table    | neil
--  public | child_8       | table    | neil
--  public | child_9       | table    | neil
--  public | master        | table    | neil
--  public | master_id_seq | sequence | neil
-- (17 rows)

-- Result of EXPLAIN ANALYZE:

-- db_practice6=# explain analyze select * from master where type_info = 10;
--                                                              QUERY PLAN
-- -------------------------------------------------------------------------------------------------------------------------------------
--  Append  (cost=0.00..13.67 rows=7 width=40) (actual time=0.052..0.053 rows=1 loops=1)
--    ->  Seq Scan on master  (cost=0.00..0.00 rows=1 width=40) (actual time=0.002..0.002 rows=0 loops=1)
--          Filter: (type_info = 10)
--    ->  Bitmap Heap Scan on child_10  (cost=4.20..13.67 rows=6 width=40) (actual time=0.050..0.051 rows=1 loops=1)
--          Recheck Cond: (type_info = 10)
--          Heap Blocks: exact=1
--          ->  Bitmap Index Scan on child_10_type_info_idx  (cost=0.00..4.20 rows=6 width=0) (actual time=0.044..0.044 rows=1 loops=1)
--                Index Cond: (type_info = 10)
--  Planning time: 0.732 ms
--  Execution time: 0.106 ms
-- (10 rows)
