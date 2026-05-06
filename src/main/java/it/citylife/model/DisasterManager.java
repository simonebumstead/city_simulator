package it.citylife.model;

import java.util.Random;

public class DisasterManager {
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
        int damageToInflict = (int) (10 * Math.pow(magnitude, 2));

        // 3. Calcola il malus per Felicità e Salute (basi diverse per bilanciamento, sempre quadratiche)
        // Usiamo Math.pow() come richiesto per la proporzionalità quadratica
        int happinessMalus = (int) (1.5 * Math.pow(magnitude, 2));
        int healthMalus = (int) (0.5 * Math.pow(magnitude, 2));

        // Stampa un messaggio di DEBUG
        System.out.println(String.format("---  Un terremoto di magnitudo %.1f ha colpito la città! ---", magnitude));
        System.out.println(String.format("Ogni edificio subirà %d danni.", damageToInflict));
        System.out.println(String.format("La felicità dei cittadini è scesa di %d punti e la salute di %d.", happinessMalus, healthMalus));

        // 4. Applica il malus a Felicità e Salute (non farli scendere sotto zero)
        state.updateHappiness(Math.max(0, state.getHappiness() - happinessMalus));
        state.updateHealth(Math.max(0, state.getHealth() - healthMalus));

        int collapsedBuildings = 0;

        // 5. Itera su tutta la griglia e applica il danno a ogni edificio
        for (int x = 0; x < grid.getHeight(); x++) {
            for (int y = 0; y < grid.getWidth(); y++) {
                Cell targetCell = grid.getCell(x, y);

                if (targetCell != null && !targetCell.isEmpty()) {
                    Structure building = targetCell.getStructure();
                    if (building != null) {
                        int remainingHp = building.takeDamage(damageToInflict);

                        if (remainingHp <= 0) {
                            targetCell.setStructure(null); // L'edificio è crollato
                            collapsedBuildings++;
                        }
                    }
                }
            }
        }

        // Stampa un messaggio di DEBUG
        if (collapsedBuildings > 0) {
            System.out.println(String.format("Sono crollati %d edifici", collapsedBuildings));
        } else {
            System.out.println("Fortunatamente, nessun edificio è crollato.");
        }
    }
}
