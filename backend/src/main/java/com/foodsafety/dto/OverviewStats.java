package com.foodsafety.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OverviewStats {
    private long totalSamples;
    private Map<String, Long> byGrade;
    private List<StatItem> byRegion;
    private List<StatItem> byCategory;
    private List<StatItem> byAdulterantCategory;
    private List<StatItem> topAdulterants;
}
