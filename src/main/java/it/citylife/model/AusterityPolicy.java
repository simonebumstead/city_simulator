package it.citylife.model;

/**
 * L'Austerity Policy impone una tassazione elevata e tagli alla spesa pubblica.
 * Risolleva rapidamente le casse della città, ma causa un forte malcontento
 * e un lento degrado della salute pubblica. 
 */
public class AusterityPolicy implements PolicyStrategy {

    @Override
    public PolicyModifiers getModifiers() {
        return new PolicyModifiers()
                // Malus FLAT sulla felicità e sulla salute! (-10 felicità fissa a turno)
                .setFixedHappinessChange(-5.0)
                .setFixedHealthChange(-2.0)
                
                // Tasse elevate
                .setFixedBudgetChange(500);
    }
}