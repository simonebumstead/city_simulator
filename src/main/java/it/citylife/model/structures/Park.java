package it.citylife.model.structures;

import it.citylife.model.core.PowerNetwork;
import it.citylife.model.core.City;
import it.citylife.model.core.CityState;

/**
 * Parco pubblico: area verde che migliora la qualità della vita e riduce l'inquinamento.
 *
 * Non richiede alimentazione elettrica né connessione stradale per funzionare.
 * Oltre agli effetti diretti applicati da applyEffects(), il parco partecipa
 * a un secondo passaggio in City.applyParkEffects(), che applica:
 *   - AC-28.3: riduzione aggiuntiva dell'inquinamento globale (−3/tick)
 *   - AC-28.2: bonus happiness (+2/tick) a ogni ResidentialBuilding entro
 *     una distanza di Chebyshev di 3 celle
 *
 * Effetti diretti per tick (applyEffects):
 *   - Budget:    −10 (costi di manutenzione del verde pubblico)
 *   - Pollution: −0.5
 *   - Happiness: +1.5
 *   - Health:    +1.0
 *
 * Costo di costruzione: 300. HP massimi: 200.
 *
 * @see Structure
 * @see City#applyParkEffects()
 * @see CityState
 */
public class Park extends Structure {

    /**
     * Crea un parco con 200 HP massimi.
     */
    public Park() {
        super(200);
    }

    /**
     * Applica gli effetti diretti del parco allo stato della città per il tick corrente.
     *
     * Gli effetti di prossimità ai Residential (AC-28.2) e la riduzione aggiuntiva
     * di inquinamento (AC-28.3) vengono gestiti separatamente da City.applyParkEffects().
     *
     * @param state lo stato della città su cui accumulare i delta
     * @param power la rete elettrica (non utilizzata dal parco, non consuma energia)
     */
    @Override
    public void applyEffects(CityState state, PowerNetwork power) {
        state.updateBudget(-10);     // Manutenzione del verde: giardinieri, irrigazione, pulizia
        state.updatePollution(-0.5); // La vegetazione assorbe CO2 e particolato fine
        state.updateHappiness(1.5);  // Spazi verdi migliorano il benessere psicofisico dei cittadini
        state.updateHealth(1.0);     // Incentivano l'attività fisica e riducono lo stress
    }

    /** Restituisce il tipo PARK, usato da City per identificare i parchi in applyParkEffects(). */
    @Override
    public StructureType getType() {
        return StructureType.PARK;
    }

    /** Costo di costruzione in budget: 300. */
    @Override
    public int getConstructionCost() { return 300; }
}
