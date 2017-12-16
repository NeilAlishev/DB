-- Инициализируем pgbench
pgbench -i db_practice9

-- NOTICE:  table "pgbench_history" does not exist, skipping
-- NOTICE:  table "pgbench_tellers" does not exist, skipping
-- NOTICE:  table "pgbench_accounts" does not exist, skipping
-- NOTICE:  table "pgbench_branches" does not exist, skipping
-- creating tables...
-- 100000 of 100000 tuples (100%) done (elapsed 0.16 s, remaining 0.00 s)
-- vacuum...
-- set primary keys...
-- done.

-- В результате выполнения этой команды были созданы 4 таблицы:
-- db_practice9=# \d
--              List of relations
--  Schema |       Name       | Type  | Owner
-- --------+------------------+-------+-------
--  public | pgbench_accounts | table | neil
--  public | pgbench_branches | table | neil
--  public | pgbench_history  | table | neil
--  public | pgbench_tellers  | table | neil
-- (4 rows)

-- Запускаем бенчмарк по умолчанию на 20 секунд.
pgbench -T 20 db_practice9

-- Результаты
-- starting vacuum...end.
-- transaction type: <builtin: TPC-B (sort of)>
-- scaling factor: 1
-- query mode: simple
-- number of clients: 1
-- number of threads: 1
-- duration: 20 s
-- number of transactions actually processed: 21565
-- latency average = 0.928 ms
-- tps = 1078.022069 (including connections establishing)
-- tps = 1078.303151 (excluding connections establishing)

-- Создаем таблицу, которая будет хранить текст
CREATE TABLE texts (
    id integer,
    content text
);

-- Загружаем текст в таблицу
INSERT INTO texts values (1, 'Some long text here...');

-- Создаем GIN - индекс на тексте
CREATE INDEX text_idx ON texts USING GIN (to_tsvector('english', content));

-- Создаем таблицу words
CREATE TABLE words (
    id integer,
    word varchar
);

-- Заполняем таблицу 1000 различными случайными словами

-- создаем файл со своим тестовым скриптом
-- custom_script.sql

pgbench -T 5 -f custom_script.sql db_practice9

-- теперь запускаем pgbench с per transaction logging
pgbench -l -T 5 -f custom_script.sql db_practice9

-- получаем файл с временем выполнения транзакций
-- изменяем файл, чтобы в нем осталось только два столбца (epoch time, время выполнения транзакции)
-- строим график в Excel