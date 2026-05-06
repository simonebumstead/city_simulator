package it.citylife.model;

public class Park extends Structure {

    public Park() {
        super(100);
    }

    @Override
    public void applyEffects(CityState state, PowerNetwork power) {
        // placeholder
    }

    @Override
    public StructureType getType() {
        return StructureType.PARK;
    }
}
