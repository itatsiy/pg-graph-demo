package ru.itatsiy.pggraphpoc;

import lombok.experimental.UtilityClass;

import java.util.HashSet;
import java.util.stream.Stream;

@UtilityClass
public class TestDataUtil {
    private static final String DELIMITER = "->";
    private static final String DATA = """
            A1->B1->C1->D1->E1->F1->G1->H1
            A1->B1->C1->D1->E1->F1->G1->H2
            A1->B1->C1->D1->E1->F1->G1->H3
            A1->B1->C1->D1->E1->F1->G1->H4
            A1->B1->C1->D1->E1->F1->G2->H5
            A1->B1->C1->D1->E1->F1->G2->H6->I1->K1
            A1->B1->C1->D1->E1->F1->G2->H6->I1->K2
            A1->B1->C1->D1->E1->F1->G2->H6->I2->K3
            A1->B1->C1->D1->E1->F1->G2->H6->I2->K4
            A1->B1->C1->D1->E1->F1->G2->H6->I2->K5->L1
            A1->B1->C1->D1->E1->F1->G2->H6->I2->K5->L2
            A1->B1->C1->D1->E1->F1->G2->H6->I2->K5->L3
            A1->B1->C1->D1->E1->F1->G2->H6->I2->K5->L4
            A1->B1->C1->D1->E1->F1->G2->H6->I2->K5->L5
            A1->B1->C1->D1->E1->F1->G2->H6->I2->K5->L6
            A1->B1->C1->D1->E1->F1->G2->H6->I2->K5->L7
            A1->B1->C1->D1->E1->F1->G2->H6->I2->K6->L8
            A1->B1->C1->D1->E1->F1->G2->H6->I2->K6->L9
            A1->B1->C1->D1->E1->F1->G2->H6->I2->K6->L10
            A1->B1->C1->D1->E1->F1->G2->H6->I2->K6->L11->P1
            A1->B1->C1->D1->E1->F1->G2->H6->I2->K6->L11->P2
            A1->B1->C1->D1->E1->F1->G2->H6->I2->K6->L11->P3
            A1->B1->C1->D1->E1->F1->G2->H6->I2->K6->L11->P4
            A1->B1->C1->D1->E1->F1->G2->H6->I2->K6->L11->P5
            A1->B1->C1->D1->E1->F1->G2->H6->I2->K6->L11->P6
            A1->B1->C1->D1->E1->F1->G2->H6->I2->K6->L11->P7
            A1->B1->C1->D1->E1->F1->G2->H6->I2->K6->L11->P8
            """;

    public TestData generate() {
        var nodes = new HashSet<TestDataNode>();
        var edges = new HashSet<TestDataEdge>();
        var result = new TestData(nodes, edges);
        Stream.of(DATA.split("\n"))
                .map(String::trim)
                .filter(x -> !x.isEmpty())
                .forEach(x -> parseLine(x, result));
        return result;
    }

    private void parseLine(String line, TestData result) {
        var nodes = line.split(DELIMITER);
        if (nodes.length == 0) {
            return;
        }
        var prev = nodes[0];
        var root = new TestDataNode(prev);
        if (result.getRoot() == null) {
            result.setRoot(root);
        }
        result.getNodes().add(root);
        for (var i = 1; i < nodes.length; i++) {
            var cur = nodes[i];
            result.getNodes().add(new TestDataNode(cur));
            result.getEdges().add(new TestDataEdge(prev, cur));
            prev = cur;
        }
    }
}
