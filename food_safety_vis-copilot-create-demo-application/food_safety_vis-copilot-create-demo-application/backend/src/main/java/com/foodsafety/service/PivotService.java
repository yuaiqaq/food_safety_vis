package com.foodsafety.service;

import com.foodsafety.entity.FoodSample;
import com.foodsafety.repository.FoodSampleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.function.Function;
import java.util.*;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@RequiredArgsConstructor
public class PivotService {

    private final FoodSampleRepository repository;

    public Map<String, Object> sampleToAttributes(List<Long> sampleIds, String mode) {
        List<FoodSample> samples = repository.findAllById(sampleIds);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("sampleCount", samples.size());

        if ("AND".equalsIgnoreCase(mode)) {
            // ========== AND 逻辑：统一格式为 Map<String, Long> ==========
            result.put("mode", "AND");

            Set<String> commonRegions = findCommonValues(samples, FoodSample::getProductionLocation);
            Set<String> commonCategories = findCommonValues(samples, FoodSample::getFoodCategory);
            Set<String> commonAdulterantCats = findCommonValues(samples, FoodSample::getAdulterantCategory);
            Set<String> commonAdulterants = findCommonValues(samples, FoodSample::getAdulterants);

            // 统一转成 { name: count } 结构
            result.put("regions", toCountMap(commonRegions, samples.size()));
            result.put("categories", toCountMap(commonCategories, samples.size()));
            result.put("adulterantCategories", toCountMap(commonAdulterantCats, samples.size()));
            result.put("adulterants", toCountMap(commonAdulterants, samples.size()));

            Map<String, Object> maxIntersection = findMaxIntersection(samples.size(),
                    commonRegions, commonCategories, commonAdulterantCats, commonAdulterants);
            result.put("maxIntersection", maxIntersection);

        } else {
            // ========== OR 逻辑（不变） ==========
            Map<String, Long> regions = samples.stream()
                    .collect(Collectors.groupingBy(FoodSample::getProductionLocation, Collectors.counting()));
            Map<String, Long> categories = samples.stream()
                    .collect(Collectors.groupingBy(FoodSample::getFoodCategory, Collectors.counting()));
            Map<String, Long> adulterantCats = samples.stream()
                    .collect(Collectors.groupingBy(FoodSample::getAdulterantCategory, Collectors.counting()));
            Map<String, Long> adulterants = samples.stream()
                    .collect(Collectors.groupingBy(FoodSample::getAdulterants, Collectors.counting()));

            result.put("regions", sortedDesc(regions));
            result.put("categories", sortedDesc(categories));
            result.put("adulterantCategories", sortedDesc(adulterantCats));
            result.put("adulterants", sortedDescTop(adulterants, 10));
        }

        return result;
    }

    // ========== 工具方法：把 Set 转成 { name: 总数 } 的统一格式 ==========
    private Map<String, Long> toCountMap(Set<String> set, long count) {
        Map<String, Long> map = new LinkedHashMap<>();
        for (String key : set) {
            map.put(key, count);
        }
        return map;
    }

    private Set<String> findCommonValues(List<FoodSample> samples, Function<FoodSample, String> extractor) {
        if (samples == null || samples.isEmpty()) return new HashSet<>();
        String first = extractor.apply(samples.get(0));
        Set<String> common = new HashSet<>();
        if (first != null) common.add(first);

        for (FoodSample sample : samples) {
            String val = extractor.apply(sample);
            if (val == null) {
                common.clear();
                break;
            }
            common.retainAll(Collections.singleton(val));
            if (common.isEmpty()) break;
        }
        return common;
    }

    private Map<String, Object> findMaxIntersection(int totalCount,
                                                    Set<String> regions,
                                                    Set<String> categories,
                                                    Set<String> adulterantCats,
                                                    Set<String> adulterants) {
        Map<String, Object> max = null;
        if (!regions.isEmpty()) max = createIntersectionInfo("regions", regions, totalCount);
        if (!categories.isEmpty()) max = createIntersectionInfo("categories", categories, totalCount);
        if (!adulterantCats.isEmpty()) max = createIntersectionInfo("adulterantCategories", adulterantCats, totalCount);
        if (!adulterants.isEmpty()) max = createIntersectionInfo("adulterants", adulterants, totalCount);
        return max;
    }

    private Map<String, Object> createIntersectionInfo(String dimension, Set<String> values, int totalCount) {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("dimension", dimension);
        info.put("commonValues", values);
        info.put("coverCount", totalCount);
        info.put("coverRatio", 1.0);
        return info;
    }

    private static final Logger log = LoggerFactory.getLogger(PivotService.class);

    public List<Long> attributesToSamples(
            List<String> nodeIds,
            String mode,
            // 页面筛选条件（基础全集）
            String region,
            String category,
            String adulterantCategory,
            String adulterants
    ) {
        log.info("=== 开始筛选 ===");
        log.info("前端传入 mode: {}", mode);
        log.info("页面筛选 region: {}, category: {}, adulterantCategory: {}, adulterants: {}",
                region, category, adulterantCategory, adulterants);
        log.info("网络图点击 nodeIds: {}", nodeIds);

        // ======================
        // 1. 解析网络图点击条件
        // ======================
        List<String> regionList = new ArrayList<>();
        List<String> categoryList = new ArrayList<>();
        List<String> acList = new ArrayList<>();
        List<String> adList = new ArrayList<>();

        for (String nodeId : nodeIds) {
            if (nodeId.startsWith("region_")) {
                regionList.add(nodeId.substring("region_".length()));
            } else if (nodeId.startsWith("cat_")) {
                categoryList.add(nodeId.substring("cat_".length()));
            } else if (nodeId.startsWith("acat_")) {
                acList.add(nodeId.substring("acat_".length()));
            } else if (nodeId.startsWith("adu_")) {
                adList.add(nodeId.substring("adu_".length()));
            }
        }

        log.info("解析后 网络图条件 -> 地区: {}, 分类: {}, 掺假分类: {}, 掺假物: {}",
                regionList, categoryList, acList, adList);

        // ======================
        // 2. 先查【页面筛选的全集】
        // ======================
        List<FoodSample> allFiltered = repository.findByFilters(
                region,
                category,
                adulterantCategory,
                adulterants,
                "AND"
        );
        log.info("✅ 页面筛选完成，得到全集数量：{} 条", allFiltered.size());

        // ======================
        // 3. 真正的 AND / OR 筛选（支持同一属性多选）
        // ======================
        List<FoodSample> result = new ArrayList<>();

        for (FoodSample sample : allFiltered) {
            boolean match;

            // 取出当前样本的各个字段
            String sampleRegion = sample.getProductionLocation();
            String sampleCategory = sample.getFoodCategory();
            String sampleAc = sample.getAdulterantCategory();
            String sampleAd = sample.getAdulterants(); // 样本的掺假物（如 "呈味核苷酸二钠"）

            if ("AND".equalsIgnoreCase(mode)) {
                // ----------------------
                // AND：必须满足所有条件（真正的交集）
                // ----------------------
                boolean regionOk = regionList.isEmpty() || regionList.contains(sampleRegion);
                boolean categoryOk = categoryList.isEmpty() || categoryList.contains(sampleCategory);
                boolean acOk = acList.isEmpty() || acList.contains(sampleAc);

                // 🔥 关键修复：选中多个掺假物 → 样本必须包含全部
                boolean adOk;
                if (adList.isEmpty()) {
                    adOk = true;
                } else {
                    // 样本必须同时拥有所有选中的掺假物
                    adOk = adList.stream().allMatch(sampleAd::contains);
                }

                // AND：所有条件必须全部满足
                match = regionOk && categoryOk && acOk && adOk;

            } else {
                // ----------------------
                // OR：满足任意一个即可
                // ----------------------
                boolean regionOk = !regionList.isEmpty() && regionList.contains(sampleRegion);
                boolean categoryOk = !categoryList.isEmpty() && categoryList.contains(sampleCategory);
                boolean acOk = !acList.isEmpty() && acList.contains(sampleAc);
                boolean adOk = !adList.isEmpty() && adList.contains(sampleAd);

                match = regionOk || categoryOk || acOk || adOk;
            }

            if (match) {
                result.add(sample);
            }
        }

        log.info("✅ 网络图 {} 筛选完成，最终符合条件数量：{} 条", mode, result.size());
        log.info("=== 筛选结束 ===");

        return result.stream()
                .map(FoodSample::getId)
                .collect(Collectors.toList());
    }

    private Map<String, Long> sortedDesc(Map<String, Long> map) {
        return map.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
    }

    private Map<String, Long> sortedDescTop(Map<String, Long> map, int limit) {
        return map.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(limit)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
    }
}