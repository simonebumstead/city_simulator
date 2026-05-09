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
                .setHappinessMultiplier(0.95)           // Riduce leggermente la felicità (-5%)
                .setHealthMultiplier(0.97)              // Impatto negativo sulla salute (-3%)
                .setFixedBudgetChange(80)               // Entrata fissa
                .setFixedHealthChange(-1.0)             // Degrado additivo salute
                .setIndustrialBudgetMultiplier(1.30)    // +30% entrate industriali (AC10.1)
                .setIndustrialPollutionMultiplier(1.20); // +20% inquinamento industriale (AC10.2)
    }
}