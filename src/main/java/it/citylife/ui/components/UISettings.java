package it.citylife.ui.components;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import javafx.stage.Stage;

/**
 * Gestisce il salvataggio e il caricamento delle impostazioni dell'interfaccia utente,
 * come le dimensioni e la posizione della finestra.
 */
public final class UISettings {

    private static final String CONFIG_DIR_NAME = ".citysimulator";
    private static final String CONFIG_FILE_NAME = "config.properties";
    private static final Path CONFIG_PATH;

    static {
        String userHome = System.getProperty("user.home");
        Path configDir = Paths.get(userHome, CONFIG_DIR_NAME);
        try {
            if (!Files.exists(configDir)) {
                Files.createDirectories(configDir);
            }
        } catch (IOException e) {
            System.err.println("Could not create config directory: " + e.getMessage());
        }
        CONFIG_PATH = configDir.resolve(CONFIG_FILE_NAME);
    }

    private UISettings() {}

    /** Salva posizione, dimensioni e stato (massimizzato) della finestra. */
    public static void saveWindowBounds(Stage stage) {
        Properties props = new Properties();
        if (Files.exists(CONFIG_PATH)) {
            try (FileInputStream in = new FileInputStream(CONFIG_PATH.toFile())) {
                props.load(in);
            } catch (IOException e) {
                // Ignora: se il file è corrotto, verrà sovrascritto.
            }
        }

        if (stage.isFullScreen()) {
            props.setProperty("fullscreen", "true");
        } else if (stage.isMaximized()) {
            // Se massimizzato, salva lo stato ma non le dimensioni, per preservarle al ripristino.
            props.setProperty("fullscreen", "false");
            props.setProperty("maximized", "true");
        } else {
            // Se in stato normale, salva tutto.
            props.setProperty("fullscreen", "false");
            props.setProperty("maximized", "false");
            props.setProperty("x", String.valueOf(stage.getX()));
            props.setProperty("y", String.valueOf(stage.getY()));
            props.setProperty("width", String.valueOf(stage.getWidth()));
            props.setProperty("height", String.valueOf(stage.getHeight()));
        }
        try (FileOutputStream out = new FileOutputStream(CONFIG_PATH.toFile())) {
            props.store(out, "City Simulator UI Settings");
        } catch (IOException e) {
            System.err.println("Could not save window settings: " + e.getMessage());
        }
    }

    /** Carica e applica posizione e dimensioni salvate alla finestra. */
    public static void loadWindowBounds(Stage stage) {
        // Per soddisfare la richiesta, impostiamo sempre la finestra massimizzata all'avvio.
        // Questo permette di avere il gioco a tutto schermo mantenendo visibili la barra degli strumenti
        // e i pulsanti di controllo della finestra (X, -, ecc.).
        stage.setMaximized(true);
    }
}