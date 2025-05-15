package com.cpt204.finalproject.ui;

import com.cpt204.finalproject.dataloader.CsvDataLoader;
import com.cpt204.finalproject.dto.TripPlan;
import com.cpt204.finalproject.model.RoadNetwork;
import com.cpt204.finalproject.model.City;
import com.cpt204.finalproject.model.Attraction;
import com.cpt204.finalproject.services.*;
import com.cpt204.finalproject.services.DenseDijkstraService;

import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class TripPlannerApp extends Application {

    private RoadNetwork roadNetwork;
    private TripPlanningService tripPlanningService;
    private FilteredList<String> filteredAttractions;
    private ObservableList<String> allAttractions;
    private ObservableList<String> selectedAttractions;
    private TextArea resultTextArea;
    private MapView mapView;
    private boolean isEnglish = false;
    
    // UI组件引用
    private ComboBox<String> startCityComboBox;
    private ComboBox<String> endCityComboBox;
    private Button calculateButton;
    private Label titleLabel;
    private Button langButton;

    // Define colors (matching MapView for consistency)
    private static final Color CITY_COLOR = Color.web("#3498db");
    private static final Color START_CITY_COLOR = Color.web("#2ecc71");
    private static final Color END_CITY_COLOR = Color.web("#e74c3c");
    private static final Color WAYPOINT_COLOR = Color.web("#f39c12");
    private static final Color ROAD_COLOR = Color.web("#c0c0c0");
    private static final Color ROUTE_COLOR = Color.web("#9b59b6");
    private static final Color ATTRACTION_COLOR = Color.web("#e67e22");
    private static final Color TEXT_COLOR = Color.web("#2c3e50");

    @Override
    public void start(Stage primaryStage) {
        // 设置窗口标题
        primaryStage.setTitle("Road Trip Planner | 道路旅行规划器");

        // 初始化数据和服务
        initializeServices();

        // 创建UI布局
        BorderPane root = new BorderPane();
        
        // 创建顶部标题栏
        HBox titleBar = createTitleBar();
        root.setTop(titleBar);
        
        // 创建左右分割的主内容区域
        SplitPane splitPane = new SplitPane();
        
        // 创建左侧输入面板
        VBox leftPanel = createLeftPanel();
        
        // 创建右侧结果面板 (MapView is inside this)
        VBox rightMapAndResultsPanel = createRightPanel(); // This VBox contains results and map
        
        splitPane.getItems().addAll(leftPanel, rightMapAndResultsPanel);
        splitPane.setDividerPositions(0.35); // Adjusted divider
        
        // Create a new HBox to hold the splitPane and the legend
        HBox mainContentArea = new HBox();
        VBox legendPane = createLegendPane();
        
        // Ensure the splitPane takes up most of the space
        HBox.setHgrow(splitPane, Priority.ALWAYS);
        
        mainContentArea.getChildren().addAll(splitPane, legendPane);
        root.setCenter(mainContentArea);

        // 创建场景
        Scene scene = new Scene(root, 1280, 820); // Slightly increased size
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private HBox createTitleBar() {
        HBox titleBar = new HBox();
        titleBar.setAlignment(Pos.CENTER_LEFT);
        titleBar.setPadding(new Insets(10));
        titleBar.setStyle("-fx-background-color: #2196F3;");
        
        titleLabel = new Label(LanguageManager.getText("appTitle"));
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 24));
        titleLabel.setTextFill(Color.WHITE);
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        langButton = new Button(LanguageManager.getText("languageSwitch"));
        langButton.setStyle("-fx-background-color: white; -fx-text-fill: #2196F3;");
        langButton.setOnAction(e -> toggleLanguage(titleLabel, langButton));
        
        titleBar.getChildren().addAll(titleLabel, spacer, langButton);
        
        return titleBar;
    }
    
    private void toggleLanguage(Label titleLabel, Button langButton) {
        LanguageManager.toggleLanguage();
        updateUILanguage(titleLabel, langButton);
    }
    
    private void updateUILanguage(Label titleLabel, Button langButton) {
        // 更新标题栏
        titleLabel.setText(LanguageManager.getText("appTitle"));
        langButton.setText(LanguageManager.getText("languageSwitch"));
        
        // 更新所有UI元素的文本
        updateControlsLanguage();
        
        // 更新地图
        if (mapView != null) {
            mapView.redraw();
        }
        
        // 更新图例文本
        BorderPane root = (BorderPane) getScene().getRoot();
        HBox mainContentArea = (HBox) root.getCenter();
        if (mainContentArea != null && mainContentArea.getChildren().size() > 1) {
            VBox legendPane = (VBox) mainContentArea.getChildren().get(1);
            if (legendPane != null && !legendPane.getChildren().isEmpty()) {
                // 更新图例标题
                if (legendPane.getChildren().get(0) instanceof Label) {
                    Label legendTitle = (Label) legendPane.getChildren().get(0);
                    legendTitle.setText(LanguageManager.isChineseLanguage() ? "图例" : "Legend");
                }
                
                // 更新图例项文本
                for (int i = 1; i < legendPane.getChildren().size(); i++) {
                    if (legendPane.getChildren().get(i) instanceof HBox) {
                        HBox item = (HBox) legendPane.getChildren().get(i);
                        if (item.getChildren().size() > 1 && item.getChildren().get(1) instanceof Label) {
                            Label itemLabel = (Label) item.getChildren().get(1);
                            String currentText = itemLabel.getText();
                            
                            // 基于当前文本更新图例项标签
                            if (currentText.equals("城市") || currentText.equals("City")) {
                                itemLabel.setText(LanguageManager.isChineseLanguage() ? "城市" : "City");
                            } else if (currentText.equals("起点") || currentText.equals("Start Point")) {
                                itemLabel.setText(LanguageManager.getText("startPoint"));
                            } else if (currentText.equals("终点") || currentText.equals("End Point")) {
                                itemLabel.setText(LanguageManager.getText("endPoint"));
                            } else if (currentText.equals("途经点") || currentText.equals("Waypoint")) {
                                itemLabel.setText(LanguageManager.isChineseLanguage() ? "途经点" : "Waypoint");
                            } else if (currentText.equals("景点") || currentText.equals("Attraction")) {
                                itemLabel.setText(LanguageManager.isChineseLanguage() ? "景点" : "Attraction");
                            } else if (currentText.equals("道路") || currentText.equals("Road")) {
                                itemLabel.setText(LanguageManager.isChineseLanguage() ? "道路" : "Road");
                            } else if (currentText.equals("规划路线") || currentText.equals("Planned Route")) {
                                itemLabel.setText(LanguageManager.isChineseLanguage() ? "规划路线" : "Planned Route");
                            }
                        }
                    }
                }
            }
        }
    }
    
    private void updateControlsLanguage() {
        // 遍历所有TitledPane并更新标题
        updateTitledPaneLabels();
        
        // 更新按钮文本
        updateButtonLabels();
        
        // 更新其他标签和提示文本
        updateLabelsAndPrompts();
    }
    
    private void updateTitledPaneLabels() {
        BorderPane root = (BorderPane) getScene().getRoot();
        HBox mainContentArea = (HBox) root.getCenter();
        if (mainContentArea != null && mainContentArea.getChildren().size() > 0) {
            // 获取 SplitPane
            javafx.scene.Node splitPaneNode = mainContentArea.getChildren().get(0);
            if (splitPaneNode instanceof SplitPane) {
                SplitPane splitPane = (SplitPane) splitPaneNode;
                // 遍历SplitPane中的每个面板
                for (javafx.scene.Node node : splitPane.getItems()) {
            if (node instanceof VBox) {
                for (javafx.scene.Node child : ((VBox)node).getChildren()) {
                    if (child instanceof TitledPane) {
                        TitledPane pane = (TitledPane)child;
                        if (pane.getText().equals("城市选择") || pane.getText().equals("City Selection")) {
                            pane.setText(LanguageManager.getText("citySelection"));
                        } else if (pane.getText().equals("景点选择") || pane.getText().equals("Attraction Selection")) {
                            pane.setText(LanguageManager.getText("attractionSelection"));
                        } else if (pane.getText().equals("计算结果") || pane.getText().equals("Results")) {
                            pane.setText(LanguageManager.getText("results"));
                        } else if (pane.getText().equals("路线地图") || pane.getText().equals("Route Map")) {
                            pane.setText(LanguageManager.getText("routeMap"));
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    
    private void updateButtonLabels() {
        // 更新主计算按钮
        if (calculateButton != null) {
            calculateButton.setText(LanguageManager.getText("calculateRoute"));
        }
        
        // 遍历SplitPane中的所有按钮
        BorderPane root = (BorderPane) getScene().getRoot();
        HBox mainContentArea = (HBox) root.getCenter();
        if (mainContentArea != null && mainContentArea.getChildren().size() > 0) {
            javafx.scene.Node splitPaneNode = mainContentArea.getChildren().get(0);
            if (splitPaneNode instanceof SplitPane) {
                SplitPane splitPane = (SplitPane) splitPaneNode;
                for (javafx.scene.Node node : splitPane.getItems()) {
            if (node instanceof VBox) {
                        updateButtonsInContainer((VBox)node);
                    }
                }
            }
        }
    }
    
    private void updateButtonsInContainer(Pane container) {
        for (javafx.scene.Node node : container.getChildren()) {
            if (node instanceof Button) {
                Button button = (Button)node;
                if (button.getText().equals("添加 >") || button.getText().equals("Add >")) {
                    button.setText(LanguageManager.getText("add"));
                } else if (button.getText().equals("< 删除") || button.getText().equals("< Remove")) {
                    button.setText(LanguageManager.getText("remove"));
                }
            } else if (node instanceof HBox) {
                updateButtonsInContainer((Pane)node);
            } else if (node instanceof VBox) {
                updateButtonsInContainer((Pane)node);
            }
        }
    }
    
    private void updateLabelsAndPrompts() {
        // 遍历所有Label和带有提示文本的控件
        for (javafx.scene.Node node : ((SplitPane)((BorderPane)getScene().getRoot()).getCenter()).getItems()) {
            if (node instanceof VBox) {
                for (javafx.scene.Node child : ((VBox)node).getChildren()) {
                    if (child instanceof TitledPane) {
                        TitledPane pane = (TitledPane)child;
                        VBox content = (VBox)pane.getContent();
                        updateLabelsInContainer(content);
                    }
                }
            }
        }
    }
    
    private void updateLabelsInContainer(Pane container) {
        for (javafx.scene.Node node : container.getChildren()) {
            if (node instanceof Label) {
                Label label = (Label)node;
                String text = label.getText();
                if (text.endsWith(":")) {
                    if (text.equals("起始城市:") || text.equals("Start City:")) {
                        label.setText(LanguageManager.getText("startCity"));
                    } else if (text.equals("目的地城市:") || text.equals("Destination City:")) {
                        label.setText(LanguageManager.getText("endCity"));
                    } else if (text.equals("搜索城市:") || text.equals("Search City:")) {
                        label.setText(LanguageManager.getText("searchCity"));
                    } else if (text.equals("搜索景点:") || text.equals("Search Attraction:")) {
                        label.setText(LanguageManager.getText("searchAttraction"));
                    } else if (text.equals("可选景点:") || text.equals("Available Attractions:")) {
                        label.setText(LanguageManager.getText("availableAttractions"));
                    } else if (text.equals("已选景点:") || text.equals("Selected Attractions:")) {
                        label.setText(LanguageManager.getText("selectedAttractions"));
                    }
                }
            } else if (node instanceof TextField) {
                TextField textField = (TextField)node;
                String promptText = textField.getPromptText();
                if (promptText.equals("输入城市名或州/省缩写") || promptText.equals("Enter city name or state")) {
                    textField.setPromptText(LanguageManager.getText("cityPrompt"));
                } else if (promptText.equals("输入景点名称") || promptText.equals("Enter attraction name")) {
                    textField.setPromptText(LanguageManager.getText("attractionPrompt"));
                }
            } else if (node instanceof ComboBox) {
                ComboBox<?> comboBox = (ComboBox<?>)node;
                String promptText = comboBox.getPromptText();
                if (promptText.equals("选择起始城市") || promptText.equals("Select starting city")) {
                    comboBox.setPromptText(LanguageManager.getText("selectStartCity"));
                } else if (promptText.equals("选择目的地城市") || promptText.equals("Select destination city")) {
                    comboBox.setPromptText(LanguageManager.getText("selectEndCity"));
                }
            } else if (node instanceof HBox) {
                updateLabelsInContainer((Pane)node);
            } else if (node instanceof VBox) {
                updateLabelsInContainer((Pane)node);
            }
        }
    }

    private VBox createLeftPanel() {
        VBox panel = new VBox(20);
        panel.setPadding(new Insets(20));
        
        // 城市选择部分
        TitledPane cityPane = createCitySelectionPane();
        
        // 景点选择部分
        TitledPane attractionPane = createAttractionSelectionPane();
        
        // 计算按钮
        calculateButton = new Button("计算最佳路线");
        calculateButton.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; -fx-font-size: 14px;");
        calculateButton.setPrefHeight(40);
        calculateButton.setMaxWidth(Double.MAX_VALUE);
        calculateButton.setOnAction(e -> calculateRoute());
        
        panel.getChildren().addAll(cityPane, attractionPane, calculateButton);
        
        return panel;
    }
    
    private TitledPane createCitySelectionPane() {
        VBox content = new VBox(10);
        content.setPadding(new Insets(10));
        
        Label searchLabel = new Label(LanguageManager.getText("searchCity"));
        TextField citySearchField = new TextField();
        citySearchField.setPromptText(LanguageManager.getText("cityPrompt"));
        
        Label startCityLabel = new Label(LanguageManager.getText("startCity"));
        startCityComboBox = new ComboBox<>();
        startCityComboBox.setMaxWidth(Double.MAX_VALUE);
        startCityComboBox.setPromptText(LanguageManager.getText("selectStartCity"));
        
        Label endCityLabel = new Label(LanguageManager.getText("endCity"));
        endCityComboBox = new ComboBox<>();
        endCityComboBox.setMaxWidth(Double.MAX_VALUE);
        endCityComboBox.setPromptText(LanguageManager.getText("selectEndCity"));
        
        if (roadNetwork != null && roadNetwork.getAllCities() != null) {
            List<String> allCityNamesList = new ArrayList<>();
            for (City city : roadNetwork.getAllCities()) {
                if (city != null && city.getName() != null) {
                    allCityNamesList.add(city.getName());
                }
            }
            // Master list of all cities
            ObservableList<String> masterCityList = FXCollections.observableArrayList(allCityNamesList);

            // Separate lists for each ComboBox to allow independent filtering and item management
            ObservableList<String> startCityItems = FXCollections.observableArrayList(allCityNamesList);
            ObservableList<String> endCityItems = FXCollections.observableArrayList(allCityNamesList);

            startCityComboBox.setItems(startCityItems);
            endCityComboBox.setItems(endCityItems);
            
            // Listener for city search field
            citySearchField.textProperty().addListener((observable, oldValue, newValue) -> {
                String filter = (newValue == null) ? "" : newValue.toLowerCase().trim();
                String selectedStart = startCityComboBox.getValue();
                String selectedEnd = endCityComboBox.getValue();

                // Update startCityItems
                List<String> newStartItems = masterCityList.stream()
                        .filter(city -> city.toLowerCase().contains(filter))
                        .collect(Collectors.toList());
                if (selectedEnd != null) {
                    newStartItems.remove(selectedEnd);
                }
                if (selectedStart != null && !newStartItems.contains(selectedStart)) {
                    newStartItems.add(0, selectedStart); // Add selected to the top if filtered out
                }
                startCityItems.setAll(newStartItems);
                 // If selectedStart was valid but now list is empty except for it (or just empty), re-ensure it's set if list allows
                if (selectedStart != null && startCityItems.isEmpty() && masterCityList.contains(selectedStart) && (selectedEnd == null || !selectedEnd.equals(selectedStart))){
                    startCityItems.add(selectedStart);
                }
                if(selectedStart != null && !startCityItems.contains(selectedStart) && masterCityList.contains(selectedStart) && (selectedEnd == null || !selectedEnd.equals(selectedStart))) {
                     startCityItems.add(0, selectedStart); // One more check if it got removed somehow
                }
                if (startCityItems.isEmpty() && selectedStart == null && !filter.isEmpty()){
                    startCityComboBox.setPromptText(LanguageManager.getText("noMatchPrompt"));
                } else {
                    startCityComboBox.setPromptText(LanguageManager.getText("selectStartCity"));
                    }

                // Update endCityItems
                List<String> newEndItems = masterCityList.stream()
                        .filter(city -> city.toLowerCase().contains(filter))
                        .collect(Collectors.toList());
                if (selectedStart != null) {
                    newEndItems.remove(selectedStart);
                }
                if (selectedEnd != null && !newEndItems.contains(selectedEnd)) {
                    newEndItems.add(0, selectedEnd); // Add selected to the top if filtered out
                }
                endCityItems.setAll(newEndItems);
                if (selectedEnd != null && endCityItems.isEmpty() && masterCityList.contains(selectedEnd) && (selectedStart == null || !selectedStart.equals(selectedEnd))){
                    endCityItems.add(selectedEnd);
                }
                if(selectedEnd != null && !endCityItems.contains(selectedEnd) && masterCityList.contains(selectedEnd) && (selectedStart == null || !selectedStart.equals(selectedEnd))) {
                     endCityItems.add(0, selectedEnd); // One more check
                }
                if (endCityItems.isEmpty() && selectedEnd == null && !filter.isEmpty()){
                    endCityComboBox.setPromptText(LanguageManager.getText("noMatchPrompt"));
                } else {
                    endCityComboBox.setPromptText(LanguageManager.getText("selectEndCity"));
                }
            });

            // Listener for startCityComboBox selection
            startCityComboBox.valueProperty().addListener((obs, oldStartCity, newStartCity) -> {
                String currentSearch = citySearchField.getText().toLowerCase();
                String currentEndSelection = endCityComboBox.getValue();

                // Update endCityItems: remove newStartCity, add back oldStartCity if valid
                if (newStartCity != null) {
                    endCityItems.remove(newStartCity);
                }
                if (oldStartCity != null && !oldStartCity.equals(newStartCity) && !oldStartCity.equals(currentEndSelection)) {
                    if (masterCityList.contains(oldStartCity) && !endCityItems.contains(oldStartCity)) {
                        // Add back only if it matches current search or if search is empty
                        if (currentSearch.isEmpty() || oldStartCity.toLowerCase().contains(currentSearch)) {
                            endCityItems.add(oldStartCity);
                        }
                    }
                }
                // Ensure currentEndSelection is still in endCityItems if it was selected
                if (currentEndSelection != null && !endCityItems.contains(currentEndSelection) && masterCityList.contains(currentEndSelection)){
                     if (currentSearch.isEmpty() || currentEndSelection.toLowerCase().contains(currentSearch)) {
                        endCityItems.add(0, currentEndSelection);
                     }
                }
                // Sort for consistency, though this might not be desired if order is important from masterList
                // FXCollections.sort(endCityItems);
            });

            // Listener for endCityComboBox selection
            endCityComboBox.valueProperty().addListener((obs, oldEndCity, newEndCity) -> {
                String currentSearch = citySearchField.getText().toLowerCase();
                String currentStartSelection = startCityComboBox.getValue();

                // Update startCityItems: remove newEndCity, add back oldEndCity if valid
                if (newEndCity != null) {
                    startCityItems.remove(newEndCity);
                }
                if (oldEndCity != null && !oldEndCity.equals(newEndCity) && !oldEndCity.equals(currentStartSelection)) {
                    if (masterCityList.contains(oldEndCity) && !startCityItems.contains(oldEndCity)) {
                        // Add back only if it matches current search or if search is empty
                        if (currentSearch.isEmpty() || oldEndCity.toLowerCase().contains(currentSearch)) {
                             startCityItems.add(oldEndCity);
                        }
                    }
                }
                // Ensure currentStartSelection is still in startCityItems if it was selected
                if (currentStartSelection != null && !startCityItems.contains(currentStartSelection) && masterCityList.contains(currentStartSelection)){
                    if (currentSearch.isEmpty() || currentStartSelection.toLowerCase().contains(currentSearch)) {
                        startCityItems.add(0, currentStartSelection);
                    }
                }
                // FXCollections.sort(startCityItems);
            });
            
            // Initialize default selections if list is not empty
            if (!masterCityList.isEmpty()) {
                startCityComboBox.setValue(masterCityList.get(0));
                if (masterCityList.size() > 1) {
                    // endCityComboBox items will be updated by startCityComboBox listener
                    // but we need to ensure the second unique city is available if selected as default
                    if (masterCityList.get(0).equals(masterCityList.get(1)) && masterCityList.size() >2) {
                         endCityComboBox.setValue(masterCityList.get(2)); // pick 3rd if 1st and 2nd are same
                    } else if (!masterCityList.get(0).equals(masterCityList.get(1))){
                         endCityComboBox.setValue(masterCityList.get(1));
                    } else if (endCityItems.contains(masterCityList.get(0)) && endCityItems.size() == 1) {
                         // if only one unique city, let end be the same
                         endCityComboBox.setValue(masterCityList.get(0));
                    } else {
                        // Fallback if lists are small or have many duplicates initially
                        if (!endCityItems.isEmpty()) endCityComboBox.setValue(endCityItems.get(0));
                    }
                } else {
                    endCityComboBox.setValue(masterCityList.get(0)); // Only one city in list
                }
            }
        } else {
             startCityComboBox.setPromptText("No cities loaded");
             endCityComboBox.setPromptText("No cities loaded");
        }

        content.getChildren().addAll(searchLabel, citySearchField, startCityLabel, startCityComboBox, 
                                    endCityLabel, endCityComboBox);
        
        TitledPane pane = new TitledPane(LanguageManager.getText("citySelection"), content);
        pane.setExpanded(true);
        
        return pane;
    }
    
    private TitledPane createAttractionSelectionPane() {
        VBox content = new VBox(10);
        content.setPadding(new Insets(10));
        
        Label searchLabel = new Label("搜索景点:");
        TextField attractionSearchField = new TextField();
        attractionSearchField.setPromptText("输入景点名称");
        
        Label availableLabel = new Label("可选景点:");
        ListView<String> availableAttractionsListView = new ListView<>();
        availableAttractionsListView.setPrefHeight(150);
        
        Label selectedLabel = new Label("已选景点:");
        selectedAttractions = FXCollections.observableArrayList();
        ListView<String> selectedAttractionsListView = new ListView<>(selectedAttractions);
        selectedAttractionsListView.setPrefHeight(150);
        
        if (roadNetwork != null) {
            List<String> attractionNamesList = new ArrayList<>();
            if (roadNetwork.getAllAttractions() != null) {
                for (Attraction attraction : roadNetwork.getAllAttractions()) {
                    if (attraction != null && attraction.getAttractionName() != null) {
                        attractionNamesList.add(attraction.getAttractionName());
                    }
                }
            }
            allAttractions = FXCollections.observableArrayList(attractionNamesList);
            filteredAttractions = new FilteredList<>(allAttractions, p -> true);
            availableAttractionsListView.setItems(filteredAttractions);
        }
        
        attractionSearchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredAttractions.setPredicate(attraction -> {
                if (newValue == null || newValue.isEmpty()) {
                    return true;
                }
                return attraction.toLowerCase().contains(newValue.toLowerCase());
            });
        });
        
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER);
        
        Button addButton = new Button("添加 >");
        addButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
        addButton.setOnAction(e -> {
            String selectedItem = availableAttractionsListView.getSelectionModel().getSelectedItem();
            if (selectedItem != null && !selectedAttractions.contains(selectedItem)) {
                selectedAttractions.add(selectedItem);
                allAttractions.remove(selectedItem);
            }
        });
        
        Button removeButton = new Button("< 删除");
        removeButton.setStyle("-fx-background-color: #F44336; -fx-text-fill: white;");
        removeButton.setOnAction(e -> {
            String selectedItem = selectedAttractionsListView.getSelectionModel().getSelectedItem();
            if (selectedItem != null) {
                selectedAttractions.remove(selectedItem);
                if (!allAttractions.contains(selectedItem)) {
                    allAttractions.add(selectedItem);
                }
            }
        });
        
        buttonBox.getChildren().addAll(addButton, removeButton);
        
        content.getChildren().addAll(searchLabel, attractionSearchField, availableLabel, 
                                    availableAttractionsListView, buttonBox, selectedLabel, 
                                    selectedAttractionsListView);
        
        TitledPane pane = new TitledPane("景点选择", content);
        pane.setExpanded(true);
        
        return pane;
    }

    private VBox createRightPanel() {
        VBox panel = new VBox(20);
        panel.setPadding(new Insets(20));
        
        // 计算结果部分
        TitledPane resultPane = createResultPane();
        
        // 路线地图部分
        TitledPane mapPane = createMapPane();
        
        panel.getChildren().addAll(resultPane, mapPane);
        VBox.setVgrow(mapPane, Priority.ALWAYS);
        
        return panel;
    }
    
    private TitledPane createResultPane() {
        VBox content = new VBox(10);
        content.setPadding(new Insets(10));
        
        resultTextArea = new TextArea();
        resultTextArea.setEditable(false);
        resultTextArea.setPrefHeight(150);
        resultTextArea.setWrapText(true);
        
        content.getChildren().add(resultTextArea);
        
        TitledPane pane = new TitledPane("计算结果", content);
        pane.setExpanded(true);
        
        return pane;
    }
    
    private TitledPane createMapPane() {
        VBox content = new VBox(10);
        content.setPadding(new Insets(10));
        
        mapView = new MapView(roadNetwork);
        VBox.setVgrow(mapView, Priority.ALWAYS);
        
        // content.getChildren().addAll(mapView); // Initially add mapView
        // Delay adding to ensure TitledPane is ready

        TitledPane pane = new TitledPane(LanguageManager.getText("routeMap"), content);
        pane.setExpanded(true); // Start expanded

        // Listener to handle map redraw when TitledPane is expanded or collapsed
        pane.expandedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) { // Pane is expanded
                if (!content.getChildren().contains(mapView)) {
                    content.getChildren().add(mapView);
                }
                // Delay redraw slightly to ensure layout pass is complete after adding
                javafx.application.Platform.runLater(() -> {
                    if (mapView != null) {
                        mapView.requestLayout(); // Request layout first
                        mapView.redraw();        // Then redraw
                    }
                });
            } else { // Pane is collapsed
                // Optionally remove from content to save resources, though this might cause issues if not handled carefully
                // For now, let's not remove, but ensure it's added back correctly when expanded.
                // content.getChildren().remove(mapView);
            }
        });
        
        // Initial addition of mapView, needs to be done after listeners might be set up or pane is ready
        // Adding it directly or via Platform.runLater if pane starts expanded
        if (pane.isExpanded()) {
             content.getChildren().add(mapView);
             javafx.application.Platform.runLater(() -> {
                 if (mapView != null) {
                    mapView.requestLayout();
                    mapView.redraw();
                 }
             });
        }
        
        return pane;
    }

    private void calculateRoute() {
        // 获取起始城市和目的地
        String startCity = startCityComboBox.getValue();
        String endCity = endCityComboBox.getValue();
        
        if (startCity == null || endCity == null) {
            showAlert(LanguageManager.getText("inputError"), LanguageManager.getText("selectCities"));
            return;
        }

        List<String> attractionsToVisit = new ArrayList<>(selectedAttractions);
        
        try {
            // 计算路线
            TripPlan plan = tripPlanningService.planTrip(startCity, endCity, attractionsToVisit, true, 30000L);
            
            // 显示结果
            displayTripPlan(plan);
            
            // 在地图上绘制路线
            mapView.setTripPlan(plan);
        } catch (Exception e) {
            showAlert(LanguageManager.getText("routeError"), 
                     LanguageManager.getText("routeErrorDesc") + " " + e.getMessage());
        }
    }

    private Scene getScene() {
        // 获取当前场景 - 简化实现
        return mapView.getScene();
    }

    private void initializeServices() {
        // 加载数据
        CsvDataLoader dataLoader = new CsvDataLoader();
        String roadsCsvPath = "/data/roads.csv";
        String attractionsCsvPath = "/data/attractions.csv";
        
        roadNetwork = dataLoader.loadData(roadsCsvPath, attractionsCsvPath);
        if (roadNetwork == null) {
            showAlert("错误", "无法加载道路网络数据，请检查CSV文件路径。");
            return;
        }

        // 初始化服务
        PathfindingService dijkstraService = new DenseDijkstraService();
        PoiOptimizerService permutationOptimizer = new PermutationPoiOptimizerService(dijkstraService);
        PoiOptimizerService dpOptimizer = new DynamicProgrammingPoiOptimizerService(roadNetwork, dijkstraService);

        tripPlanningService = new TripPlanningService(
                roadNetwork,
                dijkstraService,
                permutationOptimizer,
                dpOptimizer
        );
    }

    private void displayTripPlan(TripPlan plan) {
        if (plan != null) {
            resultTextArea.setText(plan.toString());
        } else {
            resultTextArea.setText("无法找到有效的旅行路线。");
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }

    public VBox createLegendPane() {
        VBox legendPane = new VBox(10);
        legendPane.setPadding(new Insets(15));
        legendPane.setStyle("-fx-background-color: rgba(255, 255, 255, 0.9); -fx-background-radius: 10; -fx-border-radius: 10; -fx-border-color: lightgray; -fx-min-width: 200;");
        legendPane.setAlignment(Pos.TOP_LEFT);

        Label title = new Label(LanguageManager.isChineseLanguage() ? "图例" : "Legend");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        title.setTextFill(TEXT_COLOR);
        legendPane.getChildren().add(title);

        // City
        Circle cityIcon = new Circle(10, CITY_COLOR);
        cityIcon.setStroke(Color.WHITE);
        cityIcon.setStrokeWidth(1.5);
        Label cityLabel = new Label(LanguageManager.isChineseLanguage() ? "城市" : "City");
        legendPane.getChildren().add(createLegendItem(cityIcon, cityLabel));

        // Start Point
        Circle startIcon = new Circle(10, START_CITY_COLOR);
        startIcon.setStroke(Color.WHITE);
        startIcon.setStrokeWidth(1.5);
        Label startLabelText = new Label(LanguageManager.getText("startPoint"));
        legendPane.getChildren().add(createLegendItem(startIcon, startLabelText));

        // End Point
        Circle endIcon = new Circle(10, END_CITY_COLOR);
        endIcon.setStroke(Color.WHITE);
        endIcon.setStrokeWidth(1.5);
        Label endLabelText = new Label(LanguageManager.getText("endPoint"));
        legendPane.getChildren().add(createLegendItem(endIcon, endLabelText));
        
        // Waypoint (if you want to show this specifically in legend)
        Circle waypointIcon = new Circle(8, WAYPOINT_COLOR); // Smaller for waypoint
        waypointIcon.setStroke(Color.WHITE);
        waypointIcon.setStrokeWidth(1.5);
        Label waypointLabel = new Label(LanguageManager.isChineseLanguage() ? "途经点" : "Waypoint");
        legendPane.getChildren().add(createLegendItem(waypointIcon, waypointLabel));


        // Attraction
        Circle attractionIcon = new Circle(8, ATTRACTION_COLOR); // Assuming attractions are circles now
        attractionIcon.setStroke(Color.WHITE);
        attractionIcon.setStrokeWidth(1.5);
        Label attractionLabelText = new Label(LanguageManager.isChineseLanguage() ? "景点" : "Attraction");
        legendPane.getChildren().add(createLegendItem(attractionIcon, attractionLabelText));
        
        // Road
        Rectangle roadIcon = new Rectangle(20, 3); 
        roadIcon.setFill(ROAD_COLOR.deriveColor(0,1,1,0.3)); // Fainter road color
        Label roadLabel = new Label(LanguageManager.isChineseLanguage() ? "道路" : "Road");
        legendPane.getChildren().add(createLegendItem(roadIcon, roadLabel));

        // Route
        Rectangle routeIcon = new Rectangle(20, 5);
        routeIcon.setFill(ROUTE_COLOR); // Thicker route color
        Label routeLabel = new Label(LanguageManager.isChineseLanguage() ? "规划路线" : "Planned Route");
        legendPane.getChildren().add(createLegendItem(routeIcon, routeLabel));


        return legendPane;
    }

    private HBox createLegendItem(javafx.scene.Node icon, Label label) {
        HBox itemBox = new HBox(8);
        itemBox.setAlignment(Pos.CENTER_LEFT);
        label.setFont(Font.font("Arial", FontWeight.NORMAL, 14));
        label.setTextFill(TEXT_COLOR);
        itemBox.getChildren().addAll(icon, label);
        return itemBox;
    }
} 