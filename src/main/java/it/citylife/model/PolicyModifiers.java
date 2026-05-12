package it.citylife.model;

/**
 * Un semplice contenitore di dati (POJO) per trasportare i modificatori
 * di una policy al motore di gioco (che li userà nell'advanceTick).
 * 
 * I moltiplicatori agiscono sulla GENERAZIONE per turno, non sul totale.
 * I valori "Flat" vengono sommati o sottratti a prescindere dagli edifici.
 */
public class PolicyModifiers {

    // Moltiplicatori della generazione (1.0 = inalterata)
    private double pollutionGenerationMultiplier = 1.0;
    private double happinessGenerationMultiplier = 1.0;
    private double healthGenerationMultiplier = 1.0;

    // Addendi fissi a ogni turno
    private double fixedHappinessChange = 0.0;
    private double fixedHealthChange = 0.0;
    private double fixedPollutionChange = 0.0;
    private int fixedBudgetChange = 0;

    private double wasteGenerationMultiplier = 1.0;

    // --- Moltiplicatori vecchi (per retrocompatibilità se ci sono test) ---
    private double industrialBudgetMultiplier = 1.0;
    private double industrialPollutionMultiplier = 1.0;

    public PolicyModifiers() {}

    // Getters
    public double getPollutionGenerationMultiplier() { return pollutionGenerationMultiplier; }
    public double getHappinessGenerationMultiplier() { return happinessGenerationMultiplier; }
    public double getHealthGenerationMultiplier() { return healthGenerationMultiplier; }

    public double getFixedHappinessChange() { return fixedHappinessChange; }
    public double getFixedHealthChange() { return fixedHealthChange; }
    public double getFixedPollutionChange() { return fixedPollutionChange; }
    public int getFixedBudgetChange() { return fixedBudgetChange; }

    public double getIndustrialBudgetMultiplier() { return industrialBudgetMultiplier; }
    public double getIndustrialPollutionMultiplier() { return industrialPollutionMultiplier; }

    // Setters Moltiplicatori
    public PolicyModifiers setPollutionGenerationMultiplier(double value) { this.pollutionGenerationMultiplier = value; return this; }
    public PolicyModifiers setHappinessGenerationMultiplier(double value) { this.happinessGenerationMultiplier = value; return this; }
    public PolicyModifiers setHealthGenerationMultiplier(double value) { this.healthGenerationMultiplier = value; return this; }

    // Setters Flat Bonus
    public PolicyModifiers setFixedHappinessChange(double value) { this.fixedHappinessChange = value; return this; }
    public PolicyModifiers setFixedHealthChange(double value) { this.fixedHealthChange = value; return this; }
    public PolicyModifiers setFixedPollutionChange(double value) { this.fixedPollutionChange = value; return this; }
    public PolicyModifiers setFixedBudgetChange(int value) { this.fixedBudgetChange = value; return this; }

    public PolicyModifiers setIndustrialBudgetMultiplier(double value) { this.industrialBudgetMultiplier = value; return this; }
    public PolicyModifiers setIndustrialPollutionMultiplier(double value) { this.industrialPollutionMultiplier = value; return this; }


    public double getWasteGenerationMultiplier() { return wasteGenerationMultiplier; }
    public PolicyModifiers setWasteGenerationMultiplier(double value) { this.wasteGenerationMultiplier = value; return this; }
}