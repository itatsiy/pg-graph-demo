CREATE TABLE nodes
(
    id      BIGSERIAL PRIMARY KEY,
    payload TEXT NOT NULL
);

CREATE TABLE edges
(

    id        BIGSERIAL PRIMARY KEY,
    parent_id BIGINT NOT NULL REFERENCES nodes (id) ON DELETE CASCADE,
    child_id  BIGINT NOT NULL REFERENCES nodes (id) ON DELETE CASCADE
);
CREATE INDEX ix__edges__parent_id ON edges (parent_id);
CREATE UNIQUE INDEX uix__edges__child_id ON edges (child_id);