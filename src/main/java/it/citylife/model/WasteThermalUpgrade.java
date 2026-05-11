package it.citylife.model;

public class WasteThermalUpgrade extends StructureDecorator {

    public static final int COST = 700;
    private static final double EXTRA_WASTE_REDUCTION = 15.0;
    private static final double THERMAL_BUDGET_BONUS  = 50.0;

    public WasteThermalUpgrade(Structure wrapped) {
        super(wrapped);
    }

    @Override
    public void applyEffects(CityState state, PowerNetwork power) {
        wrapped.applyEffects(state, power);
        if (wrapped.isPowered()) {
            state.updateWaste(-EXTRA_WASTE_REDUCTION);
            state.updateBudget(THERMAL_BUDGET_BONUS);
        }
    }

    @Override
    public String getUpgradeName() {
        return "WASTE_THERMAL";
    }
}
