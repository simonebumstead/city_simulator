package it.citylife.ui.components;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import it.citylife.model.AusterityPolicy;
import it.citylife.model.DefaultPolicy;
import it.citylife.model.FossilFuelPolicy;
import it.citylife.model.GreenPolicy;
import it.citylife.model.PolicyStrategy;
import it.citylife.ui.SimulationController;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.util.Duration;

/**
 * Barra inferiore: start/stop/tick manuale, save/load, pulsanti politica e slider velocità.
 * Possiede la {@link Timeline} che scandisce i tick automatici e il contatore {@code tickCount}.
 */
public final class SimulationControlsBar {

    private static final int AUTOSAVE_EVERY_TICKS = 5;

    private final SimulationController controller;
    private final Stage primaryStage;
    private final MetricsPanel metricsPanel;
    private final MapGridView mapView;
    private final DashboardChart chartView;

    private final StackPane root;
    private final Timeline timeline;

    private final Button defaultBtn, greenBtn, austerityBtn, fossilBtn;
    private final Button startBtn, stopBtn;
    private Button activeBtn;
    private String activePolicyName = "Default";

    private int tickCount = 0;

    public SimulationControlsBar(SimulationController controller, Stage primaryStage,
                                 MetricsPanel metricsPanel, MapGridView mapView, DashboardChart chartView) {
        this.controller = controller;
        this.primaryStage = primaryStage;
        this.metricsPanel = metricsPanel;
        this.mapView = mapView;
        this.chartView = chartView;

        startBtn        = new Button("Start");
        stopBtn         = new Button("Stop");
        Button nextBtn  = new Button("⏭ Tick");
        Button saveBtn  = new Button("Save");
        Button loadBtn  = new Button("Load");
        defaultBtn      = new Button("Default");
        greenBtn        = new Button("Green");
        austerityBtn    = new Button("Austerity");
        fossilBtn       = new Button("Fossil Fuel");

        timeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> doTick()));
        timeline.setCycleCount(Timeline.INDEFINITE);

        Slider speedSlider = new Slider(0.5, 3.0, 1.0);
        speedSlider.setShowTickLabels(true);
        speedSlider.setMajorTickUnit(0.5);
        Label speedLabel = new Label("Speed: 1.0x");
        speedLabel.setStyle("-fx-text-fill: #e4e6eb; -fx-font-size: 14px;");
        speedSlider.valueProperty().addListener((obs, old, val) -> {
            speedLabel.setText(String.format("Speed: %.1fx", val.doubleValue()));
            timeline.setRate(val.doubleValue());
        });

        stopBtn.setVisible(false);
        stopBtn.setManaged(false);

        startBtn.setOnAction(e -> {
            timeline.play();
            startBtn.setVisible(false); startBtn.setManaged(false);
            stopBtn.setVisible(true);  stopBtn.setManaged(true);
        });
        stopBtn.setOnAction(e -> {
            timeline.pause();
            stopBtn.setVisible(false); stopBtn.setManaged(false);
            startBtn.setVisible(true); startBtn.setManaged(true);
        });
        nextBtn.setOnAction(e -> performManualTick());

        defaultBtn.setOnAction(e   -> setActivePolicy(defaultBtn,   new DefaultPolicy()));
        greenBtn.setOnAction(e     -> setActivePolicy(greenBtn,     new GreenPolicy()));
        austerityBtn.setOnAction(e -> setActivePolicy(austerityBtn, new AusterityPolicy()));
        fossilBtn.setOnAction(e    -> setActivePolicy(fossilBtn,    new FossilFuelPolicy()));

        saveBtn.setOnAction(e -> handleSave(saveBtn));
        loadBtn.setOnAction(e -> handleLoad());

        HBox leftGroup   = new HBox(10, startBtn, stopBtn, nextBtn, saveBtn, loadBtn);
        HBox centerGroup = new HBox(10, defaultBtn, greenBtn, austerityBtn, fossilBtn);
        HBox rightGroup  = new HBox(10, speedSlider, speedLabel);
        leftGroup.setAlignment(Pos.CENTER_LEFT);
        centerGroup.setAlignment(Pos.CENTER);
        rightGroup.setAlignment(Pos.CENTER_RIGHT);
        leftGroup.setPickOnBounds(false);
        centerGroup.setPickOnBounds(false);
        rightGroup.setPickOnBounds(false);

        root = new StackPane();
        root.setPadding(new Insets(8, 16, 8, 16));
        root.setStyle("-fx-background-color: #242526; -fx-border-color: #3e4042; -fx-border-width: 1 0 0 0;");
        StackPane.setAlignment(leftGroup, Pos.CENTER_LEFT);
        StackPane.setAlignment(rightGroup, Pos.CENTER_RIGHT);
        root.getChildren().addAll(centerGroup, leftGroup, rightGroup);
    }

    public StackPane getNode() { return root; }
    public int getTickCount() { return tickCount; }
    public void setTickCount(int v) { tickCount = v; }

    public void performManualTick() { doTick(); }

    private void doTick() {
        tickCount++;
        controller.tick();
        if (tickCount % AUTOSAVE_EVERY_TICKS == 0) {
            try {
                controller.save(tickCount);
                metricsPanel.log("Autosave (tick " + tickCount + ")", "#58a6ff");
            } catch (IOException ex) {
                metricsPanel.log("Autosave failed!", "#f85149");
            }
        }
    }

    public void syncPolicyButtonWithModel() {
        if (controller.getActivePolicy() == null) return;
        PolicyStrategy p = controller.getActivePolicy();
        Button target = defaultBtn;
        String name = "Default";
        if (p instanceof GreenPolicy)         { target = greenBtn;     name = "Green"; }
        else if (p instanceof AusterityPolicy){ target = austerityBtn; name = "Austerity"; }
        else if (p instanceof FossilFuelPolicy){ target = fossilBtn;   name = "FossilFuel"; }
        if (activeBtn != null) activeBtn.setStyle("");
        activeBtn = target;
        activeBtn.setStyle("-fx-border-color: #2374e1; -fx-background-color: #242526;");
        activePolicyName = name;
    }

    private void setActivePolicy(Button btn, PolicyStrategy policy) {
        if (activeBtn != null) activeBtn.setStyle("");
        activeBtn = btn;
        btn.setStyle("-fx-border-color: #2374e1; -fx-background-color: #242526;");
        String newName = policy.getClass().getSimpleName().replace("Policy", "");
        metricsPanel.log("Policy " + activePolicyName + " deactivated — " + newName + " now active.", "#8b949e");
        activePolicyName = newName;
        controller.setPolicy(policy);
    }

    private void handleSave(Button saveBtn) {
        try {
            controller.save(tickCount);
            saveBtn.setText("✓ Saved!");
            saveBtn.setStyle("-fx-border-color: #3fb950;");
            new Timeline(new KeyFrame(Duration.seconds(2.5), ev -> {
                saveBtn.setText("Save");
                saveBtn.setStyle("");
            })).play();
        } catch (IOException ex) {
            DialogHelper.showError(primaryStage, "Save error", ex.getMessage());
        }
    }

    private void handleLoad() {
        try {
            List<Path> saves = controller.listSaves();
            if (saves.isEmpty()) {
                DialogHelper.showError(primaryStage, "No saves found", "The 'saves/' folder is empty or does not exist.");
                return;
            }
            Path chosen;
            if (saves.size() == 1) {
                chosen = saves.get(0);
            } else {
                List<String> names = saves.stream().map(p -> p.getFileName().toString()).toList();
                ChoiceDialog<String> dialog = new ChoiceDialog<>(names.get(names.size() - 1), names);
                dialog.setGraphic(null);
                dialog.setTitle("Load game");
                dialog.setHeaderText("Choose a save file");
                dialog.setContentText("File:");
                DialogHelper.style(dialog, "dialog-info", primaryStage);
                Optional<String> result = dialog.showAndWait();
                if (result.isEmpty()) return;
                chosen = saves.stream().filter(p -> p.getFileName().toString().equals(result.get())).findFirst().orElseThrow();
            }

            timeline.pause();
            startBtn.setVisible(true);  startBtn.setManaged(true);
            stopBtn.setVisible(false);  stopBtn.setManaged(false);
            chartView.clearSeries();
            tickCount = controller.load(chosen);

            mapView.refresh();
            metricsPanel.update(controller.getState());
            syncPolicyButtonWithModel();
        } catch (IOException ex) {
            DialogHelper.showError(primaryStage, "Load error", ex.getMessage());
        }
    }
}
