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
                .setPollutionMultiplier(1.05) // Aumenta l'inquinamento del 5% a turno
                .setHappinessMultiplier(0.95) // Riduce leggermente la felicità (-5%)
                .setHealthMultiplier(0.97)    // Impatto negativo sulla salute (-3%)
                .setFixedBudgetChange(80);    // Entrata fissa
    }
}