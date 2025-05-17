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

    // Additional UI component references for dynamic text updates
    // For CitySelectionPane
    private Label citySearchLabel;
    private TextField citySearchField;
    private Label startCityLabel;
    private Label endCityLabel;

    // For AttractionSelectionPane
    private Label attractionSearchLabel;
    private TextField attractionSearchField;
    private Label attractionAvailableLabel;
    private Label attractionSelectedLabel;
    private Button attractionAddButton;
    private Button attractionRemoveButton;
    // ListView<String> availableAttractionsListView; // Not directly changing its own text
    // ObservableList<String> selectedAttractions; // Data, not a label itself

    // For TitledPanes
    private TitledPane citySelectionTitledPane;
    private TitledPane attractionSelectionTitledPane;
    private TitledPane resultTitledPane;
    private TitledPane mapTitledPane;

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
        splitPane.setDividerPositions(0.30); // Adjusted divider: smaller left panel, larger right panel
        
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
        if (mainContentArea != null && mainContentArea.getChildren().size() > 1 && mainContentArea.getChildren().get(1) instanceof VBox) {
            VBox legendPane = (VBox) mainContentArea.getChildren().get(1);
            if (legendPane != null && !legendPane.getChildren().isEmpty()) {
                // 更新图例标题
                if (legendPane.getChildren().get(0) instanceof Label) {
                    Label legendTitle = (Label) legendPane.getChildren().get(0);
                    legendTitle.setText(LanguageManager.getText("legend.title")); // 使用键
                }
                
                // 更新图例项文本 - 假设顺序是固定的或可以推断出键
                // 需要确保 createLegendPane 创建的顺序与这些键对应
                String[] legendItemKeys = {
                    "legend.city", 
                    "legend.startPoint", 
                    "legend.endPoint", 
                    "legend.waypoint", 
                    "legend.attraction", 
                    "legend.road", 
                    "legend.plannedRoute"
                };

                for (int i = 1; i < legendPane.getChildren().size(); i++) {
                    if (i -1 < legendItemKeys.length && legendPane.getChildren().get(i) instanceof HBox) {
                        HBox item = (HBox) legendPane.getChildren().get(i);
                        if (item.getChildren().size() > 1 && item.getChildren().get(1) instanceof Label) {
                            Label itemLabel = (Label) item.getChildren().get(1);
                            itemLabel.setText(LanguageManager.getText(legendItemKeys[i-1]));
                        }
                    }
                }
            }
        }
    }
    
    private void updateControlsLanguage() {
        // 更新所有TitledPane的标题
        if (citySelectionTitledPane != null) {
            citySelectionTitledPane.setText(LanguageManager.getText("citySelection"));
        }
        if (attractionSelectionTitledPane != null) {
            attractionSelectionTitledPane.setText(LanguageManager.getText("attractionSelection"));
        }
        if (resultTitledPane != null) {
            resultTitledPane.setText(LanguageManager.getText("results"));
        }
        if (mapTitledPane != null) {
            mapTitledPane.setText(LanguageManager.getText("routeMap"));
        }

        // 更新按钮文本
        if (calculateButton != null) {
            calculateButton.setText(LanguageManager.getText("calculateRoute"));
        }
        if (attractionAddButton != null) {
            attractionAddButton.setText(LanguageManager.getText("add"));
        }
        if (attractionRemoveButton != null) {
            attractionRemoveButton.setText(LanguageManager.getText("remove"));
        }

        // 更新标签和提示文本 (Prompts)
        // City Selection Pane
        if (citySearchLabel != null) {
            citySearchLabel.setText(LanguageManager.getText("searchCity"));
        }
        if (citySearchField != null) {
            citySearchField.setPromptText(LanguageManager.getText("cityPrompt"));
        }
        if (startCityLabel != null) {
            startCityLabel.setText(LanguageManager.getText("startCity"));
        }
        if (startCityComboBox != null) {
            // Preserve current value if any, only update prompt if it's showing
            if (startCityComboBox.getValue() == null || startCityComboBox.getEditor().getText().isEmpty()) {
                 startCityComboBox.setPromptText(LanguageManager.getText("selectStartCity"));
            } // else: dynamic prompt based on no match might be active, or a value is selected.
              // Consider if noMatchPrompt also needs LanguageManager key.
        }
        if (endCityLabel != null) {
            endCityLabel.setText(LanguageManager.getText("endCity"));
        }
        if (endCityComboBox != null) {
            if (endCityComboBox.getValue() == null || endCityComboBox.getEditor().getText().isEmpty()) {
                endCityComboBox.setPromptText(LanguageManager.getText("selectEndCity"));
            } // else: similar to startCityComboBox
        }

        // Attraction Selection Pane
        if (attractionSearchLabel != null) {
            attractionSearchLabel.setText(LanguageManager.getText("searchAttraction"));
        }
        if (attractionSearchField != null) {
            attractionSearchField.setPromptText(LanguageManager.getText("attractionPrompt"));
        }
        if (attractionAvailableLabel != null) {
            attractionAvailableLabel.setText(LanguageManager.getText("availableAttractions"));
                        }
        if (attractionSelectedLabel != null) {
            attractionSelectedLabel.setText(LanguageManager.getText("selectedAttractions"));
        }
        
        // showAlert titles and messages are typically handled at the point of calling showAlert,
        // by passing LanguageManager.getText("key") directly to showAlert.
    }
    
    private void updateTitledPaneLabels() {
        // This logic is now in updateControlsLanguage()
    }
    
    private void updateButtonLabels() {
        // This logic is now in updateControlsLanguage()
        // The old updateButtonsInContainer can also be removed.
    }
    
    private void updateButtonsInContainer(Pane container) {
        // This method is no longer needed if buttons are updated via member references.
    }
    
    private void updateLabelsAndPrompts() {
        // This logic is now in updateControlsLanguage()
        // The old updateLabelsInContainer can also be removed.
    }
    
    private void updateLabelsInContainer(Pane container) {
        // This method is no longer needed if labels/prompts are updated via member references.
    }

    private VBox createLeftPanel() {
        VBox panel = new VBox(20);
        panel.setPadding(new Insets(20));
        
        // 城市选择部分
        this.citySelectionTitledPane = createCitySelectionPane();
        
        // 景点选择部分
        this.attractionSelectionTitledPane = createAttractionSelectionPane();
        
        // 计算按钮
        calculateButton = new Button(LanguageManager.getText("calculateRoute"));
        calculateButton.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; -fx-font-size: 14px;");
        calculateButton.setPrefHeight(40);
        calculateButton.setMaxWidth(Double.MAX_VALUE);
        calculateButton.setOnAction(e -> calculateRoute());
        
        panel.getChildren().addAll(this.citySelectionTitledPane, this.attractionSelectionTitledPane, calculateButton);
        
        return panel;
    }
    
    private TitledPane createCitySelectionPane() {
        VBox content = new VBox(10);
        content.setPadding(new Insets(10));
        
        citySearchLabel = new Label(LanguageManager.getText("searchCity"));
        citySearchField = new TextField();
        citySearchField.setPromptText(LanguageManager.getText("cityPrompt"));
        
        startCityLabel = new Label(LanguageManager.getText("startCity"));
        startCityComboBox = new ComboBox<>();
        startCityComboBox.setMaxWidth(Double.MAX_VALUE);
        startCityComboBox.setPromptText(LanguageManager.getText("selectStartCity"));
        
        endCityLabel = new Label(LanguageManager.getText("endCity"));
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

        content.getChildren().addAll(citySearchLabel, citySearchField, startCityLabel, startCityComboBox, 
                                    endCityLabel, endCityComboBox);
        
        TitledPane pane = new TitledPane(LanguageManager.getText("citySelection"), content);
        pane.setId("citySelectionPane");
        pane.setExpanded(true);
        
        return pane;
    }
    
    private TitledPane createAttractionSelectionPane() {
        VBox content = new VBox(10);
        content.setPadding(new Insets(10));
        
        attractionSearchLabel = new Label(LanguageManager.getText("searchAttraction"));
        attractionSearchField = new TextField();
        attractionSearchField.setPromptText(LanguageManager.getText("attractionPrompt"));
        
        attractionAvailableLabel = new Label(LanguageManager.getText("availableAttractions"));
        ListView<String> availableAttractionsListView = new ListView<>();
        availableAttractionsListView.setPrefHeight(150);
        
        attractionSelectedLabel = new Label(LanguageManager.getText("selectedAttractions"));
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
                // Ensure attraction is not null before calling toLowerCase
                return attraction != null && attraction.toLowerCase().contains(newValue.toLowerCase());
            });
        });
        
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER);
        
        attractionAddButton = new Button(LanguageManager.getText("add"));
        attractionAddButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
        attractionAddButton.setOnAction(e -> {
            String selectedItem = availableAttractionsListView.getSelectionModel().getSelectedItem();
            if (selectedItem != null && !selectedAttractions.contains(selectedItem)) {
                selectedAttractions.add(selectedItem);
                allAttractions.remove(selectedItem);
            }
        });
        
        attractionRemoveButton = new Button(LanguageManager.getText("remove"));
        attractionRemoveButton.setStyle("-fx-background-color: #F44336; -fx-text-fill: white;");
        attractionRemoveButton.setOnAction(e -> {
            String selectedItem = selectedAttractionsListView.getSelectionModel().getSelectedItem();
            if (selectedItem != null) {
                selectedAttractions.remove(selectedItem);
                if (!allAttractions.contains(selectedItem)) {
                    allAttractions.add(selectedItem);
                }
            }
        });
        
        buttonBox.getChildren().addAll(attractionAddButton, attractionRemoveButton);
        
        content.getChildren().addAll(attractionSearchLabel, attractionSearchField, attractionAvailableLabel, 
                                    availableAttractionsListView, buttonBox, attractionSelectedLabel, 
                                    selectedAttractionsListView);
        
        TitledPane pane = new TitledPane(LanguageManager.getText("attractionSelection"), content);
        pane.setId("attractionSelectionPane");
        pane.setExpanded(true);
        
        return pane;
    }

    private VBox createRightPanel() {
        VBox panel = new VBox(20);
        panel.setPadding(new Insets(20));
        
        // 计算结果部分
        this.resultTitledPane = createResultPane();
        
        // 路线地图部分
        this.mapTitledPane = createMapPane();
        
        panel.getChildren().addAll(this.resultTitledPane, this.mapTitledPane);
        VBox.setVgrow(this.mapTitledPane, Priority.ALWAYS);
        
        return panel;
    }
    
    private TitledPane createResultPane() {
        VBox content = new VBox(10);
        content.setPadding(new Insets(10));
        
        resultTextArea = new TextArea();
        resultTextArea.setEditable(false);
        resultTextArea.setPrefHeight(100);
        resultTextArea.setWrapText(true);
        
        content.getChildren().add(resultTextArea);
        
        TitledPane pane = new TitledPane(LanguageManager.getText("results"), content);
        pane.setId("resultsPane");
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
        
        pane.setId("mapPane");
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

        Label title = new Label(LanguageManager.getText("legend.title"));
        title.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        title.setTextFill(TEXT_COLOR);
        legendPane.getChildren().add(title);

        // City
        Circle cityIcon = new Circle(10, CITY_COLOR);
        cityIcon.setStroke(Color.WHITE);
        cityIcon.setStrokeWidth(1.5);
        Label cityLabel = new Label(LanguageManager.getText("legend.city"));
        legendPane.getChildren().add(createLegendItem(cityIcon, cityLabel));

        // Start Point
        Circle startIcon = new Circle(10, START_CITY_COLOR);
        startIcon.setStroke(Color.WHITE);
        startIcon.setStrokeWidth(1.5);
        Label startLabelText = new Label(LanguageManager.getText("legend.startPoint"));
        legendPane.getChildren().add(createLegendItem(startIcon, startLabelText));

        // End Point
        Circle endIcon = new Circle(10, END_CITY_COLOR);
        endIcon.setStroke(Color.WHITE);
        endIcon.setStrokeWidth(1.5);
        Label endLabelText = new Label(LanguageManager.getText("legend.endPoint"));
        legendPane.getChildren().add(createLegendItem(endIcon, endLabelText));
        
        // Waypoint (if you want to show this specifically in legend)
        Circle waypointIcon = new Circle(8, WAYPOINT_COLOR); // Smaller for waypoint
        waypointIcon.setStroke(Color.WHITE);
        waypointIcon.setStrokeWidth(1.5);
        Label waypointLabel = new Label(LanguageManager.getText("legend.waypoint"));
        legendPane.getChildren().add(createLegendItem(waypointIcon, waypointLabel));


        // Attraction
        Circle attractionIcon = new Circle(8, ATTRACTION_COLOR); // Assuming attractions are circles now
        attractionIcon.setStroke(Color.WHITE);
        attractionIcon.setStrokeWidth(1.5);
        Label attractionItemLabelText = new Label(LanguageManager.getText("legend.attraction")); // Changed variable name to avoid conflict
        legendPane.getChildren().add(createLegendItem(attractionIcon, attractionItemLabelText));
        
        // Road
        Rectangle roadIcon = new Rectangle(20, 3); 
        roadIcon.setFill(ROAD_COLOR.deriveColor(0,1,1,0.3)); // Fainter road color
        Label roadLabel = new Label(LanguageManager.getText("legend.road"));
        legendPane.getChildren().add(createLegendItem(roadIcon, roadLabel));

        // Route
        Rectangle routeIcon = new Rectangle(20, 5);
        routeIcon.setFill(ROUTE_COLOR); // Thicker route color
        Label routeLabel = new Label(LanguageManager.getText("legend.plannedRoute"));
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