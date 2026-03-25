package com.foodsafety.controller;

import com.foodsafety.dto.OverviewStats;
import com.foodsafety.service.StatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class StatsController {

    private final StatsService statsService;

    /** Full overview statistics */
    @GetMapping("/overview")
    public OverviewStats overview() {
        return statsService.getOverview();
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
}
