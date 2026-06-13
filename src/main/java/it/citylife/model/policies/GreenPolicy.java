package it.citylife.model.policies;

import it.citylife.model.core.CityState;

/**
 * Politica ecologica: riduce l'inquinamento e migliora il benessere dei cittadini
 * a fronte di un costo fisso di mantenimento ogni tick.
 *
 * È la politica più equilibrata sul lungo periodo: sacrifica entrate di budget
 * per ottenere metriche ambientali e sanitarie migliori, riducendo anche le
 * penalità di soglia su pollution e wasteLevel applicate in resolveTick().
 *
 * Effetti per tick (applicati in {@link CityState#resolveTick}):
 *   - Pollution:  ×0.50 sul delta generato dagli edifici − 2.0 flat
 *   - Happiness:  ×1.20 sul delta generato dagli edifici + 1.0 flat
 *   - Health:     ×1.25 sul delta generato dagli edifici + 2.0 flat
 *   - Budget:     −200 flat (costo di mantenimento dei programmi verdi)
 *   - Waste:      ×0.80 sul delta generato dagli edifici
 *
 * @see PolicyStrategy
 * @see PolicyModifiers
 * @see CityState#resolveTick(PolicyModifiers)
 */
public class GreenPolicy implements PolicyStrategy {

    /**
     * Restituisce i modificatori della politica ecologica.
     *
     * I moltiplicatori agiscono sui delta accumulati dagli edifici nel tick;
     * i modificatori flat vengono sommati indipendentemente dalla presenza di edifici.
     *
     * @return un {@link PolicyModifiers} configurato con i valori della politica verde
     */
    @Override
    public PolicyModifiers getModifiers() {
        return new PolicyModifiers()
                // Dimezza l'inquinamento generato dagli edifici nel tick (incentivi alle emissioni ridotte)
                .setPollutionGenerationMultiplier(0.50)

                // Riduzione aggiuntiva flat: programmi di riforestazione e bonifiche ambientali
                .setFixedPollutionChange(-2)

                // Gli edifici producono il 20% di felicità in più (qualità della vita migliorata)
                .setHappinessGenerationMultiplier(1.20)

                // Gli edifici producono il 25% di salute in più (aria più pulita, meno malattie)
                .setHealthGenerationMultiplier(1.25)

                // Bonus flat: i cittadini apprezzano consapevolmente la scelta ecologica dell'amministrazione
                .setFixedHappinessChange(1.0)

                // Bonus flat sulla salute: effetto diretto della riduzione dell'inquinamento atmosferico
                .setFixedHealthChange(2.0)

                // Costo fisso di mantenimento: sussidi verdi, infrastrutture ecologiche e controlli ambientali
                .setFixedBudgetChange(-200)

                // L'economia verde riduce del 20% i rifiuti prodotti dagli edifici
                .setWasteGenerationMultiplier(0.8);
    }
}
