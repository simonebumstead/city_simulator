package it.citylife.ui;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;

import it.citylife.model.CityState;
import it.citylife.model.StateObserver;
import it.citylife.ui.components.BuildToolbar;
import it.citylife.ui.components.DashboardChart;
import it.citylife.ui.components.DialogHelper;
import it.citylife.ui.components.MapGridView;
import it.citylife.ui.components.MetricsPanel;
import it.citylife.ui.components.SimulationControlsBar;
import it.citylife.ui.components.UISettings;
import javafx.animation.FadeTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

/**
 * Finestra principale di CityLogic. Assembla i componenti UI in
 * {@code ui/components/} e fa da {@link StateObserver}: a ogni tick distribuisce
 * lo stato aggiornato a {@link MetricsPanel}, {@link DashboardChart} e
 * {@link MapGridView}, e mostra le notifiche di evento (terremoto, budget
 * negativo, sovrappopolazione, edifici critici).
 */
public class DashboardView extends Application implements StateObserver {

    private static final String APP_FONT = "monospace";
    private static final String APP_NAME = "CityLogic";

    private SimulationController controller;
    private Stage primaryStage;
    private Label tickLabel;

    private MetricsPanel metricsPanel;
    private DashboardChart chartView;
    private MapGridView mapView;
    private SimulationControlsBar controlsBar;
    private StackPane mapOverlayPane;

    private boolean budgetWasNegative = false;
    private boolean wasOverpopulated = false;

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        controller = new SimulationController();
        controller.addObserver(this);
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #18191a; -fx-font-family: " + APP_FONT + ";");

        FontIcon headerIcon = new FontIcon(FontAwesomeSolid.CITY);
        headerIcon.setIconSize(18);
        headerIcon.setIconColor(Color.web("#2374e1"));
        tickLabel = new Label(APP_NAME + "  |  Tick: 0", headerIcon);
        tickLabel.setStyle("-fx-text-fill: #e4e6eb; -fx-font-size: 16px; -fx-font-weight: bold; -fx-padding: 12px 16px; -fx-background-color: #242526; -fx-border-color: #2374e1; -fx-border-width: 0 0 2px 0;");
        root.setTop(tickLabel);

        metricsPanel = new MetricsPanel(controller);
        chartView = new DashboardChart();

        // BuildToolbar deve esistere prima del MapGridView per fornire il selectedTool
        BuildToolbar buildToolbar = new BuildToolbar(controller, primaryStage, metricsPanel, () -> {
            mapView.refresh();
            metricsPanel.update(controller.getState());
        });
        mapView = new MapGridView(controller, primaryStage, metricsPanel, buildToolbar::getSelectedTool);
        controlsBar = new SimulationControlsBar(controller, primaryStage, metricsPanel, mapView, chartView);

        BorderPane mapPane = new BorderPane();
        VBox toolbarNode = buildToolbar.getNode();
        toolbarNode.prefWidthProperty().bind(Bindings.min(mapPane.widthProperty().multiply(0.20), 350));
        mapPane.setLeft(toolbarNode);

        // Usiamo uno StackPane per racchiudere la mappa. Ci permetterà di centrarci sopra qualsiasi popup!
        mapOverlayPane = new StackPane();
        mapOverlayPane.getChildren().add(mapView.getNode());
        mapPane.setCenter(mapOverlayPane);

        TabPane tabPane = new TabPane();
        Tab mapTab  = new Tab("City Map",  mapPane);
        Tab dashTab = new Tab("Dashboard", chartView.getNode());
        mapTab.setClosable(false);
        dashTab.setClosable(false);
        tabPane.getTabs().addAll(mapTab, dashTab);
        root.setCenter(tabPane);

        VBox metricsNode = metricsPanel.getNode();
        metricsNode.prefWidthProperty().bind(Bindings.min(root.widthProperty().multiply(0.22), 400));
        root.setRight(metricsNode);

        root.setBottom(controlsBar.getNode());

        primaryStage.setTitle(APP_NAME);
        Scene scene = new Scene(root, 1300, 750);
        scene.getStylesheets().add(getClass().getResource("/it/citylife/ui/dashboard.css").toExternalForm());

        scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.SPACE) {
                controlsBar.performManualTick();
                event.consume();
            }
        });

        primaryStage.setScene(scene);
        primaryStage.getIcons().clear();
        try (var stream = getClass().getResourceAsStream("/it/citylife/ui/icon.png")) {
            if (stream != null) primaryStage.getIcons().add(new Image(stream));
        } catch (Exception ignored) {
            // Ignored: if the icon fails to load, the app will run without it. This is not a critical error.
        }

        UISettings.loadWindowBounds(primaryStage);
        primaryStage.show();
        showStartupDialog();
    }

    private void showStartupDialog() {
        // Il gioco parte sempre con uno stato "nuova partita".
        // Aggiorniamo subito la UI per mostrare questo stato iniziale.
        controlsBar.syncPolicyButtonWithModel();
        onStateChanged(controller.getState());

        List<Path> saves;
        try { saves = controller.listSaves(); } catch (IOException ex) { return; }

        if (saves.isEmpty()) {
            // Nessun salvataggio, mostriamo solo un semplice messaggio di benvenuto non modale.
            Alert welcome = new Alert(Alert.AlertType.INFORMATION);
            welcome.setGraphic(null);
            welcome.setTitle(APP_NAME);
            welcome.setHeaderText("🎮 Welcome to " + APP_NAME + "!");
            welcome.setContentText("No saves found. Press Start to begin your journey!");
            welcome.getButtonTypes().setAll(new ButtonType("Start", ButtonBar.ButtonData.OK_DONE));

            welcome.initModality(Modality.NONE); // Rende il dialog non-modale
            DialogHelper.style(welcome, "dialog-info", primaryStage);
            welcome.show(); // Mostra il dialog senza bloccare la finestra principale
            return;
        }

        // Trovati dei salvataggi, offriamo la possibilità di caricare l'ultimo.
        Path latest = saves.get(saves.size() - 1);
        ButtonType newGameType  = new ButtonType("New Game",  ButtonBar.ButtonData.LEFT);
        ButtonType loadGameType = new ButtonType("Load Save", ButtonBar.ButtonData.RIGHT);

        Alert dialog = new Alert(Alert.AlertType.CONFIRMATION);
        dialog.setGraphic(null);
        dialog.setTitle(APP_NAME);
        dialog.setHeaderText("🎮 Welcome back to " + APP_NAME + "!");
        dialog.setContentText("Start a new city or resume your previous one?");
        dialog.getButtonTypes().setAll(newGameType, loadGameType);

        dialog.initModality(Modality.NONE); // Rende il dialog non-modale
        DialogHelper.style(dialog, "dialog-info", primaryStage);

        // Mostra il dialog e gestisce il risultato in modo asincrono.
        dialog.show();
        dialog.resultProperty().addListener((obs, old, result) -> {
            if (result == loadGameType) {
                try {
                    controlsBar.setTickCount(controller.load(latest));
                    metricsPanel.log("Game restored: " + latest.getFileName(), "#58a6ff");
                    controlsBar.syncPolicyButtonWithModel();
                    onStateChanged(controller.getState());
                } catch (IOException ex) {
                    DialogHelper.showError(primaryStage, "Load error", ex.getMessage());
                }
            }
            // Se l'utente sceglie "New Game" o chiude il dialog, non facciamo nulla,
            // perché lo stato di "nuova partita" è già attivo e visualizzato.
        });
    }

    @Override
    public void onStateChanged(CityState state) {
        Platform.runLater(() -> {
            int tick = controlsBar.getTickCount();
            tickLabel.setText(APP_NAME + "  |  Tick: " + tick);

            metricsPanel.update(state);
            chartView.update(tick, state);

            if (state.isEarthquakeOccurred()) {
                chartView.showEarthquakeAlert(tick);
                metricsPanel.log("EARTHQUAKE!", "#f38ba8");
                showEarthquakeWarning();
            }
            if (state.getCriticalBuildingCount() > 0) {
                metricsPanel.log(state.getCriticalBuildingCount() + " building(s) in critical condition! (HP < 20%)", "#f9e64f");
            }
            if (state.getBudget() < 0 && !budgetWasNegative) {
                metricsPanel.log("NEGATIVE BUDGET! The city is in deficit.", "#f85149");
                budgetWasNegative = true;
            } else if (state.getBudget() >= 0) {
                budgetWasNegative = false;
            }
            if (state.isOverpopulated() && !wasOverpopulated) {
                metricsPanel.log("OVERPOPULATION! Not enough homes. Happiness and Health dropping!", "#f85149");
                wasOverpopulated = true;
            } else if (!state.isOverpopulated()) {
                wasOverpopulated = false;
            }

            mapView.refresh();
        });
    }

    /**
     * Mostra un avviso popup centrato perfettamente sulla mappa per il Terremoto.
     */
    private void showEarthquakeWarning() {
        if (mapOverlayPane == null) return;

        VBox popup = new VBox(15);
        popup.setAlignment(Pos.CENTER);
        popup.setMouseTransparent(true);
        
        // Lo StackPane rispetta il maxSize impostato e posizionerà il popup al millimetro al centro
        popup.setMaxSize(450, 250); 

        // Design rosso/nero molto più minaccioso
        popup.setStyle("-fx-background-color: rgba(40, 0, 0, 0.95); " +
                       "-fx-background-radius: 15; " +
                       "-fx-border-color: #ff3333; " +
                       "-fx-border-width: 4; " +
                       "-fx-border-radius: 15; " +
                       "-fx-padding: 30; " +
                       "-fx-effect: dropshadow(gaussian, rgba(255, 50, 50, 0.8), 20, 0.5, 0, 0);");

        FontIcon warningIcon = new FontIcon(FontAwesomeSolid.EXCLAMATION_TRIANGLE);
        warningIcon.setIconSize(80);
        warningIcon.setIconColor(Color.web("#ff3333"));

        Label warningText = new Label("EARTHQUAKE!");
        warningText.setStyle("-fx-text-fill: #ff3333; -fx-font-size: 45px; -fx-font-weight: bold; -fx-font-family: " + APP_FONT + ";");

        popup.getChildren().addAll(warningIcon, warningText);
        mapOverlayPane.getChildren().add(popup);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(200), popup);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);

        FadeTransition fadeOut = new FadeTransition(Duration.millis(500), popup);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);
        fadeOut.setDelay(Duration.millis(2000)); // Allungato leggermente per maggiore visibilità
        fadeOut.setOnFinished(e -> mapOverlayPane.getChildren().remove(popup));

        fadeIn.setOnFinished(e -> fadeOut.play());
        fadeIn.play();
    }
}
