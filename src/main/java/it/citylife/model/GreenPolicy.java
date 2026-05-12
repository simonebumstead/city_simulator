package it.citylife.model;

/**
 * La Green Policy si concentra sull'abbattimento dell'inquinamento prodotto
 * e sul benessere dei cittadini.
 * Ha un costo fisso di mantenimento a ogni turno.
 */
public class GreenPolicy implements PolicyStrategy {
    
    @Override
    public PolicyModifiers getModifiers() {
        return new PolicyModifiers()
                // Riduce l'inquinamento GENERATO in questo turno del 50%
                .setPollutionGenerationMultiplier(0.50)

                // Riduce l'inquinamento di 2
                .setFixedPollutionChange(-2)
                
                // Aumenta la felicità GENERATA dagli edifici del 20%
                .setHappinessGenerationMultiplier(1.20)

                // Aumenta la salute GENERATA dagli edifici del 25%
                .setHealthGenerationMultiplier(1.25)
                
                // Bonus fisso: le persone sono felici che la città sia green (+1 fissa)
                .setFixedHappinessChange(1.0)

                // Bonus fisso sulla salute (+2)
                .setFixedHealthChange(2.0)
                
                // Costo fisso di mantenimento
                .setFixedBudgetChange(-200)

                // Meno rifiuti prodotti (gestione verde dei consumi)
                .setWasteGenerationMultiplier(0.8);
    }
}