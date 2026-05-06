package it.citylife.model;

/**
 * L'Austerity Policy impone una tassazione elevata e tagli alla spesa pubblica.
 * Risolleva rapidamente le casse della città, ma causa un forte malcontento
 * e un lento degrado della salute pubblica. Come effetto collaterale, 
 * riduce la produzione di rifiuti.
 */
public class AusterityPolicy implements PolicyStrategy {

    @Override
    public PolicyModifiers getModifiers() {
        return new PolicyModifiers()
                .setHappinessMultiplier(0.95) // Calo della felicità (-5%)
                .setHealthMultiplier(0.95)    // Lento declino dei servizi sanitari (-5%)
                .setWasteMultiplier(0.90)     // Meno consumi = meno rifiuti (-10%)
                .setFixedBudgetChange(200);   // Tasse elevate
    }
}