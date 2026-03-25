package com.foodsafety.controller;

import com.foodsafety.dto.NetworkData;
import com.foodsafety.service.NetworkService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/network")
@RequiredArgsConstructor
public class NetworkController {

    private final NetworkService networkService;

    /**
     * Substrate (sample) network.
     * Returns force-directed graph data for food sample nodes coloured by grade.
     *
     * @param region            filter by production province
     * @param category          filter by food category
     * @param adulterantCategory filter by adulterant category
     * @param grade             filter by grade (0-3)
     * @param maxNodes          max number of sample nodes to return (default 200)
     */
    @GetMapping("/substrate")
    public NetworkData substrate(
            @RequestParam(required = false) String region,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String adulterantCategory,
            @RequestParam(required = false) Integer grade,
            @RequestParam(defaultValue = "200") int maxNodes) {
        return networkService.buildSubstrateNetwork(region, category, adulterantCategory, grade, maxNodes);
    }

    /**
     * Catalyst (attribute co-occurrence) network.
     * Returns attribute nodes (province, category, adulterant type, adulterant)
     * and their co-occurrence edges.
     */
    @GetMapping("/catalyst")
    public NetworkData catalyst(
            @RequestParam(required = false) String region,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String adulterantCategory,
            @RequestParam(required = false) Integer grade) {
        return networkService.buildCatalystNetwork(region, category, adulterantCategory, grade);
    }
}
