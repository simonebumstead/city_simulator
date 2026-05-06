package it.citylife.model;

public abstract class Structure implements Placeable {

    protected int hp;
    protected int maxHp;

    public Structure(int maxHp) {
        this.maxHp = maxHp;
        this.hp = maxHp;
    }

    public int takeDamage(int amount) {
        hp = Math.max(0, hp - amount);
        return hp;
    }

    public boolean isDestroyed() {
        return hp <= 0;
    }

    public abstract void applyEffects(CityState state, PowerNetwork power);

    public abstract StructureType getType();

    public abstract int getConstructionCost();
}
