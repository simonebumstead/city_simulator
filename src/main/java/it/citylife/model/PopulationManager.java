package it.citylife.model;

/**
 * Gestisce la logica di crescita o decrescita dei cittadini usando una formula a pesi.
 * Include logiche di sovrappopolazione in base al numero di edifici residenziali.
 */
public class PopulationManager {
    
    private static final double HAPPINESS_WEIGHT = 0.15;  
    private static final double HEALTH_WEIGHT    = 0.10;  
    private static final double POLLUTION_WEIGHT = -0.10; 
    private static final double WASTE_WEIGHT     = -0.05; 
    private static final int BASE_GROWTH         = 1;     
    private static final int NEUTRAL_POINT       = 50;
    private static final int MAX_GROWTH          = 8;     
    private static final int MAX_DECLINE         = -15;   

    public void updateDemographics(CityState state, boolean hasPowerNearby, int maxCapacity,
            int industrialCount, int commercialCount, int hospitalCount, int residentialCount) {

        // AC-19.2/19.3: aggiorna le soddisfazioni del gruppo demografico
        PopulationGroup pg = state.getPopulationGroup();
        if (residentialCount == 0) {
            // Nessun residente: nessuna domanda insoddisfatta
            pg.setJobSatisfaction(100.0);
            pg.setHealthSatisfaction(100.0);
            pg.setSafetySatisfaction(100.0);
        } else {
            pg.setJobSatisfaction(Math.min(100.0, (industrialCount + commercialCount) * 100.0 / residentialCount));
            pg.setHealthSatisfaction(Math.min(100.0, hospitalCount * 200.0 / residentialCount));
            pg.setSafetySatisfaction(Math.max(0.0, 100.0 - state.getPollution()));
        }

        // 1. Calcolo base della crescita
        double happinessEffect = (state.getHappiness() - NEUTRAL_POINT) * HAPPINESS_WEIGHT;
        double healthEffect = (state.getHealth() - NEUTRAL_POINT) * HEALTH_WEIGHT;
        double pollutionEffect = (state.getPollution() - NEUTRAL_POINT) * POLLUTION_WEIGHT;
        //double wasteEffect = (state.getWasteLevel() - NEUTRAL_POINT) * WASTE_WEIGHT;

        int deltaPop = (int) Math.min(MAX_GROWTH, Math.max(MAX_DECLINE,
            BASE_GROWTH + happinessEffect + healthEffect + pollutionEffect));

        // 2. Assenza di corrente (crescita bloccata)
        if (!hasPowerNearby) {
            deltaPop = Math.min(0, deltaPop);
        }

        int currentPop = state.getPopulation();
        
        // 3. Logica di Sovrappopolazione!
        if (currentPop > maxCapacity) {
            // Se siamo oltre il limite, la crescita massima consentita è +1
            deltaPop = Math.min(1, deltaPop);
            
            // Malus per sovrappopolazione: la gente sta stretta, i servizi scarseggiano
            // Poiché usiamo il nuovo sistema MVC, li aggiorniamo nei totali direttamente,
            // oppure possiamo usare i setter diretti dato che siamo a fine turno
            state.setHappiness(state.getHappiness() - 2.0);
            state.setHealth(state.getHealth() - 1.0);
            
            System.out.println("⚠️ OVERPOPULATION: Penalties applied to Happiness and Health.");
        }

        // 4. Applica la variazione di popolazione
        int newPop = Math.max(10, currentPop + deltaPop); 
        state.setPopulation(newPop);

        if (deltaPop > 0) {
            System.out.println(String.format("Population increased by %d. Total: %d (Capacity: %d)", deltaPop, newPop, maxCapacity));
        } else {
            System.out.println(String.format("Population decreased by %d. Total: %d (Capacity: %d)", -deltaPop, newPop, maxCapacity));
        }
    }
}