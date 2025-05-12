# Trip Planner Application

## 描述

这是一个基于 Java 的命令行应用程序，用于规划两个城市之间的公路旅行路线。它可以计算起点和终点之间的最短路径，并能优化沿途需要访问的兴趣点 (POI) 的访问顺序。

当前实现的核心特点是 **按需计算**：每当需要两个城市之间的最短距离时，它会 **实时运行 Dijkstra 算法**，而不是依赖任何预计算（如 Floyd-Warshall）或存储在本地的路径信息。

## 功能

*   **数据加载：** 从 CSV 文件 (`roads.csv`, `attractions.csv`) 加载城市、直接道路距离和景点信息。
*   **实时路径查找：** 使用 Dijkstra 算法实时计算任意两个城市之间的最短路径。
*   **POI 顺序优化：**
    *   对于少量 POI (<=10)，使用 **排列组合 (暴力) 算法** 查找最佳访问顺序。
    *   对于中等数量 POI (>10)，使用 **动态规划 (类 Held-Karp) 算法** 查找最佳访问顺序。
    *   两个优化器都依赖实时的 Dijkstra 算法来获取路径段距离。
*   **基本模糊匹配：** 在查找城市和景点名称时，不区分大小写并忽略首尾空格。
*   **高精度计时：** 使用 `System.nanoTime()` 测量路径查找和优化算法的执行时间，提供更精确的性能指标。
*   **输出：** 显示最终的行程计划，包括：
    *   状态（成功、失败、超时）
    *   使用的优化器和寻路器算法名称
    *   优化器和总寻路计算时间（毫秒，带小数精度）
    *   总行程距离
    *   按顺序访问的完整城市列表

## 技术栈

*   Java (推荐 JDK 11 或更高版本)
*   Maven (用于依赖管理和构建 - 如果使用了 `pom.xml`)

## 项目结构

```
TripPlannerApp/
├── pom.xml                 # Maven 项目配置文件 (如果存在)
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/cpt204/finalproject/
│   │   │       ├── Main.java         # 程序入口
│   │   │       ├── dataloader/       # CSV 数据加载器
│   │   │       ├── dto/              # 数据传输对象 (TripPlan)
│   │   │       ├── model/            # 数据模型 (City, Road, Attraction, RoadNetwork)
│   │   │       └── services/         # 核心服务 (Pathfinding, POI Optimization, Trip Planning)
│   │   └── resources/
│   │       └── data/
│   │           ├── attractions.csv   # 景点数据
│   │           └── roads.csv         # 道路数据
│   └── test/                     # 测试代码 (如果添加)
└── README.md               # 本文档
```

## 数据格式

应用程序期望以下 CSV 文件位于 `src/main/resources/data/` 目录下：

1.  **`roads.csv`**
    *   格式: `CityA,CityB,Distance`
    *   示例: `New York NY,Chicago IL,790`
    *   描述: 表示 `CityA` 和 `CityB` 之间的 **直接** 道路距离。该文件应包含所有必要的直接连接，因为程序会基于这些连接实时计算最短路径。
2.  **`attractions.csv`**
    *   格式: `Place of Interest,Location`
    *   示例: `The Alamo,San Antonio TX`
    *   描述: 列出兴趣点及其所在的城市 (`Location` 必须与 `roads.csv` 中的城市名称匹配)。

## 设置

1.  **克隆仓库:**
    ```bash
    git clone git@github.com:MuggleZzzH/TSP2.git
    cd TripPlannerApp
    ```
2.  **准备数据文件:**
    *   确保 `roads.csv` 和 `attractions.csv` 文件存在于 `src/main/resources/data/` 目录下。
    *   确保文件内容符合上述格式。
3.  **环境要求:**
    *   安装 Java Development Kit (JDK)，推荐版本 11 或更高。
    *   (可选，推荐) 安装 Apache Maven。

## 如何运行

### 方法一：使用 IDE (推荐)

1.  将项目导入您喜欢的 Java IDE (如 IntelliJ IDEA, Eclipse) 中。
    *   如果项目包含 `pom.xml`，通常可以作为 Maven 项目导入。
2.  找到 `src/main/java/com/cpt204/finalproject/Main.java` 文件。
3.  直接运行 `Main.java`。IDE 通常会自动处理编译和资源文件的 classpath。

### 方法二：使用 Maven (如果配置了 `pom.xml`)

1.  打开命令行/终端，导航到项目根目录 (包含 `pom.xml` 的目录)。
2.  编译项目:
    ```bash
    mvn compile
    ```
3.  运行主类:
    ```bash
    mvn exec:java -Dexec.mainClass="com.cpt204.finalproject.Main"
    ```

### 方法三：手动编译和运行 (不推荐，较复杂)

1.  打开命令行/终端，导航到 `src/main/java` 目录。
2.  编译所有 Java 文件，确保将编译后的 `.class` 文件输出到合适的目录 (例如，项目根目录下的 `target/classes`)，并且要包含资源文件目录到 classpath：
    ```bash
    # 需要手动管理 classpath 和输出目录，比较繁琐
    # javac -d ../../../target/classes -cp ../../../target/classes $(find . -name "*.java")
    ```
3.  导航回项目根目录。
4.  运行主类，确保 classpath 包含了编译后的类和资源目录：
    ```bash
    # java -cp target/classes;src/main/resources com.cpt204.finalproject.Main # Windows 示例
    # java -cp target/classes:src/main/resources com.cpt204.finalproject.Main # Linux/macOS 示例
    ```
    *注意: 手动管理 classpath 容易出错，推荐使用 IDE 或 Maven。*

## 配置

*   **行程参数:** 可以在 `Main.java` 文件中修改以下变量来配置要规划的行程：
    *   `startCityName`: 起始城市名称。
    *   `endCityName`: 目的城市名称。
    *   `attractionsToVisit`: 一个包含要访问的景点名称的 `List<String>`。
*   **优化器阈值:** `TripPlanningService.java` 中的 `PERMUTATION_THRESHOLD` 常量决定了何时从排列组合切换到动态规划优化器。
*   **超时:** 可以在调用 `planTrip` 方法时设置 `useTimeout` 和 `timeoutMillis` 参数来控制优化器的最大执行时间。

## (可选) 未来改进

*   添加图形用户界面 (GUI)。
*   实现更高级的模糊搜索功能（例如，Levenshtein 距离）来处理拼写错误。
*   支持不同的路径查找算法（例如，A*）。
*   允许将行程计划保存到文件。
*   添加对交通状况或偏好的考虑（例如，避开高速公路）。
*   增加单元测试和集成测试。 
