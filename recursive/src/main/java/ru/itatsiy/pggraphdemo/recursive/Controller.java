package ru.itatsiy.pggraphdemo.recursive;


import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.itatsiy.pggraphpoc.TestDataUtil;

import java.util.ArrayList;
import java.util.stream.Collectors;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequiredArgsConstructor
@RequestMapping(produces = APPLICATION_JSON_VALUE)
public class Controller {
    private final NodeRepository nodeRepository;

    @PostMapping("/api/v1/nodes")
    public void post() {
        var data = TestDataUtil.generate();
        var nodes = data.getNodes()
                .stream()
                .map(x -> new NodeEntity().setPayload(x.name()).setChildren(new ArrayList<>()))
                .collect(Collectors.toMap(NodeEntity::getPayload, x -> x));
        data.getEdges().forEach(edge -> {
            var parent = nodes.get(edge.parent());
            var child = nodes.get(edge.child());
            parent.getChildren().add(child);
        });
        nodeRepository.save(nodes.get(data.getRoot().name()));
    }

    @GetMapping("/api/v1/nodes/{id}")
    public NodeEntity get(@PathVariable Long id) {
        return nodeRepository.findById(id).orElseThrow(IllegalArgumentException::new);
    }
}
