package com.foodsafety.controller;

import com.foodsafety.entity.FoodSample;
import com.foodsafety.repository.FoodSampleRepository;
import com.foodsafety.service.PivotService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class SampleController {

    private final FoodSampleRepository repository;
    private final PivotService pivotService;

    /**
     * List samples with optional filters and pagination.
     */
    @GetMapping("/samples")
    public Page<FoodSample> list(
            @RequestParam(required = false) String region,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String adulterantCategory,
            @RequestParam(required = false) String adulterants,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        if (region == null && category == null && adulterantCategory == null && adulterants == null) {
            return repository.findAll(PageRequest.of(page, size));
        }
        List<FoodSample> filtered = repository.findByFilters(region, category, adulterantCategory, adulterants,"AND");
        int start = page * size;
        int end = Math.min(start + size, filtered.size());
        List<FoodSample> pageContent = start < filtered.size() ? filtered.subList(start, end) : List.of();
        return new org.springframework.data.domain.PageImpl<>(pageContent,
                PageRequest.of(page, size), filtered.size());
    }

    /**
     * Get a single sample by ID.
     */
    @GetMapping("/samples/{id}")
    public FoodSample getById(@PathVariable Long id) {
        return repository.findById(id).orElseThrow();
    }

    /**
     * Pivot: from selected sample IDs → attribute profile.
     * POST body: {"ids": [1, 2, 3, ...]}
     */
    @PostMapping("/pivot/sample-to-attr")
    public Map<String, Object> sampleToAttr(@RequestBody Map<String, Object> body) {
        List<Long> ids = (List<Long>) body.get("ids");
        String mode = (String) body.getOrDefault("mode", "OR");
        return pivotService.sampleToAttributes(ids, mode);
    }

    /**
     * Pivot: from selected catalyst node IDs → matching sample IDs.
     * POST body: {"nodeIds": ["region_广东省", "cat_pastry"]}
     */
    @PostMapping("/pivot/attr-to-sample")
    public Map<String, Object> attrToSample(@RequestBody Map<String, Object> body) {
        List<String> nodeIds = (List<String>) body.get("ids");
        String mode = (String) body.getOrDefault("mode", "OR");

        // 👇 这是你漏掉的！接收前端传过来的筛选条件
        String region = (String) body.get("region");
        String category = (String) body.get("category");
        String adulterantCategory = (String) body.get("adulterantCategory");
        String adulterants = (String) body.get("adulterants");

        // 👇 全部传给 service
        List<Long> sampleIds = pivotService.attributesToSamples(
                nodeIds,
                mode,
                region,
                category,
                adulterantCategory,
                adulterants
        );

        return Map.of("sampleIds", sampleIds, "count", sampleIds.size());
    }
}