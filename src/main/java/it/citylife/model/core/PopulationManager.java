package it.citylife.model.core;

/**
 * Gestisce la crescita e il declino demografico della città ogni tick.
 *
 * La variazione di popolazione è calcolata come somma pesata degli effetti
 * di job, health e safety satisfaction, più gli indici generali di felicità
 * e salute, rispetto a un punto neutro (50), poi clampata in
 * [MAX_DECLINE, MAX_GROWTH]. La crescita è bloccata
 * se nessun edificio residenziale è alimentato.
 *
 * Ogni tick aggiorna anche le tre soddisfazioni del {@link PopulationGroup} (AC-25.2/25.3):
 *   - jobSatisfaction:    ((industrial × 200) + (commercial × 50)) / population × 100
 *   - healthSatisfaction: (hospital × 400) / population × 100
 *   - safetySatisfaction: 100 − pollution − (criticalBuildings × 5) − (terremoto ? 50 : 0)
 *
 * Se la popolazione supera la capacità massima (residentialCount × 200),
 * si applica un malus diretto a happiness e health per sovrappopolazione.
 *
 * @see CityState
 * @see PopulationGroup
 * @see City#updateState()
 */
public class PopulationManager {

    // Pesi delle soddisfazioni sulla variazione demografica per tick
    private static final double JOB_SAT_WEIGHT    = 0.10;
    private static final double HEALTH_SAT_WEIGHT = 0.10;
    private static final double SAFETY_SAT_WEIGHT = 0.10;

    // Pesi delle metriche generali sulla variazione demografica
    private static final double HAPPINESS_WEIGHT  = 0.05;
    private static final double HEALTH_WEIGHT     = 0.05;

    // Crescita minima garantita ogni tick in condizioni normali
    private static final int BASE_GROWTH         = 1;

    // Valore di riferimento neutro per happiness, health e pollution (effetto zero sul delta demografico).
    // Nota: la soglia "critica" che blocca la crescita (25) è deliberatamente asimmetrica rispetto
    // a NEUTRAL_POINT (50): i valori fra 25 e 50 riducono il delta ma non bloccano la crescita,
    // creando una "zona di allerta" dove la città rallenta senza entrare in crisi immediata.
    private static final int NEUTRAL_POINT       = 50;

    // Variazione massima positiva di popolazione per tick
    private static final int MAX_GROWTH          = 8;

    // Variazione massima negativa di popolazione per tick
    private static final int MAX_DECLINE         = -15;

    /**
     * Aggiorna la popolazione e le soddisfazioni demografiche per il tick corrente.
     *
     * Sequenza di operazioni:
     *   1. Calcola e aggiorna le soddisfazioni del PopulationGroup (AC-25.2/25.3)
     *   2. Calcola il delta demografico pesato su happiness, health e pollution
     *   3. Azzera l'effetto della felicità se la salute è critica (< 20)
     *   4. Blocca la crescita se nessun Residential è alimentato
     *   5. Applica malus diretti se la popolazione supera la capacità massima
     *   6. Aggiorna il contatore di popolazione (minimo garantito: 10)
     *
     * @param state           lo stato della città contenente metriche e PopulationGroup
     * @param hasPowerNearby  true se almeno un Residential è coperto da una PowerPlant
     * @param maxCapacity     capacità massima: residentialCount × 200
     * @param industrialCount numero di edifici industriali attivi nella griglia
     * @param commercialCount numero di edifici commerciali attivi nella griglia
     * @param hospitalCount   numero di ospedali attivi nella griglia
     * @param residentialCount numero di edifici residenziali attivi nella griglia
     */
    public void updateDemographics(CityState state, boolean hasPowerNearby, int maxCapacity,
            int industrialCount, int commercialCount, int hospitalCount, int residentialCount) {

        // AC-25.2/25.3: aggiorna le soddisfazioni del gruppo demografico
        PopulationGroup pg = state.getPopulationGroup();
        int currentPop = state.getPopulation();
        
        if (currentPop == 0) {
            // Nessuna popolazione: nessuna domanda insoddisfatta, tutte le soddisfazioni al massimo
            pg.setJobSatisfaction(100.0);
            pg.setHealthSatisfaction(100.0);
            pg.setSafetySatisfaction(100.0);
        } else {
            // jobSat: ogni industria garantisce 200 posti, ogni commerciale 50. Diviso per popolazione corrente
            double availableJobs = (industrialCount * 200.0) + (commercialCount * 50.0);
            pg.setJobSatisfaction(Math.min(100.0, availableJobs * 100.0 / currentPop));

            // healthSat: ogni ospedale cura 400 persone. Diviso per popolazione corrente
            double availableHealthCare = hospitalCount * 400.0;
            pg.setHealthSatisfaction(Math.min(100.0, availableHealthCare * 100.0 / currentPop));

            // safetySat: penalizzata da inquinamento, edifici danneggiati ed eventuali terremoti in corso
            double safetySat = 100.0 - (state.getPollution() / 4) - (state.getCriticalBuildingCount() * 5.0);
            if (state.isEarthquakeOccurred()) {
                safetySat -= 50.0;
            }
            pg.setSafetySatisfaction(Math.max(0.0, safetySat));
        }

        // Calcolo del delta demografico: basato sulle soddisfazioni demografiche...
        double jobEffect    = (pg.getJobSatisfaction()    - NEUTRAL_POINT) * JOB_SAT_WEIGHT;
        double healthSatEffect = (pg.getHealthSatisfaction() - NEUTRAL_POINT) * HEALTH_SAT_WEIGHT;
        double safetyEffect = (pg.getSafetySatisfaction() - NEUTRAL_POINT) * SAFETY_SAT_WEIGHT;
        
        // ...e sulle metriche generali di città
        double generalHappinessEffect = (state.getHappiness() - NEUTRAL_POINT) * HAPPINESS_WEIGHT;
        double generalHealthEffect    = (state.getHealth()    - NEUTRAL_POINT) * HEALTH_WEIGHT;

        int deltaPop = (int) Math.min(MAX_GROWTH, Math.max(MAX_DECLINE,
            BASE_GROWTH + jobEffect + healthSatEffect + safetyEffect + generalHappinessEffect + generalHealthEffect));

        // Se anche solo un parametro vitale è critico, la popolazione non può crescere.
        // Soglia 25 (metà di NEUTRAL_POINT): scelta deliberatamente sotto il punto neutro
        // per creare una "zona di allerta" [25,50] in cui il delta demografico rallenta
        // ma non si blocca completamente (design intenzionale, non asimmetria accidentale).
        boolean criticalConditions = state.getHappiness() < 25.0 ||
                                     state.getHealth() < 25.0 ||
                                     state.getPollution() > 75.0 ||
                                     pg.getJobSatisfaction() < 25.0 ||
                                     pg.getHealthSatisfaction() < 25.0 ||
                                     pg.getSafetySatisfaction() < 25.0;

        if (criticalConditions) {
            deltaPop = Math.min(0, deltaPop);
        }

        // Senza corrente nei Residential la popolazione non cresce (può solo calare o restare stabile)
        if (!hasPowerNearby) {
            deltaPop = Math.min(0, deltaPop);
        }

        // Sovrappopolazione: la crescita è limitata a +1 e si applicano malus diretti su happiness e health
        if (currentPop > maxCapacity) {
            deltaPop = Math.min(1, deltaPop);

            // Setter diretti: il malus è istantaneo, fuori dal ciclo delta di fine tick
            state.setHappiness(state.getHappiness() - 20.0);
            state.setHealth(state.getHealth() - 10.0);
            state.setOverpopulated(true);

            System.out.println("⚠️ OVERPOPULATION: Penalties applied to Happiness and Health.");
        }

        // Applica la variazione; la popolazione non scende mai sotto 10 (città non abbandona il minimo vitale)
        int newPop = Math.max(10, currentPop + deltaPop);
        state.setPopulation(newPop);

        if (deltaPop > 0) {
            System.out.println(String.format("Population increased by %d. Total: %d (Capacity: %d)", deltaPop, newPop, maxCapacity));
        } else {
            System.out.println(String.format("Population decreased by %d. Total: %d (Capacity: %d)", -deltaPop, newPop, maxCapacity));
        }
    }
}
