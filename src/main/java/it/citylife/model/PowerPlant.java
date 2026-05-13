package it.citylife.model;

/**
 * Centrale elettrica: fornisce energia a tutte le strutture nel raggio di 5 celle
 * (distanza di Chebyshev), ma genera inquinamento e riduce felicità e salute.
 *
 * Non richiede connessione stradale né è soggetta a blackout (produce energia
 * indipendentemente dalla rete). La copertura geografica è calcolata da
 * City.isPowered() e GameController.isPowered(), non da questa classe.
 *
 * Effetti per tick:
 *   - Budget:     −20 (costi di combustibile e manutenzione)
 *   - Pollution:  +3.5 (emissioni delle ciminiere)
 *   - Happiness:  −1.0 (impatto visivo e acustico sul quartiere)
 *   - Health:     −1.0 (inquinamento atmosferico cronico)
 *   - Produzione: +250 unità di energia nella PowerNetwork
 *
 * Costo di costruzione: 2000. HP massimi: 500.
 *
 * @see Structure
 * @see PowerNetwork
 * @see City#isPowered(int, int)
 */
public class PowerPlant extends Structure {

    /**
     * Crea una centrale elettrica con 500 HP massimi.
     */
    public PowerPlant() {
        super(500);
    }

    /**
     * Applica gli effetti della centrale allo stato della città e alla rete elettrica.
     *
     * @param state lo stato della città su cui accumulare i delta
     * @param power la rete elettrica a cui aggiungere la produzione energetica
     */
    @Override
    public void applyEffects(CityState state, PowerNetwork power) {
        state.updateBudget(-20);     // Costi operativi: combustibile, personale e manutenzione
        state.updatePollution(3.5);  // Emissioni delle ciminiere: la fonte più inquinante della città
        state.updateHappiness(-1.0); // Impatto visivo, acustico e odori sul quartiere circostante
        state.updateHealth(-1.0);    // Esposizione cronica alle emissioni atmosferiche
        power.addProduction(250);    // Produce 250 unità di energia, coprendo numerosi edifici
    }

    /** Restituisce il tipo POWER_PLANT, usato da City per il calcolo della copertura energetica. */
    @Override
    public StructureType getType() {
        return StructureType.POWER_PLANT;
    }

    /** Costo di costruzione in budget: 2000 (infrastruttura energetica strategica). */
    @Override
    public int getConstructionCost() { return 2000; }
}
