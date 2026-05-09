package it.citylife.ui;

import it.citylife.model.AusterityPolicy;
import it.citylife.model.CityState;
import it.citylife.model.FossilFuelPolicy;
import it.citylife.model.GreenPolicy;
import it.citylife.model.StateObserver;
import it.citylife.model.Structure;
import it.citylife.model.StructureType;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.Slider;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;

public class DashboardView extends Application implements StateObserver {

    private SimulationController controller;
    private Timeline timeline;
    private int tickCount = 0;

    // Labels
    private Label budgetLabel;
    private Label populationLabel;
    private Label happinessLabel;
    private Label healthLabel;
    private Label pollutionLabel;
    private Label wasteLabel;
    private Label energyLabel;
    private Label tickLabel;

    // Policy highlight
    private Button activeBtn;

    // Build tool
    private String selectedTool = null;
    private Button activeBuildBtn = null;

    // Panels
    private VBox leftPanel;
    private StackPane[][] cells = new StackPane[20][20];

    // Chart
    private LineChart<Number, Number> chart;

    // Dashboard summary labels (barra centrata sotto il tick header)
    private Label dashPopLabel;
    private Label dashHapLabel;
    private Label dashHealthLabel;
    private Label dashPollLabel;

    // Chart series
    private XYChart.Series<Number, Number> populationSeries;
    private XYChart.Series<Number, Number> happinessSeries;
    private XYChart.Series<Number, Number> healthSeries;
    private XYChart.Series<Number, Number> pollutionSeries;

    @Override
    public void start(Stage primaryStage) {
        controller = new SimulationController();
        controller.addObserver(this);

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #0d1117;");

        FontIcon headerIcon = new FontIcon(FontAwesomeSolid.CITY);
        headerIcon.setIconSize(18);
        headerIcon.setIconColor(Color.web("#58a6ff"));
        tickLabel = new Label("CityLogic  |  Tick: 0", headerIcon);
        tickLabel.setStyle("-fx-text-fill: #e6edf3; -fx-font-size: 16px; -fx-font-weight: bold; -fx-padding: 12px 16px; -fx-border-color: #30363d; -fx-border-width: 0 0 1 0;");
        root.setTop(tickLabel);

        TabPane tabPane = new TabPane();
        Tab mapTab  = new Tab("City Map",  buildMapView());
        Tab dashTab = new Tab("Dashboard", buildChart());
        mapTab.setClosable(false);
        dashTab.setClosable(false);
        tabPane.getTabs().addAll(mapTab, dashTab);
        root.setCenter(tabPane);
        root.setRight(buildMetricsPanel());
        root.setBottom(buildBottomBar());

        primaryStage.setTitle("CityLogic");
        Scene scene = new Scene(root, 1300, 750);
        scene.getStylesheets().add(getClass().getResource("/it/citylife/ui/dashboard.css").toExternalForm());
        primaryStage.setScene(scene);

        primaryStage.getIcons().clear();
        try (var stream = getClass().getResourceAsStream("/it/citylife/ui/icon.png")) {
            if (stream != null) primaryStage.getIcons().add(new javafx.scene.image.Image(stream));
        } catch (Exception e) { /* fallback: nessuna icona */ }

        primaryStage.show();
    }

    // ── Tab 1: City Map ──────────────────────────────────────────────────────

    private BorderPane buildMapView() {
        BorderPane mapPane = new BorderPane();
        mapPane.setLeft(buildLeftPanel());

        StackPane gridWrapper = new StackPane(buildGridPane());
        gridWrapper.setStyle("-fx-background-color: #0d1117;");
        gridWrapper.setAlignment(javafx.geometry.Pos.CENTER);
        mapPane.setCenter(gridWrapper);

        return mapPane;
    }

    private Label makeMetricLabel(String text, FontAwesomeSolid icon, String hexColor) {
        FontIcon fi = new FontIcon(icon);
        fi.setIconSize(14);
        fi.setIconColor(Color.web(hexColor));
        Label lbl = new Label(text, fi);
        lbl.setStyle("-fx-text-fill: " + hexColor + "; -fx-font-size: 14px;");
        return lbl;
    }

    private VBox buildMetricsPanel() {
        Label metricsTitle = new Label("METRICS");
        metricsTitle.setStyle("-fx-text-fill: #8b949e; -fx-font-size: 10px; -fx-font-weight: bold;");

        budgetLabel     = makeMetricLabel(String.format("Budget: %.0f",     controller.getState().getBudget()),     FontAwesomeSolid.COINS,       "#facc15");
        populationLabel = makeMetricLabel("Population: " +                  controller.getState().getPopulation(),  FontAwesomeSolid.USERS,       "#e6edf3");
        happinessLabel  = makeMetricLabel(String.format("Happiness: %.1f",  controller.getState().getHappiness()),  FontAwesomeSolid.SMILE,       "#fb923c");
        healthLabel     = makeMetricLabel(String.format("Health: %.1f",     controller.getState().getHealth()),     FontAwesomeSolid.HEART,       "#f472b6");
        pollutionLabel  = makeMetricLabel(String.format("Pollution: %.1f",  controller.getState().getPollution()),  FontAwesomeSolid.SMOG,        "#4ade80");
        wasteLabel      = makeMetricLabel("Waste: " +                       controller.getState().getWasteLevel(),  FontAwesomeSolid.TRASH,       "#8b949e");

        boolean powered = controller.hasPower();
        FontIcon boltIcon = new FontIcon(FontAwesomeSolid.BOLT);
        boltIcon.setIconSize(14);
        boltIcon.setIconColor(Color.web(powered ? "#3fb950" : "#f85149"));
        energyLabel = new Label(powered ? "Power: OK" : "Power: BLACKOUT", boltIcon);
        energyLabel.setStyle("-fx-text-fill: " + (powered ? "#3fb950" : "#f85149") + "; -fx-font-size: 14px; -fx-font-weight: bold;");

        VBox vbox = new VBox(10,
            metricsTitle,
            budgetLabel, populationLabel, happinessLabel,
            healthLabel, pollutionLabel, wasteLabel,
            new Separator(),
            energyLabel
        );
        vbox.setPadding(new Insets(14));
        vbox.setMinWidth(200);
        vbox.setMaxWidth(200);
        vbox.setStyle("-fx-background-color: #161b22; -fx-border-color: #30363d; -fx-border-width: 0 0 0 1;");
        return vbox;
    }

    private VBox buildLeftPanel() {
        Label buildTitle = new Label("BUILD");
        buildTitle.setStyle("-fx-text-fill: #8b949e; -fx-font-size: 10px; -fx-font-weight: bold;");

        Button resBtn   = buildToolButton("Residential", "RESIDENTIAL", FontAwesomeSolid.HOME,     colorForType(StructureType.RESIDENTIAL));
        Button indBtn   = buildToolButton("Industrial",  "INDUSTRIAL",  FontAwesomeSolid.INDUSTRY, colorForType(StructureType.INDUSTRIAL));
        Button comBtn   = buildToolButton("Commercial",  "COMMERCIAL",  FontAwesomeSolid.STORE,    colorForType(StructureType.COMMERCIAL));
        Button ppBtn    = buildToolButton("Power Plant", "POWER_PLANT", FontAwesomeSolid.BOLT,     colorForType(StructureType.POWER_PLANT));
        Button parkBtn  = buildToolButton("Park",        "PARK",        FontAwesomeSolid.TREE,     colorForType(StructureType.PARK));
        Button roadBtn  = buildToolButton("Road",        "ROAD",        FontAwesomeSolid.ROAD,     colorForType(StructureType.ROAD));
        Button demolBtn = buildToolButton("Demolish",    "DEMOLISH",    FontAwesomeSolid.HAMMER,   Color.web("#f38ba8"));

        VBox vbox = new VBox(8,
            buildTitle,
            resBtn, indBtn, comBtn, ppBtn, parkBtn, roadBtn, demolBtn
        );
        vbox.setPadding(new Insets(14));
        vbox.setMinWidth(160);
        vbox.setMaxWidth(160);
        vbox.setStyle("-fx-background-color: #161b22; -fx-border-color: #30363d; -fx-border-width: 0 1 0 0;");
        return vbox;
    }

    private Button buildToolButton(String label, String tool, FontAwesomeSolid icon, Color iconColor) {
        FontIcon fi = new FontIcon(icon);
        fi.setIconSize(14);
        fi.setIconColor(iconColor);
        Button btn = new Button(label, fi);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setMinHeight(32);
        btn.setStyle("-fx-background-color: #21262d; -fx-text-fill: #e6edf3; -fx-font-size: 12px;");
        btn.setOnAction(e -> {
            if (activeBuildBtn != null)
                activeBuildBtn.setStyle("-fx-background-color: #21262d; -fx-text-fill: #e6edf3; -fx-font-size: 12px;");
            activeBuildBtn = btn;
            btn.setStyle("-fx-background-color: #21262d; -fx-text-fill: #e6edf3; -fx-font-size: 12px; -fx-border-color: #58a6ff; -fx-border-width: 2px;");
            selectedTool = tool;
        });
        return btn;
    }

    private GridPane buildGridPane() {
        GridPane grid = new GridPane();
        grid.setHgap(1);
        grid.setVgap(1);
        grid.setPadding(new Insets(10));
        grid.setStyle("-fx-background-color: #0d1117;");
        grid.setMaxSize(javafx.scene.layout.Region.USE_PREF_SIZE, javafx.scene.layout.Region.USE_PREF_SIZE);

        for (int x = 0; x < 20; x++) {
            for (int y = 0; y < 20; y++) {
                StackPane cell = new StackPane();
                cell.setPrefSize(33, 33);
                cell.setStyle("-fx-background-color: #0d1117; -fx-border-color: #21262d; -fx-border-width: 0.5;");
                final int fx = x, fy = y;
                cell.setOnMouseClicked(e -> onCellClick(fx, fy));
                cells[x][y] = cell;
                grid.add(cell, x, y);
            }
        }
        updateGrid();
        return grid;
    }

    private void onCellClick(int x, int y) {
        if (selectedTool == null) return;
        boolean ok;
        if (selectedTool.equals("DEMOLISH")) {
            ok = controller.demolish(x, y);
        } else {
            ok = controller.placeBuilding(selectedTool, x, y);
        }
        if (!ok) showPlacementError();
        updateGrid();
        refreshMetricsDisplay();
    }

    private void showPlacementError() {
        Label err = new Label("⚠️ Impossibile costruire!");
        err.setStyle("-fx-text-fill: #f9e64f; -fx-font-weight: bold;");
        leftPanel.getChildren().add(0, err);
        new javafx.animation.Timeline(new javafx.animation.KeyFrame(
            javafx.util.Duration.seconds(2), e -> leftPanel.getChildren().remove(err)
        )).play();
    }

    private void refreshMetricsDisplay() {
        CityState s = controller.getState();
        budgetLabel.setText(String.format("Budget: %.0f",    s.getBudget()));
        populationLabel.setText("Population: " +             s.getPopulation());
        happinessLabel.setText(String.format("Happiness: %.1f", s.getHappiness()));
        healthLabel.setText(String.format("Health: %.1f",    s.getHealth()));
        pollutionLabel.setText(String.format("Pollution: %.1f", s.getPollution()));
        wasteLabel.setText("Waste: " +                       s.getWasteLevel());
        boolean powered = controller.hasPower();
        energyLabel.setText(powered ? "Power: OK" : "Power: BLACKOUT");
        energyLabel.setStyle("-fx-text-fill: " + (powered ? "#3fb950" : "#f85149") + "; -fx-font-size: 14px; -fx-font-weight: bold;");
    }

    private void updateGrid() {
        for (int x = 0; x < 20; x++) {
            for (int y = 0; y < 20; y++) {
                StackPane cell = cells[x][y];
                cell.getChildren().clear();
                var gridCell = controller.getGrid().getCell(x, y);
                if (gridCell == null || gridCell.isEmpty()) {
                    cell.setStyle("-fx-background-color: #0d1117; -fx-border-color: #21262d; -fx-border-width: 0.5;");
                } else if (gridCell.getStructure() instanceof Structure s) {
                    Color bg = colorForType(s.getType());
                    String hex = String.format("#%02x%02x%02x",
                        (int)(bg.getRed()*255),
                        (int)(bg.getGreen()*255),
                        (int)(bg.getBlue()*255));
                    cell.setStyle("-fx-background-color: " + hex + "55; -fx-border-color: " + hex + "; -fx-border-width: 1;");
                    FontIcon icon = new FontIcon(iconForType(s.getType()));
                    icon.setIconSize(16);
                    icon.setIconColor(bg);
                    cell.getChildren().add(icon);
                }
            }
        }
    }

    private FontAwesomeSolid iconForType(StructureType type) {
        return switch (type) {
            case RESIDENTIAL -> FontAwesomeSolid.HOME;
            case INDUSTRIAL  -> FontAwesomeSolid.INDUSTRY;
            case COMMERCIAL  -> FontAwesomeSolid.STORE;
            case POWER_PLANT -> FontAwesomeSolid.BOLT;
            case PARK        -> FontAwesomeSolid.TREE;
            case ROAD        -> FontAwesomeSolid.ROAD;
        };
    }

    private Color colorForType(StructureType type) {
        return switch (type) {
            case RESIDENTIAL -> Color.web("#60a5fa");
            case INDUSTRIAL  -> Color.web("#fb923c");
            case COMMERCIAL  -> Color.web("#facc15");
            case POWER_PLANT -> Color.web("#f472b6");
            case PARK        -> Color.web("#4ade80");
            case ROAD        -> Color.web("#94a3b8");
        };
    }

    // ── Tab 2: Dashboard ─────────────────────────────────────────────────────

    private Label makeDashStat(String text, FontAwesomeSolid icon, String hexColor) {
        FontIcon fi = new FontIcon(icon);
        fi.setIconSize(15);
        fi.setIconColor(Color.web(hexColor));
        Label lbl = new Label(text, fi);
        lbl.setStyle("-fx-text-fill: " + hexColor + "; -fx-font-size: 14px; -fx-font-weight: bold;");
        return lbl;
    }

    private VBox buildChart() {
        NumberAxis xAxis = new NumberAxis(); xAxis.setLabel("Tick");
        NumberAxis yAxis = new NumberAxis(0, 110, 10); yAxis.setLabel("Value (0-110)");

        chart = new LineChart<>(xAxis, yAxis);
        chart.setTitle("City Trends");
        chart.setCreateSymbols(false);
        chart.setAnimated(false);

        populationSeries = new XYChart.Series<>(); populationSeries.setName("Population (×1k)");
        happinessSeries  = new XYChart.Series<>(); happinessSeries.setName("Happiness");
        healthSeries     = new XYChart.Series<>(); healthSeries.setName("Health");
        pollutionSeries  = new XYChart.Series<>(); pollutionSeries.setName("Pollution");
        chart.getData().addAll(populationSeries, happinessSeries, healthSeries, pollutionSeries);

        // Colori allineati alle series JavaFX Modena default (1→pop, 2→hap, 3→health, 4→poll)
        dashPopLabel    = makeDashStat("Population: 0",   FontAwesomeSolid.USERS, "#f3622d");
        dashHapLabel    = makeDashStat("Happiness: 67.0", FontAwesomeSolid.SMILE, "#fba71b");
        dashHealthLabel = makeDashStat("Health: 100.0",   FontAwesomeSolid.HEART, "#57b757");
        dashPollLabel   = makeDashStat("Pollution: 0.0",  FontAwesomeSolid.SMOG,  "#41a9c9");

        HBox statsBar = new HBox(40, dashPopLabel, dashHapLabel, dashHealthLabel, dashPollLabel);
        statsBar.setAlignment(javafx.geometry.Pos.CENTER);
        statsBar.setPadding(new Insets(10, 0, 10, 0));
        statsBar.setStyle("-fx-background-color: #0d1117; -fx-border-color: #30363d; -fx-border-width: 0 0 1 0;");

        VBox container = new VBox(statsBar, chart);
        VBox.setVgrow(chart, javafx.scene.layout.Priority.ALWAYS);
        return container;
    }

    // ── Bottom bar ────────────────────────────────────────────────────────────

    private StackPane buildBottomBar() {
        Button startBtn     = buildBarButton("Start");
        Button stopBtn      = buildBarButton("Stop");
        Button saveBtn      = buildBarButton("Save");
        Button loadBtn      = buildBarButton("Load");
        Button greenBtn     = buildBarButton("Green");
        Button austerityBtn = buildBarButton("Austerity");
        Button fossilBtn    = buildBarButton("Fossil Fuel");

        timeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            tickCount++;
            controller.tick();
        }));
        timeline.setCycleCount(Timeline.INDEFINITE);

        Slider speedSlider = new Slider(0.5, 3.0, 1.0);
        speedSlider.setShowTickLabels(true);
        speedSlider.setMajorTickUnit(0.5);
        speedSlider.setValue(1.0);
        Label speedLabel = new Label("Speed: 1.0x");
        speedLabel.setStyle("-fx-text-fill: #e6edf3; -fx-font-size: 14px;");
        speedSlider.valueProperty().addListener((obs, old, val) -> {
            speedLabel.setText(String.format("Speed: %.1fx", val.doubleValue()));
            timeline.setRate(val.doubleValue());
        });

        stopBtn.setVisible(false);
        stopBtn.setManaged(false);

        startBtn.setOnAction(e -> {
            timeline.play();
            startBtn.setVisible(false);
            startBtn.setManaged(false);
            stopBtn.setVisible(true);
            stopBtn.setManaged(true);
        });
        stopBtn.setOnAction(e -> {
            timeline.pause();
            stopBtn.setVisible(false);
            stopBtn.setManaged(false);
            startBtn.setVisible(true);
            startBtn.setManaged(true);
        });
        greenBtn.setOnAction(e -> setActivePolicy(greenBtn, new GreenPolicy()));
        austerityBtn.setOnAction(e -> setActivePolicy(austerityBtn, new AusterityPolicy()));
        fossilBtn.setOnAction(e -> setActivePolicy(fossilBtn, new FossilFuelPolicy()));

        saveBtn.setOnAction(e -> {
            try {
                controller.save(tickCount);
                saveBtn.setText("✓ Salvato!");
                saveBtn.setStyle("-fx-background-color: #21262d; -fx-text-fill: #3fb950; -fx-font-size: 14px; -fx-border-color: #3fb950; -fx-border-width: 2px;");
                new Timeline(new KeyFrame(Duration.seconds(2.5), ev -> {
                    saveBtn.setText("Save");
                    saveBtn.setStyle("-fx-background-color: #21262d; -fx-text-fill: #e6edf3; -fx-font-size: 14px;");
                })).play();
            } catch (IOException ex) {
                showErrorAlert("Errore durante il salvataggio", ex.getMessage());
            }
        });

        loadBtn.setOnAction(e -> {
            try {
                List<Path> saves = controller.listSaves();
                if (saves.isEmpty()) {
                    showErrorAlert("Nessun salvataggio trovato", "La cartella 'saves/' è vuota o non esiste.");
                    return;
                }
                Path chosen;
                if (saves.size() == 1) {
                    chosen = saves.get(0);
                } else {
                    List<String> names = saves.stream()
                            .map(p -> p.getFileName().toString())
                            .toList();
                    ChoiceDialog<String> dialog = new ChoiceDialog<>(names.get(names.size() - 1), names);
                    dialog.setTitle("Carica partita");
                    dialog.setHeaderText("Scegli un salvataggio");
                    dialog.setContentText("File:");
                    applyDarkTheme(dialog);
                    Optional<String> result = dialog.showAndWait();
                    if (result.isEmpty()) return;
                    chosen = saves.stream()
                            .filter(p -> p.getFileName().toString().equals(result.get()))
                            .findFirst().orElseThrow();
                }
                timeline.pause();
                startBtn.setVisible(true);  startBtn.setManaged(true);
                stopBtn.setVisible(false);  stopBtn.setManaged(false);
                tickCount = 0;
                populationSeries.getData().clear();
                happinessSeries.getData().clear();
                healthSeries.getData().clear();
                pollutionSeries.getData().clear();
                int loadedTick = controller.load(chosen);
                tickCount = loadedTick;
                if (activeBtn != null) {
                    activeBtn.setStyle("-fx-background-color: #21262d; -fx-text-fill: #e6edf3; -fx-font-size: 14px;");
                    activeBtn = null;
                }
            } catch (IOException ex) {
                showErrorAlert("Errore durante il caricamento", ex.getMessage());
            }
        });

        HBox leftGroup   = new HBox(10, startBtn, stopBtn, saveBtn, loadBtn);
        HBox centerGroup = new HBox(10, greenBtn, austerityBtn, fossilBtn);
        HBox rightGroup  = new HBox(10, speedSlider, speedLabel);

        leftGroup.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        centerGroup.setAlignment(javafx.geometry.Pos.CENTER);
        rightGroup.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);

        // setPickOnBounds(false) → i click passano attraverso le aree trasparenti
        // di ogni HBox, evitando che il gruppo soprastante blocchi i click dei gruppi sotto
        leftGroup.setPickOnBounds(false);
        centerGroup.setPickOnBounds(false);
        rightGroup.setPickOnBounds(false);

        StackPane bar = new StackPane();
        bar.setPadding(new Insets(8, 16, 8, 16));
        bar.setStyle("-fx-background-color: #161b22; -fx-border-color: #30363d; -fx-border-width: 1 0 0 0;");

        StackPane.setAlignment(leftGroup,  javafx.geometry.Pos.CENTER_LEFT);
        StackPane.setAlignment(rightGroup, javafx.geometry.Pos.CENTER_RIGHT);

        bar.getChildren().addAll(centerGroup, leftGroup, rightGroup);
        return bar;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void showEarthquakeAlert() {
        XYChart.Series<Number, Number> markerSeries = new XYChart.Series<>();
        markerSeries.setName("🌍 Earthquake");
        markerSeries.getData().add(new XYChart.Data<>(tickCount, 0));
        markerSeries.getData().add(new XYChart.Data<>(tickCount, 110));
        chart.getData().add(markerSeries);

        Label alert = new Label("⚠️ EARTHQUAKE!");
        alert.setStyle("-fx-text-fill: #f38ba8; -fx-font-weight: bold;");
        leftPanel.getChildren().add(0, alert);

        new Timeline(new KeyFrame(Duration.seconds(3), e -> {
            leftPanel.getChildren().remove(alert);
        })).play();
    }

    private Button buildBarButton(String label) {
        Button btn = new Button(label);
        btn.setStyle("-fx-background-color: #21262d; -fx-text-fill: #e6edf3; -fx-font-size: 14px;");
        return btn;
    }

    private void setActivePolicy(Button btn, it.citylife.model.PolicyStrategy policy) {
        if (activeBtn != null)
            activeBtn.setStyle("-fx-background-color: #21262d; -fx-text-fill: #e6edf3; -fx-font-size: 14px;");
        activeBtn = btn;
        btn.setStyle("-fx-background-color: #21262d; -fx-text-fill: #e6edf3; -fx-font-size: 14px; -fx-border-color: #58a6ff; -fx-border-width: 2px;");
        controller.setPolicy(policy);
    }

    private void applyDarkTheme(javafx.scene.control.Dialog<?> dialog) {
        javafx.scene.control.DialogPane dp = dialog.getDialogPane();
        String css = getClass().getResource("/it/citylife/ui/dashboard.css").toExternalForm();
        dp.getStylesheets().add(css);
        dp.setStyle(
            "-fx-background-color: #161b22;" +
            "-fx-border-color: #30363d; -fx-border-width: 1px;"
        );
        if (dp.lookup(".header-panel") != null)
            dp.lookup(".header-panel").setStyle("-fx-background-color: #0d1117;");
        dp.lookupAll(".label").forEach(n ->
            n.setStyle("-fx-text-fill: #e6edf3; -fx-font-size: 13px;"));
        dp.lookupAll(".button").forEach(n ->
            n.setStyle("-fx-background-color: #21262d; -fx-text-fill: #e6edf3; -fx-font-size: 13px;"));
        dp.lookupAll(".combo-box").forEach(n ->
            n.setStyle("-fx-background-color: #21262d; -fx-text-fill: #e6edf3;"));
    }

    private void showErrorAlert(String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Errore");
        alert.setHeaderText(header);
        alert.setContentText(content);
        applyDarkTheme(alert);
        alert.showAndWait();
    }

    // ── Observer ──────────────────────────────────────────────────────────────

    @Override
    public void onStateChanged(CityState state) {
        Platform.runLater(() -> {
            tickLabel.setText("CityLogic  |  Tick: " + tickCount);
            budgetLabel.setText(String.format("Budget: %.0f",     state.getBudget()));
            populationLabel.setText("Population: " +              state.getPopulation());
            happinessLabel.setText(String.format("Happiness: %.1f", state.getHappiness()));
            healthLabel.setText(String.format("Health: %.1f",     state.getHealth()));
            pollutionLabel.setText(String.format("Pollution: %.1f", state.getPollution()));
            wasteLabel.setText("Waste: " +                        state.getWasteLevel());

            boolean powered = controller.hasPower();
            energyLabel.setText(powered ? "Power: OK" : "Power: BLACKOUT");
            energyLabel.setStyle("-fx-text-fill: " + (powered ? "#3fb950" : "#f85149") + "; -fx-font-size: 14px; -fx-font-weight: bold;");

            dashPopLabel.setText("Population: "  + state.getPopulation());
            dashHapLabel.setText(String.format("Happiness: %.1f", state.getHappiness()));
            dashHealthLabel.setText(String.format("Health: %.1f", state.getHealth()));
            dashPollLabel.setText(String.format("Pollution: %.1f", state.getPollution()));

            populationSeries.getData().add(new XYChart.Data<>(tickCount, state.getPopulation() / 1000.0));
            happinessSeries.getData().add(new XYChart.Data<>(tickCount, state.getHappiness()));
            healthSeries.getData().add(new XYChart.Data<>(tickCount, state.getHealth()));
            pollutionSeries.getData().add(new XYChart.Data<>(tickCount, state.getPollution()));

            if (state.isEarthquakeOccurred()) {
                showEarthquakeAlert();
            }

            updateGrid();
        });
    }
}
