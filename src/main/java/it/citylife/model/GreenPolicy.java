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
                .setPollutionMultiplier(0.85) // Riduce l'inquinamento del 15% a turno
                .setHappinessMultiplier(1.10) // Aumenta la felicità del 10% a turno
                .setHealthMultiplier(1.05)    // Piccolo bonus anche alla salute (5%)
                .setFixedBudgetChange(-60);   // Costo fisso di mantenimento
    }
}