package it.citylife.model;

import java.util.Random;

public class DisasterManager {

    /** AC-14.1: probabilità di terremoto per tick (configurabile qui). */
    public static final double EARTHQUAKE_PROBABILITY = 0.01;

    private final Random random = new Random();

    /**
     * Scatena un terremoto che colpisce l'intera città.
     * La magnitudo è un valore decimale casuale tra 1.0 e 7.0.
     * Il danno inflitto è proporzionale al quadrato della magnitudo, partendo da una base di 10.
     * Formula: Danno = 10 * (magnitudo^2)
     */
    public void triggerEarthquake(Grid grid, CityState state) {
        // 1. Genera una magnitudo casuale tra 1.0 e 7.0
        double magnitude = 1.0 + (random.nextDouble() * 6.0);

        // 2. Calcola il danno in base alla formula quadratica
        int damageToInflict = (int) (1 * Math.pow(magnitude, 2));

        // 3. Calcola il malus per Felicità e Salute (basi diverse per bilanciamento, sempre quadratiche)
        // Usiamo Math.pow() come richiesto per la proporzionalità quadratica
        int happinessMalus = (int) (1.5 * Math.pow(magnitude, 2));
        int healthMalus = (int) (0.5 * Math.pow(magnitude, 2));

        // Stampa un messaggio di DEBUG
        System.out.println(String.format("--- Earthquake of magnitude %.1f has struck the city! ---", magnitude));
        System.out.println(String.format("Each building will suffer %d damage.", damageToInflict));
        System.out.println(String.format("Citizens' happiness dropped by %d points and health by %d.", happinessMalus, healthMalus));

        // 4. Applica il malus a Felicità e Salute (non farli scendere sotto zero)
        state.updateHappiness(-happinessMalus);
        state.updateHealth(-healthMalus);

        int collapsedBuildings = 0;

        // 5. Itera su tutta la griglia e applica il danno a ogni edificio
        for (int x = 0; x < grid.getWidth(); x++) {
            for (int y = 0; y < grid.getHeight(); y++) {
                Cell targetCell = grid.getCell(x, y);

                if (targetCell != null && targetCell.getStructure() instanceof Structure building) {
                    building.onEarthquake(damageToInflict);
                    if (building.isDestroyed()) {
                        targetCell.setStructure(null);
                        collapsedBuildings++;
                    }
                }
            }
        }

        // Stampa un messaggio di DEBUG
        if (collapsedBuildings > 0) {
            System.out.println(String.format("%d buildings have collapsed.", collapsedBuildings));
        } else {
            System.out.println("Fortunately, no buildings collapsed.");
        }
    }
}
