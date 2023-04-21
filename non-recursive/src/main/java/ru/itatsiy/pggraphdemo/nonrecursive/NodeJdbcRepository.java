package ru.itatsiy.pggraphdemo.nonrecursive;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.Types;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@Repository
@RequiredArgsConstructor
public class NodeJdbcRepository {
    private static final int[] NODE_INSERT_BATCH_TYPES = new int[]{Types.BIGINT, Types.VARCHAR};
    private static final int[] EDGE_INSERT_BATCH_TYPES = new int[]{Types.BIGINT, Types.BIGINT};
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    private final JdbcTemplate jdbcTemplate;

    @Transactional(readOnly = true)
    public NodeEntity requireById(Long id) {
        var sql = """
                SELECT e.parent_id, e.child_id FROM descendants d
                    JOIN edges e on e.id = d.edge_id
                    WHERE d.node_id = :id
                """;
        var edges = namedParameterJdbcTemplate.query(
                sql,
                fromId(id),
                this::extractEdges
        );
        var nodeIds = new HashSet<>(edges.keySet());
        edges.values().forEach(nodeIds::addAll);
        nodeIds.add(id);
        var nodes = namedParameterJdbcTemplate.query(
                "SELECT n.id, n.payload FROM nodes n WHERE n.id IN (:ids)",
                fromIds(nodeIds),
                this::extractNodes
        );
        if (nodes.isEmpty()) {
            throw new IllegalStateException();
        }
        edges.forEach((parentId, childIds) ->
                nodes.get(parentId).getChildren().addAll(
                        childIds.stream().map(nodes::get)
                                .sorted(Comparator.comparingLong(NodeEntity::getId))
                                .toList()
                )
        );
        return nodes.get(id);
    }

    @Transactional
    public void save(NodeEntity root, Collection<NodeEntity> entities) {
        var size = entities.stream()
                .filter(x -> x.getId() == null)
                .count();
        var ids = namedParameterJdbcTemplate.query(
                "SELECT nextval('nodes_id_seq') FROM generate_series(1, :size)",
                new MapSqlParameterSource().addValue("size", size),
                this::extractLongList
        );
        var nodeBatch = new ArrayList<Object[]>();
        var edgeBatch = new ArrayList<Object[]>();
        collectEdges(root, ids, new AtomicInteger(), nodeBatch, edgeBatch);
        jdbcTemplate.batchUpdate("INSERT INTO nodes (id, payload) VALUES (?, ?)", nodeBatch, NODE_INSERT_BATCH_TYPES);
        jdbcTemplate.batchUpdate("INSERT INTO edges (parent_id, child_id) VALUES (?, ?)", edgeBatch, EDGE_INSERT_BATCH_TYPES);
    }

    private void collectEdges(NodeEntity parent, List<Long> ids, AtomicInteger index, List<Object[]> nodeBatch, List<Object[]> edgeBatch) {
        if (parent.getId() == null) {
            parent.setId(ids.get(index.getAndIncrement()));
        }
        nodeBatch.add(new Object[]{parent.getId(), parent.getPayload()});
        for (var child : parent.getChildren()) {
            if (child.getId() == null) {
                child.setId(ids.get(index.getAndIncrement()));
            }
            edgeBatch.add(new Object[]{parent.getId(), child.getId()});
            collectEdges(child, ids, index, nodeBatch, edgeBatch);
        }
    }

    @SneakyThrows
    private List<Long> extractLongList(ResultSet set) {
        var result = new ArrayList<Long>();
        while (set.next()) {
            result.add(set.getLong(1));
        }
        return result;
    }

    @SneakyThrows
    private Map<Long, NodeEntity> extractNodes(ResultSet set) {
        var result = new HashMap<Long, NodeEntity>();
        while (set.next()) {
            var id = set.getLong("id");
            result.put(id, new NodeEntity().setId(id).setPayload(set.getString("payload")).setChildren(new ArrayList<>()));
        }
        return result;
    }

    @SneakyThrows
    private Map<Long, Set<Long>> extractEdges(ResultSet set) {
        var result = new HashMap<Long, Set<Long>>();
        while (set.next()) {
            result.computeIfAbsent(set.getLong("parent_id"), x -> new HashSet<>()).add(set.getLong("child_id"));
        }
        return result;
    }

    private static MapSqlParameterSource fromId(Long id) {
        return new MapSqlParameterSource()
                .addValue("id", id);
    }

    private static MapSqlParameterSource fromIds(Set<Long> ids) {
        return new MapSqlParameterSource()
                .addValue("ids", ids);
    }
}
