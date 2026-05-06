package it.citylife.model;

public class Road extends Structure {

    public Road() {
        super(100);
    }

    @Override
    public void applyEffects(CityState state, PowerNetwork power) {
        // placeholder
    }

    @Override
    public StructureType getType() {
        return StructureType.ROAD;
    }
}
