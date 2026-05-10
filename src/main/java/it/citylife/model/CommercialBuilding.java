package it.citylife.model;

public class CommercialBuilding extends Structure {

    public CommercialBuilding() {
        super(300);
    }

    @Override
    public void applyEffects(CityState state, PowerNetwork power) {
        // Se non c'è corrente, l'edificio è chiuso e non produce alcun effetto
        if (!this.powered) {
            return; 
        }

        // Genera revenue (budget) SOLO se è connesso a una strada
        if (this.connectedToRoad) {
            state.updateBudget(10);
        }

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
