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

/**
 * Vista principale dell'applicazione CityLogic (JavaFX Application).
 *
 * Implementa {@link StateObserver}: viene notificata da City al termine di ogni tick
 * tramite onStateChanged(), che aggiorna tutte le label, il grafico e la griglia
 * sull'Application Thread tramite Platform.runLater().
 *
 * La UI è strutturata in tre aree principali:
 *   - Pannello sinistro (buildLeftPanel): strumenti di costruzione, riparazione e upgrade
 *   - Area centrale (buildMapView): griglia 20×20 con celle cliccabili
 *   - Pannello destro (buildMetricsPanel): metriche della città e notifiche
 *   - Barra inferiore (buildBottomBar): controlli della simulazione (start/stop/tick, politiche, save/load)
 *
 * La simulazione avanza tramite un {@link Timeline} JavaFX con intervallo di 1 secondo
 * (modificabile via slider). L'autosave avviene ogni AUTOSAVE_EVERY_TICKS tick.
 *
 * @see SimulationController
 * @see StateObserver
 * @see CityState
 */
public class DashboardView extends Application implements StateObserver {

    // Controller facade verso il modello di dominio
    private SimulationController controller;

    // Stage principale della finestra JavaFX
    private Stage primaryStage;

    // Timeline che scandisce i tick automatici della simulazione
    private Timeline timeline;

    // Contatore dei tick avanzati dall'avvio o dall'ultimo caricamento
    private int tickCount = 0;

    // Frequenza dell'autosave: ogni 5 tick viene salvato automaticamente
    private static final int AUTOSAVE_EVERY_TICKS = 5;

    // Flag per evitare notifiche ripetute di budget negativo ogni tick
    private boolean budgetWasNegative = false;

    // --- Label del pannello metriche (destra) ---
    private Label budgetLabel;
    private Label populationLabel;
    private Label happinessLabel;
    private Label healthLabel;
    private Label pollutionLabel;
    private Label wasteLabel;
    private Label jobSatLabel;      // Soddisfazione lavorativa (AC-19.5)
    private Label healthSatLabel;   // Soddisfazione sanitaria (AC-19.5)
    private Label safetySatLabel;   // Soddisfazione sicurezza (AC-19.5)
    private Label energyLabel;
    private Label tickLabel;

    // Pulsante della politica attiva (evidenziato con bordo blu)
    private Button activeBtn;
    private String activePolicyName = "Default";

    // Strumento di costruzione attualmente selezionato (es. "RESIDENTIAL", "DEMOLISH")
    private String selectedTool = null;

    // Pulsante dello strumento di costruzione attivo (evidenziato con bordo blu)
    private Button activeBuildBtn = null;

    // Pannello sinistro con i pulsanti di costruzione
    private VBox leftPanel;

    // Pannello notifiche sulla destra (messaggi temporanei)
    private VBox logPanel;

    // Matrice di StackPane che rappresenta visivamente la griglia 20×20
    private StackPane[][] cells = new StackPane[20][20];

    // Grafico a linee nella tab Dashboard
    private LineChart<Number, Number> chart;

    // Label di riepilogo nella Dashboard (sopra al grafico)
    private Label dashPopLabel;
    private Label dashHapLabel;
    private Label dashHealthLabel;
    private Label dashPollLabel;

    // Serie dati per il grafico storico delle metriche
    private XYChart.Series<Number, Number> populationSeries;
    private XYChart.Series<Number, Number> happinessSeries;
    private XYChart.Series<Number, Number> healthSeries;
    private XYChart.Series<Number, Number> pollutionSeries;

    // Divisore per la serie popolazione: scala automaticamente per mantenerla nel range [0,110]
    private double populationDivisor = 10.0;

    // Label rossa che appare sul grafico quando avviene un terremoto
    private Label earthquakeNotifLabel;

    /**
     * Punto di ingresso JavaFX: costruisce e mostra la finestra principale.
     * Inizializza il SimulationController, registra DashboardView come Observer,
     * costruisce il layout e mostra il dialogo di avvio (nuovo gioco / carica).
     *
     * @param primaryStage lo Stage principale fornito dal runtime JavaFX
     */
    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        controller = new SimulationController();
        controller.addObserver(this);

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #0d1117;");

        // Header superiore con titolo e contatore tick
        FontIcon headerIcon = new FontIcon(FontAwesomeSolid.CITY);
        headerIcon.setIconSize(18);
        headerIcon.setIconColor(Color.web("#58a6ff"));
        tickLabel = new Label("CityLogic  |  Tick: 0", headerIcon);
        tickLabel.setStyle("-fx-text-fill: #e6edf3; -fx-font-size: 16px; -fx-font-weight: bold; -fx-padding: 12px 16px; -fx-border-color: #30363d; -fx-border-width: 0 0 1 0;");
        root.setTop(tickLabel);

        // Layout a schede: City Map e Dashboard
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

        // Icona della finestra (fallback silenzioso se non trovata)
        primaryStage.getIcons().clear();
        try (var stream = getClass().getResourceAsStream("/it/citylife/ui/icon.png")) {
            if (stream != null) primaryStage.getIcons().add(new javafx.scene.image.Image(stream));
        } catch (Exception e) { /* fallback */ }

        primaryStage.show();

        // Dialogo di benvenuto: nuovo gioco o carica l'ultimo salvataggio (AC-12.2)
        showStartupDialog();
    }

    // ── Tab 1: City Map ──────────────────────────────────────────────────────

    /**
     * Costruisce la vista della mappa: pannello sinistro + griglia centrale.
     *
     * @return un BorderPane con il pannello strumenti a sinistra e la griglia al centro
     */
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

    /**
     * Crea una label con icona FontAwesome colorata, usata nel pannello metriche.
     *
     * @param text     testo iniziale della label
     * @param icon     icona FontAwesome da mostrare
     * @param hexColor colore esadecimale del testo e dell'icona
     * @return la label configurata
     */
    private Label makeMetricLabel(String text, FontAwesomeSolid icon, String hexColor) {
        FontIcon fi = new FontIcon(icon);
        fi.setIconSize(14);
        fi.setIconColor(Color.web(hexColor));
        Label lbl = new Label(text, fi);
        lbl.setStyle("-fx-text-fill: " + hexColor + "; -fx-font-size: 14px;");
        return lbl;
    }

    /**
     * Costruisce il pannello delle metriche sulla destra della finestra.
     * Contiene le label delle metriche principali, l'indicatore energetico
     * e il pannello delle notifiche temporanee.
     *
     * @return un VBox con tutte le metriche e il log delle notifiche
     */
    private VBox buildMetricsPanel() {
        Label metricsTitle = new Label("METRICS");
        metricsTitle.setStyle("-fx-text-fill: #8b949e; -fx-font-size: 10px; -fx-font-weight: bold;");

        budgetLabel     = makeMetricLabel(String.format("Budget: %.0f",     controller.getState().getBudget()),     FontAwesomeSolid.COINS,       "#facc15");
        populationLabel = makeMetricLabel("Population: " +                  controller.getState().getPopulation(),  FontAwesomeSolid.USERS,       "#e6edf3");
        happinessLabel  = makeMetricLabel(String.format("Happiness: %.1f",  controller.getState().getHappiness()),  FontAwesomeSolid.SMILE,       "#fb923c");
        healthLabel     = makeMetricLabel(String.format("Health: %.1f",     controller.getState().getHealth()),     FontAwesomeSolid.HEART,       "#f472b6");
        pollutionLabel  = makeMetricLabel(String.format("Pollution: %.1f",  controller.getState().getPollution()),  FontAwesomeSolid.SMOG,        "#4ade80");
        wasteLabel      = makeMetricLabel("Waste: " +                        controller.getState().getWasteLevel(),  FontAwesomeSolid.TRASH,       "#8b949e");

        // Label soddisfazioni demografiche (AC-19.5)
        jobSatLabel    = makeMetricLabel("Job Sat.: 50%",    FontAwesomeSolid.BRIEFCASE,    "#60a5fa");
        healthSatLabel = makeMetricLabel("Health Sat.: 50%", FontAwesomeSolid.NOTES_MEDICAL, "#34d399");
        safetySatLabel = makeMetricLabel("Safety Sat.: 50%", FontAwesomeSolid.SHIELD_ALT,    "#a78bfa");

        // Indicatore rete elettrica: verde se OK, rosso se blackout
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

    /**
     * Costruisce il pannello sinistro con i pulsanti di costruzione, riparazione e upgrade.
     * Ogni pulsante imposta selectedTool al tipo corrispondente; il click sulla griglia
     * esegue l'azione tramite onCellClick().
     *
     * @return un VBox con tutti i pulsanti degli strumenti
     */
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
        Button repairBtn = buildToolButton("Repair",     "REPAIR",      FontAwesomeSolid.WRENCH,   Color.web("#a3e635"));
        Button demolBtn = buildToolButton("Demolish",    "DEMOLISH",    FontAwesomeSolid.HAMMER,   Color.web("#f38ba8"));

        // Pulsante "Repair All": calcola il costo totale e chiede conferma prima di riparare
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

    /**
     * Crea un pulsante strumento che, quando cliccato, imposta selectedTool
     * e si evidenzia con un bordo blu (rimuovendo l'evidenziazione dal precedente).
     *
     * @param label     testo del pulsante
     * @param tool      valore di selectedTool da impostare al click
     * @param icon      icona FontAwesome
     * @param iconColor colore dell'icona
     * @return il pulsante configurato
     */
    private Button buildToolButton(String label, String tool, FontAwesomeSolid icon, Color iconColor) {
        FontIcon fi = new FontIcon(icon);
        fi.setIconSize(14);
        fi.setIconColor(iconColor);
        Button btn = new Button(label, fi);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setMinHeight(32);
        btn.setStyle("-fx-background-color: #21262d; -fx-text-fill: #e6edf3; -fx-font-size: 12px;");
        btn.setOnAction(e -> {
            // Rimuove l'evidenziazione dal pulsante precedentemente attivo
            if (activeBuildBtn != null)
                activeBuildBtn.setStyle("-fx-background-color: #21262d; -fx-text-fill: #e6edf3; -fx-font-size: 12px;");
            activeBuildBtn = btn;
            btn.setStyle("-fx-background-color: #21262d; -fx-text-fill: #e6edf3; -fx-font-size: 12px; -fx-border-color: #58a6ff; -fx-border-width: 2px;");
            selectedTool = tool;
        });
        return btn;
    }

    /**
     * Costruisce la griglia 20×20 di StackPane cliccabili che rappresenta la mappa della città.
     * Ogni cella registra un handler onClick che chiama onCellClick().
     * Dopo la creazione, aggiorna visivamente la griglia con updateGrid().
     *
     * @return un GridPane con 400 celle configurate
     */
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

    /**
     * Gestisce il click su una cella della griglia in base allo strumento selezionato.
     *
     * Esegue l'operazione corrispondente a selectedTool (piazzamento, demolizione,
     * riparazione, upgrade) e aggiorna la griglia e le metriche al termine.
     * Le eccezioni vengono catturate silenziosamente per evitare crash della UI.
     *
     * @param x colonna della cella cliccata
     * @param y riga della cella cliccata
     */
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
                    logMessage("Cannot repair (insufficient funds or HP already at max)", "#f85149");
                }
            } else if (selectedTool.equals("UPGRADE_SEISMIC")) {
                ok = controller.upgrade(x, y, "SEISMIC");
                if (ok) {
                    logMessage("Seismic Upgrade applied (-500$)", "#38bdf8");
                } else {
                    logMessage("Upgrade failed (max level, insufficient funds, or empty cell)", "#f85149");
                }
            } else if (selectedTool.equals("UPGRADE_WASTE_THERMAL")) {
                ok = controller.upgrade(x, y, "WASTE_THERMAL");
                if (ok) {
                    logMessage("Waste Thermal Upgrade applied (-700$)", "#f97316");
                } else {
                    logMessage("Upgrade failed (max level, insufficient funds, or empty cell)", "#f85149");
                }
            } else {
                ok = controller.placeBuilding(selectedTool, x, y);
            }
        } catch (Exception ex) {
            ok = false;
        }

        // Mostra errore generico solo per azioni di piazzamento/demolizione (non per repair/upgrade che gestiscono i propri messaggi)
        boolean upgradeOrRepair = selectedTool.equals("REPAIR")
                || selectedTool.equals("UPGRADE_SEISMIC")
                || selectedTool.equals("UPGRADE_WASTE_THERMAL");
        if (!ok && !upgradeOrRepair) {
            logMessage("Cannot perform this action!", "#f9e64f");
        }

        updateGrid();
        refreshMetricsDisplay();
    }

    /**
     * Aggiunge un messaggio temporaneo al pannello notifiche.
     * Il messaggio scompare automaticamente dopo 10 secondi.
     *
     * @param text  testo del messaggio
     * @param color colore esadecimale del testo
     */
    private void logMessage(String text, String color) {
        if (logPanel == null) return;
        Label msg = new Label(text);
        msg.setStyle("-fx-text-fill: " + color + "; -fx-font-weight: bold; -fx-font-size: 11px;");
        msg.setWrapText(true);
        logPanel.getChildren().add(0, msg);

        // Rimozione automatica dopo 10 secondi tramite Timeline
        new Timeline(new KeyFrame(Duration.seconds(10), e -> logPanel.getChildren().remove(msg))).play();
    }

    /**
     * Mostra un dialogo di anteprima del costo totale di "Repair All" e,
     * se confermato, ripara tutti gli edifici danneggiati in un'unica operazione.
     *
     * Il costo viene calcolato come somma di (maxHp − hp) × 2 per ogni edificio
     * danneggiato. Se il budget non è sufficiente, viene mostrato un errore.
     */
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

        // Nessun edificio danneggiato: informa il giocatore
        if (totalCost == 0) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION, "No buildings need repairs.");
            alert.setHeaderText(null);
            applyDarkTheme(alert);
            alert.showAndWait();
            return;
        }

        // Chiede conferma prima di procedere con la riparazione globale
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

    /**
     * Aggiorna tutte le label del pannello metriche con i valori correnti di CityState.
     * Chiamato dopo ogni operazione che modifica lo stato fuori dal tick (piazzamento, demolizione, ecc.).
     */
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

    /**
     * Colora in rosso le label delle metriche che hanno superato le soglie critiche (AC-04.4/13.3):
     *   - Budget < 500
     *   - Happiness < 25
     *   - Health < 25
     *   - Pollution > 75
     *
     * @param s lo stato corrente della città
     */
    private void applyMetricAlerts(CityState s) {
        budgetLabel.setStyle("-fx-text-fill: "    + (s.getBudget()     <  500 ? "#f85149" : "#facc15") + "; -fx-font-size: 14px;");
        happinessLabel.setStyle("-fx-text-fill: " + (s.getHappiness()  <   25 ? "#f85149" : "#fb923c") + "; -fx-font-size: 14px;");
        healthLabel.setStyle("-fx-text-fill: "    + (s.getHealth()     <   25 ? "#f85149" : "#f472b6") + "; -fx-font-size: 14px;");
        pollutionLabel.setStyle("-fx-text-fill: " + (s.getPollution()  >   75 ? "#f85149" : "#4ade80") + "; -fx-font-size: 14px;");
    }

    /**
     * Verifica se la cella (rx, ry) è coperta da una PowerPlant (distanza di Chebyshev ≤ 5).
     * Usata da updateGrid() per mostrare il triangolo di avviso sugli edifici non alimentati.
     *
     * Nota: questa logica è duplicata rispetto a City.isPowered() — consolidamento pendente.
     *
     * @param rx colonna della cella da verificare
     * @param ry riga della cella da verificare
     * @return true se la cella è coperta da almeno una PowerPlant
     */
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

    /**
     * Ridisegna visivamente tutte le 400 celle della griglia in base allo stato corrente del modello.
     *
     * Per ogni cella contenente una struttura:
     *   - Mostra icona e colore del tipo di struttura
     *   - Badge con il livello di upgrade (stellina gialla) se decorata
     *   - Barra HP verde/rossa se la struttura è danneggiata (AC-15.2)
     *   - Tooltip con dettagli al passaggio del mouse
     *   - Triangolo giallo e bordo rosso se l'edificio richiede corrente ma non è alimentato (AC-04.4)
     */
    private void updateGrid() {
        for (int x = 0; x < 20; x++) {
            for (int y = 0; y < 20; y++) {
                StackPane cell = cells[x][y];
                cell.getChildren().clear();

                // Rimuove tooltip e handler hover installati al tick precedente
                Tooltip oldTip = (Tooltip) cell.getProperties().get("cellTooltip");
                if (oldTip != null) {
                    Tooltip.uninstall(cell, oldTip);
                    cell.getProperties().remove("cellTooltip");
                }
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

                    // Badge upgrade: stellina gialla in alto a sinistra con il numero di livelli
                    if (s instanceof StructureDecorator dec) {
                        FontIcon upgIcon = new FontIcon(FontAwesomeSolid.STAR);
                        upgIcon.setIconSize(8);
                        upgIcon.setIconColor(Color.web("#fde047"));
                        Label upgLabel = new Label("" + dec.getUpgradeLevel(), upgIcon);
                        upgLabel.setStyle("-fx-text-fill: #fde047; -fx-font-size: 8px;");
                        StackPane.setAlignment(upgLabel, javafx.geometry.Pos.TOP_LEFT);
                        cell.getChildren().add(upgLabel);
                    }

                    // Barra HP: visibile quando l'edificio è danneggiato ma non distrutto (AC-15.2)
                    if (s.getHp() < s.getMaxHp() && s.getHp() > 0) {
                        Rectangle hpBarBg = new Rectangle(30, 4, Color.web("#f85149"));
                        Rectangle hpBar = new Rectangle(30 * ((double)s.getHp() / s.getMaxHp()), 4, Color.web("#3fb950"));
                        VBox hpContainer = new VBox(new StackPane(hpBarBg, hpBar));
                        hpContainer.setAlignment(javafx.geometry.Pos.BOTTOM_CENTER);
                        hpContainer.setPadding(new Insets(0,0,2,0));
                        cell.getChildren().add(hpContainer);
                    }

                    // Tooltip con dettagli della struttura al passaggio del mouse
                    Tooltip tt = buildCellTooltip(s);
                    Tooltip.install(cell, tt);
                    cell.getProperties().put("cellTooltip", tt);

                    // Triangolo di avviso e bordo rosso per edifici non alimentati (AC-04.4)
                    boolean requiresPower = (s.getType() == StructureType.RESIDENTIAL ||
                                             s.getType() == StructureType.COMMERCIAL ||
                                             s.getType() == StructureType.INDUSTRIAL ||
                                             s.getType() == StructureType.HOSPITAL ||
                                             s.getType() == StructureType.WASTE_CENTER);

                    if (requiresPower && !isPowered(x, y)) {
                        FontIcon warn = new FontIcon(FontAwesomeSolid.EXCLAMATION_TRIANGLE);
                        warn.setIconSize(10);
                        warn.setIconColor(Color.web("#facc15"));
                        StackPane.setAlignment(warn, javafx.geometry.Pos.TOP_RIGHT);
                        cell.getChildren().add(warn);
                        cell.setStyle("-fx-background-color: " + hex + "22; -fx-border-color: #f85149; -fx-border-width: 1;");
                    }
                }
            }
        }
    }

    /**
     * Restituisce l'icona FontAwesome corrispondente al tipo di struttura.
     *
     * @param type il tipo di struttura
     * @return l'icona corrispondente
     */
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

    /**
     * Restituisce il colore JavaFX associato al tipo di struttura, usato
     * per le icone sulla griglia e i pulsanti del pannello sinistro.
     *
     * @param type il tipo di struttura
     * @return il colore corrispondente
     */
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

    /**
     * Restituisce l'etichetta testuale leggibile del tipo di struttura.
     *
     * @param type il tipo di struttura
     * @return il nome leggibile (es. "Power Plant", "Waste Center")
     */
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

    /**
     * Costruisce il Tooltip da mostrare al passaggio del mouse su una cella occupata.
     * Include tipo, HP, stato di alimentazione, connessione stradale e lista upgrade.
     *
     * @param s la struttura della cella
     * @return il Tooltip configurato con stile scuro
     */
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

    /**
     * Crea una label con icona FontAwesome in grassetto, usata nella barra statistica della Dashboard.
     *
     * @param text     testo iniziale
     * @param icon     icona FontAwesome
     * @param hexColor colore del testo e dell'icona
     * @return la label configurata
     */
    private Label makeDashStat(String text, FontAwesomeSolid icon, String hexColor) {
        FontIcon fi = new FontIcon(icon);
        fi.setIconSize(15);
        fi.setIconColor(Color.web(hexColor));
        Label lbl = new Label(text, fi);
        lbl.setStyle("-fx-text-fill: " + hexColor + "; -fx-font-size: 14px; -fx-font-weight: bold;");
        return lbl;
    }

    /**
     * Costruisce la tab Dashboard con il grafico storico delle metriche e la barra riepilogativa.
     *
     * Le serie tracciate sono: popolazione (scalata per stare nel range [0,110]),
     * happiness, health e pollution. La scala della popolazione si adatta automaticamente
     * se supera 1000 (divisore passa da ×10 a ×100).
     *
     * @return un VBox con la barra statistiche sopra e il LineChart sotto
     */
    private VBox buildChart() {
        NumberAxis xAxis = new NumberAxis(); xAxis.setLabel("Tick");
        NumberAxis yAxis = new NumberAxis(0, 110, 10); yAxis.setLabel("Value (0-110)");

        chart = new LineChart<>(xAxis, yAxis);
        chart.setTitle("City Trends");
        chart.setCreateSymbols(false);
        chart.setAnimated(false);
        chart.setLegendVisible(false);

        populationSeries = new XYChart.Series<>(); populationSeries.setName("Population (×10)");
        happinessSeries  = new XYChart.Series<>(); happinessSeries.setName("Happiness");
        healthSeries     = new XYChart.Series<>(); healthSeries.setName("Health");
        pollutionSeries  = new XYChart.Series<>(); pollutionSeries.setName("Pollution");
        chart.getData().addAll(populationSeries, happinessSeries, healthSeries, pollutionSeries);

        // Colori allineati alle serie JavaFX Modena: 1→pop, 2→happiness, 3→health, 4→pollution
        dashPopLabel    = makeDashStat("Population: 0",   FontAwesomeSolid.USERS, "#f3622d");
        dashHapLabel    = makeDashStat("Happiness: 67.0", FontAwesomeSolid.SMILE, "#fba71b");
        dashHealthLabel = makeDashStat("Health: 100.0",   FontAwesomeSolid.HEART, "#57b757");
        dashPollLabel   = makeDashStat("Pollution: 0.0",  FontAwesomeSolid.SMOG,  "#41a9c9");

        // Label rossa del terremoto: visibile solo al tick in cui avviene l'evento
        earthquakeNotifLabel = new Label();
        earthquakeNotifLabel.setStyle("-fx-text-fill: #f85149; -fx-font-size: 13px; -fx-font-weight: bold;");
        earthquakeNotifLabel.setVisible(false);

        HBox statsBar = new HBox(40, dashPopLabel, dashHapLabel, dashHealthLabel, dashPollLabel, earthquakeNotifLabel);
        statsBar.setAlignment(javafx.geometry.Pos.CENTER);
        statsBar.setPadding(new Insets(10, 0, 10, 0));
        statsBar.setStyle("-fx-background-color: #0d1117; -fx-border-color: #30363d; -fx-border-width: 0 0 1 0;");

        VBox container = new VBox(statsBar, chart);
        VBox.setVgrow(chart, javafx.scene.layout.Priority.ALWAYS);
        return container;
    }

    // ── Bottom bar ────────────────────────────────────────────────────────────

    /**
     * Costruisce la barra inferiore con i controlli della simulazione.
     *
     * Contiene tre gruppi:
     *   - Sinistra: Start, Stop, Tick manuale, Save, Load
     *   - Centro: pulsanti delle politiche (Default, Green, Austerity, Fossil Fuel)
     *   - Destra: slider velocità simulazione
     *
     * La Timeline avanza di un tick ogni secondo (frequenza modificabile via slider).
     * L'autosave viene eseguito ogni AUTOSAVE_EVERY_TICKS tick (AC-12.1).
     *
     * @return uno StackPane con i tre gruppi sovrapposti e allineati
     */
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

        // Timeline: avanza un tick al secondo; autosave ogni AUTOSAVE_EVERY_TICKS tick (AC-12.1)
        timeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            tickCount++;
            controller.tick();
            if (tickCount % AUTOSAVE_EVERY_TICKS == 0) {
                try {
                    controller.save(tickCount);
                    logMessage("Autosave (tick " + tickCount + ")", "#58a6ff");
                } catch (IOException ex) {
                    logMessage("Autosave failed!", "#f85149");
                }
            }
        }));
        timeline.setCycleCount(Timeline.INDEFINITE);

        // Slider velocità: modifica il rate della Timeline (0.5x – 3.0x)
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

        // Stop inizialmente nascosto: visibile solo quando la simulazione è in esecuzione
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

        // Tick manuale: avanza di un tick e gestisce l'autosave come la Timeline
        nextTickBtn.setOnAction(e -> {
            tickCount++;
            controller.tick();
            if (tickCount % AUTOSAVE_EVERY_TICKS == 0) {
                try {
                    controller.save(tickCount);
                    logMessage("Autosave (tick " + tickCount + ")", "#58a6ff");
                } catch (IOException ex) {
                    logMessage("Autosave failed!", "#f85149");
                }
            }
        });

        defaultBtn.setOnAction(e -> setActivePolicy(defaultBtn, new it.citylife.model.DefaultPolicy()));
        greenBtn.setOnAction(e -> setActivePolicy(greenBtn, new GreenPolicy()));
        austerityBtn.setOnAction(e -> setActivePolicy(austerityBtn, new AusterityPolicy()));
        fossilBtn.setOnAction(e -> setActivePolicy(fossilBtn, new FossilFuelPolicy()));

        // Save: feedback visivo temporaneo sul pulsante dopo il salvataggio
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

        // Load: mostra un ChoiceDialog con tutti i file disponibili; carica quello selezionato
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
                // Pausa la simulazione e azzera il grafico prima di caricare
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
                // Rimuove l'evidenziazione del pulsante politica (verrà reimpostata al prossimo cambio)
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

        // setPickOnBounds(false): i click attraversano le aree trasparenti degli HBox
        // evitando che il gruppo sovrapposto intercetti i click dei gruppi sottostanti
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

    /**
     * Mostra la notifica di terremoto sul grafico e nel pannello log.
     *
     * Aggiunge una linea verticale rossa al grafico nel tick corrente e
     * mostra la label rossa per 6 secondi, poi la nasconde automaticamente.
     */
    private void showEarthquakeAlert() {
        // Linea verticale rossa sul grafico nel tick del terremoto
        XYChart.Series<Number, Number> markerSeries = new XYChart.Series<>();
        markerSeries.getData().add(new XYChart.Data<>(tickCount, 0));
        markerSeries.getData().add(new XYChart.Data<>(tickCount, 110));
        chart.getData().add(markerSeries);

        // Platform.runLater necessario: il nodo esiste solo dopo che JavaFX lo ha aggiunto al grafico
        Platform.runLater(() -> {
            if (markerSeries.getNode() != null)
                markerSeries.getNode().setStyle("-fx-stroke: #f85149; -fx-stroke-width: 2px;");
        });

        // Notifica testuale sopra il grafico per 6 secondi
        earthquakeNotifLabel.setText("🌍 EARTHQUAKE at tick " + tickCount + "!");
        earthquakeNotifLabel.setVisible(true);
        new Timeline(new KeyFrame(Duration.seconds(6), e -> earthquakeNotifLabel.setVisible(false))).play();

        logMessage("EARTHQUAKE!", "#f38ba8");
    }

    /**
     * Crea un pulsante stilizzato per la barra inferiore.
     *
     * @param label testo del pulsante
     * @return il pulsante con stile scuro
     */
    private Button buildBarButton(String label) {
        Button btn = new Button(label);
        btn.setStyle("-fx-background-color: #21262d; -fx-text-fill: #e6edf3; -fx-font-size: 14px;");
        return btn;
    }

    /**
     * Cambia la politica attiva e aggiorna l'evidenziazione del pulsante corrispondente.
     * Logga nel pannello notifiche la disattivazione della vecchia politica e l'attivazione della nuova.
     *
     * @param btn    il pulsante della nuova politica da evidenziare
     * @param policy la nuova politica da applicare
     */
    private void setActivePolicy(Button btn, it.citylife.model.PolicyStrategy policy) {
        // Rimuove il bordo blu dal pulsante della politica precedente
        if (activeBtn != null)
            activeBtn.setStyle("-fx-background-color: #21262d; -fx-text-fill: #e6edf3; -fx-font-size: 14px;");
        activeBtn = btn;
        btn.setStyle("-fx-background-color: #21262d; -fx-text-fill: #e6edf3; -fx-font-size: 14px; -fx-border-color: #58a6ff; -fx-border-width: 2px;");
        String newName = policy.getClass().getSimpleName().replace("Policy", "");
        logMessage("Policy " + activePolicyName + " deactivated — " + newName + " now active.", "#8b949e");
        activePolicyName = newName;
        controller.setPolicy(policy);
    }

    /**
     * Applica il tema scuro a un dialogo JavaFX, allineando i colori con quelli della UI principale.
     * Modifica sfondo, label, pulsanti e combo box del DialogPane.
     *
     * @param dialog il dialogo a cui applicare il tema scuro
     */
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

    /**
     * Mostra il dialogo di benvenuto all'avvio dell'applicazione (AC-12.2).
     *
     * Se non esistono salvataggi, mostra un semplice messaggio di benvenuto.
     * Se esiste almeno un salvataggio, propone di avviare una nuova partita
     * o di caricare automaticamente l'ultimo file disponibile.
     */
    private void showStartupDialog() {
        List<Path> saves;
        try {
            saves = controller.listSaves();
        } catch (IOException ex) {
            return;
        }

        if (saves.isEmpty()) {
            // Prima partita: nessun salvataggio disponibile
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
                dp.setPrefWidth(380);
                javafx.scene.Node headerLabel = dp.lookup(".header-panel .label");
                if (headerLabel != null)
                    headerLabel.setStyle("-fx-text-fill: #e6edf3; -fx-font-size: 14px; -fx-font-weight: bold;");
                Button b = (Button) dp.lookupButton(startType);
                if (b != null) { b.setStyle("-fx-background-color: #21262d; -fx-text-fill: #e6edf3; -fx-font-size: 13px;"); b.setMinWidth(80); }
            }));
            welcome.showAndWait();
            return;
        }

        // Esiste almeno un salvataggio: propone nuovo gioco o carica l'ultimo
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
            dp.setPrefWidth(420);
            javafx.scene.Node headerLabel = dp.lookup(".header-panel .label");
            if (headerLabel != null)
                headerLabel.setStyle("-fx-text-fill: #e6edf3; -fx-font-size: 14px; -fx-font-weight: bold;");
            String btnStyle = "-fx-background-color: #21262d; -fx-text-fill: #e6edf3; -fx-font-size: 13px;";
            Button b1 = (Button) dp.lookupButton(newGameType);
            Button b2 = (Button) dp.lookupButton(loadGameType);
            if (b1 != null) { b1.setStyle(btnStyle); b1.setMinWidth(110); }
            if (b2 != null) { b2.setStyle(btnStyle); b2.setMinWidth(110); }
        }));

        Optional<ButtonType> result = dialog.showAndWait();

        // Se l'utente sceglie "Load Save", carica automaticamente l'ultimo file
        if (result.isEmpty() || result.get() != loadGameType) return;

        try {
            tickCount = controller.load(latest);
            logMessage("Game restored: " + latest.getFileName(), "#58a6ff");
        } catch (IOException ex) {
            showErrorAlert("Load error", ex.getMessage());
        }
    }

    /**
     * Mostra un dialogo di errore con header e messaggio personalizzati.
     *
     * @param header  titolo dell'errore
     * @param content descrizione dettagliata dell'errore
     */
    private void showErrorAlert(String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(header);
        alert.setContentText(content);
        applyDarkTheme(alert);
        alert.showAndWait();
    }

    // ── Observer ──────────────────────────────────────────────────────────────

    /**
     * Callback del Pattern Observer: invocato da City al termine di ogni tick.
     *
     * Tutti gli aggiornamenti della UI vengono eseguiti sull'Application Thread
     * tramite Platform.runLater(), come richiesto da JavaFX per le modifiche ai nodi.
     *
     * Aggiorna in sequenza:
     *   - Tutte le label delle metriche e delle soddisfazioni demografiche
     *   - Le serie del grafico storico (con eventuale riscalatura della popolazione)
     *   - La griglia (colori, badge, barre HP, avvisi di blackout)
     *   - Le notifiche di terremoto e edifici critici
     *   - L'avviso di budget negativo (mostrato solo la prima volta che diventa negativo)
     *
     * @param state lo stato aggiornato della città dopo la risoluzione del tick
     */
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

            // Riscala la serie popolazione se supera 1000: cambia il divisore e aggiorna i dati storici
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

            // Notifica terremoto: linea rossa sul grafico + label temporanea
            if (state.isEarthquakeOccurred()) {
                showEarthquakeAlert();
            }

            // Avviso edifici in stato critico (HP < 20% maxHp) — AC-15.2
            if (state.getCriticalBuildingCount() > 0) {
                logMessage(state.getCriticalBuildingCount() + " building(s) in critical condition! (HP < 20%)", "#f9e64f");
            }

            // Avviso budget negativo: mostrato solo la prima volta che diventa negativo
            if (state.getBudget() < 0 && !budgetWasNegative) {
                logMessage("NEGATIVE BUDGET! The city is in deficit.", "#f85149");
                budgetWasNegative = true;
            } else if (state.getBudget() >= 0) {
                budgetWasNegative = false;
            }

            applyMetricAlerts(state);
            updateGrid();
        });
    }
}
