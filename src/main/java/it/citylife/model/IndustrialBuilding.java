package it.citylife.model;

public class IndustrialBuilding extends Structure {

    public IndustrialBuilding() {
        super(100);
    }

    @Override
    public void applyEffects(CityState state, PowerNetwork power) {
        state.updateBudget(200);
        state.updatePollution(15);
        state.updateHappiness(-5);
        state.updateHealth(-3);
        power.addConsumption(20);
    }

    @Override
    public StructureType getType() {
        return StructureType.INDUSTRIAL;
    }

    @Override
    public int getConstructionCost() { return 1000; }
}
