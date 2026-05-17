package it.citylife.ui.components;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;

import it.citylife.model.Cell;
import it.citylife.model.Road;
import it.citylife.model.Structure;
import it.citylife.model.StructureDecorator;
import it.citylife.model.StructureType;
import it.citylife.ui.SimulationController;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.util.Duration;

/**
 * Tab "City Map": griglia 20×20 cliccabile + drag&build per strade/demolish/repair.
 * Mantiene lo stato di drag e la scala delle celle; delega le azioni al
 * {@link SimulationController}.
 */
public final class MapGridView {

    private final SimulationController controller;
    private final Stage primaryStage;
    private final MetricsPanel metricsPanel;
    private final Supplier<String> selectedToolSupplier;

    private final BorderPane root;
    private final StackPane[][] cells = new StackPane[20][20];
    private double currentCellSize = 35.0;

    private boolean isDragging = false;
    private int dragStartX = -1, dragStartY = -1, dragEndX = -1, dragEndY = -1;
    private boolean justFinishedDrag = false;

    public MapGridView(SimulationController controller, Stage primaryStage,
                       MetricsPanel metricsPanel, Supplier<String> selectedToolSupplier) {
        this.controller = controller;
        this.primaryStage = primaryStage;
        this.metricsPanel = metricsPanel;
        this.selectedToolSupplier = selectedToolSupplier;
        this.root = buildLayout();
    }

    public BorderPane getNode() { return root; }
    public void refresh() { updateGrid(); }

    private BorderPane buildLayout() {
        BorderPane pane = new BorderPane();
        StackPane gridWrapper = new StackPane();
        gridWrapper.setStyle("-fx-background-color: #101f13;");
        gridWrapper.setAlignment(Pos.CENTER);

        GridPane grid = buildGridPane();
        gridWrapper.getChildren().add(grid);

        gridWrapper.layoutBoundsProperty().addListener((obs, oldB, newB) -> {
            double w = newB.getWidth() - 20;
            double h = newB.getHeight() - 20;
            double min = Math.min(w, h);
            if (min <= 0) return;
            double cellSize = (min - 19) / 20.0;
            if (cellSize > 0 && Math.abs(currentCellSize - cellSize) > 0.5) {
                currentCellSize = cellSize;
                for (int x = 0; x < 20; x++) {
                    for (int y = 0; y < 20; y++) {
                        StackPane c = cells[x][y];
                        if (c != null) {
                            c.setPrefSize(cellSize, cellSize);
                            c.setMinSize(0, 0);
                            c.setMaxSize(cellSize, cellSize);
                        }
                    }
                }
                updateGrid();
            }
        });

        pane.setCenter(gridWrapper);
        return pane;
    }

    private GridPane buildGridPane() {
        GridPane grid = new GridPane();
        grid.setHgap(1);
        grid.setVgap(1);
        grid.setPadding(new Insets(10));
        grid.setStyle("-fx-background-color: #101f13;");
        grid.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);

        for (int x = 0; x < 20; x++) {
            for (int y = 0; y < 20; y++) {
                StackPane cell = new StackPane();
                cell.setPrefSize(currentCellSize, currentCellSize);
                String bg = ((x + y) % 2 == 0) ? "#1a3320" : "#152b1a";
                cell.setStyle("-fx-background-color: " + bg + "; -fx-border-color: #26472d; -fx-border-width: 0.5; -fx-background-radius: 2px; -fx-border-radius: 2px;");

                final int fx = x, fy = y;
                cell.setOnMouseClicked(e -> {
                    if (!justFinishedDrag && !isDragging) onCellClick(fx, fy);
                });
                cell.setOnDragDetected(e -> {
                    String tool = selectedToolSupplier.get();
                    if (isDragTool(tool)) {
                        cell.startFullDrag();
                        isDragging = true;
                        dragStartX = dragEndX = fx;
                        dragStartY = dragEndY = fy;
                        updateGrid();
                    }
                });
                cell.setOnMouseDragEntered(e -> {
                    if (isDragging && isDragTool(selectedToolSupplier.get())) {
                        dragEndX = fx;
                        dragEndY = fy;
                        updateGrid();
                    }
                });
                cell.setOnMouseDragReleased(e -> { if (isDragging) finishDragAndReset(); });

                cells[x][y] = cell;
                grid.add(cell, x, y);
            }
        }

        grid.setOnMouseReleased(e -> { if (isDragging) finishDragAndReset(); });
        grid.setOnMouseDragReleased(e -> { if (isDragging) finishDragAndReset(); });

        updateGrid();
        return grid;
    }

    private static boolean isDragTool(String t) {
        return "ROAD".equals(t) || "DEMOLISH".equals(t) || "REPAIR".equals(t);
    }

    private void finishDragAndReset() {
        finishDrag();
        justFinishedDrag = true;
        Platform.runLater(() -> justFinishedDrag = false);
    }

    private void onCellClick(int x, int y) {
        String tool = selectedToolSupplier.get();
        if (tool == null) return;
        boolean ok;
        try {
            ok = switch (tool) {
                case "DEMOLISH" -> controller.demolish(x, y);
                case "REPAIR" -> {
                    boolean r = controller.repair(x, y);
                    if (r) metricsPanel.log("Building repaired", "#3fb950");
                    yield r;
                }
                case "UPGRADE_SEISMIC" -> {
                    boolean r = controller.upgrade(x, y, "SEISMIC");
                    if (r) metricsPanel.log("Seismic Upgrade applied (-500$)", "#38bdf8");
                    yield r;
                }
                case "UPGRADE_WASTE_THERMAL" -> {
                    boolean r = controller.upgrade(x, y, "WASTE_THERMAL");
                    if (r) metricsPanel.log("Waste Thermal Upgrade applied (-700$)", "#f97316");
                    yield r;
                }
                default -> controller.placeBuilding(tool, x, y);
            };
        } catch (Exception ex) { ok = false; }

        if (!ok) {
            String error = controller.getLastError();
            metricsPanel.log(error != null && !error.isEmpty() ? error : "Cannot perform this action!",
                    error != null && !error.isEmpty() ? "#f85149" : "#f9e64f");
        }
        updateGrid();
        metricsPanel.update(controller.getState());
    }

    private void finishDrag() {
        isDragging = false;
        String tool = selectedToolSupplier.get();

        int minX = Math.min(dragStartX, dragEndX);
        int maxX = Math.max(dragStartX, dragEndX);
        int minY = Math.min(dragStartY, dragEndY);
        int maxY = Math.max(dragStartY, dragEndY);

        if ("ROAD".equals(tool)) {
            if (maxX - minX > maxY - minY) { minY = dragStartY; maxY = dragStartY; }
            else { minX = dragStartX; maxX = dragStartX; }
        }

        int toDemolish = 0, refund = 0, toRepair = 0, repairCost = 0;
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                Cell c = controller.getGrid().getCell(x, y);
                if (c != null && !c.isEmpty() && c.getStructure() instanceof Structure s) {
                    if ("DEMOLISH".equals(tool)) {
                        toDemolish++;
                        refund += (int)(s.getConstructionCost() * 0.4);
                    } else if ("REPAIR".equals(tool) && !s.isDestroyed() && s.getHp() < s.getMaxHp()) {
                        toRepair++;
                        repairCost += (s.getMaxHp() - s.getHp()) * 2;
                    }
                }
            }
        }

        if (toDemolish > 1 && "DEMOLISH".equals(tool) && !confirmArea("🔨 Demolish Area",
                "⚠️ WARNING: You are about to demolish " + toDemolish + " structures.",
                "Estimated refund: +" + refund + " $\nThis action cannot be undone. Proceed?",
                "dialog-error")) {
            resetDragSelection();
            return;
        }
        if (toRepair > 1 && "REPAIR".equals(tool) && !confirmArea("🔧 Repair Area",
                "You are about to repair " + toRepair + " structures.",
                "Estimated total cost: -" + repairCost + " $\nProceed?",
                "dialog-success")) {
            resetDragSelection();
            return;
        }

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                if ("DEMOLISH".equals(tool)) controller.demolish(x, y);
                else if ("ROAD".equals(tool)) {
                    Cell c = controller.getGrid().getCell(x, y);
                    if (c != null && c.isEmpty()) controller.placeBuilding("ROAD", x, y);
                }
                else if ("REPAIR".equals(tool)) controller.repair(x, y);
            }
        }
        resetDragSelection();
        metricsPanel.update(controller.getState());
    }

    private boolean confirmArea(String title, String header, String body, String style) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION);
        a.setGraphic(null);
        a.setTitle(title);
        a.setHeaderText(header);
        a.setContentText(body);
        DialogHelper.style(a, style, primaryStage);
        Optional<ButtonType> r = a.showAndWait();
        return r.isPresent() && r.get() == ButtonType.OK;
    }

    private void resetDragSelection() {
        dragStartX = dragStartY = dragEndX = dragEndY = -1;
        updateGrid();
    }

    private void updateGrid() {
        String tool = selectedToolSupplier.get();
        boolean showSelection = isDragging && isDragTool(tool);
        int selMinX = 0, selMaxX = 0, selMinY = 0, selMaxY = 0;
        if (showSelection) {
            selMinX = Math.min(dragStartX, dragEndX);
            selMaxX = Math.max(dragStartX, dragEndX);
            selMinY = Math.min(dragStartY, dragEndY);
            selMaxY = Math.max(dragStartY, dragEndY);
            if ("ROAD".equals(tool)) {
                if (selMaxX - selMinX > selMaxY - selMinY) { selMinY = dragStartY; selMaxY = dragStartY; }
                else { selMinX = dragStartX; selMaxX = dragStartX; }
            }
        }

        for (int x = 0; x < 20; x++) {
            for (int y = 0; y < 20; y++) {
                drawCell(x, y);
                if (showSelection && x >= selMinX && x <= selMaxX && y >= selMinY && y <= selMaxY) {
                    overlaySelection(x, y, tool);
                }
            }
        }
    }

    private void drawCell(int x, int y) {
        StackPane cell = cells[x][y];
        cell.getChildren().clear();
        Tooltip oldTip = (Tooltip) cell.getProperties().get("cellTooltip");
        if (oldTip != null) {
            Tooltip.uninstall(cell, oldTip);
            cell.getProperties().remove("cellTooltip");
        }
        cell.setOnMouseEntered(null);
        cell.setOnMouseExited(null);

        Cell gc = controller.getGrid().getCell(x, y);
        String emptyBg = ((x + y) % 2 == 0) ? "#1a3320" : "#152b1a";
        if (gc == null || gc.isEmpty()) {
            cell.setStyle("-fx-background-color: " + emptyBg + "; -fx-border-color: #26472d; -fx-border-width: 0.5; -fx-background-radius: 2px; -fx-border-radius: 2px;");
            return;
        }
        if (!(gc.getStructure() instanceof Structure s)) return;

        Color bg = IconCatalog.colorFor(s.getType());
        String hex = String.format("#%02x%02x%02x", (int)(bg.getRed()*255), (int)(bg.getGreen()*255), (int)(bg.getBlue()*255));

        Structure base = s.getBaseStructure();
        if (base instanceof Road road) {
            drawRoad(cell, emptyBg, road);
        } else {
            cell.setStyle("-fx-background-color: " + hex + "44; -fx-border-color: " + hex + "aa; -fx-border-width: 1.5; -fx-background-radius: 6px; -fx-border-radius: 6px;");
            FontIcon icon = new FontIcon(IconCatalog.iconFor(s.getType()));
            icon.setIconSize((int) (currentCellSize * 0.5));
            icon.setIconColor(bg);
            DropShadow shadow = new DropShadow();
            shadow.setColor(Color.web("#00000088"));
            shadow.setRadius(3.0);
            shadow.setSpread(0.1);
            icon.setEffect(shadow);
            cell.getChildren().add(icon);
        }

        if (s instanceof StructureDecorator dec) {
            FontIcon star = new FontIcon(FontAwesomeSolid.STAR);
            star.setIconSize(8);
            star.setIconColor(Color.web("#fde047"));
            Label badge = new Label("" + dec.getUpgradeLevel(), star);
            badge.setStyle("-fx-text-fill: #fde047; -fx-font-size: 9px; -fx-font-weight: bold; -fx-background-color: #000000aa; -fx-padding: 0 3px; -fx-background-radius: 3px;");
            StackPane.setAlignment(badge, Pos.TOP_LEFT);
            cell.getChildren().add(badge);
        }

        if (s.getHp() < s.getMaxHp() && s.getHp() > 0) {
            double barW = currentCellSize * 0.85;
            Rectangle hpBg  = new Rectangle(barW, 4, Color.web("#f85149"));
            Rectangle hpBar = new Rectangle(barW * ((double)s.getHp() / s.getMaxHp()), 4, Color.web("#3fb950"));
            VBox hpContainer = new VBox(new StackPane(hpBg, hpBar));
            hpContainer.setAlignment(Pos.BOTTOM_CENTER);
            hpContainer.setPadding(new Insets(0, 0, 2, 0));
            cell.getChildren().add(hpContainer);
        }

        boolean requiresPower = switch (s.getType()) {
            case RESIDENTIAL, COMMERCIAL, INDUSTRIAL, HOSPITAL, WASTE_CENTER -> true;
            default -> false;
        };
        boolean powerWarning = requiresPower && !controller.isPowered(x, y);

        Tooltip tt = buildCellTooltip(s, powerWarning);
        Tooltip.install(cell, tt);
        cell.getProperties().put("cellTooltip", tt);

        if (powerWarning) {
            FontIcon warn = new FontIcon(FontAwesomeSolid.EXCLAMATION_TRIANGLE);
            warn.setIconSize(10);
            warn.setIconColor(Color.web("#facc15"));
            StackPane.setAlignment(warn, Pos.TOP_RIGHT);
            cell.getChildren().add(warn);
            cell.setStyle("-fx-background-color: " + hex + "22; -fx-border-color: #f85149; -fx-border-width: 1;");
        }
    }

    private void drawRoad(StackPane cell, String emptyBg, Road road) {
        cell.setStyle("-fx-background-color: " + emptyBg + "; -fx-border-color: #26472d; -fx-border-width: 0.5; -fx-background-radius: 2px; -fx-border-radius: 2px;");

        double thickness = currentCellSize * 0.45;
        double length = currentCellSize * 0.28;
        Color roadBg = Color.web("#334155");
        Color marking = Color.web("#facc15");

        cell.getChildren().add(new Rectangle(thickness, thickness, roadBg));

        if (road.isConnectedNorth()) addArm(cell, thickness, length, roadBg, marking, Pos.TOP_CENTER, true);
        if (road.isConnectedSouth()) addArm(cell, thickness, length, roadBg, marking, Pos.BOTTOM_CENTER, true);
        if (road.isConnectedWest())  addArm(cell, thickness, length, roadBg, marking, Pos.CENTER_LEFT, false);
        if (road.isConnectedEast())  addArm(cell, thickness, length, roadBg, marking, Pos.CENTER_RIGHT, false);

        boolean vert = (road.isConnectedNorth() || road.isConnectedSouth()) && !road.isConnectedWest() && !road.isConnectedEast();
        boolean horiz = (road.isConnectedWest() || road.isConnectedEast()) && !road.isConnectedNorth() && !road.isConnectedSouth();
        boolean inter = (road.isConnectedNorth() || road.isConnectedSouth()) && (road.isConnectedWest() || road.isConnectedEast());

        if (inter) {
            Rectangle c = new Rectangle(currentCellSize * 0.17, currentCellSize * 0.17, Color.TRANSPARENT);
            c.setStroke(marking);
            c.setStrokeWidth(1.5);
            cell.getChildren().add(c);
        } else if (vert) {
            Line l = dashedLine(0, 0, 0, thickness, marking);
            cell.getChildren().add(l);
        } else if (horiz) {
            Line l = dashedLine(0, 0, thickness, 0, marking);
            cell.getChildren().add(l);
        } else {
            cell.getChildren().add(new Rectangle(4, 4, marking));
        }
    }

    private static void addArm(StackPane cell, double thickness, double length, Color bg, Color mark, Pos pos, boolean vertical) {
        Rectangle r = vertical ? new Rectangle(thickness, length, bg) : new Rectangle(length, thickness, bg);
        StackPane.setAlignment(r, pos);
        cell.getChildren().add(r);

        Line l = vertical ? dashedLine(0, 0, 0, length, mark) : dashedLine(0, 0, length, 0, mark);
        StackPane.setAlignment(l, pos);
        cell.getChildren().add(l);
    }

    private static Line dashedLine(double x1, double y1, double x2, double y2, Color stroke) {
        Line l = new Line(x1, y1, x2, y2);
        l.setStroke(stroke);
        l.setStrokeWidth(1.5);
        l.getStrokeDashArray().addAll(4d, 4d);
        return l;
    }

    private void overlaySelection(int x, int y, String tool) {
        String c = "#94a3b8";
        if ("DEMOLISH".equals(tool)) c = "#f85149";
        else if ("REPAIR".equals(tool)) c = "#a3e635";
        Rectangle ov = new Rectangle(currentCellSize, currentCellSize, Color.web(c + "88"));
        ov.setMouseTransparent(true);
        cells[x][y].getChildren().add(ov);
    }

    private Tooltip buildCellTooltip(Structure s, boolean powerWarning) {
        StringBuilder sb = new StringBuilder();
        sb.append(IconCatalog.labelFor(s.getType())).append("\n");
        sb.append("HP: ").append(s.getHp()).append(" / ").append(s.getMaxHp());
        if (s.isDestroyed()) sb.append("  [DESTROYED]");
        sb.append("\n");
        if (powerWarning) sb.append("⚠️ WARNING: Not powered!\n");
        else sb.append("Powered: ").append(s.isPowered() ? "Yes" : "No").append("\n");
        sb.append("Adjacent road: ").append(s.isConnectedToRoad() ? "Yes" : "No").append("\n");
        if (s instanceof StructureDecorator dec) {
            List<String> ups = dec.collectUpgrades();
            sb.append("Upgrade Lv.").append(dec.getUpgradeLevel());
            if (!ups.isEmpty()) sb.append(": ").append(String.join(", ", ups));
            sb.append("\n");
        }
        sb.append("Build cost: ").append(s.getConstructionCost());
        Tooltip tt = new Tooltip(sb.toString());
        tt.setShowDelay(Duration.millis(150));
        return tt;
    }

    @SuppressWarnings("unused")
    private static void unused(StructureType t) {} // keep StructureType import live
}
