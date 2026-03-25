package com.foodsafety.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NetworkNode {
    /** Unique node identifier */
    private String id;

    /** Display label */
    private String name;

    /**
     * Node category (for substrate: riskLevel; for catalyst: attribute type)
     * Used for ECharts graph category coloring
     */
    private String category;

    /** Visual size weight (e.g., count of connections) */
    private int value;

    /** Additional properties */
    private Map<String, Object> properties;
}
