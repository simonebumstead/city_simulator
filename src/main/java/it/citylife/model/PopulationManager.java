package it.citylife.model;

/**
 * Gestisce la crescita e il declino demografico della città ogni tick.
 *
 * La variazione di popolazione è calcolata come somma pesata degli effetti
 * di happiness, health e pollution rispetto a un punto neutro (50),
 * poi clampata in [MAX_DECLINE, MAX_GROWTH]. La crescita è bloccata
 * se nessun edificio residenziale è alimentato.
 *
 * Ogni tick aggiorna anche le tre soddisfazioni del {@link PopulationGroup} (AC-19.2/19.3):
 *   - jobSatisfaction:    (industrial + commercial) / residential × 100
 *   - healthSatisfaction: hospital / residential × 200
 *   - safetySatisfaction: 100 − pollution
 *   - safetySatisfaction: 100 − (criticalBuildings × 5) − malus disoccupazione
 *
 * Se la popolazione supera la capacità massima (residentialCount × 200),
 * si applica un malus diretto a happiness e health per sovrappopolazione.
 *
 * @see CityState
 * @see PopulationGroup
 * @see City#updateState()
 */
public class PopulationManager {

    // Peso della felicità sulla variazione demografica per tick
    private static final double HAPPINESS_WEIGHT = 0.15;

    // Peso della salute sulla variazione demografica per tick
    private static final double HEALTH_WEIGHT    = 0.10;

    // Peso dell'inquinamento sulla variazione demografica per tick (negativo: frena la crescita)
    private static final double POLLUTION_WEIGHT = -0.10;

    // Peso dei rifiuti sulla variazione demografica (definito ma attualmente commentato)
    private static final double WASTE_WEIGHT     = -0.05;

    // Crescita minima garantita ogni tick in condizioni normali
    private static final int BASE_GROWTH         = 1;

    // Valore di riferimento neutro per happiness, health e pollution (effetto zero)
    private static final int NEUTRAL_POINT       = 50;

    // Variazione massima positiva di popolazione per tick
    private static final int MAX_GROWTH          = 8;

    // Variazione massima negativa di popolazione per tick
    private static final int MAX_DECLINE         = -15;

    /**
     * Aggiorna la popolazione e le soddisfazioni demografiche per il tick corrente.
     *
     * Sequenza di operazioni:
     *   1. Calcola e aggiorna le soddisfazioni del PopulationGroup (AC-19.2/19.3)
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

        // AC-19.2/19.3: aggiorna le soddisfazioni del gruppo demografico
        PopulationGroup pg = state.getPopulationGroup();
        if (residentialCount == 0) {
            // Nessun residente: nessuna domanda insoddisfatta, tutte le soddisfazioni al massimo
            pg.setJobSatisfaction(100.0);
            pg.setHealthSatisfaction(100.0);
            pg.setSafetySatisfaction(100.0);
        } else {
            // jobSat: ogni edificio produttivo copre un residente; oltre 100% si clampa
            pg.setJobSatisfaction(Math.min(100.0, (industrialCount + commercialCount) * 100.0 / residentialCount));
            // healthSat: ogni ospedale copre 200 residenti (coefficiente 200)
            pg.setHealthSatisfaction(Math.min(100.0, hospitalCount * 200.0 / residentialCount));
            // safetySat: più inquinamento, meno sicurezza percepita
            pg.setSafetySatisfaction(Math.max(0.0, 100.0 - state.getPollution()));
        }

        // Calcolo del delta demografico: ogni metrica contribuisce in proporzione alla deviazione dal punto neutro
        double happinessEffect = (state.getHappiness() - NEUTRAL_POINT) * HAPPINESS_WEIGHT;
        double healthEffect    = (state.getHealth()    - NEUTRAL_POINT) * HEALTH_WEIGHT;
        double pollutionEffect = (state.getPollution() - NEUTRAL_POINT) * POLLUTION_WEIGHT;

        // Se la salute è critica (< 20) la felicità non può compensare il declino demografico
        double effectiveHappinessEffect = (state.getHealth() < 20.0) ? 0.0 : happinessEffect;

        int deltaPop = (int) Math.min(MAX_GROWTH, Math.max(MAX_DECLINE,
            BASE_GROWTH + effectiveHappinessEffect + healthEffect + pollutionEffect));

        // Senza corrente nei Residential la popolazione non cresce (può solo calare o restare stabile)
        if (!hasPowerNearby) {
            deltaPop = Math.min(0, deltaPop);
        }

        int currentPop = state.getPopulation();

        // Sovrappopolazione: la crescita è limitata a +1 e si applicano malus diretti su happiness e health
        if (currentPop > maxCapacity) {
            deltaPop = Math.min(1, deltaPop);

            // Setter diretti: il malus è istantaneo, fuori dal ciclo delta di fine tick
            state.setHappiness(state.getHappiness() - 2.0);
            state.setHealth(state.getHealth() - 1.0);

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
