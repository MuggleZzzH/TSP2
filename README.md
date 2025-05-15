# 道路旅行规划器 (Road Trip Planner)

## 描述

这是一个基于 Java 和 JavaFX 构建的桌面应用程序，用于规划城市间的公路旅行路线。它不仅可以计算起点和终点之间的最短路径，还能优化沿途需要访问的兴趣点 (POI) 的访问顺序，并通过图形用户界面 (GUI) 直观地展示路线和地图。

与纯粹依赖预计算数据的系统不同，本应用的一个核心特点是在需要时进行 **按需路径计算**：当两个城市间的路径信息未被直接提供或需要重新评估时，它会实时运行寻路算法（如 Dijkstra）。

## 功能

*   **数据加载：** 从 CSV 文件 (`roads.csv`, `attractions.csv`) 加载城市、道路连接、距离以及景点信息。
*   **图形用户界面 (GUI)：**
    *   使用 JavaFX 构建，提供用户友好的交互界面。
    *   包含城市选择下拉框、景点选择列表、结果显示区域和交互式地图视图。
    *   支持中英文界面切换。
*   **实时路径查找：** 使用 Dijkstra 算法实时计算任意两个城市之间的最短路径。
*   **POI 顺序优化：**
    *   根据 POI 数量自动选择优化策略：
        *   少量 POI (例如 <=3)：使用 **排列组合 (暴力枚举) 算法**。
        *   中等数量 POI (例如 >3)：使用 **动态规划 (类 Held-Karp) 算法**。
    *   优化器依赖实时的 Dijkstra 算法来获取各路径段的距离。
*   **地图可视化：**
    *   在地图上绘制城市、道路、规划的路线和途径点。
    *   高亮显示起点、终点和途经城市。
    *   支持地图缩放和平移。
*   **高精度计时：** 使用 `System.nanoTime()` 测量路径查找和优化算法的执行时间，以提供精确的性能指标（主要在后端服务中体现）。
*   **输出：**
    *   在GUI的结果区域显示详细的行程计划，包括状态、使用的算法、耗时、总距离以及按顺序访问的城市列表。
    *   在地图上直观展示路线。

## 技术栈

*   Java (推荐 JDK 11 或更高版本，需包含 JavaFX)
*   JavaFX (用于 GUI)
*   Maven (用于依赖管理和构建)

## 项目结构

```
TripPlannerApp/
├── pom.xml                 # Maven 项目配置文件
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/cpt204/finalproject/
│   │   │       ├── TripPlannerApp.java # JavaFX GUI 应用主入口
│   │   │       ├── ui/                 # GUI 相关类 (MapView, LanguageManager 等)
│   │   │       ├── dataloader/       # CSV 数据加载器
│   │   │       ├── dto/              # 数据传输对象 (TripPlan)
│   │   │       ├── model/            # 数据模型 (City, Road, Attraction, RoadNetwork)
│   │   │       └── services/         # 核心服务 (Pathfinding, POI Optimization, Trip Planning)
│   │   └── resources/
│   │       ├── data/
│   │       │   ├── attractions.csv   # 景点数据
│   │       │   └── roads.csv         # 道路数据
│   │       └── lang/                 # 语言配置文件
│   │           ├── lang_en.properties
│   │           └── lang_zh.properties
│   └── test/                     # 测试代码 (如果添加)
└── README.md               # 本文档
```

## 数据格式

应用程序期望以下 CSV 文件位于 `src/main/resources/data/` 目录下：

1.  **`roads.csv`**
    *   格式: `CityA,CityB,Distance`
    *   示例: `New York NY,Chicago IL,790`
    *   描述: 表示 `CityA` 和 `CityB` 之间的 **直接** 道路距离。
2.  **`attractions.csv`**
    *   格式: `Place of Interest,Location`
    *   示例: `The Alamo,San Antonio TX`
    *   描述: 列出兴趣点及其所在的城市 (`Location` 必须与 `roads.csv` 中的城市名称匹配)。

## 设置

1.  **克隆仓库:**
    ```bash
    git clone <your-repository-url>
    cd TripPlannerApp
    ```
2.  **准备数据文件:**
    *   确保 `roads.csv` 和 `attractions.csv` 文件存在于 `src/main/resources/data/` 目录下。
    *   确保 `lang_en.properties` 和 `lang_zh.properties` 文件存在于 `src/main/resources/lang/` 目录下。
    *   确保文件内容符合上述格式和项目期望。
3.  **环境要求:**
    *   安装 Java Development Kit (JDK)，推荐版本 11 或更高。**确保您的 JDK 版本包含 JavaFX，或者您已单独配置 JavaFX SDK。**
    *   安装 Apache Maven。

## 如何运行应用程序

### 方法一：使用 IDE (推荐)

1.  将项目作为 Maven 项目导入您喜欢的 Java IDE (如 IntelliJ IDEA, Eclipse)。
2.  IDE 通常会自动下载 `pom.xml` 中定义的依赖项 (包括 JavaFX)。
3.  找到 `src/main/java/com/cpt204/finalproject/TripPlannerApp.java` 文件。
4.  直接运行 `TripPlannerApp.java`。IDE 会处理编译和运行 JavaFX 应用程序。

### 方法二：使用 Maven (通过命令行)

1.  打开命令行/终端，导航到项目根目录 (包含 `pom.xml` 的目录)。
2.  编译项目:
    ```bash
    mvn clean compile
    ```
3.  运行 JavaFX 应用 (通常使用 `javafx-maven-plugin`):
    ```bash
    mvn javafx:run
    ```
    *注意：确保 `pom.xml` 中已正确配置 `javafx-maven-plugin` 并且指定了主类为 `com.cpt204.finalproject.TripPlannerApp`。*

### 方法三：手动编译和运行 (极不推荐，尤其对于JavaFX项目)

对于包含 JavaFX 和 Maven 的项目，手动编译和运行非常繁琐且容易出错，强烈建议使用 IDE 或 Maven。

## GUI 操作简介

1.  **语言切换**：通过应用窗口右上角的按钮可以切换界面语言（中文/英文）。
2.  **城市选择**：
    *   在左侧面板的"城市选择"区域，使用"搜索城市"文本框可以筛选城市列表。
    *   从"起始城市"和"目的地城市"下拉框中选择您的起点和终点。
3.  **景点选择**：
    *   在"景点选择"区域，使用"搜索景点"文本框筛选可选景点。
    *   从"可选景点"列表中选择一个景点，点击"添加 >"按钮将其加入"已选景点"列表。
    *   从"已选景点"列表中选择一个景点，点击"< 删除"按钮将其移除。
4.  **路线计算**：完成城市和景点选择后，点击"计算最佳路线"按钮。
5.  **查看结果**：
    *   规划的详细路线信息将显示在右侧面板的"计算结果"文本区域。
    *   路线将在下方的"路线地图"区域可视化展示。地图支持通过鼠标滚轮缩放，通过拖拽平移。

## 配置 (开发者相关)

*   **数据文件**：如上所述，道路和景点数据通过 CSV 文件配置。语言文本通过 `.properties` 文件配置。
*   **优化器阈值**：`TripPlanningService.java` 中的 `PERMUTATION_THRESHOLD` 常量决定了何时从排列组合切换到动态规划优化器。
*   **超时控制**：`TripPlanningService.java` 在调用优化器时，可以配置超时参数，以防止算法运行时间过长。

## (可选) 未来改进

*   实现更高级的模糊搜索功能（例如，Levenshtein 距离）来处理拼写错误。
*   支持用户在地图上直接点击选择城市或添加自定义途经点。
*   允许将行程计划保存到文件或导出。
*   考虑交通状况、道路类型偏好（如避开高速公路）等更多实际因素。
*   增加更全面的单元测试和集成测试。
*   提供更多地图交互功能，如显示城市/景点详细信息弹窗。 
