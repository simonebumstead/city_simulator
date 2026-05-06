package it.citylife.model;

/**
 * La Fossil Fuel Policy massimizza la produzione industriale e i profitti,
 * a scapito dell'ambiente e della salute pubblica.
 * Fornisce un'entrata fissa elevata a ogni turno.
 */
public class FossilFuelPolicy implements PolicyStrategy {
    
    @Override
    public PolicyModifiers getModifiers() {
        return new PolicyModifiers()
                .setPollutionMultiplier(1.25) // Aumenta l'inquinamento del 25% a turno
                .setHappinessMultiplier(0.95) // Riduce leggermente la felicità (-5%)
                .setHealthMultiplier(0.90)    // Impatto negativo sulla salute (-10%)
                .setFixedBudgetChange(800);   // Entrata fissa
    }
}