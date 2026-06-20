package it.citylife.model.structures;

import it.citylife.model.core.PopulationManager;
import it.citylife.model.core.PowerNetwork;
import it.citylife.model.core.CityState;

/**
 * Ospedale: struttura di servizio pubblico che migliora la salute e la felicità
 * dei cittadini a fronte di un costo di mantenimento elevato.
 *
 * Richiede alimentazione elettrica per funzionare; se spento non produce alcun effetto.
 * Non richiede connessione stradale per applicare i propri effetti.
 *
 * L'ospedale contribuisce al calcolo della healthSatisfaction del gruppo demografico
 * (AC-25.3): ogni ospedale copre fino a 200 residenti prima che la soddisfazione
 * sanitaria inizi a calare (la logica in PopulationManager usa 400 residenti).
 *
 * Effetti per tick (quando alimentato):
 *   - Health:    +10.0
 *   - Happiness: +0.5
 *   - Budget:    −25 (costi di personale e manutenzione)
 *   - Consumo:   15 unità di energia
 *
 * Costo di costruzione: 1200. HP massimi: 350.
 *
 * @see Structure
 * @see PopulationManager
 * @see CityState
 */
public class Hospital extends Structure {

    /**
     * Crea un ospedale con 350 HP massimi.
     */
    public Hospital() {
        super(350);
    }

    /**
     * Applica gli effetti dell'ospedale allo stato della città per il tick corrente.
     *
     * Se l'ospedale non è alimentato non produce nulla: senza corrente
     * le apparecchiature mediche non funzionano.
     *
     * @param state lo stato della città su cui accumulare i delta
     * @param power la rete elettrica a cui dichiarare il consumo energetico
     */
    @Override
    public void applyEffects(CityState state, PowerNetwork power) {
        // Senza corrente le apparecchiature mediche non funzionano: nessun effetto
        if (!this.powered) {
            return;
        }

        state.updateHealth(10.0);     // Cure mediche e prevenzione migliorano sensibilmente la salute pubblica
        state.updateHappiness(0.5);  // La presenza di un ospedale rassicura i cittadini
        state.updateBudget(-25);     // Costi di personale, farmaci e manutenzione delle strutture
        power.addConsumption(15);    // Consumo elevato: apparecchiature mediche, illuminazione H24
    }

    /** Restituisce il tipo HOSPITAL, usato da City e PopulationManager per il conteggio demografico. */
    @Override
    public StructureType getType() {
        return StructureType.HOSPITAL;
    }

    /** Costo di costruzione in budget: 1200 (struttura pubblica complessa). */
    @Override
    public int getConstructionCost() {
        return 1200;
    }
}
