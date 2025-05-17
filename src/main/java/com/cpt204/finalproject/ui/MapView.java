package com.cpt204.finalproject.ui;

import com.cpt204.finalproject.dto.TripPlan;
import com.cpt204.finalproject.model.Attraction;
import com.cpt204.finalproject.model.City;
import com.cpt204.finalproject.model.Road;
import com.cpt204.finalproject.model.RoadNetwork;
import com.cpt204.finalproject.services.PathfindingService;

import javafx.geometry.Insets;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Glow;
import javafx.util.Duration;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;

/**
 * 路线地图视图 - 用于可视化城市、道路和旅行路线
 */
public class MapView extends BorderPane {
    private Canvas canvas;
    private Pane canvasContainer;
    private RoadNetwork roadNetwork;
    private Map<String, Point> cityPositions;
    private Map<String, Point> attractionPositions;
    private TripPlan currentPlan;
    private Button languageSwitchButton;
    private Button zoomInButton;
    private Button zoomOutButton;
    private Button resetViewButton;
    private Slider zoomSlider;
    
    // Tooltip Label for city/attraction info
    private Label infoTooltipLabel;
    
    // 视图变换参数
    private double zoomFactor = 1.0;
    private double translateX = 0;
    private double translateY = 0;
    private double dragStartX, dragStartY;
    
    // 定义美观的颜色方案
    private static final Color BACKGROUND_COLOR = Color.web("#f8f9fa");
    private static final Color CITY_COLOR = Color.web("#3498db");
    private static final Color START_CITY_COLOR = Color.web("#2ecc71");
    private static final Color END_CITY_COLOR = Color.web("#e74c3c");
    private static final Color WAYPOINT_COLOR = Color.web("#f39c12");
    private static final Color ROAD_COLOR = Color.web("#c0c0c0");
    private static final Color ROUTE_COLOR = Color.web("#9b59b6");
    private static final Color ATTRACTION_COLOR = Color.web("#e67e22");
    private static final Color TEXT_COLOR = Color.web("#2c3e50");
    private static final Color ROUTE_HIGHLIGHT_COLOR = Color.web("#8e44ad");
    
    // 尺寸参数
    private static final double CITY_RADIUS = 15;
    private static final double ATTRACTION_RADIUS = 10;
    private static final double ARROW_LENGTH = 18;
    private static final double ARROW_WIDTH = 10;
    private static final double ROAD_WIDTH = 1.0;
    private static final double ROUTE_WIDTH = 6.0;
    private static final double WAYPOINT_LABEL_OFFSET = 35;
    private static final double DISTANCE_LABEL_THRESHOLD = 200; // 只有较长的路才显示距离标签

    public MapView(RoadNetwork roadNetwork) {
        this.roadNetwork = roadNetwork;
        
        // 创建画布和容器
        canvasContainer = new Pane();
        canvas = new Canvas(800, 600);
        canvasContainer.getChildren().add(canvas);
        
        // Initialize and add the info tooltip label
        infoTooltipLabel = new Label();
        infoTooltipLabel.setVisible(false);
        infoTooltipLabel.setStyle(
            "-fx-background-color: rgba(240, 240, 240, 0.95); " +
            "-fx-text-fill: #2c3e50; " +
            "-fx-padding: 8px; " +
            "-fx-border-color: #bdc3c7; " +
            "-fx-border-width: 1px; " +
            "-fx-border-radius: 4px; " +
            "-fx-background-radius: 4px; " +
            "-fx-font-size: 13px;"
        );
        infoTooltipLabel.setWrapText(true); // Allow multi-line text
        infoTooltipLabel.setMaxWidth(250); // Prevent it from becoming too wide
        
        // Add drop shadow for better visibility
        DropShadow tooltipShadow = new DropShadow();
        tooltipShadow.setRadius(5.0);
        tooltipShadow.setOffsetX(2.0);
        tooltipShadow.setOffsetY(2.0);
        tooltipShadow.setColor(Color.color(0, 0, 0, 0.3));
        infoTooltipLabel.setEffect(tooltipShadow);
        
        canvasContainer.getChildren().add(infoTooltipLabel); // Add to container so it's on top of canvas
        
        setCenter(canvasContainer);
        
        // 设置地图背景
        setBackground(new Background(
            new BackgroundFill(BACKGROUND_COLOR, CornerRadii.EMPTY, Insets.EMPTY)));
        
        // 添加边框效果
        setStyle("-fx-border-color: #e0e0e0; -fx-border-width: 1px;");
        
        // 创建控制按钮面板
        HBox controlPanel = createControlPanel();
        setTop(controlPanel);
        
        // 设置拖拽事件处理
        setupDragHandlers();
        setupMouseHoverHandler();
        
        // 初始化城市和景点位置
        initializePositions();
        
        // 画布尺寸随容器变化
        canvasContainer.widthProperty().addListener((obs, oldVal, newVal) -> {
            canvas.setWidth(newVal.doubleValue());
            redraw();
        });
        
        canvasContainer.heightProperty().addListener((obs, oldVal, newVal) -> {
            canvas.setHeight(newVal.doubleValue());
            redraw();
        });
        
        // 初始绘制
        redraw();
    }
    
    private HBox createControlPanel() {
        HBox panel = new HBox(10);
        panel.setPadding(new Insets(5));
        panel.setStyle("-fx-background-color: #ecf0f1; -fx-border-color: #bdc3c7; -fx-border-width: 0 0 1 0;");
        
        // 语言切换按钮
        languageSwitchButton = new Button(LanguageManager.getText("languageSwitch"));
        languageSwitchButton.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-weight: bold;");
        languageSwitchButton.setOnAction(e -> {
            LanguageManager.toggleLanguage();
            languageSwitchButton.setText(LanguageManager.getText("languageSwitch"));
            redraw();
        });
        
        // 缩放按钮
        zoomInButton = new Button("+");
        zoomInButton.setStyle("-fx-background-color: #2ecc71; -fx-text-fill: white; -fx-font-weight: bold;");
        zoomInButton.setOnAction(e -> zoom(1.2));
        
        zoomOutButton = new Button("-");
        zoomOutButton.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold;");
        zoomOutButton.setOnAction(e -> zoom(0.8));
        
        // 重置视图按钮
        resetViewButton = new Button("🔄");
        resetViewButton.setStyle("-fx-background-color: #f39c12; -fx-text-fill: white; -fx-font-weight: bold;");
        resetViewButton.setOnAction(e -> resetView());
        
        Tooltip.install(resetViewButton, new Tooltip("重置视图"));
        
        // 缩放滑块
        zoomSlider = new Slider(0.5, 3.0, 1.0);
        zoomSlider.setPrefWidth(150);
        zoomSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            zoomFactor = newVal.doubleValue();
            redraw();
        });
        
        panel.getChildren().addAll(
            languageSwitchButton, 
            zoomInButton, 
            zoomSlider, 
            zoomOutButton, 
            resetViewButton
        );
        
        return panel;
    }
    
    private void setupDragHandlers() {
        canvas.setOnMousePressed(e -> {
            dragStartX = e.getX();
            dragStartY = e.getY();
        });
        
        canvas.setOnMouseDragged(e -> {
            double deltaX = e.getX() - dragStartX;
            double deltaY = e.getY() - dragStartY;
            translateX += deltaX / zoomFactor;
            translateY += deltaY / zoomFactor;
            dragStartX = e.getX();
            dragStartY = e.getY();
            redraw();
        });
        
        canvas.setOnScroll(e -> {
            double delta = e.getDeltaY() > 0 ? 1.1 : 0.9;
            zoom(delta);
        });
    }
    
    private void setupMouseHoverHandler() {
        canvas.setOnMouseMoved(e -> {
            // Convert mouse coordinates from canvas space to world space (considering zoom and translation)
            double mouseWorldX = (e.getX() / zoomFactor) - translateX;
            double mouseWorldY = (e.getY() / zoomFactor) - translateY;

            String tooltipText = null;
            Point hoveredItemPosition = null; // Store the position of the item being hovered to position the tooltip

            // Check cities first
            for (Map.Entry<String, Point> entry : cityPositions.entrySet()) {
                String cityName = entry.getKey();
                Point cityScreenPos = entry.getValue(); // cityPositions stores screen coordinates (after zoom/translate in redraw)
                
                // We need to check against the *original* logical positions before zoom/translate
                // Or, convert mouse acreen coordinates to the same space as cityPositions are calculated in initializePositions
                // For simplicity, let's assume cityPositions are already in a consistent coordinate space for hit testing relative to mouseWorldX/Y.
                // This part might need refinement based on how cityPositions are truly calculated and updated.
                // The current cityPositions are screen positions, so we compare with e.getX(), e.getY()

                double distSq = Math.pow(e.getX() - cityScreenPos.x, 2) + Math.pow(e.getY() - cityScreenPos.y, 2);
                if (distSq < CITY_RADIUS * CITY_RADIUS * zoomFactor * zoomFactor) { // Consider zoomFactor for hit radius
                    City city = roadNetwork.getCityByName(cityName);
                    if (city != null) {
                        StringBuilder sb = new StringBuilder();
                        sb.append(LanguageManager.getText("cityLabelPrefix")).append(city.getName()); // e.g., "City: "
                        
                        Set<Attraction> attractionsInCity = roadNetwork.getAttractionsInCity(cityName); // Changed to use existing method and Set
                        if (attractionsInCity != null && !attractionsInCity.isEmpty()) {
                            sb.append("\n").append(LanguageManager.getText("attractionsLabelPrefix")); // e.g., "Attractions:"
                            for (Attraction attraction : attractionsInCity) {
                                sb.append("\n - ").append(attraction.getAttractionName());
                            }
                        }
                        tooltipText = sb.toString();
                        hoveredItemPosition = cityScreenPos;
                        break; 
                    }
                }
            }

            // If no city hovered, check attractions (optional, can be added later)
            // For now, focusing on city hover with attractions

            if (tooltipText != null && hoveredItemPosition != null) {
                infoTooltipLabel.setText(tooltipText);
                // Position the tooltip slightly offset from the mouse or the item
                // Adjust these offsets as needed
                double tooltipX = hoveredItemPosition.x + 15; // Offset from city center
                double tooltipY = hoveredItemPosition.y - infoTooltipLabel.getBoundsInLocal().getHeight() / 2; // Center vertically or above
                
                // Ensure tooltip stays within canvasContainer bounds
                if (tooltipX + infoTooltipLabel.getWidth() > canvasContainer.getWidth()) {
                    tooltipX = hoveredItemPosition.x - infoTooltipLabel.getWidth() - 15;
                }
                if (tooltipY < 0) {
                    tooltipY = 0;
                }
                if (tooltipY + infoTooltipLabel.getHeight() > canvasContainer.getHeight()) {
                    tooltipY = canvasContainer.getHeight() - infoTooltipLabel.getHeight();
                }

                infoTooltipLabel.setLayoutX(tooltipX);
                infoTooltipLabel.setLayoutY(tooltipY);
                infoTooltipLabel.setVisible(true);
            } else {
                infoTooltipLabel.setVisible(false);
            }
        });

        // Hide tooltip when mouse exits the canvas area
        canvas.setOnMouseExited(e -> {
            infoTooltipLabel.setVisible(false);
        });
    }
    
    private void zoom(double factor) {
        zoomFactor *= factor;
        zoomFactor = Math.max(0.5, Math.min(zoomFactor, 3.0));
        zoomSlider.setValue(zoomFactor);
        redraw();
    }
    
    private void resetView() {
        zoomFactor = 1.0;
        translateX = 0;
        translateY = 0;
        zoomSlider.setValue(1.0);
        redraw();
    }
    
    // Point类现在支持带标签的点位置
    private static class Point {
        double x;
        double y;
        boolean isHighlighted = false;
        
        Point(double x, double y) {
            this.x = x;
            this.y = y;
        }
        
        Point copy() {
            return new Point(x, y);
        }
        
        // 添加偏移量以防止标签重叠
        void offset(double dx, double dy) {
            this.x += dx;
            this.y += dy;
        }
    }
    
    private void initializePositions() {
        if (roadNetwork == null) return;
        
        cityPositions = new HashMap<>();
        attractionPositions = new HashMap<>();
        
        // 获取画布尺寸
        double viewWidth = canvas.getWidth();
        double viewHeight = canvas.getHeight();

        if (viewWidth <= 0) viewWidth = 800;
        if (viewHeight <= 0) viewHeight = 600;
        
        double marginX = viewWidth * 0.12;
        double marginY = viewHeight * 0.12;
        double availableWidth = viewWidth - 2 * marginX;
        double availableHeight = viewHeight - 2 * marginY;
        
        List<City> cities = roadNetwork.getAllCities();
        int cityCount = cities.size();
        
        boolean usePresetLayout = tryUsePresetLayout(cities, viewWidth, viewHeight, marginX, marginY);
        
        if (!usePresetLayout) {
            if (cityCount > 0) {
                double centerX = viewWidth / 2.0;
                double centerY = viewHeight / 2.0;

                double radiusX = availableWidth / 2.0 * 0.9;
                double radiusY = availableHeight / 2.0 * 0.9;

                if (cityCount < 5) {
                    radiusX *= 0.6;
                    radiusY *= 0.6;
                } else if (cityCount < 10) {
                    radiusX *= 0.8;
                    radiusY *= 0.8;
                }

                double angleStep = 2 * Math.PI / cityCount;

                for (int i = 0; i < cityCount; i++) {
                    City city = cities.get(i);
                    double currentAngle = i * angleStep + (Math.PI / (cityCount * 2.0));

                    double x = centerX + radiusX * Math.cos(currentAngle);
                    double y = centerY + radiusY * Math.sin(currentAngle);
                    
                    cityPositions.put(city.getName(), new Point(x, y));
                }
                    }
                }
                
        // 为景点分配位置 - 围绕城市，但间距更大
        Random attrRandom = new Random(42); // 使用固定种子
        Collection<Attraction> attractions = roadNetwork.getAllAttractions();
        // Map<String, Integer> cityAttractionCount = new HashMap<>(); // Old counter logic

        // Pass 1: Count attractions per city
        Map<String, Integer> attractionsPerCityMap = new HashMap<>();
        if (attractions != null) {
            for (Attraction attraction : attractions) {
                attractionsPerCityMap.put(attraction.getCityName(), 
                                        attractionsPerCityMap.getOrDefault(attraction.getCityName(), 0) + 1);
            }
        }
        
        // Pass 2: Position attractions
        Map<String, Integer> currentAttractionIndexForCity = new HashMap<>(); // To get 0-indexed 'count' for angle
        
        if (attractions != null) {
        for (Attraction attraction : attractions) {
            String cityName = attraction.getCityName();
            String attractionName = attraction.getAttractionName();
            
            Point cityPos = cityPositions.get(cityName);
            if (cityPos != null) {
                    int totalAttractionsInThisCity = attractionsPerCityMap.getOrDefault(cityName, 1);
                    int attractionOIndexed = currentAttractionIndexForCity.getOrDefault(cityName, 0);

                    double angle = 0;
                    if (totalAttractionsInThisCity > 1) { // Only apply varied angle if more than one
                        angle = 2 * Math.PI * attractionOIndexed / totalAttractionsInThisCity;
                    } else {
                        // For a single attraction, a default angle (e.g., downwards) might be nicer than 0
                        angle = -Math.PI / 2; 
                    }
                    
                    // Calculate the maximum radius within the city for the center of the attraction icon
                    double maxPlacementRadius = CITY_RADIUS - ATTRACTION_RADIUS - 2.0; // 2.0 is padding
                    if (maxPlacementRadius < 0) {
                        maxPlacementRadius = 0; // Place at city center if no room (attraction as large as city or larger)
                    }

                    double distance;
                    if (totalAttractionsInThisCity == 1) {
                        distance = 0; // Place single attraction at the city center
                    } else {
                        // Multiple attractions: distribute them in a ring.
                        // Range: 0.5 * maxPlacementRadius to 0.9 * maxPlacementRadius
                        distance = maxPlacementRadius * (0.5 + attrRandom.nextDouble() * 0.4);
                    }
                
                double x = cityPos.x + distance * Math.cos(angle);
                double y = cityPos.y + distance * Math.sin(angle);
                
                attractionPositions.put(attractionName, new Point(x, y));
                    currentAttractionIndexForCity.put(cityName, attractionOIndexed + 1);
                }
            }
        }
    }
    
    /**
     * 尝试使用预设的城市布局（针对美国主要城市）
     * @param cities 城市列表
     * @param width 画布宽度
     * @param height 画布高度
     * @param marginX 水平边距
     * @param marginY 垂直边距
     * @return 是否使用了预设布局
     */
    private boolean tryUsePresetLayout(List<City> cities, double width, double height, double marginX, double marginY) {
        // 常见美国城市位置映射（基于相对地理位置）
        Map<String, double[]> cityCoordinates = new HashMap<>();
        
        // 东部城市
        cityCoordinates.put("New York", new double[]{0.80, 0.30});
        cityCoordinates.put("New York NY", new double[]{0.80, 0.30});
        cityCoordinates.put("Boston", new double[]{0.85, 0.20});
        cityCoordinates.put("Boston MA", new double[]{0.85, 0.20});
        cityCoordinates.put("Washington", new double[]{0.75, 0.35});
        cityCoordinates.put("Washington DC", new double[]{0.75, 0.35});
        cityCoordinates.put("Philadelphia", new double[]{0.78, 0.32});
        cityCoordinates.put("Philadelphia PA", new double[]{0.78, 0.32});
        cityCoordinates.put("Miami", new double[]{0.75, 0.85});
        cityCoordinates.put("Miami FL", new double[]{0.75, 0.85});
        cityCoordinates.put("Atlanta", new double[]{0.65, 0.60});
        cityCoordinates.put("Atlanta GA", new double[]{0.65, 0.60});
        
        // 中部城市
        cityCoordinates.put("Chicago", new double[]{0.60, 0.30});
        cityCoordinates.put("Chicago IL", new double[]{0.60, 0.30});
        cityCoordinates.put("Detroit", new double[]{0.65, 0.25});
        cityCoordinates.put("Detroit MI", new double[]{0.65, 0.25});
        cityCoordinates.put("St. Louis", new double[]{0.55, 0.45});
        cityCoordinates.put("St. Louis MO", new double[]{0.55, 0.45});
        cityCoordinates.put("Dallas", new double[]{0.45, 0.65});
        cityCoordinates.put("Dallas TX", new double[]{0.45, 0.65});
        cityCoordinates.put("Houston", new double[]{0.48, 0.75});
        cityCoordinates.put("Houston TX", new double[]{0.48, 0.75});
        cityCoordinates.put("New Orleans", new double[]{0.55, 0.80});
        cityCoordinates.put("New Orleans LA", new double[]{0.55, 0.80});
        
        // 西部城市
        cityCoordinates.put("Denver", new double[]{0.35, 0.40});
        cityCoordinates.put("Denver CO", new double[]{0.35, 0.40});
        cityCoordinates.put("Salt Lake City", new double[]{0.25, 0.35});
        cityCoordinates.put("Salt Lake City UT", new double[]{0.25, 0.35});
        cityCoordinates.put("Phoenix", new double[]{0.25, 0.60});
        cityCoordinates.put("Phoenix AZ", new double[]{0.25, 0.60});
        cityCoordinates.put("Los Angeles", new double[]{0.15, 0.55});
        cityCoordinates.put("Los Angeles CA", new double[]{0.15, 0.55});
        cityCoordinates.put("San Francisco", new double[]{0.10, 0.40});
        cityCoordinates.put("San Francisco CA", new double[]{0.10, 0.40});
        cityCoordinates.put("Seattle", new double[]{0.15, 0.15});
        cityCoordinates.put("Seattle WA", new double[]{0.15, 0.15});
        cityCoordinates.put("Portland", new double[]{0.12, 0.20});
        cityCoordinates.put("Portland OR", new double[]{0.12, 0.20});
        
        // 中部更多城市
        cityCoordinates.put("Nashville", new double[]{0.62, 0.52});
        cityCoordinates.put("Nashville TN", new double[]{0.62, 0.52});
        cityCoordinates.put("Kansas City", new double[]{0.50, 0.42});
        cityCoordinates.put("Kansas City MO", new double[]{0.50, 0.42});
        cityCoordinates.put("Minneapolis", new double[]{0.55, 0.20});
        cityCoordinates.put("Minneapolis MN", new double[]{0.55, 0.20});
        cityCoordinates.put("Jacksonville", new double[]{0.72, 0.70});
        cityCoordinates.put("Jacksonville FL", new double[]{0.72, 0.70});
        cityCoordinates.put("Columbus", new double[]{0.70, 0.35});
        cityCoordinates.put("Columbus OH", new double[]{0.70, 0.35});
        
        // 中国主要城市
        cityCoordinates.put("北京", new double[]{0.20, 0.30});
        cityCoordinates.put("上海", new double[]{0.40, 0.50});
        cityCoordinates.put("广州", new double[]{0.35, 0.75});
        cityCoordinates.put("深圳", new double[]{0.38, 0.78});
        cityCoordinates.put("成都", new double[]{0.15, 0.60});
        cityCoordinates.put("重庆", new double[]{0.20, 0.65});
        cityCoordinates.put("西安", new double[]{0.25, 0.45});
        cityCoordinates.put("南京", new double[]{0.45, 0.45});
        cityCoordinates.put("武汉", new double[]{0.40, 0.60});
        cityCoordinates.put("杭州", new double[]{0.50, 0.55});
        cityCoordinates.put("天津", new double[]{0.25, 0.35});
        cityCoordinates.put("哈尔滨", new double[]{0.40, 0.15});
        cityCoordinates.put("长春", new double[]{0.45, 0.20});
        cityCoordinates.put("沈阳", new double[]{0.35, 0.25});
        cityCoordinates.put("长沙", new double[]{0.30, 0.65});
        cityCoordinates.put("郑州", new double[]{0.40, 0.40});
        
        // 检查是否可以使用预设布局
        int matchCount = 0;
        for (City city : cities) {
            if (cityCoordinates.containsKey(city.getName())) {
                matchCount++;
            }
        }
        
        // 如果超过一半的城市可以使用预设布局，则使用预设布局
        boolean usePresetLayout = matchCount > cities.size() / 2;
        
        if (usePresetLayout) {
            double availableWidth = width - 2 * marginX;
            double availableHeight = height - 2 * marginY;
            
            for (City city : cities) {
                double[] coordinates = cityCoordinates.get(city.getName());
                
                if (coordinates != null) {
                    // 使用预设坐标
                    double x = marginX + coordinates[0] * availableWidth;
                    double y = marginY + coordinates[1] * availableHeight;
                    cityPositions.put(city.getName(), new Point(x, y));
                } else {
                    // 对于没有预设坐标的城市，使用随机位置
                    Random random = new Random(city.getName().hashCode());
                    double x = marginX + random.nextDouble() * availableWidth;
                    double y = marginY + random.nextDouble() * availableHeight;
                    cityPositions.put(city.getName(), new Point(x, y));
                }
            }
        }
        
        // return usePresetLayout; // Temporarily disable preset layout
        return false; // Force disable preset layout to test elliptical layout
    }
    
    public void setTripPlan(TripPlan plan) {
        this.currentPlan = plan;
        // 重置视图以便看到完整路线
        resetView();
        redraw();
    }
    
    public void redraw() {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        double width = canvas.getWidth();
        double height = canvas.getHeight();
        
        // 清空画布
        gc.clearRect(0, 0, width, height);
        
        // 应用变换
        gc.save();
        gc.translate(width / 2 + translateX, height / 2 + translateY);
        gc.scale(zoomFactor, zoomFactor);
        gc.translate(-width / 2, -height / 2);
        
        // 绘制所有道路
        drawRoads(gc);
        
        // 绘制所有景点 - 注释掉此行以取消独立绘制景点
        // drawAttractions(gc);
        
        // 如果有行程计划，则绘制路线
        if (currentPlan != null) {
            drawRoutes(gc);
        }
        
        // 绘制所有城市（最后绘制以确保它们位于顶层）
        drawCities(gc);
        
        // 绘制图例
        // drawLegend(gc); // Legend will be handled outside MapView
        
        // 恢复变换
        gc.restore();
    }
    
    private void drawRoads(GraphicsContext gc) {
        // 设置线条样式以增强可视效果
        Color currentRoadColor = ROAD_COLOR;
        if (currentPlan != null && currentPlan.getDetailedSegments() != null && !currentPlan.getDetailedSegments().isEmpty()) {
            currentRoadColor = ROAD_COLOR.deriveColor(0, 1, 1, 0.3); // Make roads fainter if a route is active
        }
        gc.setStroke(currentRoadColor);
        gc.setLineWidth(ROAD_WIDTH);
        gc.setLineCap(StrokeLineCap.ROUND);
        gc.setLineJoin(StrokeLineJoin.ROUND);
        
        // 获取有规划路线的城市对
        Set<String> routeCityPairs = new HashSet<>();
        if (currentPlan != null && currentPlan.getDetailedSegments() != null) {
            for (PathfindingService.PathResult pathResult : currentPlan.getDetailedSegments()) {
                List<City> cities = pathResult.getPath();
                for (int i = 0; i < cities.size() - 1; i++) {
                    String fromCity = cities.get(i).getName();
                    String toCity = cities.get(i + 1).getName();
                    routeCityPairs.add(fromCity + "-" + toCity);
                    routeCityPairs.add(toCity + "-" + fromCity);
                }
            }
        }
        
        // 先绘制所有道路
        for (Road road : roadNetwork.getAllRoads()) {
            String fromCityName = road.getSource().getName();
            String toCityName = road.getDestination().getName();
            
            // 如果这条道路是规划路线的一部分，则不在这里绘制（稍后会绘制高亮路线）
            String cityPair = fromCityName + "-" + toCityName;
            if (routeCityPairs.contains(cityPair)) {
                continue;
            }
            
            Point fromPos = cityPositions.get(fromCityName);
            Point toPos = cityPositions.get(toCityName);
            
            if (fromPos != null && toPos != null) {
                // 绘制道路
                gc.setStroke(currentRoadColor);
                gc.setLineWidth(ROAD_WIDTH);
                gc.strokeLine(fromPos.x, fromPos.y, toPos.x, toPos.y);
                
                // 计算两点间距离
                double dx = toPos.x - fromPos.x;
                double dy = toPos.y - fromPos.y;
                double visualDistance = Math.sqrt(dx * dx + dy * dy);
                
                // 只有当可视距离大于阈值且这条道路不是主要路线时才显示距离标签
                /*
                if (visualDistance > DISTANCE_LABEL_THRESHOLD && !routeCityPairs.contains(cityPair)) {
                    // 绘制距离标签
                    double midX = (fromPos.x + toPos.x) / 2;
                    double midY = (fromPos.y + toPos.y) / 2;
                    
                    // 计算标签偏移以避免与道路重叠
                    double offsetX = -dy * 0.1;
                    double offsetY = dx * 0.1;
                    
                    String distanceLabel = String.format("%.0f", road.getDistance());
                    
                    // 标签背景
                    double labelWidth = distanceLabel.length() * 8 + 10;
                    double labelHeight = 18;
                    
                    gc.setFill(Color.WHITE.deriveColor(0, 1, 1, 0.85));
                    gc.fillRoundRect(midX - labelWidth/2 + offsetX, midY - labelHeight/2 + offsetY, 
                                   labelWidth, labelHeight, 8, 8);
                    
                    gc.setFill(ROAD_COLOR.darker());
                    gc.setFont(Font.font("Arial", FontWeight.NORMAL, 11));
                    gc.setTextAlign(TextAlignment.CENTER);
                    gc.fillText(distanceLabel, midX + offsetX, midY + 4 + offsetY);
                }
                */
            }
        }
    }
    
    private void drawCities(GraphicsContext gc) {
        gc.setFont(Font.font("Arial", FontWeight.BOLD, 14)); // Default font for city name part
        gc.setTextAlign(TextAlignment.LEFT);
        
        // 添加阴影效果
        DropShadow shadow = new DropShadow();
        shadow.setRadius(5.0);
        shadow.setOffsetX(2.0);
        shadow.setOffsetY(2.0);
        shadow.setColor(Color.color(0, 0, 0, 0.5));
        
        // 计算城市标签的位置，避免重叠 (这部分可能需要调整，因为标签大小变了)
        Map<String, Point> labelPositions = calculateLabelPositions(); // This might need adjustment if label sizes change significantly due to multi-line text.
        
        for (Map.Entry<String, Point> entry : cityPositions.entrySet()) {
            String cityName = entry.getKey();
            Point pos = entry.getValue(); // This is the city circle's center
            
            // 外圈白色光晕
            gc.setFill(Color.WHITE);
            gc.fillOval(pos.x - CITY_RADIUS - 3, pos.y - CITY_RADIUS - 3, 
                     (CITY_RADIUS + 3) * 2, (CITY_RADIUS + 3) * 2);
            
            // 城市填充
            gc.setEffect(shadow);
            gc.setFill(CITY_COLOR);
            gc.fillOval(pos.x - CITY_RADIUS, pos.y - CITY_RADIUS, 
                      CITY_RADIUS * 2, CITY_RADIUS * 2);
            gc.setEffect(null);
            
            // 城市边框
            gc.setStroke(Color.WHITE);
            gc.setLineWidth(2.0);
            gc.strokeOval(pos.x - CITY_RADIUS, pos.y - CITY_RADIUS,
                       CITY_RADIUS * 2, CITY_RADIUS * 2);
            
            // 构建城市和景点信息文本
            StringBuilder labelTextBuilder = new StringBuilder(cityName);
            List<String> attractionNameList = new ArrayList<>();
            if (roadNetwork != null) {
                Set<Attraction> attractionsInCity = roadNetwork.getAttractionsInCity(cityName);
                if (attractionsInCity != null && !attractionsInCity.isEmpty()) {
                    // labelTextBuilder.append("\n").append(LanguageManager.getText("attractionsLabelPrefix")); // Optional: "Attractions:" prefix
                    for (Attraction attraction : attractionsInCity) {
                        String name = attraction.getAttractionName();
                        // Optionally shorten attraction names if they are too long for the label
                        // if (name.length() > 15) name = name.substring(0, 13) + "..";
                        labelTextBuilder.append("\n- ").append(name);
                        attractionNameList.add("- " + name);
                    }
                }
            }
            String fullLabelText = labelTextBuilder.toString();
            
            // 城市名称与景点标签
            Point labelCenterPos = labelPositions.get(cityName); // Get pre-calculated label position
            if (labelCenterPos != null) {
                Font cityNameFont = Font.font("Arial", FontWeight.BOLD, 13); // Font for city name
                Font attractionFont = Font.font("Arial", FontWeight.NORMAL, 11); // Font for attractions
                
                // Estimate text width and height for drawing the background box
                // This needs to be more robust for multi-line text.
                // We'll sum heights and take max width.
                double totalTextHeight = cityNameFont.getSize() + 2; // Padding for city name line
                double maxTextWidth = estimateTextWidth(cityName, cityNameFont) + 10; // Padding

                for (String attractionLine : attractionNameList) {
                    totalTextHeight += attractionFont.getSize() + 1; // Line height + spacing for attractions
                    maxTextWidth = Math.max(maxTextWidth, estimateTextWidth(attractionLine, attractionFont) + 10);
                }
                totalTextHeight += 8; // Overall padding for the box

                double boxWidth = Math.max(40, maxTextWidth); 
                double boxHeight = totalTextHeight;
                
                // 标签背景带阴影
                gc.setEffect(new DropShadow(3, 1, 1, Color.color(0, 0, 0, 0.3)));
                gc.setFill(Color.WHITE);
                // Adjust labelCenterPos to be top-left of the box for easier multi-line drawing
                double boxX = labelCenterPos.x - boxWidth / 2;
                double boxY = labelCenterPos.y - boxHeight / 2;
                gc.fillRoundRect(boxX, boxY, boxWidth, boxHeight, 6, 6);
                gc.setEffect(null);
                
                // Draw text line by line
                gc.setTextAlign(TextAlignment.CENTER); 
                gc.setTextBaseline(javafx.geometry.VPos.TOP); // Align to top for multi-line

                double currentY = boxY + 5; // Start Y for text drawing, with some top padding

                // Draw City Name
                gc.setFill(TEXT_COLOR); // City name color
                gc.setFont(cityNameFont);
                gc.fillText(cityName, labelCenterPos.x, currentY);
                currentY += cityNameFont.getSize() + 2; // Move to next line

                // Draw Attractions
                gc.setFont(attractionFont);
                // gc.setFill(ATTRACTION_COLOR.darker()); // Example: Different color for attractions
                for (String attractionLine : attractionNameList) {
                    gc.fillText(attractionLine, labelCenterPos.x, currentY);
                    currentY += attractionFont.getSize() + 1; // Move to next line
            }
        }
        }
    }

    // Helper method to estimate text width (JavaFX doesn't have a direct GraphicsContext.measureText)
    private double estimateTextWidth(String text, Font font) {
        // This is a heuristic. For precise measurement, you'd use a javafx.scene.text.Text node:
        // Text textNode = new Text(text); textNode.setFont(font); return textNode.getLayoutBounds().getWidth();
        if (text == null || text.isEmpty()) return 0;
        // A common approximation: average char width is around 0.6 to 0.7 times font size.
        return text.length() * font.getSize() * 0.65; 
    }
    
    /**
     * 计算城市标签的位置，避免重叠
     */
    private Map<String, Point> calculateLabelPositions() {
        Map<String, Point> labelCenterPositions = new HashMap<>();
        if (canvas == null || cityPositions == null) return labelCenterPositions;
                
        double canvasCenterX = canvas.getWidth() / 2.0;
        double canvasCenterY = canvas.getHeight() / 2.0;
        
        Font labelFont = Font.font("Arial", FontWeight.NORMAL, 12);

        for (Map.Entry<String, Point> entry : cityPositions.entrySet()) {
            String cityName = entry.getKey();
            Point cityPos = entry.getValue();
            
            double vecX = cityPos.x - canvasCenterX;
            double vecY = cityPos.y - canvasCenterY;
            double mag = Math.sqrt(vecX * vecX + vecY * vecY);

            double normX = (mag == 0) ? 0 : vecX / mag; 
            double normY = (mag == 0) ? -1 : vecY / mag; // Default to upward if at center

            // Estimate text width for offsetting label center
            // This is a heuristic. For more accuracy, JavaFX Text node bounds could be used.
            double estimatedTextWidth = cityName.length() * (labelFont.getSize() * 0.65) + 10; // Adjust multiplier as needed
            double estimatedTextHeight = labelFont.getSize() + 10;

            // Offset from city edge to the *center* of the label box
            // The offset should be from city center to label center
            double offsetMagnitude = CITY_RADIUS + 15 + Math.max(estimatedTextWidth / 2, estimatedTextHeight / 2);

            double labelCenterX = cityPos.x + normX * offsetMagnitude;
            double labelCenterY = cityPos.y + normY * offsetMagnitude;
            
            labelCenterPositions.put(cityName, new Point(labelCenterX, labelCenterY));
        }
        return labelCenterPositions;
    }
    
    private void drawAttractions(GraphicsContext gc) {
        gc.setFont(Font.font("Arial", FontWeight.NORMAL, 12));
        
        // 阴影效果
        DropShadow shadow = new DropShadow();
        shadow.setRadius(3.0);
        shadow.setOffsetX(1.0);
        shadow.setOffsetY(1.0);
        shadow.setColor(Color.color(0, 0, 0, 0.4));
        
        // 如果正在显示一个路线，就不显示景点标签，只显示图标
        boolean showLabels = currentPlan == null || currentPlan.getFullPath() == null || currentPlan.getFullPath().isEmpty();
        
        for (Map.Entry<String, Point> entry : attractionPositions.entrySet()) {
            String attractionName = entry.getKey();
            Point pos = entry.getValue();
            
            // 绘制景点图标
            gc.setEffect(shadow);
            gc.setFill(ATTRACTION_COLOR);
            gc.fillOval(pos.x - ATTRACTION_RADIUS, pos.y - ATTRACTION_RADIUS, 
                       ATTRACTION_RADIUS * 2, ATTRACTION_RADIUS * 2);
            gc.setEffect(null);
            
            gc.setStroke(Color.WHITE);
            gc.setLineWidth(1.5);
            gc.strokeOval(pos.x - ATTRACTION_RADIUS, pos.y - ATTRACTION_RADIUS, 
                         ATTRACTION_RADIUS * 2, ATTRACTION_RADIUS * 2);
            
            // 只有未显示路线时才显示景点标签
            if (showLabels) {
                // 绘制景点名称
                String shortName = attractionName;
                if (attractionName.length() > 12) {
                    shortName = attractionName.substring(0, 10) + "...";
                }
                
                // 标签背景
                double textWidth = shortName.length() * 7 + 6;
                double textHeight = 16;
                
                gc.setEffect(new DropShadow(2, 1, 1, Color.color(0, 0, 0, 0.3)));
                gc.setFill(Color.WHITE);
                gc.fillRoundRect(pos.x + ATTRACTION_RADIUS + 2, pos.y - 8, textWidth, textHeight, 4, 4);
                gc.setEffect(null);
                
                gc.setFill(ATTRACTION_COLOR.darker());
                gc.fillText(shortName, pos.x + ATTRACTION_RADIUS + 5, pos.y + 5);
            }
        }
    }
    
    private void drawRoutes(GraphicsContext gc) {
        if (currentPlan == null) {
            return;
        }
        
        // 检查是否有详细路径信息
        if (currentPlan.getDetailedSegments() == null || currentPlan.getDetailedSegments().isEmpty()) {
            return;
        }
        
        // 添加发光效果
        Glow glow = new Glow();
        glow.setLevel(0.7);
        gc.setEffect(glow);
        
        // 设置路线样式
        gc.setStroke(ROUTE_COLOR);
        gc.setLineWidth(ROUTE_WIDTH);
        gc.setLineCap(StrokeLineCap.ROUND);
        gc.setLineJoin(StrokeLineJoin.ROUND);
        
        // 清除效果绘制特殊点
        gc.setEffect(null);
        
        // 获取完整路径以确定起点和终点
        List<City> fullPath = currentPlan.getFullPath();
        String startCityName = null;
        String endCityName = null;
        
        if (fullPath != null && !fullPath.isEmpty()) {
            startCityName = fullPath.get(0).getName();
            endCityName = fullPath.get(fullPath.size() - 1).getName();
        }
        
        // 先画一条带白色边缘的路线作为底色
        gc.setStroke(Color.WHITE);
        gc.setLineWidth(ROUTE_WIDTH + 3);
        for (PathfindingService.PathResult pathResult : currentPlan.getDetailedSegments()) {
            List<City> cities = pathResult.getPath();
            for (int i = 0; i < cities.size() - 1; i++) {
                String fromCityName = cities.get(i).getName();
                String toCityName = cities.get(i + 1).getName();
                
                Point from = cityPositions.get(fromCityName);
                Point to = cityPositions.get(toCityName);
                
                if (from != null && to != null) {
                    gc.strokeLine(from.x, from.y, to.x, to.y);
                }
            }
        }
        
        // 再绘制主要路线
        gc.setEffect(glow);
        gc.setStroke(ROUTE_COLOR);
        gc.setLineWidth(ROUTE_WIDTH);
        
        // 画主路线
        for (PathfindingService.PathResult pathResult : currentPlan.getDetailedSegments()) {
            List<City> cities = pathResult.getPath();
            for (int i = 0; i < cities.size() - 1; i++) {
                String fromCityName = cities.get(i).getName();
                String toCityName = cities.get(i + 1).getName();
                
                Point from = cityPositions.get(fromCityName);
                Point to = cityPositions.get(toCityName);
                
                if (from != null && to != null) {
                    // 使用渐变色更直观地表示路径方向
                    LinearGradient gradient = new LinearGradient(
                        from.x, from.y, to.x, to.y,
                        false, CycleMethod.NO_CYCLE,
                        new Stop(0, ROUTE_COLOR),
                        new Stop(1, ROUTE_COLOR.deriveColor(0, 1.2, 0.8, 1))
                    );
                    gc.setStroke(gradient);
                    gc.strokeLine(from.x, from.y, to.x, to.y);
                    
                    // 计算两点间距离
                    double dx = to.x - from.x;
                    double dy = to.y - from.y;
                    double visualDistance = Math.sqrt(dx * dx + dy * dy);
                    
                    // 只有当距离足够大时才绘制箭头
                    if (visualDistance > 80) {
                        // 绘制箭头
                        drawArrow(gc, from, to);
                        
                        // 在路线上显示距离，只显示主要路线距离
                        /*
                        double midX = (from.x + to.x) / 2;
                        double midY = (from.y + to.y) / 2;
                        
                        // 计算垂直于线段的偏移方向
                        double normX = -dy / visualDistance;
                        double normY = dx / visualDistance;
                        double offsetDistance = 15;
                        
                        // 查找道路并获取距离
                        double distance = 0;
                        for (Road road : roadNetwork.getRoadsFrom(cities.get(i))) {
                            if (road.getDestination().equals(cities.get(i+1))) {
                                distance = road.getDistance();
                                break;
                            }
                        }
                        
                        if (distance > 0) {
                            String distanceLabel = String.format("%.0f", distance);
                            
                            // 绘制距离标签，带白色背景
                            double labelWidth = distanceLabel.length() * 8 + 10;
                            double labelHeight = 18;
                            
                            gc.setEffect(null); // 临时移除发光效果
                            
                            // 背景稍微偏移，不要正好在线上
                            gc.setFill(Color.WHITE);
                            gc.fillRoundRect(
                                midX - labelWidth/2 + normX * offsetDistance, 
                                midY - labelHeight/2 + normY * offsetDistance, 
                                labelWidth, labelHeight, 8, 8
                            );
                            
                            // 文字
                            gc.setFill(ROUTE_COLOR);
                            gc.setFont(Font.font("Arial", FontWeight.BOLD, 12));
                            gc.setTextAlign(TextAlignment.CENTER);
                            gc.fillText(
                                distanceLabel, 
                                midX + normX * offsetDistance, 
                                midY + 4 + normY * offsetDistance
                            );
                            
                            gc.setEffect(glow); // 恢复发光效果
                        }
                        */
                    }
                }
            }
        }
        
        gc.setEffect(null);
        
        // 绘制起点和终点
        Point startPos = null;
        Point endPos = null;
        
        if (startCityName != null) {
            startPos = cityPositions.get(startCityName);
        }
        
        if (endCityName != null) {
            endPos = cityPositions.get(endCityName);
        }
        
        // 绘制起点（绿色）
        if (startPos != null) {
            DropShadow pointShadow = new DropShadow();
            pointShadow.setRadius(8.0);
            pointShadow.setColor(START_CITY_COLOR.deriveColor(0, 1, 1, 0.6));
            gc.setEffect(pointShadow);
            
            gc.setFill(START_CITY_COLOR);
            gc.fillOval(startPos.x - CITY_RADIUS - 5, startPos.y - CITY_RADIUS - 5, 
                      (CITY_RADIUS + 5) * 2, (CITY_RADIUS + 5) * 2);
            
            gc.setEffect(null);
            
            gc.setStroke(Color.WHITE);
            gc.setLineWidth(3.0);
            gc.strokeOval(startPos.x - CITY_RADIUS - 5, startPos.y - CITY_RADIUS - 5, 
                        (CITY_RADIUS + 5) * 2, (CITY_RADIUS + 5) * 2);
            
            // 起点标签
            gc.setFill(START_CITY_COLOR);
            gc.setFont(Font.font("Arial", FontWeight.BOLD, 18));
            String startLabel = LanguageManager.getText("startPoint");
            
            // 绘制白色背景
            double textWidth = startLabel.length() * 12;
            double textHeight = 25;
            
            gc.setEffect(new DropShadow(5, 2, 2, Color.color(0, 0, 0, 0.3)));
            gc.setFill(Color.WHITE);
            gc.fillRoundRect(startPos.x - textWidth/2, startPos.y - CITY_RADIUS - 35, textWidth, textHeight, 10, 10);
            gc.setEffect(null);
            
            gc.setFill(START_CITY_COLOR);
            gc.setTextAlign(TextAlignment.CENTER);
            gc.fillText(startLabel, startPos.x, startPos.y - CITY_RADIUS - 15);
        }
        
        // 绘制终点（红色）
        if (endPos != null) {
            DropShadow pointShadow = new DropShadow();
            pointShadow.setRadius(8.0);
            pointShadow.setColor(END_CITY_COLOR.deriveColor(0, 1, 1, 0.6));
            gc.setEffect(pointShadow);
            
            gc.setFill(END_CITY_COLOR);
            gc.fillOval(endPos.x - CITY_RADIUS - 5, endPos.y - CITY_RADIUS - 5, 
                      (CITY_RADIUS + 5) * 2, (CITY_RADIUS + 5) * 2);
            
            gc.setEffect(null);
            
            gc.setStroke(Color.WHITE);
            gc.setLineWidth(3.0);
            gc.strokeOval(endPos.x - CITY_RADIUS - 5, endPos.y - CITY_RADIUS - 5, 
                        (CITY_RADIUS + 5) * 2, (CITY_RADIUS + 5) * 2);
            
            // 终点标签
            gc.setFill(END_CITY_COLOR);
            gc.setFont(Font.font("Arial", FontWeight.BOLD, 18));
            String endLabel = LanguageManager.getText("endPoint");
            
            // 绘制白色背景
            double textWidth = endLabel.length() * 12;
            double textHeight = 25;
            
            gc.setEffect(new DropShadow(5, 2, 2, Color.color(0, 0, 0, 0.3)));
            gc.setFill(Color.WHITE);
            gc.fillRoundRect(endPos.x - textWidth/2, endPos.y - CITY_RADIUS - 35, textWidth, textHeight, 10, 10);
            gc.setEffect(null);
            
            gc.setFill(END_CITY_COLOR);
            gc.setTextAlign(TextAlignment.CENTER);
            gc.fillText(endLabel, endPos.x, endPos.y - CITY_RADIUS - 15);
        }
        
        // 绘制中间城市
        System.out.println("[MapView.drawRoutes] Drawing waypoints. Full path size: " + (fullPath != null ? fullPath.size() : "null"));
        if (fullPath != null) {
            for (City city : fullPath) {
                System.out.println("[MapView.drawRoutes] Path City: " + city.getName());
            }

            for (int i = 1; i < fullPath.size() - 1; i++) { // Loop from 1 to size-2 to exclude start/end
                String cityName = fullPath.get(i).getName();
                Point pos = cityPositions.get(cityName);
                System.out.println("[MapView.drawRoutes] Waypoint " + i + ": " + cityName + ", Position: " + (pos != null ? pos.x+","+pos.y : "null"));
                
                if (pos != null) {
                    // 绘制中间城市（橙色）
                    gc.setFill(WAYPOINT_COLOR);
                    gc.fillOval(pos.x - CITY_RADIUS - 3, pos.y - CITY_RADIUS - 3, 
                              (CITY_RADIUS + 3) * 2, (CITY_RADIUS + 3) * 2);
                    
                    gc.setStroke(Color.WHITE);
                    gc.setLineWidth(2.0);
                    gc.strokeOval(pos.x - CITY_RADIUS - 3, pos.y - CITY_RADIUS - 3, 
                                (CITY_RADIUS + 3) * 2, (CITY_RADIUS + 3) * 2);
                    
                    // 城市序号 - 修改为在圆点上方显示序号
                    String waypointNumber = String.valueOf(i);
                    gc.setFont(Font.font("Arial", FontWeight.BOLD, 16));
                    double textWidth = waypointNumber.length() * 12;
                    double textHeight = 25;
                    
                    // 绘制白色背景框
                    gc.setEffect(new DropShadow(4, 1, 1, Color.color(0, 0, 0, 0.3)));
                    gc.setFill(Color.WHITE);
                    gc.fillRoundRect(pos.x - textWidth/2, pos.y - CITY_RADIUS - WAYPOINT_LABEL_OFFSET, textWidth, textHeight, 8, 8);
                    gc.setEffect(null);
                    
                    // 绘制序号文本
                    gc.setFill(WAYPOINT_COLOR);
                    gc.setTextAlign(TextAlignment.CENTER);
                    gc.fillText(waypointNumber, pos.x, pos.y - CITY_RADIUS - WAYPOINT_LABEL_OFFSET + 18);
                    
                    System.out.println("[MapView.drawRoutes] Drew number " + i + " for " + cityName + " at " + pos.x + "," + (pos.y - CITY_RADIUS - WAYPOINT_LABEL_OFFSET + 18));
                } else {
                    System.out.println("[MapView.drawRoutes] Position for waypoint " + cityName + " is null. Cannot draw number.");
                }
            }
        } else {
            System.out.println("[MapView.drawRoutes] fullPath is null. Cannot draw waypoints.");
        }
        
        // 清除效果
        gc.setEffect(null);
    }
    
    private void drawArrow(GraphicsContext gc, Point from, Point to) {
        // 计算方向
        double dx = to.x - from.x;
        double dy = to.y - from.y;
        double length = Math.sqrt(dx * dx + dy * dy);
        
        if (length < 60) return; // 距离太短不绘制箭头
        
        // 正规化
        double udx = dx / length;
        double udy = dy / length;
        
        // 箭头位于线段75%处
        double arrowPosX = from.x + dx * 0.75;
        double arrowPosY = from.y + dy * 0.75;
        
        // 计算箭头两翼的位置
        double perpX = -udy;
        double perpY = udx;
        
        double leftX = arrowPosX - ARROW_LENGTH * udx + ARROW_WIDTH * perpX;
        double leftY = arrowPosY - ARROW_LENGTH * udy + ARROW_WIDTH * perpY;
        
        double rightX = arrowPosX - ARROW_LENGTH * udx - ARROW_WIDTH * perpX;
        double rightY = arrowPosY - ARROW_LENGTH * udy - ARROW_WIDTH * perpY;
        
        // 添加阴影效果
        DropShadow arrowShadow = new DropShadow();
        arrowShadow.setRadius(5.0);
        arrowShadow.setColor(Color.color(0, 0, 0, 0.4));
        gc.setEffect(arrowShadow);
        
        // 绘制箭头
        gc.setFill(ROUTE_HIGHLIGHT_COLOR);
        gc.fillPolygon(
            new double[] {arrowPosX, leftX, rightX},
            new double[] {arrowPosY, leftY, rightY},
            3
        );
        
        gc.setEffect(null);
        
        // 添加白色边框使箭头更醒目
        gc.setStroke(Color.WHITE);
        gc.setLineWidth(1.5);
        gc.strokePolygon(
            new double[] {arrowPosX, leftX, rightX},
            new double[] {arrowPosY, leftY, rightY},
            3
        );
    }
} 