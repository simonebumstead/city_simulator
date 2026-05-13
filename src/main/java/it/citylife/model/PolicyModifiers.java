package it.citylife.model;

/**
 * DTO (Data Transfer Object) che trasporta i modificatori di una politica economica
 * verso il motore di simulazione, che li applica in {@link CityState#resolveTick}.
 *
 * Contiene due categorie di modificatori:
 *   - Moltiplicatori di generazione: agiscono sui delta accumulati dagli edifici
 *     nel tick corrente (es. ×2.0 raddoppia tutto l'inquinamento prodotto).
 *     Valore neutro: 1.0.
 *   - Modificatori flat: vengono sommati algebricamente ai delta indipendentemente
 *     dalla presenza di edifici (es. −200 scala il budget anche senza costruzioni).
 *     Valore neutro: 0.
 *
 * I setter restituiscono this per permettere la costruzione fluente (method chaining),
 * come usato nelle implementazioni di {@link PolicyStrategy}.
 *
 * @see PolicyStrategy
 * @see CityState#resolveTick(PolicyModifiers)
 */
public class PolicyModifiers {

    // Moltiplicatori di generazione: applicati ai delta accumulati dagli edifici (neutro = 1.0)
    private double pollutionGenerationMultiplier = 1.0;
    private double happinessGenerationMultiplier = 1.0;
    private double healthGenerationMultiplier    = 1.0;
    private double wasteGenerationMultiplier     = 1.0;

    // Modificatori flat: sommati ai delta indipendentemente dagli edifici (neutro = 0)
    private double fixedHappinessChange  = 0.0;
    private double fixedHealthChange     = 0.0;
    private double fixedPollutionChange  = 0.0;
    private int    fixedBudgetChange     = 0;

    // Moltiplicatore dedicato al budget degli edifici industriali (usato da FossilFuelPolicy)
    private double industrialBudgetMultiplier    = 1.0;
    private double industrialPollutionMultiplier = 1.0;

    /** Crea un PolicyModifiers con tutti i valori neutri (moltiplicatori 1.0, flat 0). */
    public PolicyModifiers() {}

    // --- Getter moltiplicatori ---
    public double getPollutionGenerationMultiplier() { return pollutionGenerationMultiplier; }
    public double getHappinessGenerationMultiplier() { return happinessGenerationMultiplier; }
    public double getHealthGenerationMultiplier()    { return healthGenerationMultiplier; }
    public double getWasteGenerationMultiplier()     { return wasteGenerationMultiplier; }
    public double getIndustrialBudgetMultiplier()    { return industrialBudgetMultiplier; }
    public double getIndustrialPollutionMultiplier() { return industrialPollutionMultiplier; }

    // --- Getter flat ---
    public double getFixedHappinessChange() { return fixedHappinessChange; }
    public double getFixedHealthChange()    { return fixedHealthChange; }
    public double getFixedPollutionChange() { return fixedPollutionChange; }
    public int    getFixedBudgetChange()    { return fixedBudgetChange; }

    // --- Setter moltiplicatori (restituiscono this per method chaining) ---

    /** Imposta il moltiplicatore applicato al delta di inquinamento generato dagli edifici. */
    public PolicyModifiers setPollutionGenerationMultiplier(double value) { this.pollutionGenerationMultiplier = value; return this; }

    /** Imposta il moltiplicatore applicato al delta di felicità generato dagli edifici. */
    public PolicyModifiers setHappinessGenerationMultiplier(double value) { this.happinessGenerationMultiplier = value; return this; }

    /** Imposta il moltiplicatore applicato al delta di salute generato dagli edifici. */
    public PolicyModifiers setHealthGenerationMultiplier(double value) { this.healthGenerationMultiplier = value; return this; }

    /** Imposta il moltiplicatore applicato al delta di rifiuti generato dagli edifici. */
    public PolicyModifiers setWasteGenerationMultiplier(double value) { this.wasteGenerationMultiplier = value; return this; }

    /** Imposta il moltiplicatore applicato al solo delta budget degli edifici industriali. */
    public PolicyModifiers setIndustrialBudgetMultiplier(double value) { this.industrialBudgetMultiplier = value; return this; }

    /** Imposta il moltiplicatore applicato al solo delta inquinamento degli edifici industriali. */
    public PolicyModifiers setIndustrialPollutionMultiplier(double value) { this.industrialPollutionMultiplier = value; return this; }

    // --- Setter flat (restituiscono this per method chaining) ---

    /** Imposta il modificatore flat sommato al delta di felicità ogni tick. */
    public PolicyModifiers setFixedHappinessChange(double value) { this.fixedHappinessChange = value; return this; }

    /** Imposta il modificatore flat sommato al delta di salute ogni tick. */
    public PolicyModifiers setFixedHealthChange(double value) { this.fixedHealthChange = value; return this; }

    /** Imposta il modificatore flat sommato al delta di inquinamento ogni tick. */
    public PolicyModifiers setFixedPollutionChange(double value) { this.fixedPollutionChange = value; return this; }

    /** Imposta il modificatore flat sommato al delta di budget ogni tick. */
    public PolicyModifiers setFixedBudgetChange(int value) { this.fixedBudgetChange = value; return this; }
}
