package it.citylife.model;

public class Hospital extends Structure {

    public Hospital() {
        super(350); // HP base per un ospedale
    }

    @Override
    public void applyEffects(CityState state, PowerNetwork power) {
        if (!this.powered) {
            return; // Se manca la corrente, l'ospedale non funziona
        }

        // L'ospedale aumenta sensibilmente la salute e un po' la felicità
        state.updateHealth(2.0);
        state.updateHappiness(0.5);
        
        // Costa un bel po' per essere mantenuto e consuma energia
        state.updateBudget(-25);
        power.addConsumption(15);
    }

    @Override
    public StructureType getType() {
        return StructureType.HOSPITAL;
    }

    @Override
    public int getConstructionCost() { 
        return 1200; // Costo di costruzione elevato
    }
}
