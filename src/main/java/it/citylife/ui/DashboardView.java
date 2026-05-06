package it.citylife.ui;

import it.citylife.model.AusterityPolicy;
import it.citylife.model.CityState;
import it.citylife.model.FossilFuelPolicy;
import it.citylife.model.GreenPolicy;
import it.citylife.model.StateObserver;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.Slider;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;

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
    private Button activeBtn;
    private VBox leftPanel;
    private LineChart<Number, Number> chart;
    private double lastHappiness = 67.0;

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
        root.setStyle("-fx-background-color: #1e1e2e;");

        tickLabel = new Label("🏙️ CityLogic  |  Tick: 0");
        tickLabel.setStyle("-fx-text-fill: #cdd6f4; -fx-font-size: 18px; -fx-font-weight: bold; -fx-padding: 10px;");
        root.setTop(tickLabel);
        leftPanel = buildLeftPanel();
        root.setLeft(leftPanel);
        chart = buildChart();
        root.setCenter(chart);
        root.setBottom(buildBottomBar());

        primaryStage.setTitle("City Simulator");
        Scene scene = new Scene(root, 1000, 600);
        scene.getStylesheets().add(getClass().getResource("/it/citylife/ui/dashboard.css").toExternalForm());
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private VBox buildLeftPanel() {
        budgetLabel     = new Label("💰 Budget: 1000");
        budgetLabel.setTextFill(Color.web("#f9e64f"));
        populationLabel = new Label("👥 Population: 0");
        populationLabel.setTextFill(Color.WHITE);
        happinessLabel  = new Label("😊 Happiness: 67.0");
        happinessLabel.setTextFill(Color.web("#fab387"));
        healthLabel     = new Label("❤️ Health: 100.0");
        healthLabel.setTextFill(Color.web("#f38ba8"));
        pollutionLabel  = new Label("🏭 Pollution: 0.0");
        pollutionLabel.setTextFill(Color.web("#a6e3a1"));
        wasteLabel      = new Label("🗑️ Waste: 0");
        wasteLabel.setTextFill(Color.web("#cdd6f4"));
        energyLabel     = new Label("Power: OK");
        energyLabel.setTextFill(Color.GREEN);

        VBox vbox = new VBox(10,
            new Label("=== Metrics ==="),
            budgetLabel,
            populationLabel,
            happinessLabel,
            healthLabel,
            pollutionLabel,
            wasteLabel,
            new Separator(),
            energyLabel
        );
        vbox.setPadding(new Insets(15));
        vbox.setMinWidth(180);
        vbox.setStyle("-fx-background-color: #313244;");
        return vbox;
    }

    private LineChart<Number, Number> buildChart() {
        NumberAxis xAxis = new NumberAxis(); xAxis.setLabel("Tick");
        NumberAxis yAxis = new NumberAxis(0, 110, 10); yAxis.setLabel("Value (0-110)");

        LineChart<Number, Number> chart = new LineChart<>(xAxis, yAxis);
        chart.setTitle("City Trends");
        chart.setCreateSymbols(false);
        chart.setAnimated(false);

        populationSeries = new XYChart.Series<>(); populationSeries.setName("Population");
        happinessSeries  = new XYChart.Series<>(); happinessSeries.setName("Happiness");
        healthSeries     = new XYChart.Series<>(); healthSeries.setName("Health");
        pollutionSeries  = new XYChart.Series<>(); pollutionSeries.setName("Pollution");
        chart.getData().addAll(populationSeries, happinessSeries, healthSeries, pollutionSeries);

        return chart;
    }

    private HBox buildBottomBar() {
        Button startBtn    = new Button("Start");
        Button stopBtn     = new Button("Stop");
        Button greenBtn    = new Button("Green");
        Button austerityBtn = new Button("Austerity");
        Button fossilBtn   = new Button("Fossil Fuel");

        timeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            tickCount++;
            controller.tick();
        }));
        timeline.setCycleCount(Timeline.INDEFINITE);

        Slider speedSlider = new Slider(0.5, 3.0, 1.0);
        speedSlider.setShowTickLabels(true);
        speedSlider.setMajorTickUnit(0.5);
        Label speedLabel = new Label("Speed: 1.0x");
        speedSlider.valueProperty().addListener((obs, old, val) -> {
            speedLabel.setText(String.format("Speed: %.1fx", val.doubleValue()));
            timeline.setRate(val.doubleValue());
        });

        startBtn.setOnAction(e -> timeline.play());
        stopBtn.setOnAction(e -> timeline.pause());
        greenBtn.setOnAction(e -> setActivePolicy(greenBtn, new GreenPolicy()));
        austerityBtn.setOnAction(e -> setActivePolicy(austerityBtn, new AusterityPolicy()));
        fossilBtn.setOnAction(e -> setActivePolicy(fossilBtn, new FossilFuelPolicy()));

        HBox hbox = new HBox(10, startBtn, stopBtn, new Separator(), greenBtn, austerityBtn, fossilBtn, new Separator(), speedSlider, speedLabel);
        hbox.setPadding(new Insets(10));
        return hbox;
    }

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

    private void setActivePolicy(Button btn, it.citylife.model.PolicyStrategy policy) {
        if (activeBtn != null) activeBtn.setStyle("");
        activeBtn = btn;
        btn.setStyle("-fx-border-color: #a6e3a1; -fx-border-width: 2px;");
        controller.setPolicy(policy);
    }

    @Override
    public void onStateChanged(CityState state) {
        Platform.runLater(() -> {
            tickLabel.setText("🏙️ CityLogic  |  Tick: " + tickCount);
            budgetLabel.setText(String.format("💰 Budget: %.0f", state.getBudget()));
            populationLabel.setText("👥 Population: " + state.getPopulation());
            happinessLabel.setText(String.format("😊 Happiness: %.1f", state.getHappiness()));
            healthLabel.setText(String.format("❤️ Health: %.1f", state.getHealth()));
            pollutionLabel.setText(String.format("🏭 Pollution: %.1f", state.getPollution()));
            wasteLabel.setText("🗑️ Waste: " + state.getWasteLevel());

            boolean powered = controller.hasPower();
            energyLabel.setText(powered ? "Power: OK" : "Power: BLACKOUT");
            energyLabel.setTextFill(powered ? Color.GREEN : Color.RED);

            populationSeries.getData().add(new XYChart.Data<>(tickCount, state.getPopulation()));
            happinessSeries.getData().add(new XYChart.Data<>(tickCount, state.getHappiness()));
            healthSeries.getData().add(new XYChart.Data<>(tickCount, state.getHealth()));
            pollutionSeries.getData().add(new XYChart.Data<>(tickCount, state.getPollution()));

            if (lastHappiness - state.getHappiness() > 10) {
                showEarthquakeAlert();
            }
            lastHappiness = state.getHappiness();
        });
    }
}
