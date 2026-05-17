package it.citylife.ui.components;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 * Stile centralizzato per Alert/ChoiceDialog dell'app: sfondo trasparente,
 * CSS condiviso e owner agganciato allo {@link Stage} principale.
 */
public final class DialogHelper {

    private static final String CSS_PATH = "/it/citylife/ui/dashboard.css";

    private DialogHelper() {}

    public static void style(Dialog<?> dialog, String styleClass, Stage owner) {
        try { dialog.initStyle(StageStyle.TRANSPARENT); } catch (Exception ignored) {}

        if (dialog.getOwner() == null && owner != null) {
            try { dialog.initOwner(owner); } catch (Exception ignored) {}
        }

        DialogPane dp = dialog.getDialogPane();
        try {
            String css = DialogHelper.class.getResource(CSS_PATH).toExternalForm();
            if (!dp.getStylesheets().contains(css)) dp.getStylesheets().add(css);
        } catch (Exception ignored) {}

        if (dp.getScene() != null) {
            dp.getScene().setFill(Color.TRANSPARENT);
        } else {
            dp.sceneProperty().addListener((obs, oldS, newS) -> {
                if (newS != null) newS.setFill(Color.TRANSPARENT);
            });
        }

        dp.getStyleClass().add(styleClass);

        dialog.setOnShowing(evt -> Platform.runLater(() -> {
            if (dp.getScene() != null) {
                dp.getScene().setFill(Color.TRANSPARENT);
                if (dp.getScene().getWindow() != null) dp.getScene().getWindow().sizeToScene();
            }
        }));
    }

    public static void showError(Stage owner, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setGraphic(null);
        alert.setTitle("Error");
        alert.setHeaderText(header);
        alert.setContentText(content);
        style(alert, "dialog-error", owner);
        alert.showAndWait();
    }
}
