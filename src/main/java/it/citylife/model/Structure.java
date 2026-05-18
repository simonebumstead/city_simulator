package it.citylife.model;

/**
 * Classe base astratta per tutte le strutture piazzabili sulla griglia.
 *
 * Implementa {@link Placeable} e definisce il comportamento comune a ogni struttura:
 * sistema HP (danno, decadimento, riparazione), flag di stato (powered, connectedToRoad)
 * e il punto di notifica per gli eventi sismici.
 *
 * Le sottoclassi concrete (ResidentialBuilding, PowerPlant, ecc.) implementano
 * i tre metodi astratti: applyEffects(), getType() e getConstructionCost().
 *
 * Il Pattern Template Method si applica qui: applyEffects() è il metodo astratto
 * che ogni sottoclasse personalizza, mentre decayTick() e takeDamage() definiscono
 * il comportamento condiviso del ciclo di vita della struttura.
 *
 * @see Placeable
 * @see StructureDecorator
 * @see CityState
 */
public abstract class Structure implements Placeable, DisasterObserver {

    // Punti vita correnti della struttura
    protected int hp;

    // Punti vita massimi (impostati al momento della costruzione e mai modificati)
    protected int maxHp;

    // True se la struttura è adiacente a una Road (aggiornato ogni tick da GameController)
    protected boolean connectedToRoad = true;

    // True se la struttura è coperta da una PowerPlant nel raggio di 5 celle (aggiornato ogni tick)
    protected boolean powered = true;

    // Punti vita persi ogni tick per invecchiamento naturale (AC-15.1)
    private static final int HP_DECAY_PER_TICK = 1;

    /**
     * Inizializza la struttura con gli HP massimi specificati.
     * Gli HP correnti partono al massimo (struttura appena costruita).
     *
     * @param maxHp punti vita massimi della struttura
     */
    public Structure(int maxHp) {
        this.maxHp = maxHp;
        this.hp    = maxHp;
    }

    /**
     * Applica il decadimento naturale degli HP per il tick corrente (AC-15.1).
     * Riduce gli HP di HP_DECAY_PER_TICK se la struttura non è già distrutta.
     */
    public void decayTick() {
        if (hp > 0) {
            takeDamage(HP_DECAY_PER_TICK);
        }
    }

    /**
     * Infligge danno alla struttura, riducendo gli HP senza scendere sotto zero.
     *
     * Questo metodo è virtuale: SeismicUpgrade lo sovrascrive per dimezzare il danno.
     * onEarthquake() chiama takeDamage() garantendo il dispatch virtuale (AC-14.2).
     *
     * @param amount il danno da infliggere
     * @return gli HP rimanenti dopo il danno
     */
    public int takeDamage(int amount) {
        hp = Math.max(0, hp - amount);
        return hp;
    }

    /**
     * Ripara parzialmente la struttura aumentando gli HP della quantità indicata.
     * Non può superare i maxHp. Non ha effetto su strutture distrutte (HP = 0).
     *
     * @param amount i punti vita da ripristinare
     */
    public void repair(int amount) {
        if (!isDestroyed()) {
            hp = Math.min(maxHp, hp + amount);
        }
    }

    /**
     * Ripara completamente la struttura riportando gli HP al massimo.
     * Non ha effetto su strutture distrutte (HP = 0).
     */
    public void fullRepair() {
        if (!isDestroyed()) {
            hp = maxHp;
        }
    }

    /** Restituisce gli HP correnti della struttura. */
    public int getHp() { return hp; }

    /** Restituisce gli HP massimi della struttura. */
    public int getMaxHp() { return maxHp; }

    /** Restituisce true se la struttura è distrutta (HP = 0). */
    public boolean isDestroyed() { return hp <= 0; }

    /** Restituisce true se la struttura è adiacente a una Road. */
    public boolean isConnectedToRoad() { return connectedToRoad; }

    /** Imposta il flag di connessione stradale (aggiornato ogni tick da GameController). */
    public void setConnectedToRoad(boolean connectedToRoad) { this.connectedToRoad = connectedToRoad; }

    /** Restituisce true se la struttura è coperta da una PowerPlant nel raggio di 5 celle. */
    public boolean isPowered() { return powered; }

    /** Imposta il flag di alimentazione elettrica (aggiornato ogni tick da GameController). */
    public void setPowered(boolean powered) { this.powered = powered; }

    /**
     * Restituisce la struttura base originaria (scarta eventuali Decorator).
     * 
     * @return la struttura originaria.
     */
    public Structure getBaseStructure() {
        return this;
    }

    /**
     * Punto di notifica per gli eventi sismici (AC-14.2).
     *
     * Chiama takeDamage() invece di accedere direttamente agli HP, garantendo
     * che il dispatch virtuale raggiunga SeismicUpgrade.takeDamage() quando
     * la struttura è decorata con un rinforzo antisismico.
     *
     * @param damage il danno da infliggere per effetto del terremoto
     */
    public void onEarthquake(int damage) {
        takeDamage(damage);
    }

    /**
     * Applica gli effetti della struttura allo stato della città per il tick corrente.
     * Implementato da ogni sottoclasse con la propria logica di produzione e consumo.
     *
     * @param state lo stato della città su cui accumulare i delta
     * @param power la rete elettrica a cui dichiarare produzione o consumo energetico
     */
    public abstract void applyEffects(CityState state, PowerNetwork power);

    /**
     * Restituisce il tipo della struttura.
     *
     * @return il {@link StructureType} corrispondente a questa struttura
     */
    public abstract StructureType getType();

    /**
     * Restituisce il costo di costruzione della struttura in budget.
     *
     * @return il costo di costruzione in unità di budget
     */
    public abstract int getConstructionCost();
}
