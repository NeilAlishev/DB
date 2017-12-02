-- 1 Задание
-- Структура:
-- 1) Главная таблица - Main (id, name, type)
-- Поля id, name есть у всех 4 таблиц, поэтому можем выделить эти поля в
-- одну главную таблицу.
-- Также, добавим поле type, в которое будем писать, какой старой таблице
-- принадлежат добавленные id, name.
-- type может быть - shop, card, event, address

CREATE TABLE Main (
    id PRIMARY KEY,
    name varchar NOT NULL,
    type varchar NOT NULL
);

-- 2) Таблица Times (id, start, end)
-- Содержит дату начала чего-либо и дату окончания чего-либо.
-- Эта таблица необходима для покрытия функционала старой таблицы CARD_TYPE.
-- В будущем эта таблица нужна будет для любого нового справочника, где
-- есть дата начала и дата окончания.

CREATE TABLE Times (
    id bigint REFERENCES Main(id) NOT NULL,
    start_date timestamp NOT NULL,
    end_date timestamp NOT NULL
);

-- 3) Таблица Statuses (id, boolean)
-- Содержит статусы (true или false) чего-либо.
-- Эта таблица необходима для покрытия функционала старой таблицы EVENT_TYPE.
-- В будущем эта таблица нужна будет для любого нового справочника, где
-- есть статус

CREATE TABLE Statuses (
    id bigint REFERENCES Main(id) NOT NULL,
    status boolean NOT NULL
);

-- 4) Addresses (id, par_id)

CREATE TABLE Addresses (
    id bigint REFERENCES Main(id) NOT NULL,
    address_id integer NOT NULL
);

-- Теперь необходимо создать триггер, который будет добавлять
-- новые строки в необходимые НЕглавные таблицы (Times, Statuses, Addresses) в зависимости от типа.

create function dispatcher()
returns trigger
as $$
begin
    if (NEW.type = 'shop') then
        -- добавляем только в общую таблицу Main, таблица Shop содержит только поля из главной таблицы
        INSERT INTO Main VALUES (NEW.id, NEW.name, 'shop')
    elseif (NEW.type = 'card') then
        -- добавляем в таблицу Times время начала и время окончания
        INSERT INTO Times VALUES (NEW.id, NEW.start_date, NEW.end_date);
        -- добавляем в таблицу Main общие данные
        INSERT INTO Main VALUES (NEW.id, NEW.name, 'card')
    elseif (NEW.type = 'event') then
        -- добавляем в таблицу Statuses статус event'а.
        INSERT INTO Statuses VALUES (NEW.id, NEW.status)
        -- добавляем в таблицу Main общие данные
        INSERT INTO Main VALUES (NEW.id, NEW.name, 'event')
    elseif (NEW.type = 'address') then
        -- добавляем в таблицу Addresses id адреса.
        INSERT INTO Addresses VALUES (NEW.id, NEW.address_id);
        -- добавляем в таблицу Main общие данные
        INSERT INTO Main VALUES (NEW.id, NEW.name, 'address')
    end if;
return null;
end;
$$ language plpgsql;

-- связываем триггер с главной таблицей
create trigger dispatcher_trigger
before insert on Main
for each row execute PROCEDURE dispatcher();

-- Теперь, в ходе INSERT запросов придется дописывать тип данных, которые добавляются.
-- В зависимости от типа, дополнительные данные попадут в необходимую НЕглавную таблицу.

-- Пример - добавление в справочник EVENT_TYPE.
-- INSERT INTO Main (id, name, type, status) VALUES (1, 'Some event', 'event', true);

-- В таблицу Main должны попасть (1, 'Some event', 'event'), а в таблицу Statuses должны попасть (1, true)

-- 2 Задание

-- SQL-запрос:
SELECT * FROM Main, Statuses WHERE type = 'event' AND Main.id = Statuses.id AND Statuses.status = true;

-- Индексы:
CREATE UNIQUE INDEX id_idx1 ON Main(id);
CREATE UNIQUE INDEX id_idx2 ON Statuses(id);

-- 3 Задание
-- Для того, чтобы эффективно хранить такое большое количество данных, можно создавать новые партиции каждый день и
-- помещать события этого дня в соответствующую партицию.
-- Итого, в год получится 365 партиций.

CREATE TABLE Master (
    ВСЕ ПОЛЯ ИЗ EVENT_TYPE,
    created_at DATE,
    event_id integer -- 1000 возможных событий
);

create function partition_trigger()
returns trigger
as $$
begin
    date_as_string := to_char(now(),'yyyy-mm-dd');

    -- create new partition if it doesn't exist
    EXECUTE format('CREATE TABLE IF NOT EXIST partition_%I(like Master including all) inherits (Master);', date_as_string);

    -- add checks on created partition
    EXECUTE format ('ALTER TABLE partition_%I ADD CONSTRAINT partition_check CHECK (created_at = %I);', date_as_string);

    -- INSERT VALUE
    INSERT INTO Master VALUES (NEW.id, NEW.name, NEW.status, date_as_string, NEW.event_id);
return null;
end;
$$ language plpgsql;

CREATE TRIGGER partition_trigger
before insert on Master
for each row execute PROCEDURE partition_trigger();

-- Запрос

SELECT name, event_id, count(*) FROM Master GROUP BY event_id HAVING created_at = 'some_date';