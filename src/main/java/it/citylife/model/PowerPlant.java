package it.citylife.model;

public class PowerPlant extends Structure {

    public PowerPlant() {
        super(100);
    }

    @Override
    public void applyEffects(CityState state, PowerNetwork power) {
        // placeholder
    }

    @Override
    public StructureType getType() {
        return StructureType.POWER_PLANT;
    }
}
