package it.citylife.model.policies;

import it.citylife.model.core.City;
import it.citylife.model.core.CityState;
import it.citylife.model.core.GameController;

/**
 * Interfaccia del Pattern Strategy per le politiche economiche della città.
 *
 * Ogni politica non modifica direttamente lo stato della città, ma restituisce
 * un oggetto {@link PolicyModifiers} contenente moltiplicatori e modificatori flat
 * che {@link CityState#resolveTick} applica al termine di ogni tick.
 *
 * Questo approccio disaccoppia completamente la definizione di una politica
 * dalla sua applicazione: aggiungere una nuova politica significa solo
 * implementare questa interfaccia, senza toccare il motore di simulazione.
 *
 * Implementazioni disponibili:
 *   - {@link DefaultPolicy}:     politica neutrale (nessun effetto)
 *   - {@link GreenPolicy}:       favorisce ambiente e salute, costa budget
 *   - {@link FossilFuelPolicy}:  massimizza il budget, aumenta inquinamento
 *   - {@link AusterityPolicy}:   aumenta le entrate, penalizza felicità e salute
 *
 * @see PolicyModifiers
 * @see CityState#resolveTick(PolicyModifiers)
 * @see GameController#changePolicy(PolicyStrategy)
 */
public interface PolicyStrategy {

    /**
     * Restituisce i modificatori associati a questa politica.
     * Chiamato una volta per tick da {@link City#updateState()}.
     *
     * @return un {@link PolicyModifiers} con i valori da applicare al tick corrente
     */
    PolicyModifiers getModifiers();
}
