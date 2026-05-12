package it.citylife.model;

/**
 * La Fossil Fuel Policy massimizza la produzione industriale e i profitti,
 * ma raddoppia l'inquinamento generato.
 */
public class FossilFuelPolicy implements PolicyStrategy {
    
    @Override
    public PolicyModifiers getModifiers() {
        return new PolicyModifiers()
                // Raddoppia l'inquinamento GENERATO in questo turno
                .setPollutionGenerationMultiplier(2.0)

                // Inquinamento base anche senza edifici industriali (emissioni generali)
                .setFixedPollutionChange(3.0)

                .setIndustrialBudgetMultiplier(1.5)
                // Malus fisso alla salute (lo smog fa male)
                .setFixedHealthChange(-1.5)

                // Entrata fissa (Bilanciata, prima era troppo alta a 800)
                .setFixedBudgetChange(300)

                // Più rifiuti prodotti (industria pesante)
                .setWasteGenerationMultiplier(1.2);
    }
}