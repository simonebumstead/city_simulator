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
import javafx.scene.Node;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.Duration;

/**
 * Tab Dashboard: selettore a 3 viste + barra statistiche contestuale +
 * grafico storico a piena altezza.
 */
public final class DashboardChart {

    private final VBox root;
    private final LineChart<Number, Number> populationChart;
    private final LineChart<Number, Number> indicatoriChart;
    private final LineChart<Number, Number> soddisfazioneChart;
    private final Label earthquakeNotifLabel;

    // Stats bar — Population
    private final Label dashPopLabel;

    // Stats bar — Indicators
    private final Label dashHapLabel;
    private final Label dashHealthLabel;
    private final Label dashPollLabel;

    // Stats bar — Satisfaction
    private final Label dashJobSatLabel;
    private final Label dashHealthSatLabel;
    private final Label dashSafetySatLabel;

    private final XYChart.Series<Number, Number> populationSeries  = new XYChart.Series<>();
    private final XYChart.Series<Number, Number> happinessSeries = new XYChart.Series<>();
    private final XYChart.Series<Number, Number> healthSeries      = new XYChart.Series<>();
    private final XYChart.Series<Number, Number> pollutionSeries   = new XYChart.Series<>();
    private final XYChart.Series<Number, Number> jobSatSeries      = new XYChart.Series<>();
    private final XYChart.Series<Number, Number> healthSatSeries   = new XYChart.Series<>();
    private final XYChart.Series<Number, Number> safetySatSeries   = new XYChart.Series<>();

    private boolean originResetNeeded = true;

    public DashboardChart() {
        // --- Grafici (niente titolo, niente legenda) ---
        NumberAxis xAxisPop = new NumberAxis(); xAxisPop.setLabel("Tick");
        NumberAxis yAxisPop = new NumberAxis(); yAxisPop.setLabel("Inhabitants");
        populationChart = new LineChart<>(xAxisPop, yAxisPop);
        populationChart.setCreateSymbols(false);
        populationChart.setAnimated(false);
        populationChart.setLegendVisible(false);

        NumberAxis xAxisInd = new NumberAxis(); xAxisInd.setLabel("Tick");
        NumberAxis yAxisInd = new NumberAxis(0, 110, 10); yAxisInd.setLabel("Value (0-110)");
        indicatoriChart = new LineChart<>(xAxisInd, yAxisInd);
        indicatoriChart.setCreateSymbols(false);
        indicatoriChart.setAnimated(false);
        indicatoriChart.setLegendVisible(false);

        NumberAxis xAxisSod = new NumberAxis(); xAxisSod.setLabel("Tick");
        NumberAxis yAxisSod = new NumberAxis(0, 110, 10); yAxisSod.setLabel("Value (0-110)");
        soddisfazioneChart = new LineChart<>(xAxisSod, yAxisSod);
        soddisfazioneChart.setCreateSymbols(false);
        soddisfazioneChart.setAnimated(false);
        soddisfazioneChart.setLegendVisible(false);

        populationSeries.setName("Population");
        happinessSeries.setName("Happiness");
        healthSeries.setName("Health");
        pollutionSeries.setName("Pollution");
        jobSatSeries.setName("Job Satisfaction");
        healthSatSeries.setName("Health Satisfaction");
        safetySatSeries.setName("Safety Satisfaction");

        populationChart.getData().add(populationSeries);
        indicatoriChart.getData().addAll(happinessSeries, healthSeries, pollutionSeries);
        soddisfazioneChart.getData().addAll(jobSatSeries, healthSatSeries, safetySatSeries);

        // --- Labels stats (colori coerenti con le linee dei grafici) ---
        dashPopLabel      = stat("Population: 0",            FontAwesomeSolid.USERS,         "#00e5ff");
        dashHapLabel      = stat("Happiness: 67.0",          FontAwesomeSolid.SMILE,         "#fba71b");
        dashHealthLabel   = stat("Health: 100.0",            FontAwesomeSolid.HEART,         "#57b757");
        dashPollLabel     = stat("Pollution: 0.0",           FontAwesomeSolid.SMOG,          "#41a9c9");
        dashJobSatLabel   = stat("Job Satisfaction: 50%",    FontAwesomeSolid.BRIEFCASE,     "#60a5fa");
        dashHealthSatLabel= stat("Health Satisfaction: 50%", FontAwesomeSolid.NOTES_MEDICAL, "#34d399");
        dashSafetySatLabel= stat("Safety Satisfaction: 50%", FontAwesomeSolid.SHIELD_ALT,    "#a78bfa");

        // --- Barra terremoto (nella riga selettore, spinta a destra) ---
        earthquakeNotifLabel = new Label();
        earthquakeNotifLabel.setStyle("-fx-text-fill: #f85149; -fx-font-weight: bold;");
        earthquakeNotifLabel.setVisible(false);

        // --- Selettore a 3 pulsanti ---
        ToggleGroup gruppo = new ToggleGroup();
        ToggleButton btnPop = selectorBtn("Population",   FontAwesomeSolid.USERS,       gruppo);
        ToggleButton btnInd = selectorBtn("Indicators",   FontAwesomeSolid.CHART_LINE,  gruppo);
        ToggleButton btnSod = selectorBtn("Satisfaction", FontAwesomeSolid.SMILE,       gruppo);
        btnPop.setSelected(true);

        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox selectorRow = new HBox(8, btnPop, btnInd, btnSod, spacer, earthquakeNotifLabel);
        selectorRow.setAlignment(Pos.CENTER_LEFT);
        selectorRow.setPadding(new Insets(6, 12, 6, 12));
        selectorRow.setStyle("-fx-background-color: #18191a;");

        // --- 3 barre statistiche contestuali ---
        HBox statsBarPop = statsBar(dashPopLabel);
        HBox statsBarInd = statsBar(dashHapLabel, dashHealthLabel, dashPollLabel);
        HBox statsBarSod = statsBar(dashJobSatLabel, dashHealthSatLabel, dashSafetySatLabel);

        StackPane statsArea = new StackPane(statsBarPop, statsBarInd, statsBarSod);
        mostra(statsBarPop, statsBarInd, statsBarSod);

        // --- Area grafici ---
        StackPane chartArea = new StackPane(populationChart, indicatoriChart, soddisfazioneChart);
        VBox.setVgrow(chartArea, Priority.ALWAYS);
        mostra(populationChart, indicatoriChart, soddisfazioneChart);

        // --- Swap al cambio pulsante ---
        gruppo.selectedToggleProperty().addListener((obs, vecchio, nuovo) -> {
            if (nuovo == null) { vecchio.setSelected(true); return; }
            if (nuovo == btnPop) {
                mostra(populationChart,    indicatoriChart,    soddisfazioneChart);
                mostra(statsBarPop,        statsBarInd,        statsBarSod);
            }
            if (nuovo == btnInd) {
                mostra(indicatoriChart,    populationChart,    soddisfazioneChart);
                mostra(statsBarInd,        statsBarPop,        statsBarSod);
            }
            if (nuovo == btnSod) {
                mostra(soddisfazioneChart, populationChart,    indicatoriChart);
                mostra(statsBarSod,        statsBarPop,        statsBarInd);
            }
        });

        root = new VBox(0, selectorRow, statsArea, chartArea);
    }

    public VBox getNode() { return root; }

    /** Aggiorna label e serie storiche. */
    public void update(int tick, CityState s) {
        if (originResetNeeded) {
            NumberAxis[] xAxes = {
                (NumberAxis) populationChart.getXAxis(),
                (NumberAxis) indicatoriChart.getXAxis(),
                (NumberAxis) soddisfazioneChart.getXAxis()
            };
            for (NumberAxis xAxis : xAxes) {
                xAxis.setAutoRanging(false);
                xAxis.setLowerBound(tick);
                xAxis.setUpperBound(tick + 10);
            }
            originResetNeeded = false;
        }

        NumberAxis[] xAxes = {
            (NumberAxis) populationChart.getXAxis(),
            (NumberAxis) indicatoriChart.getXAxis(),
            (NumberAxis) soddisfazioneChart.getXAxis()
        };
        for (NumberAxis xAxis : xAxes) {
            if (tick > xAxis.getUpperBound()) {
                xAxis.setUpperBound(tick);
            }
            double range = xAxis.getUpperBound() - xAxis.getLowerBound();
            xAxis.setTickUnit(Math.max(1.0, Math.floor(range / 10.0)));
        }

        dashPopLabel.setText("Population: " + s.getPopulation());
        dashHapLabel.setText(String.format("Happiness: %.1f", s.getHappiness()));
        dashHealthLabel.setText(String.format("Health: %.1f", s.getHealth()));
        dashPollLabel.setText(String.format("Pollution: %.1f", s.getPollution()));

        PopulationGroup pg = s.getPopulationGroup();
        dashJobSatLabel.setText(String.format("Job Satisfaction: %.1f", pg.getJobSatisfaction()));
        dashHealthSatLabel.setText(String.format("Health Satisfaction: %.1f", pg.getHealthSatisfaction()));
        dashSafetySatLabel.setText(String.format("Safety Satisfaction: %.1f", pg.getSafetySatisfaction()));

        populationSeries.getData().add(new XYChart.Data<>(tick, s.getPopulation()));
        happinessSeries.getData().add(new XYChart.Data<>(tick, s.getHappiness()));
        healthSeries.getData().add(new XYChart.Data<>(tick, s.getHealth()));
        pollutionSeries.getData().add(new XYChart.Data<>(tick, s.getPollution()));
        jobSatSeries.getData().add(new XYChart.Data<>(tick, pg.getJobSatisfaction()));
        healthSatSeries.getData().add(new XYChart.Data<>(tick, pg.getHealthSatisfaction()));
        safetySatSeries.getData().add(new XYChart.Data<>(tick, pg.getSafetySatisfaction()));

        Platform.runLater(() -> {
            if (populationSeries.getNode() != null)
                populationSeries.getNode().setStyle("-fx-stroke: #00e5ff; -fx-stroke-width: 3px;");
            if (happinessSeries.getNode() != null)
                happinessSeries.getNode().setStyle("-fx-stroke: #fba71b; -fx-stroke-width: 2px;");
            if (healthSeries.getNode() != null)
                healthSeries.getNode().setStyle("-fx-stroke: #57b757; -fx-stroke-width: 2px;");
            if (pollutionSeries.getNode() != null)
                pollutionSeries.getNode().setStyle("-fx-stroke: #41a9c9; -fx-stroke-width: 2px;");
            if (jobSatSeries.getNode() != null)
                jobSatSeries.getNode().setStyle("-fx-stroke: #60a5fa; -fx-stroke-width: 2px;");
            if (healthSatSeries.getNode() != null)
                healthSatSeries.getNode().setStyle("-fx-stroke: #34d399; -fx-stroke-width: 2px;");
            if (safetySatSeries.getNode() != null)
                safetySatSeries.getNode().setStyle("-fx-stroke: #a78bfa; -fx-stroke-width: 2px;");
        });
    }

    /** Aggiunge una linea verticale rossa sui grafici a Y fissa + label per 6 s. */
    public void showEarthquakeAlert(int tick) {
        aggiungiMarcatore(indicatoriChart,    tick, 0, 110);
        aggiungiMarcatore(soddisfazioneChart, tick, 0, 110);

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
        originResetNeeded = true;
    }

    // --- Helpers privati ---

    private static void mostra(Node visibile, Node nascosto1, Node nascosto2) {
        visibile.setVisible(true);  visibile.setManaged(true);
        nascosto1.setVisible(false); nascosto1.setManaged(false);
        nascosto2.setVisible(false); nascosto2.setManaged(false);
    }

    private void aggiungiMarcatore(LineChart<Number, Number> chart, int tick, double yMin, double yMax) {
        XYChart.Series<Number, Number> marker = new XYChart.Series<>();
        marker.getData().add(new XYChart.Data<>(tick, yMin));
        marker.getData().add(new XYChart.Data<>(tick, yMax));
        chart.getData().add(marker);
        Platform.runLater(() -> {
            if (marker.getNode() != null)
                marker.getNode().setStyle("-fx-stroke: #f85149; -fx-stroke-width: 2px; -fx-stroke-dash-array: 5 5;");
        });
    }

    private static HBox statsBar(Label... labels) {
        HBox bar = new HBox(40);
        bar.getChildren().addAll(labels);
        bar.setAlignment(Pos.CENTER);
        bar.setPadding(new Insets(10, 0, 10, 0));
        bar.setStyle("-fx-background-color: #242526; -fx-border-color: #3e4042; -fx-border-width: 0 0 1 0;");
        return bar;
    }

    private static ToggleButton selectorBtn(String testo, FontAwesomeSolid icona, ToggleGroup gruppo) {
        FontIcon fi = new FontIcon(icona);
        fi.setIconSize(14);
        fi.setIconColor(Color.web("#e4e6eb"));
        ToggleButton btn = new ToggleButton(testo, fi);
        btn.setToggleGroup(gruppo);
        return btn;
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
