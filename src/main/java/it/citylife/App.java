package it.citylife;

import it.citylife.ui.DashboardView;

/**
 * Punto di ingresso dell'applicazione CityLogic.
 *
 * Delega immediatamente l'avvio a {@link DashboardView#launch}, che è il metodo
 * standard di JavaFX per inizializzare l'Application thread e richiamare
 * {@link DashboardView#start(javafx.stage.Stage)}.
 *
 * Questa classe non contiene logica di dominio né di UI: esiste esclusivamente
 * per soddisfare il requisito di Maven/JavaFX di avere un main() esterno
 * alla classe Application.
 *
 * @see DashboardView
 */
public class App {

    /**
     * Metodo main: avvia l'applicazione JavaFX delegando a DashboardView.
     *
     * @param args argomenti della riga di comando (passati a JavaFX, tipicamente ignorati)
     */
    public static void main(String[] args) {
        DashboardView.launch(DashboardView.class, args);
    }
}
