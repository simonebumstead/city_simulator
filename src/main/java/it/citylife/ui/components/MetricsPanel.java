package it.citylife.ui.components;

import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;

import it.citylife.model.CityState;
import it.citylife.model.PopulationGroup;
import it.citylife.ui.SimulationController;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.Duration;

/**
 * Pannello destro: label di tutte le metriche di {@link CityState}, indicatore
 * energetico e log scrollabile di notifiche temporanee.
 */
public final class MetricsPanel {

    private final SimulationController controller;
    private final VBox root;

    private final Label budgetLabel, populationLabel, happinessLabel, healthLabel;
    private final Label pollutionLabel, wasteLabel;
    private final Label jobSatLabel, healthSatLabel, safetySatLabel;
    private final Label energyLabel;
    private final VBox logPanel;

    public MetricsPanel(SimulationController controller) {
        this.controller = controller;
        CityState s = controller.getState();

        budgetLabel     = metric(String.format("Budget: %.0f", s.getBudget()),    FontAwesomeSolid.COINS, "#facc15");
        populationLabel = metric("Population: " + s.getPopulation(),              FontAwesomeSolid.USERS, "#e6edf3");
        happinessLabel  = metric(String.format("Happiness: %.1f", s.getHappiness()), FontAwesomeSolid.SMILE, "#fb923c");
        healthLabel     = metric(String.format("Health: %.1f", s.getHealth()),    FontAwesomeSolid.HEART, "#f472b6");
        pollutionLabel  = metric(String.format("Pollution: %.1f", s.getPollution()), FontAwesomeSolid.SMOG, "#4ade80");
        wasteLabel      = metric("Waste: " + s.getWasteLevel(),                   FontAwesomeSolid.TRASH, "#8b949e");
        jobSatLabel     = metric("Job Satisfaction: 50%",    FontAwesomeSolid.BRIEFCASE,     "#60a5fa");
        healthSatLabel  = metric("Health Satisfaction: 50%", FontAwesomeSolid.NOTES_MEDICAL, "#34d399");
        safetySatLabel  = metric("Safety Satisfaction: 50%", FontAwesomeSolid.SHIELD_ALT,    "#a78bfa");

        boolean powered = controller.hasPower();
        FontIcon boltIcon = new FontIcon(FontAwesomeSolid.BOLT);
        boltIcon.setIconSize(14);
        boltIcon.setIconColor(Color.web(powered ? "#3fb950" : "#f85149"));
        energyLabel = new Label(powered ? "Power: OK" : "Power: BLACKOUT", boltIcon);
        energyLabel.setStyle("-fx-text-fill: " + (powered ? "#3fb950" : "#f85149") + "; -fx-font-weight: bold;");

        Label metricsTitle = sectionTitle("METRICS");
        Label logTitle = sectionTitle("NOTIFICATIONS");

        logPanel = new VBox(5);
        logPanel.setPadding(new Insets(5, 0, 0, 0));

        ScrollPane logScroll = new ScrollPane(logPanel);
        logScroll.setFitToWidth(true);
        logScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        logScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        logScroll.setStyle("-fx-background: transparent; -fx-background-color: transparent; -fx-border-color: transparent; -fx-padding: 0;");
        VBox.setVgrow(logScroll, Priority.ALWAYS);

        root = new VBox(10,
            metricsTitle,
            budgetLabel, populationLabel, happinessLabel,
            healthLabel, pollutionLabel, wasteLabel,
            jobSatLabel, healthSatLabel, safetySatLabel,
            new Separator(),
            energyLabel,
            new Separator(),
            logTitle, logScroll
        );
        root.setPadding(new Insets(14));
        root.setMinWidth(200);
        root.setStyle("-fx-background-color: #161b22; -fx-border-color: #30363d; -fx-border-width: 0 0 0 1;");
    }

    public VBox getNode() { return root; }

    /** Aggiorna tutte le label con i valori correnti di {@code state}. */
    public void update(CityState s) {
        budgetLabel.setText(String.format("Budget: %.0f", s.getBudget()));
        populationLabel.setText("Population: " + s.getPopulation());
        happinessLabel.setText(String.format("Happiness: %.1f", s.getHappiness()));
        healthLabel.setText(String.format("Health: %.1f", s.getHealth()));
        pollutionLabel.setText(String.format("Pollution: %.1f", s.getPollution()));
        wasteLabel.setText("Waste: " + s.getWasteLevel());

        PopulationGroup pg = s.getPopulationGroup();
        jobSatLabel.setText(String.format("Job Sat.: %.0f%%",    pg.getJobSatisfaction()));
        healthSatLabel.setText(String.format("Health Sat.: %.0f%%", pg.getHealthSatisfaction()));
        safetySatLabel.setText(String.format("Safety Sat.: %.0f%%", pg.getSafetySatisfaction()));

        boolean powered = controller.hasPower();
        energyLabel.setText(powered ? "Power: OK" : "Power: BLACKOUT");
        energyLabel.setStyle("-fx-text-fill: " + (powered ? "#3fb950" : "#f85149") + "; -fx-font-weight: bold;");

        applyAlerts(s);
    }

    /** Colora di rosso le metriche oltre soglia critica (AC-04.4/13.3). */
    public void applyAlerts(CityState s) {
        budgetLabel.setStyle("-fx-text-fill: "    + (s.getBudget()    <  500 ? "#f85149" : "#facc15") + ";");
        happinessLabel.setStyle("-fx-text-fill: " + (s.getHappiness() <   25 ? "#f85149" : "#fb923c") + ";");
        healthLabel.setStyle("-fx-text-fill: "    + (s.getHealth()    <   25 ? "#f85149" : "#f472b6") + ";");
        pollutionLabel.setStyle("-fx-text-fill: " + (s.getPollution() >   75 ? "#f85149" : "#4ade80") + ";");
    }

    /** Aggiunge una notifica temporanea (auto-rimossa dopo 10 s, max 15 messaggi). */
    public void log(String text, String color) {
        Label msg = new Label(text);
        msg.setStyle("-fx-text-fill: " + color + "; -fx-font-weight: bold; -fx-font-size: 11px;");
        msg.setWrapText(true);
        logPanel.getChildren().add(0, msg);
        if (logPanel.getChildren().size() > 15) {
            logPanel.getChildren().remove(15, logPanel.getChildren().size());
        }
        new Timeline(new KeyFrame(Duration.seconds(10), e -> logPanel.getChildren().remove(msg))).play();
    }

    private static Label metric(String text, FontAwesomeSolid icon, String hex) {
        FontIcon fi = new FontIcon(icon);
        fi.setIconSize(14);
        fi.setIconColor(Color.web(hex));
        Label lbl = new Label(text, fi);
        lbl.setStyle("-fx-text-fill: " + hex + ";");
        return lbl;
    }

    private static Label sectionTitle(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill: #8b949e; -fx-font-size: 10px; -fx-font-weight: bold;");
        return l;
    }
}
