package it.citylife.ui;

import it.citylife.model.AusterityPolicy;
import it.citylife.model.CityState;
import it.citylife.model.FossilFuelPolicy;
import it.citylife.model.GreenPolicy;
import it.citylife.model.PopulationGroup;
import it.citylife.model.StateObserver;
import it.citylife.model.Structure;
import it.citylife.model.StructureDecorator;
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
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.Slider;
import javafx.scene.control.Tab;
import javafx.scene.control.Tooltip;
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
    private Stage primaryStage;
    private Timeline timeline;
    private int tickCount = 0;
    private static final int AUTOSAVE_EVERY_TICKS = 5;
    private boolean budgetWasNegative = false;

    // Labels
    private Label budgetLabel;
    private Label populationLabel;
    private Label happinessLabel;
    private Label healthLabel;
    private Label pollutionLabel;
    private Label wasteLabel;
    private Label jobSatLabel;
    private Label healthSatLabel;
    private Label safetySatLabel;
    private Label energyLabel;
    private Label tickLabel;

    // Policy highlight
    private Button activeBtn;
    private String activePolicyName = "Default";

    // Build tool
    private String selectedTool = null;
    private Button activeBuildBtn = null;

    // Panels
    private VBox leftPanel;
    private VBox logPanel; // Nuovo pannello per i log sulla destra
    private StackPane[][] cells = new StackPane[20][20];

    // Chart
    private LineChart<Number, Number> chart;

    // Dashboard summary labels
    private Label dashPopLabel;
    private Label dashHapLabel;
    private Label dashHealthLabel;
    private Label dashPollLabel;

    // Chart series
    private XYChart.Series<Number, Number> populationSeries;
    private XYChart.Series<Number, Number> happinessSeries;
    private XYChart.Series<Number, Number> healthSeries;
    private XYChart.Series<Number, Number> pollutionSeries;
    private double populationDivisor = 10.0;

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
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
        } catch (Exception e) { /* fallback */ }

        primaryStage.show();

        showStartupDialog();
    }

    // ── Tab 1: City Map ──────────────────────────────────────────────────────

    private BorderPane buildMapView() {
        BorderPane mapPane = new BorderPane();
        leftPanel = buildLeftPanel();
        mapPane.setLeft(leftPanel);

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
        wasteLabel      = makeMetricLabel("Waste: " +                        controller.getState().getWasteLevel(),  FontAwesomeSolid.TRASH,       "#8b949e");
        jobSatLabel    = makeMetricLabel("Job Sat.: 50%",    FontAwesomeSolid.BRIEFCASE,    "#60a5fa");
        healthSatLabel = makeMetricLabel("Health Sat.: 50%", FontAwesomeSolid.NOTES_MEDICAL, "#34d399");
        safetySatLabel = makeMetricLabel("Safety Sat.: 50%", FontAwesomeSolid.SHIELD_ALT,    "#a78bfa");

        boolean powered = controller.hasPower();
        FontIcon boltIcon = new FontIcon(FontAwesomeSolid.BOLT);
        boltIcon.setIconSize(14);
        boltIcon.setIconColor(Color.web(powered ? "#3fb950" : "#f85149"));
        energyLabel = new Label(powered ? "Power: OK" : "Power: BLACKOUT", boltIcon);
        energyLabel.setStyle("-fx-text-fill: " + (powered ? "#3fb950" : "#f85149") + "; -fx-font-size: 14px; -fx-font-weight: bold;");

        Label logTitle = new Label("NOTIFICATIONS");
        logTitle.setStyle("-fx-text-fill: #8b949e; -fx-font-size: 10px; -fx-font-weight: bold;");
        
        logPanel = new VBox(5);
        logPanel.setPadding(new Insets(5, 0, 0, 0));

        VBox vbox = new VBox(10,
            metricsTitle,
            budgetLabel, populationLabel, happinessLabel,
            healthLabel, pollutionLabel, wasteLabel,
            jobSatLabel, healthSatLabel, safetySatLabel,
            new Separator(),
            energyLabel,
            new Separator(),
            logTitle,
            logPanel
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
        Button hospBtn  = buildToolButton("Hospital",    "HOSPITAL",    FontAwesomeSolid.HOSPITAL, colorForType(StructureType.HOSPITAL));
        Button wasteBtn = buildToolButton("Waste Center","WASTE_CENTER",FontAwesomeSolid.TRASH,    colorForType(StructureType.WASTE_CENTER));
        Button roadBtn  = buildToolButton("Road",        "ROAD",        FontAwesomeSolid.ROAD,     colorForType(StructureType.ROAD));
        Button repairBtn = buildToolButton("Repair",      "REPAIR",      FontAwesomeSolid.WRENCH,   Color.web("#a3e635"));
        Button demolBtn = buildToolButton("Demolish",    "DEMOLISH",    FontAwesomeSolid.HAMMER,   Color.web("#f38ba8"));

        // Pulsante "Repair All" con logica custom
        FontIcon raIcon = new FontIcon(FontAwesomeSolid.TOOLS);
        raIcon.setIconSize(14);
        raIcon.setIconColor(Color.web("#a3e635"));
        Button repairAllBtn = new Button("Repair All", raIcon);
        repairAllBtn.setMaxWidth(Double.MAX_VALUE);
        repairAllBtn.setMinHeight(32);
        repairAllBtn.setStyle("-fx-background-color: #21262d; -fx-text-fill: #e6edf3; -fx-font-size: 12px;");
        repairAllBtn.setOnAction(e -> showRepairAllPreview());

        Label upgradeTitle = new Label("UPGRADE");
        upgradeTitle.setStyle("-fx-text-fill: #8b949e; -fx-font-size: 10px; -fx-font-weight: bold;");

        Button seismicBtn = buildToolButton("Seismic (500)", "UPGRADE_SEISMIC",
                FontAwesomeSolid.SHIELD_ALT, Color.web("#38bdf8"));
        Button wasteThermalBtn = buildToolButton("Waste Thermal (700)", "UPGRADE_WASTE_THERMAL",
                FontAwesomeSolid.FIRE, Color.web("#f97316"));

        VBox vbox = new VBox(8,
            buildTitle,
            resBtn, indBtn, comBtn, ppBtn, parkBtn, hospBtn, wasteBtn, roadBtn,
            new Separator(), repairBtn, repairAllBtn, demolBtn,
            new Separator(), upgradeTitle, seismicBtn, wasteThermalBtn
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
        boolean ok = false;
        
        try {
            if (selectedTool.equals("DEMOLISH")) {
                ok = controller.demolish(x, y);
            } else if (selectedTool.equals("REPAIR")) {
                ok = controller.repair(x, y);
                if (ok) {
                    logMessage("Building repaired", "#3fb950");
                } else {
                    logMessage("⚠️ Cannot repair (insufficient funds or HP already at max)", "#f85149");
                }
            } else if (selectedTool.equals("UPGRADE_SEISMIC")) {
                ok = controller.upgrade(x, y, "SEISMIC");
                if (ok) {
                    logMessage("Seismic Upgrade applied (-500$)", "#38bdf8");
                } else {
                    logMessage("⚠️ Upgrade failed (max level, insufficient funds, or empty cell)", "#f85149");
                }
            } else if (selectedTool.equals("UPGRADE_WASTE_THERMAL")) {
                ok = controller.upgrade(x, y, "WASTE_THERMAL");
                if (ok) {
                    logMessage("Waste Thermal Upgrade applied (-700$)", "#f97316");
                } else {
                    logMessage("⚠️ Upgrade failed (max level, insufficient funds, or empty cell)", "#f85149");
                }
            } else {
                ok = controller.placeBuilding(selectedTool, x, y);
            }
        } catch (Exception ex) {
            ok = false;
        }

        boolean upgradeOrRepair = selectedTool.equals("REPAIR")
                || selectedTool.equals("UPGRADE_SEISMIC")
                || selectedTool.equals("UPGRADE_WASTE_THERMAL");
        if (!ok && !upgradeOrRepair) {
            logMessage("⚠️ Cannot perform this action!", "#f9e64f");
        }
        
        updateGrid();
        refreshMetricsDisplay();
    }

    private void logMessage(String text, String color) {
        if (logPanel == null) return;
        Label msg = new Label(text);
        msg.setStyle("-fx-text-fill: " + color + "; -fx-font-weight: bold; -fx-font-size: 11px;");
        msg.setWrapText(true);
        logPanel.getChildren().add(0, msg);
        
        // Rimuove il messaggio dopo 4 secondi
        new Timeline(new KeyFrame(Duration.seconds(4), e -> logPanel.getChildren().remove(msg))).play();
    }

    private void showRepairAllPreview() {
        int totalCost = 0;
        var grid = controller.getGrid();
        for (int x = 0; x < grid.getWidth(); x++) {
            for (int y = 0; y < grid.getHeight(); y++) {
                var cell = grid.getCell(x, y);
                if (cell != null && cell.getStructure() instanceof Structure s) {
                    if (!s.isDestroyed() && s.getHp() < s.getMaxHp()) {
                        totalCost += (s.getMaxHp() - s.getHp()) * 2;
                    }
                }
            }
        }
        
        if (totalCost == 0) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION, "No buildings need repairs.");
            alert.setHeaderText(null);
            applyDarkTheme(alert);
            alert.showAndWait();
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Repair All");
        alert.setHeaderText("Estimated cost: " + totalCost + " $");
        alert.setContentText("Proceed with repairing all damaged buildings?");
        applyDarkTheme(alert);
        
        Optional<javafx.scene.control.ButtonType> res = alert.showAndWait();
        if (res.isPresent() && res.get() == javafx.scene.control.ButtonType.OK) {
            if (controller.getState().getBudget() >= totalCost) {
                for (int x = 0; x < grid.getWidth(); x++) {
                    for (int y = 0; y < grid.getHeight(); y++) {
                        var cell = grid.getCell(x, y);
                        if (cell != null && cell.getStructure() instanceof Structure s) {
                            if (!s.isDestroyed() && s.getHp() < s.getMaxHp()) {
                                s.fullRepair();
                            }
                        }
                    }
                }
                controller.getState().updateBudget(-totalCost);
                updateGrid();
                refreshMetricsDisplay();
                logMessage("Global repair completed (-" + totalCost + "$)", "#3fb950");
            } else {
                showErrorAlert("Insufficient funds", "You need " + totalCost + "$ to repair everything.");
            }
        }
    }

    private void refreshMetricsDisplay() {
        CityState s = controller.getState();
        budgetLabel.setText(String.format("Budget: %.0f",    s.getBudget()));
        populationLabel.setText("Population: " +             s.getPopulation());
        happinessLabel.setText(String.format("Happiness: %.1f", s.getHappiness()));
        healthLabel.setText(String.format("Health: %.1f",    s.getHealth()));
        pollutionLabel.setText(String.format("Pollution: %.1f", s.getPollution()));
        wasteLabel.setText("Waste: " +                        s.getWasteLevel());
        PopulationGroup pg = s.getPopulationGroup();
        jobSatLabel.setText(String.format("Job Sat.: %.0f%%",    pg.getJobSatisfaction()));
        healthSatLabel.setText(String.format("Health Sat.: %.0f%%", pg.getHealthSatisfaction()));
        safetySatLabel.setText(String.format("Safety Sat.: %.0f%%", pg.getSafetySatisfaction()));
        boolean powered = controller.hasPower();
        energyLabel.setText(powered ? "Power: OK" : "Power: BLACKOUT");
        energyLabel.setStyle("-fx-text-fill: " + (powered ? "#3fb950" : "#f85149") + "; -fx-font-size: 14px; -fx-font-weight: bold;");
        applyMetricAlerts(s);
    }

    private void applyMetricAlerts(CityState s) {
        budgetLabel.setStyle("-fx-text-fill: "    + (s.getBudget()     <  500 ? "#f85149" : "#facc15") + "; -fx-font-size: 14px;");
        happinessLabel.setStyle("-fx-text-fill: " + (s.getHappiness()  <   25 ? "#f85149" : "#fb923c") + "; -fx-font-size: 14px;");
        healthLabel.setStyle("-fx-text-fill: "    + (s.getHealth()     <   25 ? "#f85149" : "#f472b6") + "; -fx-font-size: 14px;");
        pollutionLabel.setStyle("-fx-text-fill: " + (s.getPollution()  >   75 ? "#f85149" : "#4ade80") + "; -fx-font-size: 14px;");
    }

    private boolean isPowered(int rx, int ry) {
        it.citylife.model.Grid grid = controller.getGrid();
        for (int px = 0; px < grid.getWidth(); px++) {
            for (int py = 0; py < grid.getHeight(); py++) {
                it.citylife.model.Cell pc = grid.getCell(px, py);
                if (pc != null && pc.getStructure() instanceof it.citylife.model.PowerPlant) {
                    if (Math.max(Math.abs(px - rx), Math.abs(py - ry)) <= 5) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void updateGrid() {
        for (int x = 0; x < 20; x++) {
            for (int y = 0; y < 20; y++) {
                StackPane cell = cells[x][y];
                cell.getChildren().clear();

                // Rimuove tooltip eventualmente installato al tick precedente
                Tooltip oldTip = (Tooltip) cell.getProperties().get("cellTooltip");
                if (oldTip != null) {
                    Tooltip.uninstall(cell, oldTip);
                    cell.getProperties().remove("cellTooltip");
                }

                // Rimuove eventuali event handler per l'hover impostati nei tick precedenti
                cell.setOnMouseEntered(null);
                cell.setOnMouseExited(null);
                
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

                    // Badge upgrade level (stellina in alto a sinistra)
                    if (s instanceof StructureDecorator dec) {
                        FontIcon upgIcon = new FontIcon(FontAwesomeSolid.STAR);
                        upgIcon.setIconSize(8);
                        upgIcon.setIconColor(Color.web("#fde047"));
                        Label upgLabel = new Label("" + dec.getUpgradeLevel(), upgIcon);
                        upgLabel.setStyle("-fx-text-fill: #fde047; -fx-font-size: 8px;");
                        StackPane.setAlignment(upgLabel, javafx.geometry.Pos.TOP_LEFT);
                        cell.getChildren().add(upgLabel);
                    }

                    // Barra HP sempre visibile quando l'edificio è danneggiato
                    if (s.getHp() < s.getMaxHp() && s.getHp() > 0) {
                        Rectangle hpBarBg = new Rectangle(30, 4, Color.web("#f85149"));
                        Rectangle hpBar = new Rectangle(30 * ((double)s.getHp() / s.getMaxHp()), 4, Color.web("#3fb950"));
                        VBox hpContainer = new VBox(new StackPane(hpBarBg, hpBar));
                        hpContainer.setAlignment(javafx.geometry.Pos.BOTTOM_CENTER);
                        hpContainer.setPadding(new Insets(0,0,2,0));
                        cell.getChildren().add(hpContainer);
                    }

                    // Tooltip con tutte le statistiche al passaggio del mouse
                    Tooltip tt = buildCellTooltip(s);
                    Tooltip.install(cell, tt);
                    cell.getProperties().put("cellTooltip", tt);

                    // Indicatore mancanza di corrente per QUALSIASI edificio che la richiede
                    boolean requiresPower = (s.getType() == StructureType.RESIDENTIAL ||
                                             s.getType() == StructureType.COMMERCIAL ||
                                             s.getType() == StructureType.INDUSTRIAL ||
                                             s.getType() == StructureType.HOSPITAL ||
                                             s.getType() == StructureType.WASTE_CENTER);

                    if (requiresPower && !isPowered(x, y)) {
                        FontIcon warn = new FontIcon(FontAwesomeSolid.EXCLAMATION_TRIANGLE);
                        warn.setIconSize(10);
                        warn.setIconColor(Color.web("#facc15")); // Giallo
                        StackPane.setAlignment(warn, javafx.geometry.Pos.TOP_RIGHT);
                        cell.getChildren().add(warn);
                        cell.setStyle("-fx-background-color: " + hex + "22; -fx-border-color: #f85149; -fx-border-width: 1;"); // Bordo rosso
                    }
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
            case HOSPITAL     -> FontAwesomeSolid.HOSPITAL;
            case WASTE_CENTER -> FontAwesomeSolid.TRASH;
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
            case HOSPITAL     -> Color.web("#f87171");
            case WASTE_CENTER -> Color.web("#a78bfa");
        };
    }

    private String labelForType(StructureType type) {
        return switch (type) {
            case RESIDENTIAL  -> "Residential";
            case INDUSTRIAL   -> "Industrial";
            case COMMERCIAL   -> "Commercial";
            case POWER_PLANT  -> "Power Plant";
            case PARK         -> "Park";
            case ROAD         -> "Road";
            case HOSPITAL     -> "Hospital";
            case WASTE_CENTER -> "Waste Center";
        };
    }

    private Tooltip buildCellTooltip(Structure s) {
        StringBuilder sb = new StringBuilder();
        sb.append(labelForType(s.getType())).append("\n");
        sb.append("HP: ").append(s.getHp()).append(" / ").append(s.getMaxHp());
        if (s.isDestroyed()) sb.append("  [DESTROYED]");
        sb.append("\n");
        sb.append("Powered: ").append(s.isPowered() ? "Yes" : "No").append("\n");
        sb.append("Adjacent road: ").append(s.isConnectedToRoad() ? "Yes" : "No").append("\n");
        if (s instanceof StructureDecorator dec) {
            List<String> upgrades = dec.collectUpgrades();
            sb.append("Upgrade Lv.").append(dec.getUpgradeLevel());
            if (!upgrades.isEmpty()) sb.append(": ").append(String.join(", ", upgrades));
            sb.append("\n");
        }
        sb.append("Build cost: ").append(s.getConstructionCost());
        Tooltip tt = new Tooltip(sb.toString());
        tt.setStyle(
            "-fx-background-color: #161b22;" +
            "-fx-text-fill: #e6edf3;" +
            "-fx-font-size: 12px;" +
            "-fx-padding: 8px;" +
            "-fx-border-color: #30363d;" +
            "-fx-border-width: 1px;"
        );
        tt.setShowDelay(Duration.millis(150));
        return tt;
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

        populationSeries = new XYChart.Series<>(); populationSeries.setName("Population (×10)");
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
        Button nextTickBtn  = buildBarButton("⏭ Tick");
        Button saveBtn      = buildBarButton("Save");
        Button loadBtn      = buildBarButton("Load");
        Button defaultBtn   = buildBarButton("Default");
        Button greenBtn     = buildBarButton("Green");
        Button austerityBtn = buildBarButton("Austerity");
        Button fossilBtn    = buildBarButton("Fossil Fuel");

        timeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            tickCount++;
            controller.tick();
            if (tickCount % AUTOSAVE_EVERY_TICKS == 0) {
                try {
                    controller.save(tickCount);
                    logMessage("💾 Autosave (tick " + tickCount + ")", "#58a6ff");
                } catch (IOException ex) {
                    logMessage("⚠️ Autosave failed!", "#f85149");
                }
            }
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
        nextTickBtn.setOnAction(e -> {
            tickCount++;
            controller.tick();
            if (tickCount % AUTOSAVE_EVERY_TICKS == 0) {
                try {
                    controller.save(tickCount);
                    logMessage("💾 Autosave (tick " + tickCount + ")", "#58a6ff");
                } catch (IOException ex) {
                    logMessage("⚠️ Autosave failed!", "#f85149");
                }
            }
        });

        defaultBtn.setOnAction(e -> setActivePolicy(defaultBtn, new it.citylife.model.DefaultPolicy()));
        greenBtn.setOnAction(e -> setActivePolicy(greenBtn, new GreenPolicy()));
        austerityBtn.setOnAction(e -> setActivePolicy(austerityBtn, new AusterityPolicy()));
        fossilBtn.setOnAction(e -> setActivePolicy(fossilBtn, new FossilFuelPolicy()));

        saveBtn.setOnAction(e -> {
            try {
                controller.save(tickCount);
                saveBtn.setText("✓ Saved!");
                saveBtn.setStyle("-fx-background-color: #21262d; -fx-text-fill: #3fb950; -fx-font-size: 14px; -fx-border-color: #3fb950; -fx-border-width: 2px;");
                new Timeline(new KeyFrame(Duration.seconds(2.5), ev -> {
                    saveBtn.setText("Save");
                    saveBtn.setStyle("-fx-background-color: #21262d; -fx-text-fill: #e6edf3; -fx-font-size: 14px;");
                })).play();
            } catch (IOException ex) {
                showErrorAlert("Save error", ex.getMessage());
            }
        });

        loadBtn.setOnAction(e -> {
            try {
                List<Path> saves = controller.listSaves();
                if (saves.isEmpty()) {
                    showErrorAlert("No saves found", "The 'saves/' folder is empty or does not exist.");
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
                    dialog.setTitle("Load game");
                    dialog.setHeaderText("Choose a save file");
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
                showErrorAlert("Load error", ex.getMessage());
            }
        });

        HBox leftGroup   = new HBox(10, startBtn, stopBtn, nextTickBtn, saveBtn, loadBtn);
        HBox centerGroup = new HBox(10, defaultBtn, greenBtn, austerityBtn, fossilBtn);
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

        logMessage("⚠️ EARTHQUAKE!", "#f38ba8");
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
        String newName = policy.getClass().getSimpleName().replace("Policy", "");
        logMessage("Policy " + activePolicyName + " deactivated — " + newName + " now active.", "#8b949e");
        activePolicyName = newName;
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

    private void showStartupDialog() {
        List<Path> saves;
        try {
            saves = controller.listSaves();
        } catch (IOException ex) {
            return;
        }

        if (saves.isEmpty()) {
            ButtonType startType = new ButtonType("Start", ButtonBar.ButtonData.OK_DONE);
            Alert welcome = new Alert(Alert.AlertType.INFORMATION);
            welcome.setTitle("CityLogic");
            welcome.setHeaderText("Welcome to CityLogic!");
            welcome.setContentText("No saves found. Press Start to begin!");
            welcome.getButtonTypes().setAll(startType);
            welcome.setGraphic(null);
            welcome.initOwner(this.primaryStage);
            applyDarkTheme(welcome);
            welcome.setOnShown(evt -> Platform.runLater(() -> {
                javafx.scene.control.DialogPane dp = welcome.getDialogPane();
                javafx.scene.Node headerLabel = dp.lookup(".header-panel .label");
                if (headerLabel != null)
                    headerLabel.setStyle("-fx-text-fill: #e6edf3; -fx-font-size: 14px; -fx-font-weight: bold;");
                Button b = (Button) dp.lookupButton(startType);
                if (b != null) b.setStyle("-fx-background-color: #21262d; -fx-text-fill: #e6edf3; -fx-font-size: 13px;");
            }));
            welcome.showAndWait();
            return;
        }

        Path latest = saves.get(saves.size() - 1);

        ButtonType newGameType  = new ButtonType("New Game",   ButtonBar.ButtonData.LEFT);
        ButtonType loadGameType = new ButtonType("Load Save",  ButtonBar.ButtonData.RIGHT);

        Alert dialog = new Alert(Alert.AlertType.CONFIRMATION);
        dialog.setTitle("CityLogic");
        dialog.setHeaderText("Welcome to CityLogic!");
        dialog.setContentText("Start a new game or resume from the last save?");
        dialog.getButtonTypes().setAll(newGameType, loadGameType);
        dialog.setGraphic(null);
        dialog.initOwner(this.primaryStage);
        applyDarkTheme(dialog);
        dialog.setOnShown(evt -> Platform.runLater(() -> {
            javafx.scene.control.DialogPane dp = dialog.getDialogPane();
            javafx.scene.Node headerLabel = dp.lookup(".header-panel .label");
            if (headerLabel != null)
                headerLabel.setStyle("-fx-text-fill: #e6edf3; -fx-font-size: 14px; -fx-font-weight: bold;");
            String btnStyle = "-fx-background-color: #21262d; -fx-text-fill: #e6edf3; -fx-font-size: 13px;";
            Button b1 = (Button) dp.lookupButton(newGameType);
            Button b2 = (Button) dp.lookupButton(loadGameType);
            if (b1 != null) b1.setStyle(btnStyle);
            if (b2 != null) b2.setStyle(btnStyle);
        }));

        Optional<ButtonType> result = dialog.showAndWait();

        if (result.isEmpty() || result.get() != loadGameType) return;

        try {
            tickCount = controller.load(latest);
            logMessage("💾 Game restored: " + latest.getFileName(), "#58a6ff");
        } catch (IOException ex) {
            showErrorAlert("Errore durante il caricamento", ex.getMessage());
        }
    }

    private void showErrorAlert(String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
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
            wasteLabel.setText("Waste: " +                         state.getWasteLevel());
            PopulationGroup pg2 = state.getPopulationGroup();
            jobSatLabel.setText(String.format("Job Sat.: %.0f%%",    pg2.getJobSatisfaction()));
            healthSatLabel.setText(String.format("Health Sat.: %.0f%%", pg2.getHealthSatisfaction()));
            safetySatLabel.setText(String.format("Safety Sat.: %.0f%%", pg2.getSafetySatisfaction()));

            boolean powered = controller.hasPower();
            energyLabel.setText(powered ? "Power: OK" : "Power: BLACKOUT");
            energyLabel.setStyle("-fx-text-fill: " + (powered ? "#3fb950" : "#f85149") + "; -fx-font-size: 14px; -fx-font-weight: bold;");

            dashPopLabel.setText("Population: "  + state.getPopulation());
            dashHapLabel.setText(String.format("Happiness: %.1f", state.getHappiness()));
            dashHealthLabel.setText(String.format("Health: %.1f", state.getHealth()));
            dashPollLabel.setText(String.format("Pollution: %.1f", state.getPollution()));

            double newDivisor = (state.getPopulation() > 1000) ? 100.0 : 10.0;
            if (newDivisor != populationDivisor) {
                double ratio = populationDivisor / newDivisor;
                populationSeries.getData().forEach(d -> d.setYValue(d.getYValue().doubleValue() * ratio));
                populationDivisor = newDivisor;
                populationSeries.setName("Population (×" + (int) populationDivisor + ")");
            }
            populationSeries.getData().add(new XYChart.Data<>(tickCount, state.getPopulation() / populationDivisor));
            happinessSeries.getData().add(new XYChart.Data<>(tickCount, state.getHappiness()));
            healthSeries.getData().add(new XYChart.Data<>(tickCount, state.getHealth()));
            pollutionSeries.getData().add(new XYChart.Data<>(tickCount, state.getPollution()));

            if (state.isEarthquakeOccurred()) {
                showEarthquakeAlert();
            }

            if (state.getCriticalBuildingCount() > 0) {
                logMessage("⚠️ " + state.getCriticalBuildingCount() + " building(s) in critical condition! (HP < 20%)", "#f9e64f");
            }

            if (state.getBudget() < 0 && !budgetWasNegative) {
                logMessage("🔴 NEGATIVE BUDGET! The city is in deficit.", "#f85149");
                budgetWasNegative = true;
            } else if (state.getBudget() >= 0) {
                budgetWasNegative = false;
            }

            applyMetricAlerts(state);
            updateGrid();
        });
    }
}