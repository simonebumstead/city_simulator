package it.citylife.model;

public class ResidentialBuilding extends Structure {

    public ResidentialBuilding() {
        super(300);
    }

    @Override
    public void applyEffects(CityState state, PowerNetwork power) {
        // Se non c'è corrente, la casa non genera effetti (inclusa la felicità)
        if (!this.powered) {
            return;
        }

        state.updateBudget(2);
        state.updateHappiness(0.2);
        state.updateWaste(1.0);
        power.addConsumption(5);
    }

    @Override
    public StructureType getType() {
        return StructureType.RESIDENTIAL;
    }

    @Override
    public int getConstructionCost() { return 500; }
}
