package it.citylife.model;

/**
 * Gestisce la logica di crescita o decrescita dei cittadini usando una formula a pesi.
 * La variazione della popolazione è proporzionale allo stato della città.
 * Pattern GRASP: Pure Fabrication / Information Expert
 */
public class PopulationManager {
    
    // Costanti per il bilanciamento del gioco. Modificando questi pesi,
    // puoi rendere certi fattori più o meno importanti.
    private static final double HAPPINESS_WEIGHT = 0.15;  // era 0.3
    private static final double HEALTH_WEIGHT    = 0.10;  // era 0.2
    private static final double POLLUTION_WEIGHT = -0.10; // era -0.2
    private static final double WASTE_WEIGHT     = -0.05; // era -0.1
    private static final int BASE_GROWTH         = 1;     // era 2
    private static final int NEUTRAL_POINT       = 50;
    private static final int MAX_GROWTH          = 8;     // cap crescita massima/tick
    private static final int MAX_DECLINE         = -15;   // cap declino massimo/tick

    public void updateDemographics(CityState state, boolean hasPowerNearby) {
        // Calcola il contributo di ogni fattore in base a quanto si discosta dalla media (50)
        double happinessEffect = (state.getHappiness() - NEUTRAL_POINT) * HAPPINESS_WEIGHT;
        double healthEffect = (state.getHealth() - NEUTRAL_POINT) * HEALTH_WEIGHT;
        double pollutionEffect = (state.getPollution() - NEUTRAL_POINT) * POLLUTION_WEIGHT;
        double wasteEffect = (state.getWasteLevel() - NEUTRAL_POINT) * WASTE_WEIGHT;

        // Somma tutti i contributi e aggiungi la crescita di base, con cap
        int deltaPop = (int) Math.min(MAX_GROWTH, Math.max(MAX_DECLINE,
            BASE_GROWTH + happinessEffect + healthEffect + pollutionEffect + wasteEffect));

        // AC7.2: senza centrale elettrica vicina, la popolazione non può crescere
        if (!hasPowerNearby) {
            deltaPop = Math.min(0, deltaPop);
        }

        // Applica il cambiamento alla popolazione
        int currentPop = state.getPopulation();
        int newPop = Math.max(10, currentPop + deltaPop); // Evita che la popolazione scenda sotto 10
        state.setPopulation(newPop);

        // Log per il debug e per il giocatore
        if (deltaPop > 0) {
            System.out.println(String.format("Population grew by %d units. Total population: %d", deltaPop, newPop));
        } else {
            System.out.println(String.format("Population decreased by %d units. Total population: %d", -deltaPop, newPop));
        }
    }
}
