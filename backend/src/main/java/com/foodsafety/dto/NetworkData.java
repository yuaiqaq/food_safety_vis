package com.foodsafety.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NetworkData {
    private List<NetworkNode> nodes;
    private List<NetworkEdge> edges;
}
