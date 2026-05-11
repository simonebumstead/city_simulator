package it.citylife.model;

public abstract class Structure implements Placeable {

    protected int hp;
    protected int maxHp;
    
    // Stati per le nuove meccaniche
    protected boolean connectedToRoad = true;
    protected boolean powered = true;

    private static final int HP_DECAY_PER_TICK = 2;

    public Structure(int maxHp) {
        this.maxHp = maxHp;
        this.hp = maxHp;
    }

    public void decayTick() {
        if (hp > 0) {
            takeDamage(HP_DECAY_PER_TICK);
        }
    }

    public int takeDamage(int amount) {
        hp = Math.max(0, hp - amount);
        return hp;
    }
    
    /**
     * Ripara la struttura ripristinando parte o tutti gli HP.
     * Non può superare gli HP massimi originali.
     */
    public void repair(int amount) {
        if (!isDestroyed()) {
            hp = Math.min(maxHp, hp + amount);
        }
    }
    
    /**
     * Ripara completamente la struttura riportandola al massimo degli HP.
     */
    public void fullRepair() {
        if (!isDestroyed()) {
            hp = maxHp;
        }
    }

    public int getHp() { return hp; }
    public int getMaxHp() { return maxHp; }
    public boolean isDestroyed() { return hp <= 0; }

    public boolean isConnectedToRoad() { return connectedToRoad; }
    public void setConnectedToRoad(boolean connectedToRoad) { this.connectedToRoad = connectedToRoad; }

    public boolean isPowered() { return powered; }
    public void setPowered(boolean powered) { this.powered = powered; }

    /** AC-14.2: punto di notifica per eventi terremoto. Delega a takeDamage(). */
    public void onEarthquake(int damage) {
        takeDamage(damage);
    }

    public abstract void applyEffects(CityState state, PowerNetwork power);

    public abstract StructureType getType();

    public abstract int getConstructionCost();
}
