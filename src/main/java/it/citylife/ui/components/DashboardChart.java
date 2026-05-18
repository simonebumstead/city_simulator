package it.citylife.ui.components;

import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;

import it.citylife.model.CityState;
import it.citylife.model.PopulationGroup;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.Duration;

/**
 * Tab Dashboard: barra riepilogativa + grafico storico delle metriche
 * (popolazione scalata, happiness, health, pollution).
 */
public final class DashboardChart {

    private final VBox root;
    private final LineChart<Number, Number> populationChart;
    private final LineChart<Number, Number> metricsChart;
    private final Label dashPopLabel, dashHapLabel, dashHealthLabel, dashPollLabel;
    private final Label earthquakeNotifLabel;

    private final XYChart.Series<Number, Number> populationSeries = new XYChart.Series<>();
    private final XYChart.Series<Number, Number> happinessSeries  = new XYChart.Series<>();
    private final XYChart.Series<Number, Number> healthSeries     = new XYChart.Series<>();
    private final XYChart.Series<Number, Number> pollutionSeries  = new XYChart.Series<>();
    private final XYChart.Series<Number, Number> jobSatSeries     = new XYChart.Series<>();
    private final XYChart.Series<Number, Number> healthSatSeries  = new XYChart.Series<>();
    private final XYChart.Series<Number, Number> safetySatSeries  = new XYChart.Series<>();

    public DashboardChart() {
        NumberAxis xAxisPop = new NumberAxis(); xAxisPop.setLabel("Tick");
        NumberAxis yAxisPop = new NumberAxis(); yAxisPop.setLabel("Abitanti");
        populationChart = new LineChart<>(xAxisPop, yAxisPop);
        populationChart.setTitle("Crescita Demografica");
        populationChart.setCreateSymbols(false);
        populationChart.setAnimated(false);
        populationChart.setLegendVisible(false);

        NumberAxis xAxisMet = new NumberAxis(); xAxisMet.setLabel("Tick");
        NumberAxis yAxisMet = new NumberAxis(0, 110, 10); yAxisMet.setLabel("Value (0-110)");
        metricsChart = new LineChart<>(xAxisMet, yAxisMet);
        metricsChart.setTitle("Indicatori della Città");
        metricsChart.setCreateSymbols(false);
        metricsChart.setAnimated(false);
        metricsChart.setLegendVisible(true);

        populationSeries.setName("Population");
        happinessSeries.setName("Happiness");
        healthSeries.setName("Health");
        pollutionSeries.setName("Pollution");
        jobSatSeries.setName("Job Sat.");
        healthSatSeries.setName("Health Sat.");
        safetySatSeries.setName("Safety Sat.");
        populationChart.getData().add(populationSeries);
        metricsChart.getData().addAll(happinessSeries, healthSeries, pollutionSeries, jobSatSeries, healthSatSeries, safetySatSeries);

        dashPopLabel    = stat("Population: 0",   FontAwesomeSolid.USERS, "#f3622d");
        dashHapLabel    = stat("Happiness: 67.0", FontAwesomeSolid.SMILE, "#fba71b");
        dashHealthLabel = stat("Health: 100.0",   FontAwesomeSolid.HEART, "#57b757");
        dashPollLabel   = stat("Pollution: 0.0",  FontAwesomeSolid.SMOG,  "#41a9c9");

        earthquakeNotifLabel = new Label();
        earthquakeNotifLabel.setStyle("-fx-text-fill: #f85149; -fx-font-weight: bold;");
        earthquakeNotifLabel.setVisible(false);

        HBox statsBar = new HBox(40, dashPopLabel, dashHapLabel, dashHealthLabel, dashPollLabel, earthquakeNotifLabel);
        statsBar.setAlignment(Pos.CENTER);
        statsBar.setPadding(new Insets(10, 0, 10, 0));
        statsBar.setStyle("-fx-background-color: #242526; -fx-border-color: #3e4042; -fx-border-width: 0 0 1 0;");

        root = new VBox(15, statsBar, populationChart, metricsChart);
        VBox.setVgrow(populationChart, Priority.ALWAYS);
        VBox.setVgrow(metricsChart, Priority.ALWAYS);
    }

    public VBox getNode() { return root; }

    /** Aggiorna label e serie storiche; riscala la popolazione se supera 1000. */
    public void update(int tick, CityState s) {
        dashPopLabel.setText("Population: "  + s.getPopulation());
        dashHapLabel.setText(String.format("Happiness: %.1f", s.getHappiness()));
        dashHealthLabel.setText(String.format("Health: %.1f", s.getHealth()));
        dashPollLabel.setText(String.format("Pollution: %.1f", s.getPollution()));

        // Avendo un grafico separato non serve più dividere e riscalare la popolazione!
        populationSeries.getData().add(new XYChart.Data<>(tick, s.getPopulation()));
        happinessSeries.getData().add(new XYChart.Data<>(tick, s.getHappiness()));
        healthSeries.getData().add(new XYChart.Data<>(tick, s.getHealth()));
        pollutionSeries.getData().add(new XYChart.Data<>(tick, s.getPollution()));

        PopulationGroup pg = s.getPopulationGroup();
        jobSatSeries.getData().add(new XYChart.Data<>(tick, pg.getJobSatisfaction()));
        healthSatSeries.getData().add(new XYChart.Data<>(tick, pg.getHealthSatisfaction()));
        safetySatSeries.getData().add(new XYChart.Data<>(tick, pg.getSafetySatisfaction()));

        // Applica i colori "coccolosi" nativi di JavaFX alle linee non appena i nodi vengono renderizzati
        Platform.runLater(() -> {
            if (populationSeries.getNode() != null) populationSeries.getNode().setStyle("-fx-stroke: #00e5ff; -fx-stroke-width: 3px;"); // Ciano brillante
            if (happinessSeries.getNode() != null) happinessSeries.getNode().setStyle("-fx-stroke: #fba71b; -fx-stroke-width: 2px;");  // Arancio acceso
            if (healthSeries.getNode() != null) healthSeries.getNode().setStyle("-fx-stroke: #57b757; -fx-stroke-width: 2px;");     // Verde pastello
            if (pollutionSeries.getNode() != null) pollutionSeries.getNode().setStyle("-fx-stroke: #41a9c9; -fx-stroke-width: 2px;");  // Azzurro polvere
            if (jobSatSeries.getNode() != null) jobSatSeries.getNode().setStyle("-fx-stroke: #60a5fa; -fx-stroke-width: 2px; -fx-stroke-dash-array: 5 5;"); // Blu chiaro tratteggiato
            if (healthSatSeries.getNode() != null) healthSatSeries.getNode().setStyle("-fx-stroke: #34d399; -fx-stroke-width: 2px; -fx-stroke-dash-array: 5 5;"); // Verde menta tratteggiato
            if (safetySatSeries.getNode() != null) safetySatSeries.getNode().setStyle("-fx-stroke: #a78bfa; -fx-stroke-width: 2px; -fx-stroke-dash-array: 5 5;"); // Viola tratteggiato
        });
    }

    /** Mostra linea verticale rossa sul grafico + label temporanea per 6 s. */
    public void showEarthquakeAlert(int tick) {
        XYChart.Series<Number, Number> marker = new XYChart.Series<>();
        marker.getData().add(new XYChart.Data<>(tick, 0));
        marker.getData().add(new XYChart.Data<>(tick, 110));
        metricsChart.getData().add(marker);
        Platform.runLater(() -> {
            if (marker.getNode() != null)
                marker.getNode().setStyle("-fx-stroke: #f85149; -fx-stroke-width: 2px; -fx-stroke-dash-array: 5 5;");
        });

        earthquakeNotifLabel.setText("🌍 EARTHQUAKE at tick " + tick + "!");
        earthquakeNotifLabel.setVisible(true);
        new Timeline(new KeyFrame(Duration.seconds(6), e -> earthquakeNotifLabel.setVisible(false))).play();
    }

    /** Pulisce tutte le serie storiche (chiamato dopo un load). */
    public void clearSeries() {
        populationSeries.getData().clear();
        happinessSeries.getData().clear();
        healthSeries.getData().clear();
        pollutionSeries.getData().clear();
        jobSatSeries.getData().clear();
        healthSatSeries.getData().clear();
        safetySatSeries.getData().clear();
    }

    private static Label stat(String text, FontAwesomeSolid icon, String hex) {
        FontIcon fi = new FontIcon(icon);
        fi.setIconSize(15);
        fi.setIconColor(Color.web(hex));
        Label lbl = new Label(text, fi);
        lbl.setStyle("-fx-text-fill: " + hex + "; -fx-font-weight: bold;");
        return lbl;
    }
}
