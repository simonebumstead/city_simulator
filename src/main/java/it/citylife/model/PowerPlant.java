package it.citylife.model;

public class PowerPlant extends Structure {

    public PowerPlant() {
        super(100);
    }

    @Override
    public void applyEffects(CityState state, PowerNetwork power) {
        state.updateBudget(-10);
        state.updatePollution(2);
        state.updateHappiness(-0.5);
        state.updateHealth(-0.5);
        power.addProduction(200);
    }

    @Override
    public StructureType getType() {
        return StructureType.POWER_PLANT;
    }

    @Override
    public int getConstructionCost() { return 2000; }
}
