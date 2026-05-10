package it.citylife.model;

public class Park extends Structure {

    public Park() {
        super(200);
    }

    @Override
    public void applyEffects(CityState state, PowerNetwork power) {
        state.updateBudget(-10); // Mantenerli belli costa caro al Comune
        state.updatePollution(-0.5); // Mitigano l'inquinamento in modo più incisivo
        state.updateHappiness(1.5); // Ottimi per contrastare il malumore industriale
        state.updateHealth(1.0); // Incentivano l'attività all'aria aperta
    }

    @Override
    public StructureType getType() {
        return StructureType.PARK;
    }

    @Override
    public int getConstructionCost() { return 300; }
}
