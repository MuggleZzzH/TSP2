# Codebase File Summary

本文件旨在解释 Trip Planner 应用程序中每个主要 Java 文件的用途和职责。

## com.cpt204.finalproject (根包)

### `Main.java`

*   **用途:** 应用程序的**主入口点**。
*   **职责:**
    1.  初始化数据加载组件 (`CsvDataLoader`)。
    2.  调用数据加载器加载地图和景点数据 (`RoadNetwork`)。
    3.  初始化所有需要的服务实例 (`DijkstraPathfindingService`, `PermutationPoiOptimizerService`, `DynamicProgrammingPoiOptimizerService`, `TripPlanningService`)。
    4.  初始化用户界面交互组件 (`ConsoleController`)。
    5.  将服务依赖注入到需要它们的组件中（例如，将 `TripPlanningService` 注入 `ConsoleController`）。
    6.  启动用户交互流程（调用 `consoleController.run()`)。
    7.  打印最终的“应用程序结束”消息。
*   **是否必须:** **是**。作为应用程序的起点和组装器。

## com.cpt204.finalproject.model

### `City.java`

*   **用途:** 定义**城市**的数据模型。
*   **职责:** 存储一个城市的基本属性，如名称 (`name`)、州/省 (`state`) 以及地理坐标 (`latitude`, `longitude`)。包含构造函数、getter 方法以及必要的 `equals` 和 `hashCode` 方法，以便在集合（如 Map 的键）中正确使用。
*   **是否必须:** **是**。应用程序处理的核心对象之一。

### `Road.java`

*   **用途:** 定义**道路**的数据模型。
*   **职责:** 表示两个城市 (`source`, `destination`) 之间的**直接**连接及其距离 (`distance`)。主要在数据加载时使用，用于构建 `RoadNetwork`。
*   **是否必须:** **是**。用于从 CSV 加载数据并构建初始网络（即使内部使用矩阵，加载时仍可能通过 Road 对象）。

### `Attraction.java`

*   **用途:** 定义**景点**的数据模型。
*   **职责:** 存储一个景点的名称 (`attractionName`)、所在的城市名称 (`cityName`) 以及地理坐标 (`latitude`, `longitude`)。
*   **是否必须:** **是**。应用程序需要处理和规划包含景点的路线。

### `RoadNetwork.java`

*   **用途:** 封装整个**路网数据结构**。
*   **职责:**
    1.  存储所有 `City` 对象和 `Attraction` 对象。
    2.  使用二维数组 (`distanceMatrix`) 存储城市间的**直接**距离（针对邻接矩阵优化）。
    3.  提供通过名称 (`getCityByName`) 或索引查找城市的方法。
    4.  提供获取所有城市 (`getAllCities`) 和所有景点 (`getAllAttractions`, `getAttractionsInCity`) 的方法。
    5.  提供获取两个城市间直接距离 (`getDirectDistance`) 的方法。
    6.  提供动态生成从某个城市出发的所有 `Road` 对象 (`getRoadsFrom`) 的方法（供 Dijkstra 使用）。
*   **是否必须:** **是**。作为所有地理和连接数据的中心存储库。

## com.cpt204.finalproject.repository (原 dataloader)

### `CsvDataLoader.java`

*   **用途:** 负责从 **CSV 文件加载数据**。
*   **职责:**
    1.  读取 `roads.csv` 和 `attractions.csv` 文件。
    2.  解析文件内容。
    3.  创建 `City`, `Road`, `Attraction` 对象。
    4.  使用加载的对象构建并返回一个 `RoadNetwork` 实例。
    5.  将数据加载逻辑与应用程序的其他部分分离。
*   **是否必须:** **是**。为应用程序提供初始数据。

## com.cpt204.finalproject.services

### `PathfindingService.java`

*   **用途:** 定义**路径查找服务**的**接口**（契约）。
*   **职责:** 规定了任何路径查找实现都必须提供 `findShortestPath` 方法。同时定义了内部类 `PathResult` 作为该方法的标准返回类型，封装了路径、距离、时间和状态。
*   **是否必须:** **是**（作为接口）。定义清晰的服务契约是良好设计的体现，便于未来替换或添加其他寻路算法。

### `DijkstraPathfindingService.java`

*   **用途:** `PathfindingService` 接口的**具体实现**，使用 Dijkstra 算法。
*   **职责:** 根据给定的 `RoadNetwork`、起点和终点，**实时计算**它们之间的最短路径和距离。它在需要时被 POI 优化器和 `TripPlanningService` 调用。
*   **是否必须:** **是**。这是当前应用程序实际使用的寻路算法。

### `PoiOptimizerService.java`

*   **用途:** 定义 **POI 顺序优化服务**的**接口**（契约）。
*   **职责:** 规定了任何 POI 优化实现都必须提供 `findBestPoiOrder` 方法。定义了内部类 `OptimizerResult` 作为该方法的标准返回类型，封装了最佳 POI 顺序、总距离、时间和状态。
*   **是否必须:** **是**（作为接口）。定义清晰的服务契约。

### `PermutationPoiOptimizerService.java`

*   **用途:** `PoiOptimizerService` 接口的一个**实现**，使用**排列组合（暴力）**算法。
*   **职责:** 计算所有可能的 POI 访问顺序，并为每种顺序计算总距离（通过调用 `PathfindingService` 获取段距离），最终找出距离最短的顺序。适用于 POI 数量较少的情况（由 `TripPlanningService` 中的阈值决定）。
*   **是否必须:** **是**。因为 `TripPlanningService` 会根据 POI 数量选择性地使用它。

### `DynamicProgrammingPoiOptimizerService.java`

*   **用途:** `PoiOptimizerService` 接口的另一个**实现**，使用**动态规划**（类 Held-Karp）算法。
*   **职责:** 使用 DP 状态表来计算访问所有给定 POI 的最短路径顺序。比排列组合更有效，适用于 POI 数量较多的情况。它也依赖 `PathfindingService` 来获取计算状态转移所需的段距离（并包含一个内部缓存以避免重复调用）。
*   **是否必须:** **是**。因为 `TripPlanningService` 会根据 POI 数量选择性地使用它。

### `TripPlanningService.java`

*   **用途:** **高级服务**，负责**协调整个行程规划流程**。
*   **职责:**
    1.  接收起点、终点和景点名称列表。
    2.  验证输入，将景点名称转换为 `City` 对象列表。
    3.  根据 POI 数量选择合适的 `PoiOptimizerService`（排列或 DP）。
    4.  调用优化器获取最佳 POI 顺序和估计的总距离。
    5.  使用 `PathfindingService` 计算最终路径中每个连续停靠点之间的详细路径段。
    6.  组装所有信息（完整路径、总距离、时间、状态等）到 `TripPlan` 对象中并返回。
*   **是否必须:** **是**。封装了端到端的行程规划业务逻辑。

## com.cpt204.finalproject.dto (Data Transfer Object)

### `TripPlan.java`

*   **用途:** 定义用于**传输最终行程规划结果**的数据结构。
*   **职责:** 封装一次成功或失败的行程规划的所有相关信息，包括最终路径、总距离、优化和寻路时间、使用的算法、状态消息等。它在 `TripPlanningService` 和 `ConsoleController` 之间传递数据。
*   **是否必须:** **是**。提供了一个清晰、标准的结构来返回规划结果。

## com.cpt204.finalproject.controller

### `ConsoleController.java`

*   **用途:** 处理**控制台的用户交互**。
*   **职责:**
    1.  向用户显示提示信息（输入起点、终点、POI）。
    2.  使用 `Scanner` 读取用户的输入。
    3.  调用 `TripPlanningService` 的 `planTrip` 方法来执行规划请求。
    4.  获取返回的 `TripPlan` 对象。
    5.  将 `TripPlan` 对象的内容格式化并打印到控制台。
*   **是否必须:** **是**。负责将命令行用户界面与后端服务逻辑连接起来。

## 总结：是否有无用文件？

根据上述分析，在您当前项目的结构和功能下，**所有这些 `.java` 文件都有其特定的用途，并且都被直接或间接地使用，目前没有发现明显无用的文件。** 它们共同构成了一个分层良好、职责清晰的应用程序。