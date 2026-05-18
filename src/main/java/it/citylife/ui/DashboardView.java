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
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
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
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

/**
 * Finestra principale di CityLogic. Assembla i componenti UI in
 * {@code ui/components/} e fa da {@link StateObserver}: a ogni tick distribuisce
 * lo stato aggiornato a {@link MetricsPanel}, {@link DashboardChart} e
 * {@link MapGridView}, e mostra le notifiche di evento (terremoto, budget
 * negativo, sovrappopolazione, edifici critici).
 */
public class DashboardView extends Application implements StateObserver {

    private static final String APP_FONT = "monospace";

    private SimulationController controller;
    private Stage primaryStage;
    private Label tickLabel;

    private MetricsPanel metricsPanel;
    private DashboardChart chartView;
    private MapGridView mapView;
    private BuildToolbar buildToolbar;
    private SimulationControlsBar controlsBar;

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
        tickLabel = new Label("CityLogic  |  Tick: 0", headerIcon);
        tickLabel.setStyle("-fx-text-fill: #e4e6eb; -fx-font-size: 16px; -fx-font-weight: bold; -fx-padding: 12px 16px; -fx-background-color: #242526; -fx-border-color: #2374e1; -fx-border-width: 0 0 2px 0;");
        root.setTop(tickLabel);

        metricsPanel = new MetricsPanel(controller);
        chartView = new DashboardChart();

        // BuildToolbar deve esistere prima del MapGridView per fornire il selectedTool
        buildToolbar = new BuildToolbar(controller, primaryStage, metricsPanel, () -> {
            mapView.refresh();
            metricsPanel.update(controller.getState());
        });
        mapView = new MapGridView(controller, primaryStage, metricsPanel, buildToolbar::getSelectedTool);
        controlsBar = new SimulationControlsBar(controller, primaryStage, metricsPanel, mapView, chartView);

        BorderPane mapPane = new BorderPane();
        VBox toolbarNode = buildToolbar.getNode();
        toolbarNode.prefWidthProperty().bind(Bindings.min(mapPane.widthProperty().multiply(0.20), 350));
        mapPane.setLeft(toolbarNode);
        mapPane.setCenter(mapView.getNode());

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

        primaryStage.setTitle("CityLogic");
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
        } catch (Exception ignored) {}

        primaryStage.show();
        showStartupDialog();
    }

    private void showStartupDialog() {
        List<Path> saves;
        try { saves = controller.listSaves(); } catch (IOException ex) { return; }

        if (saves.isEmpty()) {
            ButtonType startType = new ButtonType("Start", ButtonBar.ButtonData.OK_DONE);
            Alert welcome = new Alert(Alert.AlertType.INFORMATION);
            welcome.setGraphic(null);
            welcome.setTitle("CityLogic");
            welcome.setHeaderText("🎮 Welcome to CityLogic!");
            welcome.setContentText("No saves found. Press Start to begin your journey!");
            welcome.getButtonTypes().setAll(startType);
            DialogHelper.style(welcome, "dialog-info", primaryStage);
            welcome.showAndWait();
            controlsBar.syncPolicyButtonWithModel();
            return;
        }

        Path latest = saves.get(saves.size() - 1);
        ButtonType newGameType  = new ButtonType("New Game",  ButtonBar.ButtonData.LEFT);
        ButtonType loadGameType = new ButtonType("Load Save", ButtonBar.ButtonData.RIGHT);

        Alert dialog = new Alert(Alert.AlertType.CONFIRMATION);
        dialog.setGraphic(null);
        dialog.setTitle("CityLogic");
        dialog.setHeaderText("🎮 Welcome back to CityLogic!");
        dialog.setContentText("Start a new city or resume your previous one?");
        dialog.getButtonTypes().setAll(newGameType, loadGameType);
        DialogHelper.style(dialog, "dialog-info", primaryStage);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isEmpty() || result.get() != loadGameType) {
            controlsBar.syncPolicyButtonWithModel();
            return;
        }
        try {
            controlsBar.setTickCount(controller.load(latest));
            metricsPanel.log("Game restored: " + latest.getFileName(), "#58a6ff");
            controlsBar.syncPolicyButtonWithModel();
        } catch (IOException ex) {
            DialogHelper.showError(primaryStage, "Load error", ex.getMessage());
        }
    }

    @Override
    public void onStateChanged(CityState state) {
        Platform.runLater(() -> {
            int tick = controlsBar.getTickCount();
            tickLabel.setText("CityLogic  |  Tick: " + tick);

            metricsPanel.update(state);
            chartView.update(tick, state);

            if (state.isEarthquakeOccurred()) {
                chartView.showEarthquakeAlert(tick);
                metricsPanel.log("EARTHQUAKE!", "#f38ba8");
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
}
