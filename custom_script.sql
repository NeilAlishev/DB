-- generate random id between 1 and 1000
\set random_id random(1, 1000)

select id, to_tsvector(content) @@
  to_tsquery((select word from words where id = :random_id))
  as match from texts where id = 1;