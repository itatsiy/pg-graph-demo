package ru.itatsiy.pggraphpoc;

import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.Set;

@Data
@RequiredArgsConstructor
public class TestData {
    private TestDataNode root;
    private final Set<TestDataNode> nodes;
    private final Set<TestDataEdge> edges;
}
