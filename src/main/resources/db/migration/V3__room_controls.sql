ALTER TABLE room_settings
 ADD COLUMN anti_spam_enabled BOOLEAN NOT NULL DEFAULT TRUE,
 ADD COLUMN spam_window_seconds INTEGER NOT NULL DEFAULT 10 CHECK (spam_window_seconds BETWEEN 1 AND 120),
 ADD COLUMN spam_max_messages INTEGER NOT NULL DEFAULT 6 CHECK (spam_max_messages BETWEEN 2 AND 100),
 ADD COLUMN spam_max_repeats INTEGER NOT NULL DEFAULT 3 CHECK (spam_max_repeats BETWEEN 1 AND 20),
 ADD COLUMN spam_block_seconds INTEGER NOT NULL DEFAULT 20 CHECK (spam_block_seconds BETWEEN 1 AND 600),
 ADD COLUMN auto_mute_enabled BOOLEAN NOT NULL DEFAULT FALSE,
 ADD COLUMN auto_mute_seconds INTEGER NOT NULL DEFAULT 60 CHECK (auto_mute_seconds BETWEEN 1 AND 3600),
 ADD COLUMN emotes_enabled BOOLEAN NOT NULL DEFAULT TRUE,
 ADD COLUMN emote_loop_interval_seconds INTEGER NOT NULL DEFAULT 10 CHECK (emote_loop_interval_seconds BETWEEN 2 AND 300);
