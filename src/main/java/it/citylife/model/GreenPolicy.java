package it.citylife.model;

/**
 * La Green Policy si concentra sull'abbattimento dell'inquinamento e 
 * sul benessere dei cittadini (Felicità e Salute).
 * Ha un costo fisso di mantenimento a ogni turno.
 */
public class GreenPolicy implements PolicyStrategy {
    
    @Override
    public PolicyModifiers getModifiers() {
        return new PolicyModifiers()
                .setHappinessMultiplier(1.10)           // Aumenta la felicità del 10% a turno
                .setHealthMultiplier(1.05)              // Piccolo bonus anche alla salute (5%)
                .setFixedBudgetChange(-60)              // Costo fisso di mantenimento
                .setFixedHealthChange(3.0)              // Recupero additivo salute
                .setIndustrialPollutionMultiplier(0.70) // −30% inquinamento industriale (AC9.1)
                .setIndustrialBudgetMultiplier(0.80);   // −20% entrate industriali (AC9.2)
    }
}