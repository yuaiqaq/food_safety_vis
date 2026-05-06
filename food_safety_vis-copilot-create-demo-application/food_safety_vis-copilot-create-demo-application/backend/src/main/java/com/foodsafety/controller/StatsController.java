package com.foodsafety.controller;

import com.foodsafety.dto.OverviewStats;
import com.foodsafety.service.StatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class StatsController {

    private final StatsService statsService;

    /** Full overview statistics */
    @GetMapping("/overview")
    public OverviewStats overview(
        @RequestParam(required = false) String region,
        @RequestParam(required = false) String category,
        @RequestParam(required = false) String adulterantCategory,
        @RequestParam(required = false) String adulterants) {
        return statsService.getOverview(region, category, adulterantCategory, adulterants);
    }

    /** List of distinct production provinces */
    @GetMapping("/regions")
    public List<String> regions() {
        return statsService.getRegions();
    }

    /** List of distinct food categories */
    @GetMapping("/categories")
    public List<String> categories() {
        return statsService.getCategories();
    }

    /** List of distinct adulterant categories */
    @GetMapping("/adulterant-categories")
    public List<String> adulterantCategories() {
        return statsService.getAdulterantCategories();
    }
    /** List of distinct adulterants */
    @GetMapping("/adulterants")
    public List<String> adulterants(){
        return statsService.getfindDistinctAdulterants();
        }
    @GetMapping("/grade")
    public Map<String, Long> groups(
            @RequestParam(required = false) String region,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String adulterantCategory,
            @RequestParam(required = false) String adulterants) {
        return statsService.getGrade(region,category,adulterantCategory,adulterants);
    }
}
