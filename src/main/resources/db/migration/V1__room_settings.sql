CREATE TABLE room_settings (
    room_id VARCHAR(128) PRIMARY KEY,
    welcome_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    welcome_message VARCHAR(255) NOT NULL DEFAULT 'سلام {username}، خوش آمدی!',
    command_prefix VARCHAR(8) NOT NULL DEFAULT '!',
    command_cooldown_seconds INTEGER NOT NULL DEFAULT 3 CHECK (command_cooldown_seconds BETWEEN 1 AND 300),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE bot_admins (
    room_id VARCHAR(128) NOT NULL REFERENCES room_settings(room_id) ON DELETE CASCADE,
    user_id VARCHAR(128) NOT NULL,
    role VARCHAR(16) NOT NULL CHECK (role IN ('OWNER', 'ADMIN')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (room_id, user_id)
);
