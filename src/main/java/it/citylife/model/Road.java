package it.citylife.model;

/**
 * Strada: infrastruttura viaria che connette gli edifici alla rete urbana.
 *
 * La strada non produce effetti significativi di per sé, ma la sua presenza
 * è fondamentale per il funzionamento degli altri edifici:
 *   - I Residential possono essere piazzati solo adiacenti a una Road
 *   - Commercial e Industrial generano revenue solo se hanno una Road adiacente
 *     (clienti e merci devono poter transitare)
 *
 * La connessione stradale viene verificata ogni tick da City.hasAdjacentRoad()
 * e GameController.hasAdjacentRoad(), che controllano le quattro celle cardinali.
 *
 * Non richiede alimentazione elettrica.
 *
 * Effetti per tick:
 *   - Pollution: +0.1 (traffico veicolare)
 *
 * Costo di costruzione: 100. HP massimi: 250.
 *
 * @see Structure
 * @see City#hasAdjacentRoad(int, int)
 * @see GameController#hasAdjacentRoad(int, int)
 */
public class Road extends Structure {

    /**
     * Crea una strada con 250 HP massimi.
     */
    public Road() {
        super(250);
    }

    /**
     * Applica gli effetti della strada allo stato della città per il tick corrente.
     *
     * @param state lo stato della città su cui accumulare i delta
     * @param power la rete elettrica (non utilizzata dalla strada)
     */
    @Override
    public void applyEffects(CityState state, PowerNetwork power) {
        state.updatePollution(0.1); // Traffico veicolare: emissioni di scarico dei veicoli in transito
    }

    /** Restituisce il tipo ROAD, usato da City e GameController per il rilevamento della connessione stradale. */
    @Override
    public StructureType getType() {
        return StructureType.ROAD;
    }

    /** Costo di costruzione in budget: 100. */
    @Override
    public int getConstructionCost() { return 100; }
}
