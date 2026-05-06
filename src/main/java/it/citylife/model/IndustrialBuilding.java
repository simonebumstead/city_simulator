package it.citylife.model;

public class IndustrialBuilding extends Structure {

    public IndustrialBuilding() {
        super(100);
    }

    @Override
    public void applyEffects(CityState state, PowerNetwork power) {
        // placeholder
    }

    @Override
    public StructureType getType() {
        return StructureType.INDUSTRIAL;
    }
}
