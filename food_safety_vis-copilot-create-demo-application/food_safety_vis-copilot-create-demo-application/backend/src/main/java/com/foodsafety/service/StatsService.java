package com.foodsafety.service;

import com.foodsafety.dto.OverviewStats;
import com.foodsafety.dto.StatItem;
import com.foodsafety.repository.FoodSampleRepository;
import lombok.RequiredArgsConstructor;
import org.hibernate.boot.model.internal.CreateKeySecondPass;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StatsService {

    private final FoodSampleRepository repository;

    public OverviewStats getOverview(String region, String category,
                                     String adulterantCategory, String adulterants)
    {
        long total = repository.count();

        // By grade
        Map<String, Long> byGrade = new LinkedHashMap<>();
        for (Object[] row : repository.countByGrade(region,category,adulterantCategory,adulterants)) {
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
    public List<String> getfindDistinctAdulterants(){
        return repository.findDistinctAdulterants();
    }
    public Map<String, Long> getGrade(String region, String category,
                                      String adulterantCategory, String adulterants) {
        // 先初始化所有等级为 0
        Map<String, Long> byGrade = new LinkedHashMap<>();
        byGrade.put("Grade 0", 0L);
        byGrade.put("Grade 1", 0L);
        byGrade.put("Grade 2", 0L);
        byGrade.put("Grade 3", 0L);

        // 然后填充实际数据
        for (Object[] row : repository.countByGrade(region, category, adulterantCategory, adulterants)) {
            Integer gradeValue = (Integer) row[0];
            Long count = (Long) row[1];
            byGrade.put("Grade " + gradeValue, count);
        }

        return byGrade;
    }
}
