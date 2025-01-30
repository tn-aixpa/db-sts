CREATE TABLE
    IF NOT EXISTS users (
        id VARCHAR(255) NOT NULL PRIMARY KEY,
        created_at TIMESTAMP,
        web_issuer VARCHAR(255),
        web_user VARCHAR(255),
        db_database VARCHAR(255),
        db_user VARCHAR(255),
        db_roles VARCHAR(255),
        valid_until TIMESTAMP,
        _status VARCHAR(10)
    );

CREATE INDEX IF NOT EXISTS users_id_index ON users (id);

