package it.citylife.model;

import java.util.ArrayList;
import java.util.List;

public abstract class StructureDecorator extends Structure {

    protected final Structure wrapped;

    protected StructureDecorator(Structure wrapped) {
        super(0);
        this.wrapped = wrapped;
    }

    @Override public int getHp()             { return wrapped.getHp(); }
    @Override public int getMaxHp()          { return wrapped.getMaxHp(); }
    @Override public boolean isDestroyed()   { return wrapped.isDestroyed(); }
    @Override public void repair(int a)      { wrapped.repair(a); }
    @Override public void fullRepair()       { wrapped.fullRepair(); }
    @Override public void decayTick()        { wrapped.decayTick(); }
    @Override public int takeDamage(int amount) { return wrapped.takeDamage(amount); }

    @Override public boolean isConnectedToRoad()        { return wrapped.isConnectedToRoad(); }
    @Override public void setConnectedToRoad(boolean v) { wrapped.setConnectedToRoad(v); }
    @Override public boolean isPowered()                { return wrapped.isPowered(); }
    @Override public void setPowered(boolean v)         { wrapped.setPowered(v); }

    @Override public void applyEffects(CityState s, PowerNetwork p) { wrapped.applyEffects(s, p); }
    @Override public StructureType getType()        { return wrapped.getType(); }
    @Override public int getConstructionCost()      { return wrapped.getConstructionCost(); }

    /** Profondità di annidamento (1-based). Max consentito: 3. */
    public int getUpgradeLevel() {
        return (wrapped instanceof StructureDecorator d) ? d.getUpgradeLevel() + 1 : 1;
    }

    /** Nome dell'upgrade per serializzazione (es. "SEISMIC"). */
    public abstract String getUpgradeName();

    /** Lista ordinata di upgrade names (più interno → esterno). */
    public List<String> collectUpgrades() {
        List<String> list = (wrapped instanceof StructureDecorator d)
            ? new ArrayList<>(d.collectUpgrades()) : new ArrayList<>();
        list.add(getUpgradeName());
        return list;
    }
}
