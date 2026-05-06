package it.citylife.model;

public class CommercialBuilding extends Structure {

    public CommercialBuilding() {
        super(100);
    }

    @Override
    public void applyEffects(CityState state, PowerNetwork power) {
        state.updateBudget(10);
        state.updatePollution(0.5);
        state.updateHappiness(0.5);
        power.addConsumption(10);
    }

    @Override
    public StructureType getType() {
        return StructureType.COMMERCIAL;
    }

    @Override
    public int getConstructionCost() { return 750; }
}
