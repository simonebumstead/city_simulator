package it.citylife.model;

public class Park extends Structure {

    public Park() {
        super(200);
    }

    @Override
    public void applyEffects(CityState state, PowerNetwork power) {
        state.updateBudget(-3);
        state.updatePollution(-0.5);
        state.updateHappiness(1);
        state.updateHealth(0.5);
    }

    @Override
    public StructureType getType() {
        return StructureType.PARK;
    }

    @Override
    public int getConstructionCost() { return 300; }
}
