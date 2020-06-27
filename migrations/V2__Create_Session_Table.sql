CREATE TABLE strabo.session (
    id              UUID PRIMARY KEY,
    user_id         UUID NOT NULL references strabo.user (id),
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    invalidated_at  TIMESTAMP WITH TIME ZONE
);