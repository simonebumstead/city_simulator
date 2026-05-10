package it.citylife.model;

public class IndustrialBuilding extends Structure {

    public IndustrialBuilding() {
        super(400);
    }

    @Override
    public void applyEffects(CityState state, PowerNetwork power) {
        state.updateBudget(30); // Il vero motore economico
        state.updatePollution(2.5); // Ma inquina tantissimo
        state.updateHappiness(-1.0); // Nessuno vuole viverci vicino
        state.updateHealth(-0.8); // Malus serio alla salute
        power.addConsumption(25); // Consumono molto energia
        state.addIndustrialBudgetDelta(30);
        state.addIndustrialPollutionDelta(2.5);
    }

    @Override
    public StructureType getType() {
        return StructureType.INDUSTRIAL;
    }

    @Override
    public int getConstructionCost() { return 1000; }
}
