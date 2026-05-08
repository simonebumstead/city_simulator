package it.citylife.model;

public class ResidentialBuilding extends Structure {

    public ResidentialBuilding() {
        super(300);
    }

    @Override
    public void applyEffects(CityState state, PowerNetwork power) {
        state.updateBudget(-5);
        state.updateHappiness(0.5);
        power.addConsumption(10);
    }

    @Override
    public StructureType getType() {
        return StructureType.RESIDENTIAL;
    }

    @Override
    public int getConstructionCost() { return 500; }
}
