package it.citylife.model;

public class WasteManagementCenter extends Structure {

    private static final double WASTE_REDUCTION_PER_TICK = 10.0;

    public WasteManagementCenter() {
        super(350);
    }

    @Override
    public void applyEffects(CityState state, PowerNetwork power) {
        if (!this.powered) return;
        state.updateWaste(-WASTE_REDUCTION_PER_TICK);
        state.updateBudget(-20);
        power.addConsumption(10);
    }

    @Override
    public StructureType getType() { return StructureType.WASTE_CENTER; }

    @Override
    public int getConstructionCost() { return 900; }
}
