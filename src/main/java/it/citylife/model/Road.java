package it.citylife.model;

public class Road extends Structure {

    public Road() {
        super(100);
    }

    @Override
    public void applyEffects(CityState state, PowerNetwork power) {
        state.updatePollution(1);
    }

    @Override
    public StructureType getType() {
        return StructureType.ROAD;
    }

    @Override
    public int getConstructionCost() { return 100; }
}
