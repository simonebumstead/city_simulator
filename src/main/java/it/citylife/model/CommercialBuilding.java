package it.citylife.model;

/**
 * Edificio commerciale: negozi, uffici e attività terziarie della città.
 *
 * Richiede alimentazione elettrica e connessione stradale per generare revenue.
 * Se spento non produce alcun effetto; se alimentato ma privo di strada adiacente
 * applica comunque i suoi effetti su pollution e happiness, ma il delta di budget
 * viene annullato da City.updateState() (logica di isolamento stradale).
 *
 * Effetti per tick (quando alimentato):
 *   - Budget:     +15 (solo se connesso a una strada)
 *   - Pollution:  +0.3
 *   - Happiness:  +1.0
 *   - Consumo:    10 unità di energia
 *
 * Costo di costruzione: 750. HP massimi: 300 (ereditati da Structure).
 *
 * @see Structure
 * @see City#isRevenueBuilding(Structure)
 * @see CityState
 */
public class CommercialBuilding extends Structure {

    /**
     * Crea un edificio commerciale con 300 HP massimi.
     */
    public CommercialBuilding() {
        super(300);
    }

    /**
     * Applica gli effetti dell'edificio commerciale allo stato della città per il tick corrente.
     *
     * Se l'edificio non è alimentato non produce nulla.
     * La revenue di budget (+15) è condizionata alla connessione stradale;
     * pollution e happiness vengono sempre applicate se l'edificio è acceso.
     *
     * @param state lo stato della città su cui accumulare i delta
     * @param power la rete elettrica a cui dichiarare il consumo energetico
     */
    @Override
    public void applyEffects(CityState state, PowerNetwork power) {
        // Se non c'è corrente, l'edificio è chiuso e non produce alcun effetto
        if (!this.powered) {
            return;
        }

        // Revenue generata solo se raggiungibile via strada (clienti e merci possono arrivare)
        if (this.connectedToRoad) {
            state.updateBudget(15);
        }

        state.updatePollution(0.3);  // Traffico e attività commerciali generano lieve inquinamento
        state.updateHappiness(1.0);  // Servizi e negozi migliorano la qualità della vita
        power.addConsumption(10);    // Consumo energetico per illuminazione e apparecchiature
    }

    /** Restituisce il tipo COMMERCIAL, usato da City e GameController per identificare l'edificio. */
    @Override
    public StructureType getType() {
        return StructureType.COMMERCIAL;
    }

    /** Costo di costruzione in budget: 750. */
    @Override
    public int getConstructionCost() { return 750; }
}
