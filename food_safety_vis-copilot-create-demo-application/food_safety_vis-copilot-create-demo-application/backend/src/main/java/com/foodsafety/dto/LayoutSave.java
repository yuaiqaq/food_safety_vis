package com.foodsafety.dto;

import lombok.Data;
import java.util.List;

@Data
public class LayoutSave{
    private String filterKey;
    private List<NodePosition> nodes;

    @Data
    public static class NodePosition {
        private String id;
        private Integer x;
        private Integer y;
    }
}