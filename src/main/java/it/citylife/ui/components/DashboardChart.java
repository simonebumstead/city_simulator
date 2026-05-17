package it.citylife.ui.components;

import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;

import it.citylife.model.CityState;
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
    private final LineChart<Number, Number> chart;
    private final Label dashPopLabel, dashHapLabel, dashHealthLabel, dashPollLabel;
    private final Label earthquakeNotifLabel;

    private final XYChart.Series<Number, Number> populationSeries = new XYChart.Series<>();
    private final XYChart.Series<Number, Number> happinessSeries  = new XYChart.Series<>();
    private final XYChart.Series<Number, Number> healthSeries     = new XYChart.Series<>();
    private final XYChart.Series<Number, Number> pollutionSeries  = new XYChart.Series<>();

    private double populationDivisor = 10.0;

    public DashboardChart() {
        NumberAxis xAxis = new NumberAxis(); xAxis.setLabel("Tick");
        NumberAxis yAxis = new NumberAxis(0, 110, 10); yAxis.setLabel("Value (0-110)");

        chart = new LineChart<>(xAxis, yAxis);
        chart.setTitle("City Trends");
        chart.setCreateSymbols(false);
        chart.setAnimated(false);
        chart.setLegendVisible(false);

        populationSeries.setName("Population (×10)");
        happinessSeries.setName("Happiness");
        healthSeries.setName("Health");
        pollutionSeries.setName("Pollution");
        chart.getData().addAll(populationSeries, happinessSeries, healthSeries, pollutionSeries);

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
        statsBar.setStyle("-fx-background-color: #0d1117; -fx-border-color: #30363d; -fx-border-width: 0 0 1 0;");

        root = new VBox(statsBar, chart);
        VBox.setVgrow(chart, Priority.ALWAYS);
    }

    public VBox getNode() { return root; }

    /** Aggiorna label e serie storiche; riscala la popolazione se supera 1000. */
    public void update(int tick, CityState s) {
        dashPopLabel.setText("Population: "  + s.getPopulation());
        dashHapLabel.setText(String.format("Happiness: %.1f", s.getHappiness()));
        dashHealthLabel.setText(String.format("Health: %.1f", s.getHealth()));
        dashPollLabel.setText(String.format("Pollution: %.1f", s.getPollution()));

        double newDivisor = (s.getPopulation() > 1000) ? 100.0 : 10.0;
        if (newDivisor != populationDivisor) {
            double ratio = populationDivisor / newDivisor;
            populationSeries.getData().forEach(d -> d.setYValue(d.getYValue().doubleValue() * ratio));
            populationDivisor = newDivisor;
            populationSeries.setName("Population (×" + (int) populationDivisor + ")");
        }
        populationSeries.getData().add(new XYChart.Data<>(tick, s.getPopulation() / populationDivisor));
        happinessSeries.getData().add(new XYChart.Data<>(tick, s.getHappiness()));
        healthSeries.getData().add(new XYChart.Data<>(tick, s.getHealth()));
        pollutionSeries.getData().add(new XYChart.Data<>(tick, s.getPollution()));
    }

    /** Mostra linea verticale rossa sul grafico + label temporanea per 6 s. */
    public void showEarthquakeAlert(int tick) {
        XYChart.Series<Number, Number> marker = new XYChart.Series<>();
        marker.getData().add(new XYChart.Data<>(tick, 0));
        marker.getData().add(new XYChart.Data<>(tick, 110));
        chart.getData().add(marker);
        Platform.runLater(() -> {
            if (marker.getNode() != null)
                marker.getNode().setStyle("-fx-stroke: #f85149; -fx-stroke-width: 2px;");
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
