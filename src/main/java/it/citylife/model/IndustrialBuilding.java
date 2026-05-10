package it.citylife.model;

public class IndustrialBuilding extends Structure {

    public IndustrialBuilding() {
        super(400);
    }

    @Override
    public void applyEffects(CityState state, PowerNetwork power) {
        // Se non c'è corrente, la fabbrica è ferma e non produce alcun effetto
        if (!this.powered) {
            return;
        }

        // Genera revenue SOLO se è connessa a una strada per il trasporto merci
        if (this.connectedToRoad) {
            state.updateBudget(30);
            state.addIndustrialBudgetDelta(30);
        }

        state.updatePollution(2.5);
        state.updateHappiness(-1.0);
        state.updateHealth(-0.8);
        power.addConsumption(25);
        state.addIndustrialPollutionDelta(2.5);
    }

    @Override
    public StructureType getType() {
        return StructureType.INDUSTRIAL;
    }

    @Override
    public int getConstructionCost() { return 1000; }
}
