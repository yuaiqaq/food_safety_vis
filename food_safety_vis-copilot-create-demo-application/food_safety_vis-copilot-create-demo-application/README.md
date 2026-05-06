# 食品安全不合格原因可视化分析系统

> **基于多层关系网络的食品安全不合格原因可视化分析系统**  
> Design and Implementation of a Visualization Analysis System for Food Safety Non-Compliance Causes Based on Multi-Layer Relational Networks

受 Detangler 可视分析系统启发，采用 **Java Spring Boot** 后端 + **Vue 3** 前端构建的全栈 Demo。

---

## 系统截图预览

| 功能 | 说明 |
|------|------|
| 🔵 样本网络（Substrate） | 食品样本节点，颜色编码违规等级，力导向布局 |
| 🟠 属性关联网络（Catalyst） | 省份/食品类别/违规类型/违规项目的共现网络 |
| 🔗 双向透视（Pivot） | 选中样本 → 查看属性画像；选中属性 → 高亮匹配样本 |
| 📊 统计图表 | 按省份/类别/违规类型/Top20违规项目分布图 |

---

## 项目结构

```
food_safety_vis/
├── filtered_samples.csv        ← 5000 条真实食品不合格检测记录
├── backend/                    ← Spring Boot 3.2 后端
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/foodsafety/
│       │   ├── FoodSafetyApplication.java
│       │   ├── config/CorsConfig.java
│       │   ├── entity/FoodSample.java        ← JPA 实体（对应 CSV 字段）
│       │   ├── repository/FoodSampleRepository.java
│       │   ├── service/
│       │   │   ├── CsvDataLoader.java        ← 启动时自动加载 CSV 数据
│       │   │   ├── NetworkService.java       ← 构建 Substrate/Catalyst 网络
│       │   │   ├── StatsService.java         ← 统计聚合
│       │   │   └── PivotService.java         ← 双向透视查询
│       │   ├── controller/
│       │   │   ├── NetworkController.java    ← GET /api/network/{substrate,catalyst}
│       │   │   ├── StatsController.java      ← GET /api/stats/{overview,regions,...}
│       │   │   └── SampleController.java     ← GET /api/samples, POST /api/pivot/*
│       │   └── dto/                          ← 数据传输对象
│       └── resources/
│           ├── application.properties
│           └── filtered_samples.csv         ← CSV 副本（自动加载）
└── frontend/                   ← Vue 3 + Vite 前端
    ├── vite.config.js
    ├── package.json
    └── src/
        ├── main.js             ← 入口（Element Plus + Pinia）
        ├── App.vue             ← 主布局
        ├── store/index.js      ← Pinia 全局状态（网络数据、筛选、透视）
        ├── api/index.js        ← Axios API 封装
        └── components/
            ├── FilterPanel.vue     ← 多维度筛选栏
            ├── SubstrateNetwork.vue ← ECharts 力导向图（样本网络）
            ├── CatalystNetwork.vue  ← ECharts 力导向图（属性关联网络）
            ├── PivotPanel.vue      ← 双向透视结果面板
            └── StatsPanel.vue      ← 统计图表（柱图+饼图）
```

---

## 数据说明（filtered_samples.csv）

| 字段 | 说明 |
|------|------|
| `id` | 样本唯一 ID |
| `production_location` | 生产省份 |
| `production_location2` | 生产城市 |
| `sale_location` / `sale_location2` | 销售省份/城市 |
| `food_category` | 食品类别（pastry/alcohol/meat...） |
| `adulterant_category` | 违规类型（Specification/Food additive/Microbial contamination/...） |
| `adulterant` | 具体违规项目（菌落总数/过氧化值/酒精度/...） |
| `mandate_level` | 检查级别（Nationally/Provincially Mandated） |
| `grade` | 违规等级：0=轻微, 1=较轻, 2=中等, 3=严重 |

共 **5,000** 条不合格食品检测记录，覆盖全国 **26** 个省份、**20** 种食品类别、**6** 种违规类型。

---

## 快速启动

### 前置要求

- **JDK 17+** 和 **Maven 3.9+**
- **Node.js 18+** 和 **npm**

### 1. 启动后端

```bash
cd backend
mvn clean package -q
java -jar target/food-safety-vis-1.0.0.jar
# 后端在 http://localhost:8080 启动
# CSV 数据自动加载，约 3 秒完成
```

### 2. 启动前端

```bash
cd frontend
npm install
npm run dev
# 前端在 http://localhost:5173 启动
```

打开浏览器访问 **http://localhost:5173** 即可使用。

---

## API 接口说明

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/stats/overview` | 全局统计（总数/按等级/按省份/按类别/Top违规项目） |
| GET | `/api/stats/regions` | 省份列表 |
| GET | `/api/stats/categories` | 食品类别列表 |
| GET | `/api/stats/adulterant-categories` | 违规类型列表 |
| GET | `/api/network/substrate` | 样本网络（节点+边），支持筛选参数 |
| GET | `/api/network/catalyst` | 属性关联网络（节点+边），支持筛选参数 |
| GET | `/api/samples` | 样本列表（分页+多维度筛选） |
| POST | `/api/pivot/sample-to-attr` | 从样本 ID 列表 → 属性画像 |
| POST | `/api/pivot/attr-to-sample` | 从属性节点 ID 列表 → 匹配样本 ID |

**筛选参数**（substrate/catalyst/samples 共用）：
- `region`: 省份（如 `广东省`）
- `category`: 食品类别（如 `pastry`）
- `adulterantCategory`: 违规类型（如 `Food additive`）
- `grade`: 违规等级（0/1/2/3）

---

## 核心设计思想

本系统借鉴 **Detangler** 多层网络可视分析框架：

1. **双网络抽象**：基底网络（食品样本）+ 催化网络（属性关联）
2. **协调布局**：同类违规样本在力导向布局中自然聚集，形成"风险社区"
3. **枢轴刷选（Pivot Brushing）**：
   - 在样本网络选中节点 → 属性网络高亮对应属性（"这批样本为什么不合格？"）
   - 在属性网络选中节点 → 样本网络高亮匹配样本（"哪些样本有这种风险模式？"）
4. **多维度筛选**：省份/类别/违规类型/等级组合过滤，动态更新两个网络
