package it.citylife.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Classe base astratta del Pattern Decorator per le strutture della città.
 *
 * Avvolge una {@link Structure} esistente (wrapped) e delega tutti i metodi
 * alla struttura interna, permettendo alle sottoclassi di sovrascrivere solo
 * i metodi che intendono modificare senza duplicare il resto del comportamento.
 *
 * La catena di Decorator può essere annidata fino a 3 livelli (AC-16.3),
 * verificato da GameController.upgradeBuilding() tramite getUpgradeLevel().
 *
 * Al momento del salvataggio, collectUpgrades() percorre la catena dall'interno
 * verso l'esterno e raccoglie i nomi degli upgrade; al caricamento, BuildingFactory
 * riapplica gli upgrade nello stesso ordine ricostruendo la catena identica.
 *
 * Decorator concreti disponibili:
 *   - {@link SeismicUpgrade}:     dimezza i danni ricevuti (costo 500)
 *   - {@link WasteThermalUpgrade}: riduce rifiuti e aggiunge bonus budget (costo 700)
 *
 * @see Structure
 * @see SeismicUpgrade
 * @see WasteThermalUpgrade
 * @see GameController#upgradeBuilding(int, int, String)
 */
public abstract class StructureDecorator extends Structure {

    // La struttura avvolta; può essere a sua volta un StructureDecorator (catena)
    protected final Structure wrapped;

    /**
     * Inizializza il Decorator con maxHp = 0 (gli HP reali appartengono alla struttura avvolta).
     *
     * @param wrapped la struttura da avvolgere con questo Decorator
     */
    protected StructureDecorator(Structure wrapped) {
        super(0);
        this.wrapped = wrapped;
    }

    // Delega HP e stato di vita alla struttura avvolta
    @Override public int getHp()             { return wrapped.getHp(); }
    @Override public int getMaxHp()          { return wrapped.getMaxHp(); }
    @Override public boolean isDestroyed()   { return wrapped.isDestroyed(); }
    @Override public void repair(int a)      { wrapped.repair(a); }
    @Override public void fullRepair()       { wrapped.fullRepair(); }
    @Override public void decayTick()        { wrapped.decayTick(); }

    // takeDamage è delegato di default; SeismicUpgrade lo sovrascrive per dimezzare il danno
    @Override public int takeDamage(int amount) { return wrapped.takeDamage(amount); }

    // Delega i flag di stato alla struttura avvolta
    @Override public boolean isConnectedToRoad()        { return wrapped.isConnectedToRoad(); }
    @Override public void setConnectedToRoad(boolean v) { wrapped.setConnectedToRoad(v); }
    @Override public boolean isPowered()                { return wrapped.isPowered(); }
    @Override public void setPowered(boolean v)         { wrapped.setPowered(v); }

    // Delega effetti, tipo e costo alla struttura avvolta
    @Override public void applyEffects(CityState s, PowerNetwork p) { wrapped.applyEffects(s, p); }
    @Override public StructureType getType()        { return wrapped.getType(); }
    @Override public int getConstructionCost()      { return wrapped.getConstructionCost(); }

    @Override public Structure getBaseStructure()   { return wrapped.getBaseStructure(); }

    /**
     * Restituisce il livello di annidamento del Decorator (1-based).
     * Una struttura senza Decorator ha livello 0; il massimo consentito è 3 (AC-16.3).
     *
     * @return profondità della catena di Decorator a partire da questo livello
     */
    public int getUpgradeLevel() {
        return (wrapped instanceof StructureDecorator d) ? d.getUpgradeLevel() + 1 : 1;
    }

    /**
     * Restituisce il nome dell'upgrade per la serializzazione nel file di salvataggio.
     * Implementato da ogni Decorator concreto (es. "SEISMIC", "WASTE_THERMAL").
     *
     * @return il nome stringa dell'upgrade
     */
    public abstract String getUpgradeName();

    /**
     * Raccoglie la lista ordinata dei nomi di upgrade nella catena, dal più interno al più esterno.
     * Usato da SaveLoadManager per serializzare gli upgrade nell'ordine corretto di riapplicazione.
     *
     * @return lista di nomi di upgrade ordinata (più interno → più esterno)
     */
    public List<String> collectUpgrades() {
        List<String> list = (wrapped instanceof StructureDecorator d)
            ? new ArrayList<>(d.collectUpgrades()) : new ArrayList<>();
        list.add(getUpgradeName());
        return list;
    }
}
