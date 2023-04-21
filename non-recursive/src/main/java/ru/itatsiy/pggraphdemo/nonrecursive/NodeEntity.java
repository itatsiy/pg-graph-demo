package ru.itatsiy.pggraphdemo.nonrecursive;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@Accessors(chain = true)
public class NodeEntity {
    private Long id;
    private String payload;
    private List<NodeEntity> children;
}
