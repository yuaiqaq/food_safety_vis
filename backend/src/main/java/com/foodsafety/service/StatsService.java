package com.foodsafety.service;

import com.foodsafety.dto.OverviewStats;
import com.foodsafety.dto.StatItem;
import com.foodsafety.repository.FoodSampleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StatsService {

    private final FoodSampleRepository repository;

    public OverviewStats getOverview() {
        long total = repository.count();

        // By grade
        Map<String, Long> byGrade = new LinkedHashMap<>();
        for (Object[] row : repository.countByGrade()) {
            byGrade.put("Grade " + row[0], (Long) row[1]);
        }

        // By region (top 15)
        List<StatItem> byRegion = repository.countByRegion().stream()
                .limit(15)
                .map(r -> new StatItem((String) r[0], (Long) r[1]))
                .collect(Collectors.toList());

        // By food category
        List<StatItem> byCategory = repository.countByCategory().stream()
                .map(r -> new StatItem((String) r[0], (Long) r[1]))
                .collect(Collectors.toList());

        // By adulterant category
        List<StatItem> byAdulterantCategory = repository.countByAdulterantCategory().stream()
                .map(r -> new StatItem((String) r[0], (Long) r[1]))
                .collect(Collectors.toList());

        // Top 20 adulterants
        List<StatItem> topAdulterants = repository.countByAdulterant().stream()
                .limit(20)
                .map(r -> new StatItem((String) r[0], (Long) r[1]))
                .collect(Collectors.toList());

        return new OverviewStats(total, byGrade, byRegion, byCategory, byAdulterantCategory, topAdulterants);
    }

    public List<String> getRegions() {
        return repository.findDistinctRegions();
    }

    public List<String> getCategories() {
        return repository.findDistinctCategories();
    }

    public List<String> getAdulterantCategories() {
        return repository.findDistinctAdulterantCategories();
    }
}
