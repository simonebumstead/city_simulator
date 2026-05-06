package it.citylife.model;

/**
 * Un semplice contenitore di dati (POJO) per trasportare i modificatori
 * di una policy al motore di gioco (che li userà nell'advanceTick).
 * 
 * I valori di default rappresentano una "politica neutra" che non fa nulla.
 */
public class PolicyModifiers {

    private double pollutionMultiplier = 1.0;
    private double happinessMultiplier = 1.0;
    private double healthMultiplier = 1.0;
    private double wasteMultiplier = 1.0;
    private int fixedBudgetChange = 0; // Può essere positivo (bonus) o negativo (costo)

    // Costruttore vuoto (usa i valori di default)
    public PolicyModifiers() {}

    // Getters
    public double getPollutionMultiplier() { return pollutionMultiplier; }
    public double getHappinessMultiplier() { return happinessMultiplier; }
    public double getHealthMultiplier() { return healthMultiplier; }
    public double getWasteMultiplier() { return wasteMultiplier; }
    public int getFixedBudgetChange() { return fixedBudgetChange; }

    // Setters (per costruire l'oggetto in modo più leggibile)
    public PolicyModifiers setPollutionMultiplier(double value) {
        this.pollutionMultiplier = value;
        return this;
    }

    public PolicyModifiers setHappinessMultiplier(double value) {
        this.happinessMultiplier = value;
        return this;
    }

    public PolicyModifiers setHealthMultiplier(double value) {
        this.healthMultiplier = value;
        return this;
    }

    public PolicyModifiers setWasteMultiplier(double value) {
        this.wasteMultiplier = value;
        return this;
    }

    public PolicyModifiers setFixedBudgetChange(int value) {
        this.fixedBudgetChange = value;
        return this;
    }
}