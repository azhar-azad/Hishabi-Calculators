-- V5: calculations history table for Phase 5 auth/history.
-- request_json / response_json store the full DTO snapshots so history
-- remains accurate even if tax rules change in a later assessment year.
CREATE TABLE calculations (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id         BIGINT                   NOT NULL
                        REFERENCES users(id) ON DELETE CASCADE,
    assessment_year VARCHAR(10)              NOT NULL,
    request_json    TEXT                     NOT NULL,
    response_json   TEXT                     NOT NULL,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_calculations_user_id ON calculations(user_id);