package com.foodsafety.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NetworkEdge {
    private String source;
    private String target;
    /** Co-occurrence count or similarity weight */
    private double value;
}
