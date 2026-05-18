package it.citylife.ui.components;

import java.util.Optional;

import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;

import it.citylife.model.Structure;
import it.citylife.model.StructureType;
import it.citylife.ui.SimulationController;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;

/**
 * Pannello sinistro: pulsanti di costruzione/riparazione/upgrade.
 * Mantiene lo stato dello strumento attivo ({@link #getSelectedTool()}).
 */
public final class BuildToolbar {

    private final SimulationController controller;
    private final Stage primaryStage;
    private final Runnable onUiChange;
    private final MetricsPanel metricsPanel;

    private final VBox root;
    private String selectedTool;
    private Button activeBuildBtn;

    public BuildToolbar(SimulationController controller, Stage primaryStage,
                        MetricsPanel metricsPanel, Runnable onUiChange) {
        this.controller = controller;
        this.primaryStage = primaryStage;
        this.metricsPanel = metricsPanel;
        this.onUiChange = onUiChange;

        Label buildTitle = sectionTitle("BUILD");

        Button resBtn   = toolButton("Residential", "RESIDENTIAL", FontAwesomeSolid.HOME,     IconCatalog.colorFor(StructureType.RESIDENTIAL));
        Button indBtn   = toolButton("Industrial",  "INDUSTRIAL",  FontAwesomeSolid.INDUSTRY, IconCatalog.colorFor(StructureType.INDUSTRIAL));
        Button comBtn   = toolButton("Commercial",  "COMMERCIAL",  FontAwesomeSolid.STORE,    IconCatalog.colorFor(StructureType.COMMERCIAL));
        Button ppBtn    = toolButton("Power Plant", "POWER_PLANT", FontAwesomeSolid.BOLT,     IconCatalog.colorFor(StructureType.POWER_PLANT));
        Button parkBtn  = toolButton("Park",        "PARK",        FontAwesomeSolid.TREE,     IconCatalog.colorFor(StructureType.PARK));
        Button hospBtn  = toolButton("Hospital",    "HOSPITAL",    FontAwesomeSolid.HOSPITAL, IconCatalog.colorFor(StructureType.HOSPITAL));
        Button wasteBtn = toolButton("Waste Center","WASTE_CENTER",FontAwesomeSolid.TRASH,    IconCatalog.colorFor(StructureType.WASTE_CENTER));
        Button roadBtn  = toolButton("Road",        "ROAD",        FontAwesomeSolid.ROAD,     IconCatalog.colorFor(StructureType.ROAD));
        Button repairBtn = toolButton("Repair",     "REPAIR",      FontAwesomeSolid.WRENCH,   Color.web("#a3e635"));
        Button demolBtn = toolButton("Demolish",    "DEMOLISH",    FontAwesomeSolid.HAMMER,   Color.web("#f38ba8"));

        FontIcon raIcon = new FontIcon(FontAwesomeSolid.TOOLS);
        raIcon.setIconSize(14);
        raIcon.setIconColor(Color.web("#a3e635"));
        Button repairAllBtn = new Button("Repair All", raIcon);
        repairAllBtn.setMaxWidth(Double.MAX_VALUE);
        repairAllBtn.setMinHeight(32);
        repairAllBtn.setStyle("-fx-border-color: #3e4042 #3e4042 #3e4042 #a3e635; -fx-border-width: 1px 1px 1px 3px;"
                + " -fx-font-size: 13px; -fx-graphic-text-gap: 8;");
        Tooltip raTt = new Tooltip("🔧 Repair All\nAutomatically calculates and pays\nthe cost to repair every building.");
        raTt.setShowDelay(Duration.millis(200));
        repairAllBtn.setTooltip(raTt);
        repairAllBtn.setOnAction(e -> showRepairAllPreview());

        Label upgradeTitle = sectionTitle("UPGRADE");
        Button seismicBtn      = toolButton("Seismic (500)", "UPGRADE_SEISMIC", FontAwesomeSolid.SHIELD_ALT, Color.web("#4599ff"));
        Button wasteThermalBtn = toolButton("Waste Thermal (700)", "UPGRADE_WASTE_THERMAL", FontAwesomeSolid.FIRE, Color.web("#f97316"));

        root = new VBox(8,
            buildTitle,
            resBtn, indBtn, comBtn, ppBtn, parkBtn, hospBtn, wasteBtn, roadBtn,
            new Separator(), repairBtn, repairAllBtn, demolBtn,
            new Separator(), upgradeTitle, seismicBtn, wasteThermalBtn
        );
        root.setPadding(new Insets(14));
        root.setMinWidth(160);
        root.setStyle("-fx-background-color: #242526; -fx-border-color: #3e4042; -fx-border-width: 0 1 0 0;");
    }

    public VBox getNode() { return root; }
    public String getSelectedTool() { return selectedTool; }

    private Button toolButton(String label, String tool, FontAwesomeSolid icon, Color iconColor) {
        FontIcon fi = new FontIcon(icon);
        fi.setIconSize(14);
        fi.setIconColor(iconColor);
        Button btn = new Button(label, fi);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setMinHeight(32);

        String hex = "#%02x%02x%02x".formatted(
            (int)(iconColor.getRed() * 255),
            (int)(iconColor.getGreen() * 255),
            (int)(iconColor.getBlue() * 255));
        String defaultStyle = "-fx-border-color: #3e4042 #3e4042 #3e4042 " + hex
                            + "; -fx-border-width: 1px 1px 1px 3px;"
                            + " -fx-font-size: 13px; -fx-graphic-text-gap: 8;";
        btn.setStyle(defaultStyle);
        btn.setUserData(defaultStyle);

        Tooltip tt = ToolTooltips.forTool(tool);
        if (tt != null) btn.setTooltip(tt);

        btn.setOnAction(e -> {
            if (activeBuildBtn != null) activeBuildBtn.setStyle((String) activeBuildBtn.getUserData());
            activeBuildBtn = btn;
            btn.setStyle("-fx-border-color: #2374e1; -fx-background-color: #242526;"
                    + " -fx-font-size: 13px; -fx-graphic-text-gap: 8;");
            selectedTool = tool;
        });
        return btn;
    }

    private void showRepairAllPreview() {
        int totalCost = 0;
        var grid = controller.getGrid();
        for (int x = 0; x < grid.getWidth(); x++) {
            for (int y = 0; y < grid.getHeight(); y++) {
                totalCost += controller.getEstimatedRepairCost(x, y);
            }
        }

        if (totalCost == 0) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setGraphic(null);
            alert.setTitle("🔧 Repair All");
            alert.setHeaderText("No buildings need repairs.");
            alert.setContentText("Your city is in perfect condition!");
            DialogHelper.style(alert, "dialog-info", primaryStage);
            alert.showAndWait();
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setGraphic(null);
        alert.setTitle("🔧 Repair All");
        alert.setHeaderText("Proceed with repairing all damaged buildings?");
        alert.setContentText("Estimated total cost: -" + totalCost + " $");
        DialogHelper.style(alert, "dialog-success", primaryStage);

        Optional<ButtonType> res = alert.showAndWait();
        if (res.isEmpty() || res.get() != ButtonType.OK) return;

        if (controller.getState().getBudget() < totalCost) {
            DialogHelper.showError(primaryStage, "Insufficient funds",
                    "You need " + totalCost + "$ to repair everything.");
            return;
        }
        for (int x = 0; x < grid.getWidth(); x++) {
            for (int y = 0; y < grid.getHeight(); y++) {
                var cell = grid.getCell(x, y);
                if (cell != null && cell.getStructure() instanceof Structure s
                        && !s.isDestroyed() && s.getHp() < s.getMaxHp()) {
                    s.fullRepair();
                }
            }
        }
        controller.getState().updateBudget(-totalCost);
        metricsPanel.log("Global repair completed (-" + totalCost + "$)", "#3fb950");
        if (onUiChange != null) onUiChange.run();
    }

    private static Label sectionTitle(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill: #b0b3b8; -fx-font-size: 10px; -fx-font-weight: bold;");
        return l;
    }
}
