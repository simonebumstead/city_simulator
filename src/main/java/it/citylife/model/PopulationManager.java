package it.citylife.model;

/**
 * Gestisce la logica di crescita o decrescita dei cittadini usando una formula a pesi.
 * La variazione della popolazione è proporzionale allo stato della città.
 * Pattern GRASP: Pure Fabrication / Information Expert
 */
public class PopulationManager {
    
    // Costanti per il bilanciamento del gioco. Modificando questi pesi,
    // puoi rendere certi fattori più o meno importanti.
    private static final double HAPPINESS_WEIGHT = 1.0;
    private static final double HEALTH_WEIGHT = 0.8;
    private static final double POLLUTION_WEIGHT = -0.7; // Negativo perché un valore alto è un malus
    private static final double WASTE_WEIGHT = -0.5;     // Negativo perché un valore alto è un malus
    private static final int BASE_GROWTH = 5;           // Crescita naturale minima in condizioni stabili
    private static final int NEUTRAL_POINT = 50;        // Valore considerato "stabile" o "medio"

    public void updateDemographics(CityState state) {
        // Calcola il contributo di ogni fattore in base a quanto si discosta dalla media (50)
        double happinessEffect = (state.getHappiness() - NEUTRAL_POINT) * HAPPINESS_WEIGHT;
        double healthEffect = (state.getHealth() - NEUTRAL_POINT) * HEALTH_WEIGHT;
        double pollutionEffect = (state.getPollution() - NEUTRAL_POINT) * POLLUTION_WEIGHT;
        double wasteEffect = (state.getWasteLevel() - NEUTRAL_POINT) * WASTE_WEIGHT;

        // Somma tutti i contributi e aggiungi la crescita di base
        int deltaPop = (int) (BASE_GROWTH + happinessEffect + healthEffect + pollutionEffect + wasteEffect);
        
        // Applica il cambiamento alla popolazione
        int currentPop = state.getPopulation();
        int newPop = Math.max(10, currentPop + deltaPop); // Evita che la popolazione scenda sotto 10
        state.setPopulation(newPop);
        
        // Log per il debug e per il giocatore
        if (deltaPop > 0) {
            System.out.println(String.format("La popolazione è cresciuta di %d unità. Popolazione totale: %d", deltaPop, newPop));
        } else {
            System.out.println(String.format("La popolazione è diminuita di %d unità. Popolazione totale: %d", -deltaPop, newPop));
        }
    }
}
