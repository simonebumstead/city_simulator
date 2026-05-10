package it.citylife.model;

public class ResidentialBuilding extends Structure {

    public ResidentialBuilding() {
        super(300);
    }

    @Override
    public void applyEffects(CityState state, PowerNetwork power) {
        state.updateBudget(2); // I cittadini ora pagano le tasse
        state.updateHappiness(0.2); // Base neutra/positiva
        power.addConsumption(5); // Consumano meno di un'industria
    }

    @Override
    public StructureType getType() {
        return StructureType.RESIDENTIAL;
    }

    @Override
    public int getConstructionCost() { return 500; }
}
