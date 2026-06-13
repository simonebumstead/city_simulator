package it.citylife.model.structures;

import it.citylife.model.core.City;
import it.citylife.model.core.CityState;
import it.citylife.model.core.PopulationManager;
import it.citylife.model.core.PowerNetwork;

/**
 * Edificio residenziale: abitazioni dei cittadini della città.
 *
 * Richiede alimentazione elettrica per funzionare; se spento non produce alcun effetto.
 * Deve essere adiacente a una Road per essere piazzato (vincolo di GameController.placeBuilding).
 *
 * Ogni edificio residenziale alimentato contribuisce alla capacità abitativa (+200 per edificio)
 * e genera rifiuti (AC-18.1): la convivenza di più persone produce scarti domestici
 * che si accumulano in CityState.wasteLevel.
 *
 * È il tipo di edificio su cui agiscono i bonus di prossimità dei parchi (AC-05.3)
 * e le soddisfazioni demografiche calcolate da PopulationManager (AC-19).
 *
 * Effetti per tick (quando alimentato):
 *   - Budget:    +2 (tasse comunali dai residenti)
 *   - Happiness: +0.2
 *   - Waste:     +1.0 (AC-18.1)
 *   - Consumo:   5 unità di energia
 *
 * Costo di costruzione: 500. HP massimi: 300.
 *
 * @see Structure
 * @see PopulationManager
 * @see City#applyParkEffects()
 */
public class ResidentialBuilding extends Structure {

    /**
     * Crea un edificio residenziale con 300 HP massimi.
     */
    public ResidentialBuilding() {
        super(300);
    }

    /**
     * Applica gli effetti dell'edificio residenziale allo stato della città per il tick corrente.
     *
     * Se l'edificio non è alimentato non produce alcun effetto: senza corrente
     * i residenti non hanno servizi di base e la qualità della vita crolla.
     *
     * @param state lo stato della città su cui accumulare i delta
     * @param power la rete elettrica a cui dichiarare il consumo energetico
     */
    @Override
    public void applyEffects(CityState state, PowerNetwork power) {
        // Senza corrente i residenti non hanno servizi: nessun effetto positivo né negativo
        if (!this.powered) {
            return;
        }

        state.updateBudget(2);      // Gettito fiscale comunale dai residenti
        state.updateHappiness(0.2); // I cittadini contribuiscono al tessuto sociale della città
        state.updateWaste(1.0);     // AC-18.1: ogni edificio residenziale genera 1 unità di rifiuti/tick
        power.addConsumption(5);    // Consumo domestico: illuminazione, riscaldamento, elettrodomestici
    }

    /** Restituisce il tipo RESIDENTIAL, usato da City, PopulationManager e GameController. */
    @Override
    public StructureType getType() {
        return StructureType.RESIDENTIAL;
    }

    /** Costo di costruzione in budget: 500. */
    @Override
    public int getConstructionCost() { return 500; }
}
