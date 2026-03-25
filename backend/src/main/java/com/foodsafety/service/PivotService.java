package com.foodsafety.service;

import com.foodsafety.entity.FoodSample;
import com.foodsafety.repository.FoodSampleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PivotService {

    private final FoodSampleRepository repository;

    /**
     * From a set of sample IDs, return the attribute profile:
     * which regions, categories, adulterant categories, and adulterants
     * appear and how often.
     */
    public Map<String, Object> sampleToAttributes(List<Long> sampleIds) {
        List<FoodSample> samples = repository.findAllById(sampleIds);

        Map<String, Long> regions = samples.stream()
                .collect(Collectors.groupingBy(FoodSample::getProductionLocation, Collectors.counting()));
        Map<String, Long> categories = samples.stream()
                .collect(Collectors.groupingBy(FoodSample::getFoodCategory, Collectors.counting()));
        Map<String, Long> adulterantCats = samples.stream()
                .collect(Collectors.groupingBy(FoodSample::getAdulterantCategory, Collectors.counting()));
        Map<String, Long> adulterants = samples.stream()
                .collect(Collectors.groupingBy(FoodSample::getAdulterant, Collectors.counting()));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("sampleCount", samples.size());
        result.put("regions", sortedDesc(regions));
        result.put("categories", sortedDesc(categories));
        result.put("adulterantCategories", sortedDesc(adulterantCats));
        result.put("adulterants", sortedDescTop(adulterants, 10));
        return result;
    }

    /**
     * From selected attribute node IDs (e.g. "region_广东省", "cat_pastry"),
     * return the list of matching sample IDs using AND logic.
     */
    public List<Long> attributesToSamples(List<String> nodeIds) {
        // Parse nodeIds into filters
        String region = null, category = null, adulterantCategory = null;

        for (String nodeId : nodeIds) {
            if (nodeId.startsWith("region_")) {
                region = nodeId.substring("region_".length());
            } else if (nodeId.startsWith("cat_")) {
                category = nodeId.substring("cat_".length());
            } else if (nodeId.startsWith("acat_")) {
                adulterantCategory = nodeId.substring("acat_".length());
            }
            // adu_ (specific adulterant) — handled below
        }

        List<FoodSample> samples = repository.findByFilters(region, category, adulterantCategory, null);

        // Further filter by specific adulterant if requested
        final String[] adulterantNames = nodeIds.stream()
                .filter(n -> n.startsWith("adu_"))
                .map(n -> n.substring("adu_".length()))
                .toArray(String[]::new);

        if (adulterantNames.length > 0) {
            samples = samples.stream()
                    .filter(s -> Arrays.asList(adulterantNames).contains(s.getAdulterant()))
                    .collect(Collectors.toList());
        }

        return samples.stream().map(FoodSample::getId).collect(Collectors.toList());
    }

    private Map<String, Long> sortedDesc(Map<String, Long> map) {
        return map.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (e1, e2) -> e1, LinkedHashMap::new));
    }

    private Map<String, Long> sortedDescTop(Map<String, Long> map, int limit) {
        return map.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(limit)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (e1, e2) -> e1, LinkedHashMap::new));
    }
}
