package it.citylife.model;

public class IndustrialBuilding extends Structure {

    public IndustrialBuilding() {
        super(400);
    }

    @Override
    public void applyEffects(CityState state, PowerNetwork power) {
        state.updateBudget(20);
        state.updatePollution(1.5);
        state.updateHappiness(-0.5);
        state.updateHealth(-0.3);
        power.addConsumption(20);
        state.addIndustrialBudgetDelta(20);
        state.addIndustrialPollutionDelta(1.5);
    }

    @Override
    public StructureType getType() {
        return StructureType.INDUSTRIAL;
    }

    @Override
    public int getConstructionCost() { return 1000; }
}
