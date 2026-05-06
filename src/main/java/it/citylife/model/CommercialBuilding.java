package it.citylife.model;

public class CommercialBuilding extends Structure {

    public CommercialBuilding() {
        super(100);
    }

    @Override
    public void applyEffects(CityState state, PowerNetwork power) {
        // placeholder
    }

    @Override
    public StructureType getType() {
        return StructureType.COMMERCIAL;
    }
}
