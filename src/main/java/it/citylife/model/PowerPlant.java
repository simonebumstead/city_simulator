package it.citylife.model;

public class PowerPlant extends Structure {

    public PowerPlant() {
        super(500);
    }

    @Override
    public void applyEffects(CityState state, PowerNetwork power) {
        state.updateBudget(-20); // Alto costo di manutenzione
        state.updatePollution(3.5); // Le ciminiere inquinano a dismisura
        state.updateHappiness(-1.0);
        state.updateHealth(-1.0);
        power.addProduction(250); // Ma producono più energia
    }

    @Override
    public StructureType getType() {
        return StructureType.POWER_PLANT;
    }

    @Override
    public int getConstructionCost() { return 2000; }
}
