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
        root.setLeft(buildLeftPanel());
        root.setCenter(buildChart());
        root.setBottom(buildBottomBar());

        primaryStage.setTitle("City Simulator");
        primaryStage.setScene(new Scene(root, 1000, 600));
        primaryStage.show();
    }

    private VBox buildLeftPanel() {
        budgetLabel     = new Label("Budget: 1000");
        populationLabel = new Label("Population: 0");
        happinessLabel  = new Label("Happiness: 67.0");
        healthLabel     = new Label("Health: 100.0");
        pollutionLabel  = new Label("Pollution: 0.0");
        wasteLabel      = new Label("Waste: 0");
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

        startBtn.setOnAction(e -> timeline.play());
        stopBtn.setOnAction(e -> timeline.pause());
        greenBtn.setOnAction(e -> controller.setPolicy(new GreenPolicy()));
        austerityBtn.setOnAction(e -> controller.setPolicy(new AusterityPolicy()));
        fossilBtn.setOnAction(e -> controller.setPolicy(new FossilFuelPolicy()));

        HBox hbox = new HBox(10, startBtn, stopBtn, new Separator(), greenBtn, austerityBtn, fossilBtn);
        hbox.setPadding(new Insets(10));
        return hbox;
    }

    @Override
    public void onStateChanged(CityState state) {
        Platform.runLater(() -> {
            budgetLabel.setText(String.format("Budget: %.0f", state.getBudget()));
            populationLabel.setText("Population: " + state.getPopulation());
            happinessLabel.setText(String.format("Happiness: %.1f", state.getHappiness()));
            healthLabel.setText(String.format("Health: %.1f", state.getHealth()));
            pollutionLabel.setText(String.format("Pollution: %.1f", state.getPollution()));
            wasteLabel.setText("Waste: " + state.getWasteLevel());

            boolean powered = controller.hasPower();
            energyLabel.setText(powered ? "Power: OK" : "Power: BLACKOUT");
            energyLabel.setTextFill(powered ? Color.GREEN : Color.RED);

            populationSeries.getData().add(new XYChart.Data<>(tickCount, state.getPopulation()));
            happinessSeries.getData().add(new XYChart.Data<>(tickCount, state.getHappiness()));
            healthSeries.getData().add(new XYChart.Data<>(tickCount, state.getHealth()));
            pollutionSeries.getData().add(new XYChart.Data<>(tickCount, state.getPollution()));
        });
    }
}
