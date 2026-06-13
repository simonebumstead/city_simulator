package it.citylife.model.policies;

import it.citylife.model.core.CityState;

/**
 * Politica di austerità fiscale: impone tagli alla spesa pubblica e tassazione elevata.
 * Risolleva rapidamente il budget ma causa malcontento e degrado della salute pubblica.
 *
 * Effetti per tick: budget +500, felicità −5, salute −2.
 *
 * @see PolicyStrategy
 * @see PolicyModifiers
 * @see CityState#resolveTick(PolicyModifiers)
 */
public class AusterityPolicy implements PolicyStrategy {

    /**
     * Restituisce i modificatori flat applicati ogni tick dalla politica di austerità.
     * I valori vengono sommati ai delta accumulati prima del commit in CityState.
     *
     * @return PolicyModifiers con budget +500, felicità −5, salute −2
     */
    @Override
    public PolicyModifiers getModifiers() {
        return new PolicyModifiers()
                // Entrate straordinarie da tassazione elevata e tagli alla spesa
                .setFixedBudgetChange(500)

                // Malcontento generalizzato causato dai tagli ai servizi
                .setFixedHappinessChange(-15.0)

                // Degrado della sanità pubblica per riduzione dei fondi
                .setFixedHealthChange(-2.0);
    }
}
