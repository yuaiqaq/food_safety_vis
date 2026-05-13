package com.foodsafety.controller;
import com.foodsafety.dto.LayoutSave;  // ✅ 必须加
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
     * @param adulterants             filter by adulterants
     * @param maxNodes          max number of sample nodes to return (default 200)
     */
    @GetMapping("/substrate")
    public NetworkData getSubstrateNetwork(
            @RequestParam(required = false) String region,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String adulterantCategory,
            @RequestParam(required = false) String adulterants,
            @RequestParam(defaultValue = "200") int maxNodes,
            // ✅ 新增：接收 filterKey
            @RequestParam(required = false) String filterKey
    ) {
        // ✅ 把 filterKey 传给 Service
        return networkService.buildSubstrateNetwork(
                region,
                category,
                adulterantCategory,
                adulterants,
                maxNodes,
                filterKey
        );
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
            @RequestParam(required = false) String adulterants) {
        return networkService.buildCatalystNetwork(region, category, adulterantCategory, adulterants);
    }
    // ====================== ✅ 保存布局接口 ======================
    @PostMapping("/save-layout")
    public void saveLayout(@RequestBody LayoutSave dto) {
        networkService.saveLayout(dto);
    }

}
