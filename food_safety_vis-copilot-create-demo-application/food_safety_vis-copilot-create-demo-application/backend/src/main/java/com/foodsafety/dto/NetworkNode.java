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
     * Category index (maps to categories list in NetworkData).
     * Substrate: 0=minor(grade0), 1=light(grade1), 2=medium(grade2), 3=severe(grade3)
     * Catalyst: 0=region, 1=foodCategory, 2=adulterantCategory, 3=adulterant
     */
    private int category;

    /** Visual size weight */
    private int value;

    /** Additional properties for tooltip/detail */
    private Map<String, Object> properties;
}
