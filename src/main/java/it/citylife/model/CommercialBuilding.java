package it.citylife.model;

public class CommercialBuilding extends Structure {

    public CommercialBuilding() {
        super(300);
    }

    @Override
    public void applyEffects(CityState state, PowerNetwork power) {
        state.updateBudget(15); // Ottima fonte di guadagno
        state.updatePollution(0.3); // Inquinano meno delle industrie
        state.updateHappiness(1.0); // Lo shopping alza il morale!
        power.addConsumption(10);
    }

    @Override
    public StructureType getType() {
        return StructureType.COMMERCIAL;
    }

    @Override
    public int getConstructionCost() { return 750; }
}
