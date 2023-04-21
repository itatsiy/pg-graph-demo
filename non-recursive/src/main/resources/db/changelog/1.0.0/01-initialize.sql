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

CREATE TABLE descendants
(
    node_id BIGINT NOT NULL REFERENCES nodes (id) ON DELETE CASCADE,
    edge_id BIGINT NOT NULL REFERENCES edges (id) ON DELETE CASCADE,
    PRIMARY KEY (node_id, edge_id)
);
CREATE INDEX ix__descendants__node_id ON descendants (node_id);
CREATE INDEX ix__descendants__edge_id ON descendants (edge_id);

create function check_recursive_edge() returns trigger
    language plpgsql
as
$$
BEGIN
    PERFORM check_recursive_edge(new.parent_id, new.child_id);
    RETURN NEW;
END;
$$;

create function check_recursive_edge(p_parent_id bigint, p_child_id bigint) returns void
    language plpgsql
as
$$
DECLARE
    v_child_id BIGINT;
BEGIN
    FOR v_child_id IN SELECT fr.child_id FROM edges fr WHERE fr.parent_id = p_child_id
        LOOP
            IF v_child_id = p_parent_id THEN
                RAISE EXCEPTION 'infinity loop';
            END IF;
            PERFORM check_recursive_edge(p_parent_id, v_child_id);
        END LOOP;
END;
$$;

create function propagate_edge_insertion() returns trigger
    language plpgsql
as
$$
BEGIN
    PERFORM propagate_edge_insertion(NEW.id, NEW.parent_id);
    RETURN NEW;
END;
$$;

create function propagate_edge_insertion(p_edge_id bigint, p_parent_id bigint) returns void
    language plpgsql
as
$$
DECLARE
    v_parent_id BIGINT;
BEGIN
    INSERT INTO descendants (node_id, edge_id) VALUES (p_parent_id, p_edge_id);

    FOR v_parent_id IN SELECT node_edge.parent_id
                       FROM edges node_edge
                       WHERE node_edge.child_id = p_parent_id
        LOOP
            PERFORM propagate_edge_insertion(p_edge_id, v_parent_id);
        END LOOP;
END;
$$;

create function propagate_edge_deleting() returns trigger
    language plpgsql
as
$$
BEGIN
    PERFORM propagate_edge_deleting(OLD.parent_id, OLD.ID);
    RETURN OLD;
END;
$$;

create function propagate_edge_deleting(p_parent_id bigint, p_edge_id_to_delete bigint) returns void
    language plpgsql
as
$$
DECLARE
    v_parent_id BIGINT;
BEGIN
    DELETE
    FROM descendants nd
    WHERE nd.node_id = p_parent_id
      AND nd.edge_id = p_edge_id_to_delete;

    FOR v_parent_id IN SELECT fr.parent_id FROM edges fr WHERE fr.child_id = p_parent_id
        LOOP
            PERFORM propagate_edge_deleting(v_parent_id, p_edge_id_to_delete);
        END LOOP;
    RETURN;
END;
$$;

create trigger trigger_check_recursive_edge
    before insert
    on edges
    for each row
execute procedure check_recursive_edge();

create trigger trigger_propagate_edge_insertion
    after insert
    on edges
    for each row
execute procedure propagate_edge_insertion();

create trigger trigger_propagate_edge_deleting
    after delete
    on edges
    for each row
execute procedure propagate_edge_deleting();

