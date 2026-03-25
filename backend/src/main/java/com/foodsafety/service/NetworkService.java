package com.foodsafety.service;

import com.foodsafety.dto.*;
import com.foodsafety.entity.FoodSample;
import com.foodsafety.repository.FoodSampleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NetworkService {

    private final FoodSampleRepository repository;

    // Substrate network categories (by grade severity)
    private static final List<NetworkCategory> SUBSTRATE_CATEGORIES = List.of(
            new NetworkCategory("轻微 (Grade 0)"),
            new NetworkCategory("较轻 (Grade 1)"),
            new NetworkCategory("中等 (Grade 2)"),
            new NetworkCategory("严重 (Grade 3)")
    );

    // Catalyst network categories (by attribute type)
    private static final List<NetworkCategory> CATALYST_CATEGORIES = List.of(
            new NetworkCategory("省份"),
            new NetworkCategory("食品类别"),
            new NetworkCategory("违规类型"),
            new NetworkCategory("违规项目")
    );

    /**
     * Build the substrate (sample) network.
     * Nodes = food samples (up to maxNodes), coloured by grade.
     * Edges = connect samples sharing the same (foodCategory + adulterantCategory).
     */
    public NetworkData buildSubstrateNetwork(String region, String category,
                                              String adulterantCategory, Integer grade,
                                              int maxNodes) {
        List<FoodSample> samples = repository.findByFilters(region, category, adulterantCategory, grade);

        // Limit to maxNodes using deterministic shuffle for reproducibility
        if (samples.size() > maxNodes) {
            List<FoodSample> mutable = new ArrayList<>(samples);
            Collections.shuffle(mutable, new Random(42));
            samples = mutable.subList(0, maxNodes);
        }

        // Build nodes
        List<NetworkNode> nodes = new ArrayList<>();
        for (FoodSample s : samples) {
            Map<String, Object> props = new LinkedHashMap<>();
            props.put("id", s.getId());
            props.put("productionLocation", s.getProductionLocation());
            props.put("productionLocation2", s.getProductionLocation2());
            props.put("foodCategory", s.getFoodCategory());
            props.put("adulterantCategory", s.getAdulterantCategory());
            props.put("adulterant", s.getAdulterant());
            props.put("mandateLevel", s.getMandateLevel());
            props.put("sampledLocationType", s.getSampledLocationType());
            props.put("grade", s.getGrade());

            int gradeVal = s.getGrade() != null ? s.getGrade() : 0;
            int symbolSize = 6 + gradeVal * 3; // size scales with severity

            nodes.add(new NetworkNode(
                    String.valueOf(s.getId()),
                    "样品#" + s.getId(),
                    gradeVal,
                    symbolSize,
                    props
            ));
        }

        // Build edges: group by (foodCategory + adulterantCategory) and link within each group
        Map<String, List<String>> groupMap = new HashMap<>();
        for (FoodSample s : samples) {
            String key = s.getFoodCategory() + "||" + s.getAdulterantCategory();
            groupMap.computeIfAbsent(key, k -> new ArrayList<>()).add(String.valueOf(s.getId()));
        }

        List<NetworkEdge> edges = new ArrayList<>();
        Set<String> edgeSet = new HashSet<>();
        for (List<String> group : groupMap.values()) {
            if (group.size() < 2) continue;
            // Connect first node to all others in group (star topology to avoid O(n^2) edges)
            String pivot = group.get(0);
            for (int i = 1; i < group.size(); i++) {
                String edgeKey = pivot + "-" + group.get(i);
                if (edgeSet.add(edgeKey)) {
                    edges.add(new NetworkEdge(pivot, group.get(i), 1.0));
                }
            }
        }

        return new NetworkData(nodes, edges, SUBSTRATE_CATEGORIES);
    }

    /**
     * Build the catalyst (attribute co-occurrence) network.
     * Nodes = top provinces + top food categories + all adulterant categories + top adulterants.
     * Edges = how frequently two attribute nodes appear in the same sample.
     */
    public NetworkData buildCatalystNetwork(String region, String category,
                                             String adulterantCategory, Integer grade) {
        List<FoodSample> samples = repository.findByFilters(region, category, adulterantCategory, grade);

        // ---- Build candidate attribute sets ----
        // Top 10 provinces by count
        Map<String, Long> regionCount = samples.stream()
                .collect(Collectors.groupingBy(FoodSample::getProductionLocation, Collectors.counting()));
        Set<String> topRegions = regionCount.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(10).map(Map.Entry::getKey).collect(Collectors.toSet());

        // All food categories present
        Set<String> allCategories = samples.stream()
                .map(FoodSample::getFoodCategory)
                .collect(Collectors.toSet());

        // All adulterant categories
        Set<String> allAdulterantCats = samples.stream()
                .map(FoodSample::getAdulterantCategory)
                .collect(Collectors.toSet());

        // Top 15 adulterants by count
        Map<String, Long> adulterantCount = samples.stream()
                .collect(Collectors.groupingBy(FoodSample::getAdulterant, Collectors.counting()));
        Set<String> topAdulterants = adulterantCount.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(15).map(Map.Entry::getKey).collect(Collectors.toSet());

        // ---- Assign node ids ----
        Map<String, NetworkNode> nodeMap = new LinkedHashMap<>();

        for (String r : topRegions) {
            String nodeId = "region_" + r;
            Map<String, Object> props = new LinkedHashMap<>();
            props.put("type", "省份");
            props.put("count", regionCount.get(r));
            nodeMap.put(nodeId, new NetworkNode(nodeId, r, 0,
                    (int) Math.min(regionCount.getOrDefault(r, 1L), 60), props));
        }
        for (String c : allCategories) {
            String nodeId = "cat_" + c;
            long cnt = samples.stream().filter(s -> c.equals(s.getFoodCategory())).count();
            Map<String, Object> props = new LinkedHashMap<>();
            props.put("type", "食品类别");
            props.put("count", cnt);
            nodeMap.put(nodeId, new NetworkNode(nodeId, c, 1, (int) Math.min(cnt / 10 + 10, 50), props));
        }
        for (String ac : allAdulterantCats) {
            String nodeId = "acat_" + ac;
            long cnt = samples.stream().filter(s -> ac.equals(s.getAdulterantCategory())).count();
            Map<String, Object> props = new LinkedHashMap<>();
            props.put("type", "违规类型");
            props.put("count", cnt);
            nodeMap.put(nodeId, new NetworkNode(nodeId, ac, 2, (int) Math.min(cnt / 8 + 12, 55), props));
        }
        for (String a : topAdulterants) {
            String nodeId = "adu_" + a;
            long cnt = adulterantCount.getOrDefault(a, 1L);
            Map<String, Object> props = new LinkedHashMap<>();
            props.put("type", "违规项目");
            props.put("count", cnt);
            nodeMap.put(nodeId, new NetworkNode(nodeId, a, 3, (int) Math.min(cnt / 5 + 8, 45), props));
        }

        // ---- Build co-occurrence edges ----
        Map<String, Long> cooccurrence = new HashMap<>();
        for (FoodSample s : samples) {
            List<String> attrs = new ArrayList<>();
            String regionId = "region_" + s.getProductionLocation();
            String catId = "cat_" + s.getFoodCategory();
            String acatId = "acat_" + s.getAdulterantCategory();
            String aduId = "adu_" + s.getAdulterant();

            if (nodeMap.containsKey(regionId)) attrs.add(regionId);
            if (nodeMap.containsKey(catId))    attrs.add(catId);
            if (nodeMap.containsKey(acatId))   attrs.add(acatId);
            if (nodeMap.containsKey(aduId))    attrs.add(aduId);

            for (int i = 0; i < attrs.size(); i++) {
                for (int j = i + 1; j < attrs.size(); j++) {
                    String key = attrs.get(i) + "---" + attrs.get(j);
                    cooccurrence.merge(key, 1L, Long::sum);
                }
            }
        }

        // Keep top 80 edges by weight to avoid clutter
        List<NetworkEdge> edges = cooccurrence.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(80)
                .map(e -> {
                    String[] parts = e.getKey().split("---");
                    return new NetworkEdge(parts[0], parts[1], e.getValue().doubleValue());
                })
                .collect(Collectors.toList());

        return new NetworkData(new ArrayList<>(nodeMap.values()), edges, CATALYST_CATEGORIES);
    }
}
