CREATE TABLE
    IF NOT EXISTS public.users (
        id VARCHAR(255) NOT NULL PRIMARY KEY,
        created_at TIMESTAMP(6) WITH TIME ZONE,
        web_issuer VARCHAR(255),
        web_user VARCHAR(255),
        db_database VARCHAR(255),
        db_user VARCHAR(255),
        db_roles VARCHAR(255),
        valid_until TIMESTAMP(6) WITH TIME ZONE,
        _status VARCHAR(10)        
    );

CREATE INDEX IF NOT EXISTS users_id_index ON public.users (id);

-- alter table public.runnable
--     owner to postgres;