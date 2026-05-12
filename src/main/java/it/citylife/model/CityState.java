package it.citylife.model;

/**
 * CityState è una classe di tipo "Entity" (o Data Holder).
 * Il suo unico scopo è memorizzare i parametri numerici della simulazione.
 */
public class CityState {
    
    private static final int INITIAL_POPULATION = 10;
    private static final double WASTE_POLLUTION_THRESHOLD = 50.0;
    private static final double INITIAL_BUDGET = 5000.0;
    private static final double INITIAL_HAPPINESS = 67.0; 
    private static final double MAX_VAL = 100.0;
    private static final double MIN_VAL = 0.0;

    private double budget;
    private int population;
    private double pollution;
    private double happiness;
    private double health;
    private int wasteLevel;
    private int criticalBuildingCount = 0;
    private boolean earthquakeOccurred = false;
    private PopulationGroup populationGroup = new PopulationGroup();

    // --- VARIABILI PER IL TRACCIAMENTO DEI DELTA (Generazione per turno) ---
    private double deltaBudget = 0.0;
    private double deltaIndustrialBudget = 0.0;
    private double deltaHappiness = 0.0;
    private double deltaHealth = 0.0;
    private double deltaPollution = 0.0;
    private double deltaWaste = 0.0;

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
    public PopulationGroup getPopulationGroup() { return populationGroup; }

    // --- SETTER DIRETTI (Per i disastri o forzature) ---
    public void setPopulation(int population) { this.population = population; }
    public void setBudget(double value) { this.budget = value; }
    public void setHappiness(double value) { this.happiness = Math.max(MIN_VAL, Math.min(MAX_VAL, value)); }
    public void setHealth(double value) { this.health = Math.max(MIN_VAL, Math.min(MAX_VAL, value)); }
    public void setPollution(double value) { this.pollution = Math.max(MIN_VAL, Math.min(MAX_VAL, value)); }
    public void setWasteLevel(int value) { this.wasteLevel = (int) Math.max(MIN_VAL, Math.min(MAX_VAL, value)); }

    // --- METODI DI UPDATE (Chiamati dagli edifici) ---
    // Ora non modificano direttamente il totale, ma si accumulano nel Delta!
    public void updateBudget(double amount) { this.deltaBudget += amount; }
    public void updateHappiness(double amount) { this.deltaHappiness += amount; }
    public void updateHealth(double amount) { this.deltaHealth += amount; }
    public void updatePollution(double amount) { this.deltaPollution += amount; }
    public void updateWaste(double amount) { this.deltaWaste += amount; }
    
    // --- RISOLUZIONE DEL TURNO (Applica Moltiplicatori e Additivi Fissi) ---
    public void resolveTick(PolicyModifiers modifiers) {
        
        // 1. Applica i moltiplicatori di "generazione" al delta degli edifici
        double finalDeltaHappiness = (this.deltaHappiness * modifiers.getHappinessGenerationMultiplier());
        double finalDeltaHealth = (this.deltaHealth * modifiers.getHealthGenerationMultiplier());
        double finalDeltaPollution = (this.deltaPollution * modifiers.getPollutionGenerationMultiplier());
        double finalDeltaWaste = (this.deltaWaste * modifiers.getWasteGenerationMultiplier());
        double nonIndustrialBudget = this.deltaBudget - this.deltaIndustrialBudget;
        double finalDeltaBudget = nonIndustrialBudget + this.deltaIndustrialBudget * modifiers.getIndustrialBudgetMultiplier();

        // 2. Somma gli additivi fissi della policy
        finalDeltaHappiness += modifiers.getFixedHappinessChange();
        finalDeltaHealth += modifiers.getFixedHealthChange();
        finalDeltaPollution += modifiers.getFixedPollutionChange();
        finalDeltaBudget += modifiers.getFixedBudgetChange();

        // AC-19.4: malus happiness proporzionale ai bisogni insoddisfatti del gruppo demografico
        double groupMalus = 0.0;
        if (populationGroup.getJobSatisfaction()    < 50.0) groupMalus += (50.0 - populationGroup.getJobSatisfaction())    * 0.04;
        if (populationGroup.getHealthSatisfaction() < 50.0) groupMalus += (50.0 - populationGroup.getHealthSatisfaction()) * 0.04;
        if (populationGroup.getSafetySatisfaction() < 50.0) groupMalus += (50.0 - populationGroup.getSafetySatisfaction()) * 0.04;
        finalDeltaHappiness -= groupMalus;

        // 3. Se c'è troppo inquinamento, applica malus di base al delta
        if (this.pollution > 30.0) {
            double penalty = (this.pollution - 30.0) * 0.15;
            finalDeltaHappiness -= penalty;
            finalDeltaHealth -= (penalty * 1.5);
        }
        
        // Se i rifiuti superano la soglia, aumentano l'inquinamento e riducono la felicità
        if (this.wasteLevel > WASTE_POLLUTION_THRESHOLD) {
            double wastePenalty = (this.wasteLevel - WASTE_POLLUTION_THRESHOLD) * 0.10;
            finalDeltaPollution += wastePenalty;
            finalDeltaHappiness -= (wastePenalty * 0.5);
        }

        // L'inquinamento decade sempre un pochino naturalmente
        finalDeltaPollution -= 2.0;

        // 4. Applica finalmente i Delta totali ai parametri veri e propri, con il Clamping!
        this.budget += finalDeltaBudget;
        this.happiness = Math.max(MIN_VAL, Math.min(MAX_VAL, this.happiness + finalDeltaHappiness));
        this.health = Math.max(MIN_VAL, Math.min(MAX_VAL, this.health + finalDeltaHealth));
        this.pollution = Math.max(MIN_VAL, Math.min(MAX_VAL, this.pollution + finalDeltaPollution));
        this.wasteLevel = (int) Math.max(MIN_VAL, Math.min(MAX_VAL, this.wasteLevel + finalDeltaWaste));

        // 5. Azzera i delta per il turno successivo
        this.deltaBudget = 0;
        this.deltaIndustrialBudget = 0;
        this.deltaHappiness = 0;
        this.deltaHealth = 0;
        this.deltaPollution = 0;
        this.deltaWaste = 0;
    }

    public double getDeltaBudget() { return deltaBudget;}

    public void addIndustrialBudgetDelta(double v) { this.deltaIndustrialBudget += v; }
    public void resetIndustrialDeltas() { this.deltaIndustrialBudget = 0; }
    public void addIndustrialPollutionDelta(double v) {}
    public double getLastIndustrialBudgetDelta() { return deltaIndustrialBudget; }
    public double getLastIndustrialPollutionDelta() { return 0; }
}