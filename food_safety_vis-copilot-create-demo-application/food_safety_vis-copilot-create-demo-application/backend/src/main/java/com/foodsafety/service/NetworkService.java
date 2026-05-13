package com.foodsafety.service;

import com.foodsafety.dto.LayoutSave;
import com.foodsafety.dto.NetworkData;
import com.foodsafety.dto.NetworkEdge;
import com.foodsafety.dto.NetworkNode;
import com.foodsafety.dto.NetworkCategory;
import com.foodsafety.entity.FoodSample;
import com.foodsafety.entity.NodeLayout;
import com.foodsafety.repository.FoodSampleRepository;
import com.foodsafety.repository.NodeLayoutRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NetworkService {

    private final FoodSampleRepository repository;
    private final NodeLayoutRepository nodeLayoutRepository;

    private static final List<NetworkCategory> SUBSTRATE_CATEGORIES = List.of(
            new NetworkCategory("轻微 (Grade 0)"),
            new NetworkCategory("较轻 (Grade 1)"),
            new NetworkCategory("中等 (Grade 2)"),
            new NetworkCategory("严重 (Grade 3)")
    );

    private static final List<NetworkCategory> CATALYST_CATEGORIES = List.of(
            new NetworkCategory("省份"),
            new NetworkCategory("食品类别"),
            new NetworkCategory("违规类型"),
            new NetworkCategory("违规项目"),
            new NetworkCategory("包装规格"),
            new NetworkCategory("抽检级别"),
            new NetworkCategory("生产经营主体类型"),
            new NetworkCategory("抽样场所类型")
    );

    // =========================================================================
    // 保存图谱布局
    // =========================================================================
    @Transactional
    public void saveLayout(LayoutSave dto) {
        String filterKey = dto.getFilterKey();
        System.out.println("\n【布局保存】filterKey = " + filterKey);
        System.out.println("【布局保存】待保存节点数量 = " + dto.getNodes().size());

        // 删除旧布局
        nodeLayoutRepository.deleteByFilterKey(filterKey);

        // 插入新布局
        List<NodeLayout> layoutList = dto.getNodes().stream().map(node -> {
            NodeLayout layout = new NodeLayout();
            layout.setFilterKey(filterKey);
            layout.setNodeId(node.getId());
            layout.setX(Double.valueOf(node.getX()));
            layout.setY(Double.valueOf(node.getY()));
            return layout;
        }).collect(Collectors.toList());

        nodeLayoutRepository.saveAll(layoutList);
        System.out.println("【布局保存】✅ 保存成功！\n");
    }

    // =========================================================================
    // 根据 filterKey 获取布局
    // =========================================================================
    public List<NodeLayout> getLayoutByFilterKey(String filterKey) {
        if (filterKey == null || filterKey.isBlank()) {
            return Collections.emptyList();
        }
        return nodeLayoutRepository.findByFilterKey(filterKey);
    }

    // =========================================================================
    // 构建样本网络（Substrate）
    // =========================================================================
    public NetworkData buildSubstrateNetwork(
            String region,
            String category,
            String adulterantCategory,
            String adulterants,
            int maxNodes,
            String filterKey
    ) {
        List<FoodSample> samples = repository.findByFilters(region, category, adulterantCategory, adulterants, "AND");

        if (samples.size() > maxNodes) {
            List<FoodSample> mutable = new ArrayList<>(samples);
            Collections.shuffle(mutable, new Random(42));
            samples = mutable.subList(0, maxNodes);
        }

        // 构建节点
        List<NetworkNode> nodes = new ArrayList<>();
        for (FoodSample s : samples) {
            Map<String, Object> props = new LinkedHashMap<>();
            props.put("id", s.getId());
            props.put("productionLocation", s.getProductionLocation());
            props.put("productionLocation2", s.getProductionLocation2());
            props.put("foodCategory", s.getFoodCategory());
            props.put("adulterantCategory", s.getAdulterantCategory());
            props.put("adulterant", s.getAdulterants());
            props.put("foodSpecModel", s.getFoodSpecModel());
            props.put("mandateLevel", s.getMandateLevel());
            props.put("manufacturerType", s.getManufacturerType());
            props.put("sampledLocationType", s.getSampledLocationType());
            props.put("grade", s.getGrade());

            int gradeVal = s.getGrade() != null ? s.getGrade() : 0;
            int symbolSize = 6 + gradeVal * 3;

            nodes.add(new NetworkNode(
                    String.valueOf(s.getId()),
                    "样品#" + s.getId(),
                    gradeVal,
                    symbolSize,
                    props,
                    null,
                    null
            ));
        }

        // 构建边
        Map<String, List<String>> groupMap = new HashMap<>();
        for (FoodSample s : samples) {
            String key = s.getFoodCategory() + "||" + s.getAdulterantCategory();
            groupMap.computeIfAbsent(key, k -> new ArrayList<>()).add(String.valueOf(s.getId()));
        }

        List<NetworkEdge> edges = new ArrayList<>();
        Set<String> edgeSet = new HashSet<>();

        for (List<String> group : groupMap.values()) {
            if (group.size() < 2) continue;

            Map<String, FoodSample> sampleMap = new HashMap<>();
            for (FoodSample s : samples) {
                sampleMap.put(String.valueOf(s.getId()), s);
            }

            for (int i = 0; i < group.size(); i++) {
                for (int j = i + 1; j < group.size(); j++) {
                    String id1 = group.get(i);
                    String id2 = group.get(j);

                    FoodSample a = sampleMap.get(id1);
                    FoodSample b = sampleMap.get(id2);
                    double weight = calculateSimilarity(a, b);

                    if (weight > 0) {
                        String edgeKey = id1 + "-" + id2;
                        if (edgeSet.add(edgeKey)) {
                            edges.add(new NetworkEdge(id1, id2, weight));
                        }
                    }
                }
            }
        }

        NetworkData networkData = new NetworkData(nodes, edges, SUBSTRATE_CATEGORIES);

        // ===================== 【布局缓存：自动赋值 x/y】 =====================
        if (filterKey != null && !filterKey.isBlank()) {
            System.out.println("\n【布局加载】传入的 filterKey = " + filterKey);

            List<NodeLayout> layoutList = getLayoutByFilterKey(filterKey);
            System.out.println("【布局加载】查到坐标数量 = " + layoutList.size());

            if (!layoutList.isEmpty()) {
                Map<String, NodeLayout> layoutMap = layoutList.stream()
                        .collect(Collectors.toMap(NodeLayout::getNodeId, it -> it));

                int assignCount = 0;
                for (NetworkNode node : networkData.getNodes()) {
                    NodeLayout layout = layoutMap.get(node.getId());
                    if (layout != null) {
                        node.setX(layout.getX());
                        node.setY(layout.getY());
                        assignCount++;
                    }
                }
                System.out.println("【布局加载】✅ 成功赋值坐标数量 = " + assignCount + "\n");
            } else {
                System.out.println("【布局加载】⚠️ 未找到缓存坐标，将自动布局\n");
            }
        }

        return networkData;
    }

    // =========================================================================
    // 计算样本相似度
    // =========================================================================
    private double calculateSimilarity(FoodSample a, FoodSample b) {
        double weight = 0;

        if (a.getAdulterants() != null && a.getAdulterants().equals(b.getAdulterants())) weight += 3;
        if (a.getAdulterantCategory() != null && a.getAdulterantCategory().equals(b.getAdulterantCategory())) weight += 2;
        if (a.getFoodCategory() != null && a.getFoodCategory().equals(b.getFoodCategory())) weight += 1;
        if (a.getProductionLocation() != null && a.getProductionLocation().equals(b.getProductionLocation())) weight += 1;
        if (a.getProductionLocation2() != null && a.getProductionLocation2().equals(b.getProductionLocation2())) weight += 1;
        if (a.getGrade() != null && a.getGrade().equals(b.getGrade())) weight += 1;

        return weight;
    }

    // =========================================================================
    // 构建属性网络（Catalyst）
    // =========================================================================
    public NetworkData buildCatalystNetwork(
            String region,
            String category,
            String adulterantCategory,
            String adulterants
    ) {
        List<FoodSample> samples = repository.findByFilters(region, category, adulterantCategory, adulterants, "AND");

        Map<String, Long> regionCount = samples.stream()
                .collect(Collectors.groupingBy(FoodSample::getProductionLocation, Collectors.counting()));
        Set<String> topRegions = regionCount.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(10).map(Map.Entry::getKey).collect(Collectors.toSet());

        Set<String> allCategories = samples.stream().map(FoodSample::getFoodCategory).collect(Collectors.toSet());
        Set<String> allAdulterantCats = samples.stream().map(FoodSample::getAdulterantCategory).collect(Collectors.toSet());
        Set<String> allFoodSpecModels = samples.stream().map(FoodSample::getFoodSpecModel).collect(Collectors.toSet());
        Set<String> allMandateLevels = samples.stream().map(FoodSample::getMandateLevel).collect(Collectors.toSet());
        Set<String> allManufacturerTypes = samples.stream().map(FoodSample::getManufacturerType).collect(Collectors.toSet());
        Set<String> allSampledLocationTypes = samples.stream().map(FoodSample::getSampledLocationType).collect(Collectors.toSet());

        Map<String, Long> adulterantCount = samples.stream()
                .collect(Collectors.groupingBy(FoodSample::getAdulterants, Collectors.counting()));
        Set<String> topAdulterants = adulterantCount.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(15).map(Map.Entry::getKey).collect(Collectors.toSet());

        Map<String, NetworkNode> nodeMap = new LinkedHashMap<>();

        for (String r : topRegions) {
            String nodeId = "region_" + r;
            Map<String, Object> props = new LinkedHashMap<>();
            props.put("type", "省份");
            props.put("count", regionCount.get(r));
            nodeMap.put(nodeId, new NetworkNode(nodeId, r, 0,
                    (int) Math.min(regionCount.getOrDefault(r, 1L), 60), props, null, null));
        }
        for (String c : allCategories) {
            String nodeId = "cat_" + c;
            long cnt = samples.stream().filter(s -> c.equals(s.getFoodCategory())).count();
            Map<String, Object> props = new LinkedHashMap<>();
            props.put("type", "食品类别");
            props.put("count", cnt);
            nodeMap.put(nodeId, new NetworkNode(nodeId, c, 1,
                    (int) Math.min(cnt / 10 + 10, 50), props, null, null));
        }
        for (String ac : allAdulterantCats) {
            String nodeId = "acat_" + ac;
            long cnt = samples.stream().filter(s -> ac.equals(s.getAdulterantCategory())).count();
            Map<String, Object> props = new LinkedHashMap<>();
            props.put("type", "违规类型");
            props.put("count", cnt);
            nodeMap.put(nodeId, new NetworkNode(nodeId, ac, 2,
                    (int) Math.min(cnt / 8 + 12, 55), props, null, null));
        }
        for (String a : topAdulterants) {
            String nodeId = "adu_" + a;
            long cnt = adulterantCount.getOrDefault(a, 1L);
            Map<String, Object> props = new LinkedHashMap<>();
            props.put("type", "违规项目");
            props.put("count", cnt);
            nodeMap.put(nodeId, new NetworkNode(nodeId, a, 3,
                    (int) Math.min(cnt / 5 + 8, 45), props, null, null));
        }
        for (String fsm : allFoodSpecModels) {
            if (fsm == null || fsm.isBlank()) continue;
            String nodeId = "fsm_" + fsm;
            long cnt = samples.stream().filter(s -> Objects.equals(fsm, s.getFoodSpecModel())).count();
            Map<String, Object> props = new LinkedHashMap<>();
            props.put("type", "包装规格");
            props.put("count", cnt);
            nodeMap.put(nodeId, new NetworkNode(nodeId, fsm, 4,
                    (int) Math.min(cnt / 6 + 10, 45), props, null, null));
        }
        for (String ml : allMandateLevels) {
            if (ml == null || ml.isBlank()) continue;
            String nodeId = "ml_" + ml;
            long cnt = samples.stream().filter(s -> Objects.equals(ml, s.getMandateLevel())).count();
            Map<String, Object> props = new LinkedHashMap<>();
            props.put("type", "抽检级别");
            props.put("count", cnt);
            nodeMap.put(nodeId, new NetworkNode(nodeId, ml, 5,
                    (int) Math.min(cnt / 6 + 10, 45), props, null, null));
        }
        for (String mft : allManufacturerTypes) {
            if (mft == null || mft.isBlank()) continue;
            String nodeId = "mft_" + mft;
            long cnt = samples.stream().filter(s -> Objects.equals(mft, s.getManufacturerType())).count();
            Map<String, Object> props = new LinkedHashMap<>();
            props.put("type", "生产经营主体类型");
            props.put("count", cnt);
            nodeMap.put(nodeId, new NetworkNode(nodeId, mft, 6,
                    (int) Math.min(cnt / 6 + 10, 45), props, null, null));
        }
        for (String slt : allSampledLocationTypes) {
            if (slt == null || slt.isBlank()) continue;
            String nodeId = "slt_" + slt;
            long cnt = samples.stream().filter(s -> Objects.equals(slt, s.getSampledLocationType())).count();
            Map<String, Object> props = new LinkedHashMap<>();
            props.put("type", "抽样场所类型");
            props.put("count", cnt);
            nodeMap.put(nodeId, new NetworkNode(nodeId, slt, 7,
                    (int) Math.min(cnt / 6 + 10, 45), props, null, null));
        }

        Map<String, Long> cooccurrence = new HashMap<>();
        for (FoodSample s : samples) {
            List<String> attrs = new ArrayList<>();
            String regionId = "region_" + s.getProductionLocation();
            String catId = "cat_" + s.getFoodCategory();
            String acatId = "acat_" + s.getAdulterantCategory();
            String aduId = "adu_" + s.getAdulterants();
            String fsmId = "fsm_" + s.getFoodSpecModel();
            String mlId = "ml_" + s.getMandateLevel();
            String mftId = "mft_" + s.getManufacturerType();
            String sltId = "slt_" + s.getSampledLocationType();

            if (nodeMap.containsKey(regionId)) attrs.add(regionId);
            if (nodeMap.containsKey(catId)) attrs.add(catId);
            if (nodeMap.containsKey(acatId)) attrs.add(acatId);
            if (nodeMap.containsKey(aduId)) attrs.add(aduId);
            if (nodeMap.containsKey(fsmId)) attrs.add(fsmId);
            if (nodeMap.containsKey(mlId)) attrs.add(mlId);
            if (nodeMap.containsKey(mftId)) attrs.add(mftId);
            if (nodeMap.containsKey(sltId)) attrs.add(sltId);

            for (int i = 0; i < attrs.size(); i++) {
                for (int j = i + 1; j < attrs.size(); j++) {
                    String key = attrs.get(i) + "---" + attrs.get(j);
                    cooccurrence.merge(key, 1L, Long::sum);
                }
            }
        }

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
