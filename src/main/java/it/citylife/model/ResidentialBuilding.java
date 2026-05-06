package it.citylife.model;

public class ResidentialBuilding extends Structure {

    public ResidentialBuilding() {
        super(100);
    }

    @Override
    public void applyEffects(CityState state, PowerNetwork power) {
        // placeholder
    }

    @Override
    public StructureType getType() {
        return StructureType.RESIDENTIAL;
    }
}
