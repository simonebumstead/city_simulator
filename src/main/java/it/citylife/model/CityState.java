package it.citylife.model;

/**
 * Contenitore di tutti i parametri numerici della simulazione della città.
 *
 * Segue un approccio a delta-accumulation: durante un tick gli edifici non
 * modificano direttamente le metriche reali, ma accumulano variazioni nei campi
 * delta (deltaBudget, deltaHappiness, ecc.) tramite i metodi update*().
 * Al termine del tick, resolveTick(PolicyModifiers) applica i moltiplicatori
 * della politica attiva, le penalità di soglia e i bonus fissi, poi committa
 * i delta ai valori reali con clamping [0, 100] per le metriche bounded.
 *
 * Le metriche bounded (happiness, health, pollution, wasteLevel) sono sempre
 * comprese tra MIN_VAL (0) e MAX_VAL (100). Il budget non ha limite superiore.
 *
 * @see City#updateState()
 * @see PolicyModifiers
 * @see PopulationGroup
 */
public class CityState {

    // Popolazione iniziale alla prima partita
    private static final int INITIAL_POPULATION = 10;

    // Soglia di wasteLevel oltre la quale si applicano penalità a pollution e happiness (AC-18.2)
    private static final double WASTE_POLLUTION_THRESHOLD = 50.0;

    // Budget di partenza del giocatore
    private static final double INITIAL_BUDGET = 5000.0;

    // Felicità iniziale (67 su 100: città mediamente soddisfatta all'avvio)
    private static final double INITIAL_HAPPINESS = 67.0;

    // Limite superiore delle metriche bounded (happiness, health, pollution, wasteLevel)
    private static final double MAX_VAL = 100.0;

    // Limite inferiore delle metriche bounded; nessuna metrica scende sotto zero
    private static final double MIN_VAL = 0.0;

    private double budget;       // Cassa della città; può superare 100, non ha limite superiore
    private int population;      // Numero corrente di abitanti
    private double pollution;    // Inquinamento atmosferico [0, 100]
    private double happiness;    // Indice di soddisfazione dei cittadini [0, 100]
    private double health;       // Salute pubblica [0, 100]
    private int wasteLevel;      // Livello di rifiuti accumulati [0, 100]

    // Numero di strutture con HP < 20% maxHp nel tick corrente; azzerato a ogni tick
    private int criticalBuildingCount = 0;

    // True se un terremoto è avvenuto nel tick corrente; usato dalla UI per la notifica
    private boolean earthquakeOccurred = false;

    // True se la città è in sovrappopolazione nel tick corrente; usato dalla UI per la notifica
    private boolean overpopulated = false;

    // Soddisfazioni demografiche del tick corrente (job, health, safety) — AC-19
    private PopulationGroup populationGroup = new PopulationGroup();

    // Delta accumulati durante il tick; azzerati dopo resolveTick()
    private double deltaBudget = 0.0;
    private double deltaIndustrialBudget = 0.0; // Quota industrial del budget, soggetta a moltiplicatore proprio
    private double deltaHappiness = 0.0;
    private double deltaHealth = 0.0;
    private double deltaPollution = 0.0;
    private double deltaWaste = 0.0;

    /**
     * Inizializza lo stato della città con i valori di partenza.
     * Pollution e wasteLevel partono da zero; health al massimo; budget e happiness
     * ai valori di default definiti dalle costanti.
     */
    public CityState() {
        this.budget = INITIAL_BUDGET;
        this.happiness = INITIAL_HAPPINESS;
        this.population = INITIAL_POPULATION;
        this.pollution = 0.0;
        this.health = 100.0;
        this.wasteLevel = 0;
    }

    // --- GETTER TOTALI ---
    public double getBudget() { return budget; }
    public int getPopulation() { return population; }
    public double getPollution() { return pollution; }
    public double getHappiness() { return happiness; }
    public double getHealth() { return health; }
    public int getWasteLevel() { return wasteLevel; }
    public int getCriticalBuildingCount() { return criticalBuildingCount; }
    public void incrementCriticalBuildings() { criticalBuildingCount++; }
    public void resetCriticalBuildings() { criticalBuildingCount = 0; }
    public boolean isEarthquakeOccurred() { return earthquakeOccurred; }
    public void setEarthquakeOccurred(boolean v) { this.earthquakeOccurred = v; }
    public boolean isOverpopulated() { return overpopulated; }
    public void setOverpopulated(boolean v) { this.overpopulated = v; }
    public PopulationGroup getPopulationGroup() { return populationGroup; }

    /**
     * Setter diretti: bypassano il meccanismo delta e scrivono immediatamente il valore.
     * Usati da DisasterManager per applicare danni istantanei e da SaveLoadManager
     * per ripristinare lo stato al caricamento di un salvataggio.
     * Le metriche bounded vengono clampate in [MIN_VAL, MAX_VAL].
     */
    public void setPopulation(int population) { this.population = population; }
    public void setBudget(double value) { this.budget = value; }
    public void setHappiness(double value) { this.happiness = Math.max(MIN_VAL, Math.min(MAX_VAL, value)); }
    public void setHealth(double value) { this.health = Math.max(MIN_VAL, Math.min(MAX_VAL, value)); }
    public void setPollution(double value) { this.pollution = Math.max(MIN_VAL, Math.min(MAX_VAL, value)); }
    public void setWasteLevel(int value) { this.wasteLevel = (int) Math.max(MIN_VAL, Math.min(MAX_VAL, value)); }

    /**
     * Accumula una variazione nel delta corrispondente.
     * Chiamati dagli edifici durante applyEffects(); il valore reale non cambia
     * finché non viene chiamato resolveTick().
     */
    public void updateBudget(double amount) { this.deltaBudget += amount; }
    public void updateHappiness(double amount) { this.deltaHappiness += amount; }
    public void updateHealth(double amount) { this.deltaHealth += amount; }
    public void updatePollution(double amount) { this.deltaPollution += amount; }
    public void updateWaste(double amount) { this.deltaWaste += amount; }

    /**
     * Risolve il tick: applica moltiplicatori, penalità e bonus fissi ai delta accumulati,
     * poi committa i valori finali alle metriche reali.
     *
     * Ordine delle operazioni:
     *   1. Applica i moltiplicatori della politica ai delta (es. GreenPolicy riduce deltaPollution)
     *   2. Somma i modificatori flat della politica (es. AusterityPolicy aggiunge +500 budget)
     *   3. Applica il malus happiness proporzionale alle soddisfazioni demografiche < 50 (AC-19.4)
     *   4. Se pollution > 30: penalità a happiness e health proporzionale all'eccesso
     *   5. Se wasteLevel > 50: penalità a pollution (+0.10×eccesso) e happiness (−0.05×eccesso) (AC-18.2)
     *   6. Decadimento naturale dell'inquinamento: −2/tick
     *   7. Commit dei delta ai valori reali con clamping [0, 100] per le metriche bounded
     *   8. Azzeramento dei delta per il tick successivo
     *
     * @param modifiers i modificatori della politica economica attiva
     * @see PolicyModifiers
     * @see City#updateState()
     */
    public void resolveTick(PolicyModifiers modifiers) {
        TickDeltas d = applyPolicyToDeltas(modifiers);
        if (this.overpopulated) halveBonuses(d);
        applyThresholdPenalties(d);
        d.pollution -= 2.0; // decadimento naturale
        commitDeltas(d);
        resetAccumulators();
    }

    private TickDeltas applyPolicyToDeltas(PolicyModifiers m) {
        TickDeltas d = new TickDeltas();
        d.happiness = deltaHappiness * m.getHappinessGenerationMultiplier() + m.getFixedHappinessChange();
        d.health    = deltaHealth    * m.getHealthGenerationMultiplier()    + m.getFixedHealthChange();
        d.pollution = deltaPollution * m.getPollutionGenerationMultiplier() + m.getFixedPollutionChange();
        d.waste     = deltaWaste     * m.getWasteGenerationMultiplier();

        double nonIndustrial = deltaBudget - deltaIndustrialBudget;
        d.budget = nonIndustrial + deltaIndustrialBudget * m.getIndustrialBudgetMultiplier()
                 + m.getFixedBudgetChange();
        return d;
    }

    private static void halveBonuses(TickDeltas d) {
        if (d.happiness > 0) d.happiness /= 2.0;
        if (d.health > 0)    d.health    /= 2.0;
    }

    private void applyThresholdPenalties(TickDeltas d) {
        // AC-19.4: malus da soddisfazioni demografiche basse
        double groupMalus = 0.0;
        if (populationGroup.getJobSatisfaction()    < 50.0) groupMalus += (50.0 - populationGroup.getJobSatisfaction())    * 0.04;
        if (populationGroup.getHealthSatisfaction() < 50.0) groupMalus += (50.0 - populationGroup.getHealthSatisfaction()) * 0.04;
        if (populationGroup.getSafetySatisfaction() < 50.0) groupMalus += (50.0 - populationGroup.getSafetySatisfaction()) * 0.04;
        d.happiness -= groupMalus;

        if (pollution > 30.0) {
            double penalty = (pollution - 30.0) * 0.15;
            d.happiness -= penalty;
            d.health    -= penalty * 1.5;
        }

        // AC-18.2: rifiuti oltre soglia generano inquinamento extra e riducono felicità
        if (wasteLevel > WASTE_POLLUTION_THRESHOLD) {
            double wastePenalty = (wasteLevel - WASTE_POLLUTION_THRESHOLD) * 0.10;
            d.pollution += wastePenalty;
            d.happiness -= wastePenalty * 0.5;
        }
    }

    private void commitDeltas(TickDeltas d) {
        budget    += d.budget;
        happiness  = clamp(happiness + d.happiness);
        health     = clamp(health    + d.health);
        pollution  = clamp(pollution + d.pollution);
        wasteLevel = (int) clamp(wasteLevel + d.waste);
    }

    private void resetAccumulators() {
        deltaBudget = 0;
        deltaIndustrialBudget = 0;
        deltaHappiness = 0;
        deltaHealth = 0;
        deltaPollution = 0;
        deltaWaste = 0;
    }

    private static double clamp(double v) {
        return Math.max(MIN_VAL, Math.min(MAX_VAL, v));
    }

    private static final class TickDeltas {
        double budget;
        double happiness;
        double health;
        double pollution;
        double waste;
    }

    /** Restituisce il delta budget accumulato nel tick corrente (prima di resolveTick). */
    public double getDeltaBudget() { return deltaBudget; }

    /** Aggiunge al delta industrial una quota di budget generata da edifici industriali. */
    public void addIndustrialBudgetDelta(double v) { this.deltaIndustrialBudget += v; }

    /** Azzera il delta industrial (usato internamente a resolveTick). */
    public void resetIndustrialDeltas() { this.deltaIndustrialBudget = 0; }

    public double getLastIndustrialBudgetDelta() { return deltaIndustrialBudget; }
}
