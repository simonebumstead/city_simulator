package it.citylife.model;

public class SeismicUpgrade extends StructureDecorator {

    public static final int COST = 500;

    public SeismicUpgrade(Structure wrapped) {
        super(wrapped);
    }

    @Override
    public int takeDamage(int amount) {
        return wrapped.takeDamage(Math.max(1, amount / 2));
    }

    @Override
    public String getUpgradeName() {
        return "SEISMIC";
    }
}
