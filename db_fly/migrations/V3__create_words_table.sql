CREATE TABLE words (
    id serial PRIMARY KEY,
    word varchar NOT NULL,
    document_id integer references documents(id)
);