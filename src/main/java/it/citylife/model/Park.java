package it.citylife.model;

public class Park extends Structure {

    public Park() {
        super(100);
    }

    @Override
    public void applyEffects(CityState state, PowerNetwork power) {
        state.updateBudget(-30);
        state.updatePollution(-5);
        state.updateHappiness(10);
        state.updateHealth(5);
    }

    @Override
    public StructureType getType() {
        return StructureType.PARK;
    }

    @Override
    public int getConstructionCost() { return 300; }
}
