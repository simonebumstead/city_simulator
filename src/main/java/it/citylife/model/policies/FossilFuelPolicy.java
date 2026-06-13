package it.citylife.model.policies;

import it.citylife.model.core.CityState;

/**
 * Politica dei combustibili fossili: massimizza la produzione industriale e le entrate
 * fiscali, a scapito di un forte aumento dell'inquinamento e un degrado della salute pubblica.
 *
 * È la politica più redditizia a breve termine, ma penalizza pesantemente
 * le metriche ambientali e sanitarie. Va combinata con parchi e ospedali
 * per contenere gli effetti collaterali.
 *
 * Effetti per tick (applicati in {@link CityState#resolveTick}):
 *   - Pollution:  ×2.0 sul delta generato dagli edifici + 3.0 flat
 *   - Budget:     ×1.5 sul delta degli edifici industriali + 300 flat
 *   - Health:     −1.5 flat (smog cronico)
 *   - Waste:      ×1.2 sul delta generato dagli edifici (industria pesante)
 *
 * @see PolicyStrategy
 * @see PolicyModifiers
 * @see CityState#resolveTick(PolicyModifiers)
 */
public class FossilFuelPolicy implements PolicyStrategy {

    /**
     * Restituisce i modificatori della politica dei combustibili fossili.
     *
     * I moltiplicatori agiscono sui delta accumulati dagli edifici nel tick;
     * i modificatori flat vengono sommati indipendentemente dalla presenza di edifici.
     *
     * @return un {@link PolicyModifiers} configurato con i valori della politica fossile
     */
    @Override
    public PolicyModifiers getModifiers() {
        return new PolicyModifiers()
                // Raddoppia tutto l'inquinamento generato dagli edifici nel tick corrente
                .setPollutionGenerationMultiplier(2.0)

                // Emissioni di fondo da combustibili fossili, indipendenti dagli edifici presenti
                .setFixedPollutionChange(3.0)

                // Gli edifici industriali producono il 50% di budget in più (incentivi alla produzione)
                .setIndustrialBudgetMultiplier(1.5)

                // Degrado sanitario cronico causato dallo smog e dall'inquinamento atmosferico
                .setFixedHealthChange(-1.5)

                // Entrata fiscale fissa derivante dalle attività estrattive e industriali
                .setFixedBudgetChange(300)

                // L'industria pesante genera il 20% di rifiuti in più rispetto alla norma
                .setWasteGenerationMultiplier(1.2);
    }
}
