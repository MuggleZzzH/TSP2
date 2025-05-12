# 项目结构摘要

本文档总结了为旅行计划应用程序创建的 Java 类。

## 1. 模型类 (Model Classes)

这些类代表了应用程序的核心数据实体。

### `com.cpt204.finalproject.model.City`

* **目的**: 代表道路网络中的一个城市。
* **关键属性**:
    * `name` (String): 城市的唯一名称。
* **关键方法**:
    * 构造函数 `City(String name)`
    * `getName()`
    * `equals()`, `hashCode()`, `toString()`

### `com.cpt204.finalproject.model.Road`

* **目的**: 代表连接两个城市的有向道路段。
* **关键属性**:
    * `source` (City): 道路的起始城市。
    * `destination` (City): 道路的结束城市。
    * `distance` (double): 道路的长度/距离。
* **关键方法**:
    * 构造函数 `Road(City source, City destination, double distance)`
    * `getSource()`, `getDestination()`, `getDistance()`
    * `equals()`, `hashCode()`, `toString()`

### `com.cpt204.finalproject.model.Attraction`

* **目的**: 代表位于某个城市内的景点 (Attraction)。
* **关键属性**:
    * `attractionName` (String): 景点的名称。
    * `cityName` (String): 景点所在城市的名称。
* **关键方法**:
    * 构造函数 `Attraction(String attractionName, String cityName)`
    * `getAttractionName()`, `getCityName()`
    * `equals()`, `hashCode()`, `toString()`

### `com.cpt204.finalproject.model.RoadNetwork`

* **目的**: 代表整个道路网络，包含城市、道路和景点。
* **关键属性**:
    * `citiesByName` (Map<String, City>): 按名称快速查找城市的映射。
    * `adjacencyList` (Map<City, List<Road>>): 将每个城市映射到其出站道路的邻接列表。
    * `attractionsByCity` (Map<String, List<Attraction>>): 将城市名称映射到其景点的映射。
* **关键方法**:
    * 构造函数 `RoadNetwork(Collection<City> cities, Collection<Road> roads, Collection<Attraction> attractions)`
    * `getCityByName(String name)`
    * `getAllCities()`
    * `getRoadsFrom(City city)`
    * `getNumberOfCities()`
    * `getAttractionsInCity(String cityName)`
    * `getAllAttractions()`

## 2. 数据加载类 (DataLoader Classes)

此类负责将数据加载到模型对象中。

### `com.cpt204.finalproject.dataloader.CsvDataLoader`

* **目的**: 从类路径下的 CSV 文件加载城市、道路和景点数据。
* **关键方法**:
    * `loadData(String roadsCsvPath, String attractionsCsvPath)`: 加载数据并返回一个 `RoadNetwork` 对象。
    * 私有帮助方法 `loadAttractions(...)` 和 `loadRoads(...)`。
* **假设**:
    * 道路被视为双向的，除非 CSV 数据另有指示（当前实现会添加双向道路）。
    * 期望 CSV 文件包含头部行。

## 3. 服务接口 (Service Interfaces)

这些接口定义了核心应用程序服务的契约。

### `com.cpt204.finalproject.services.PathfindingService`

* **目的**: 定义用于查找城市之间路径的服务的契约。
* **关键方法**:
    * `findShortestPath(RoadNetwork roadNetwork, City startCity, City endCity, List<Attraction> attractionsToVisit, boolean useTimeout, long timeoutMillis)`: 查找最短路径。
* **内部类 `PathResult`**:
    * 封装路径查找操作的结果。
    * 属性: `path` (List<City>)，`totalDistance` (double)，`calculationTimeMillis` (long)，`timedOut` (boolean)，`algorithmName` (String)。
    * 静态工厂方法: `timedOut(...)`, `empty(...)`。

### `com.cpt204.finalproject.services.PoiOptimizerService`

* **目的**: 定义用于优化景点 (POI) 访问顺序的服务的契约。
* **关键方法**:
    * `findBestPoiOrder(RoadNetwork roadNetwork, City startCity, City endCity, List<City> poisToVisit, boolean useTimeout, long timeoutMillis)`: 查找访问 POI 的最佳顺序。
* **内部类 `OptimizerResult`**:
    * 封装 POI 优化操作的结果。
    * 属性: `bestOrder` (List<City>)，`totalDistance` (double)，`calculationTimeMillis` (long)，`timedOut` (boolean)，`algorithmName` (String)。
    * 静态工厂方法: `timedOut(...)`, `empty(...)`。

## 4. 服务实现类 (Service Implementations)

这些类提供了服务接口的具体实现。

### `com.cpt204.finalproject.services.DijkstraPathfindingService`

* **目的**: 使用 Dijkstra 算法实现 `PathfindingService`。
* **关键特性**:
    * 计算两个城市之间的最短路径。
    * 目前不处理中间的 `attractionsToVisit` 或处理 Dijkstra 算法本身的 `useTimeout`。
* **算法**: Dijkstra。

### `com.cpt204.finalproject.services.PermutationPoiOptimizerService`

* **目的**: 使用暴力排列方法实现 `PoiOptimizerService`。
* **关键特性**:
    * 生成所有可能的 POI 排列，以找到最佳顺序。
    * 依赖于注入的 `PathfindingService` 来计算路段距离。
    * 支持超时功能。
    * 计算成本高 (O(N!))，适用于 POI 数量非常少的情况。
* **算法**: 排列 (Heap 算法) + 分段路径查找。

### `com.cpt204.finalproject.services.DynamicProgrammingPoiOptimizerService`

* **目的**: 使用动态规划方法（Held-Karp 变体）实现 `PoiOptimizerService`。
* **关键特性**:
    * 使用位掩码 (bitmasking) 和 DP 表来查找最优 POI 顺序。
    * 依赖于注入的 `PathfindingService`。
    * 包含城市间距离的缓存，以避免重复计算。
    * 支持超时功能。
    * 比排列方法更高效 (O(N^2 * 2^N))，适用于中等数量的 POI。
* **算法**: 动态规划 (Held-Karp 变体) + 带缓存的分段路径查找。

## 5. Data Transfer Objects (DTOs)

这些类是用于传输数据，通常是服务操作的最终结果。

### `com.cpt204.finalproject.dto.TripPlan`

* **目的**: 代表一次旅行规划请求的最终结果。
* **关键属性**:
    * `fullPath` (List<City>): 包含起点、所有优化后的POI顺序以及终点的完整城市列表。
    * `detailedSegments` (List<PathfindingService.PathResult>): (可选)每个路段的详细路径信息。
    * `totalDistance` (double): 整个行程的总距离。
    * `poiOptimizationTimeMillis` (long): POI优化步骤花费的时间。
    * `totalPathfindingTimeMillis` (long): 所有路段路径查找花费的总时间。
    * `poiOptimizerAlgorithmName` (String): 使用的POI优化算法名称。
    * `pathfindingAlgorithmName` (String): 使用的路径查找算法名称。
    * `optimizerTimedOut` (boolean): POI优化步骤是否超时。
    * `pathfinderTimedOut` (boolean): 是否有任何路段的路径查找超时。
    * `statusMessage` (String): 描述规划结果状态的消息（如 "Success", "Optimizer timed out"）。
* **关键方法**:
    * 构造函数 `TripPlan(...)`
    * `Getters` for all attributes.
    * `toString()`: 用于打印计划摘要。
    * `static failure(String message)`: 用于创建表示失败的计划的工厂方法。

## 6. Orchestration Services

这些服务协调低级别服务以执行复杂操作。

### `com.cpt204.finalproject.services.TripPlanningService`

* **目的**: 高级别服务，负责协调整个旅行规划流程。
* **关键依赖**:
    * `RoadNetwork`: 包含地图数据。
    * `PathfindingService`: 用于计算两点间路径。
    * `PermutationPoiOptimizerService`: 用于少量POI的优化。
    * `DynamicProgrammingPoiOptimizerService`: 用于较多POI的优化。
* **关键方法**:
    * `planTrip(String startCityName, String endCityName, List<String> attractionNames, boolean useTimeout, long timeoutMillis)`: 主要的规划方法。
* **核心逻辑**:
    1. 验证输入城市和景点名称。
    2. 将景点名称转换为POI城市列表。
    3. 根据POI数量选择合适的优化器 (排列或DP)。
    4. 调用POI优化器获取最佳POI访问顺序。
    5. 根据优化后的顺序，使用路径查找服务计算并拼接各路段的路径。
    6. 封装所有信息到 `TripPlan` 对象并返回。
    * 处理超时和错误情况。

## 7. Application Entry Point

### `com.cpt204.finalproject.Main`

* **目的**: 应用程序的主入口点，用于演示服务的使用。
* **核心逻辑**:
    1. 初始化 `CsvDataLoader` 并加载路网数据。
    2. 初始化 `PathfindingService` 和 `PoiOptimizerService` 的具体实现。
    3. 初始化 `TripPlanningService`。
    4. 定义示例行程参数 (起点、终点、景点)。
    5. 调用 `tripPlanningService.planTrip(...)` 来获取行程计划。
    6. 打印 `TripPlan` 结果。
* **注意**: 需要确保CSV数据文件位于 `src/main/resources/data/` 目录下。