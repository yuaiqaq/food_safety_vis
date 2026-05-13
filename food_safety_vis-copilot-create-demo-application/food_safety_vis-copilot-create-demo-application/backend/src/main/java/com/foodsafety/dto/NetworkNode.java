package com.foodsafety.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NetworkNode {
    private String id;
    private String name;
    private int category;
    private int value;
    private Map<String, Object> properties;

    // 坐标字段
    private Double x;
    private Double y;
}