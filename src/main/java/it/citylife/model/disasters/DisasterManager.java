package it.citylife.model.disasters;

import it.citylife.model.structures.upgrades.SeismicUpgrade;
import it.citylife.model.structures.Structure;
import it.citylife.model.core.City;
import it.citylife.model.core.CityState;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Gestisce i disastri casuali che possono colpire la città.
 *
 * Utilizza il pattern Observer (tramite l'interfaccia {@link DisasterObserver})
 * per notificare gli edifici al verificarsi di un evento catastrofico, rendendo 
 * il sistema facilmente estensibile in futuro per supportare nuovi tipi di disastri.
 *
 * Ogni tick, City.updateState() verifica con probabilità EARTHQUAKE_PROBABILITY
 * se un terremoto deve scatenarsi; in caso affermativo delega a triggerEarthquake().
 *
 * Il danno è proporzionale a 5 × magnitudo² (formula quadratica scalata),
 * garantendo che terremoti forti siano significativamente più devastanti di quelli lievi.
 * Esempio: magnitudo 4 → 80 HP, magnitudo 7 → ~245 HP (su edifici con 200–500 HP totali).
 * Gli edifici dotati di SeismicUpgrade subiscono la metà del danno grazie al
 * dispatch virtuale dell'evento (es. onEarthquake() → takeDamage()) (AC-14.2).
 *
 * @see City#updateState()
 * @see DisasterObserver
 * @see SeismicUpgrade
 */
public class DisasterManager {

    // AC-14.1: probabilità di terremoto per tick; valore nominato per facilitare il bilanciamento
    public static final double EARTHQUAKE_PROBABILITY = 0.01;

    // Generatore casuale per la magnitudo del terremoto
    private final Random random = new Random();

    // Lista degli osservatori (strutture) da notificare durante il terremoto
    private final List<DisasterObserver> observers = new ArrayList<>();

    /**
     * Registra un osservatore che verrà notificato al verificarsi di un terremoto.
     * L'aggiunta è idempotente: lo stesso observer non viene inserito due volte.
     * @param obs l'osservatore da registrare (tipicamente una {@link Structure})
     */
    public void addObserver(DisasterObserver obs) {
        if (!observers.contains(obs)) observers.add(obs);
    }

    /**
     * Rimuove un osservatore dalla lista delle notifiche.
     * @param obs l'osservatore da rimuovere
     */
    public void removeObserver(DisasterObserver obs) {
        observers.remove(obs);
    }

    /**
     * Rimuove tutti gli osservatori registrati.
     * Chiamato da {@link City} al reset della partita.
     */
    public void clearObservers() {
        observers.clear();
    }

    /**
     * Scatena un terremoto che colpisce l'intera griglia della città.
     *
     * Sequenza di esecuzione:
     *   1. Genera una magnitudo casuale in [1.0, 7.0)
     *   2. Calcola il danno agli edifici: 5 × magnitudo²
     *   3. Calcola i malus a happiness (1.5 × magnitudo²) e health (0.5 × magnitudo²)
     *   4. Applica happiness e health direttamente con setter (bypass del delta,
     *      perché il terremoto è un evento istantaneo, non un effetto per tick)
     *   5. Notifica tutti gli observer registrati applicando il danno (Observer Pattern)
     *
     * @param state lo stato della città su cui applicare i malus a happiness e health
     */
    public void triggerEarthquake(CityState state) {
        // Magnitudo casuale: valori bassi (1–2) causano danni lievi, valori alti (6–7) sono catastrofici
        double magnitude = 1.0 + (random.nextDouble() * 6.0);

        // Danno quadratico scalato (×5): magnitudo 4 → 80 HP, magnitudo 7 → ~245 HP
        // Calibrato in modo che un quake forte possa abbattere edifici da 200–400 HP
        int damageToInflict = (int) (5 * Math.pow(magnitude, 2));

        // Malus happiness più pesante del malus health: il panico sociale supera il danno fisico
        int happinessMalus = (int) (1.5 * Math.pow(magnitude, 2));
        int healthMalus    = (int) (0.5 * Math.pow(magnitude, 2));

        System.out.println(String.format("--- Earthquake of magnitude %.1f has struck the city! ---", magnitude));
        System.out.println(String.format("Each building will suffer %d damage.", damageToInflict));
        System.out.println(String.format("Happiness dropped by %d points, health by %d.", happinessMalus, healthMalus));

        // Bypass del delta: happiness e health calano immediatamente, non a fine tick
        state.setHappiness(Math.max(0, state.getHappiness() - happinessMalus));
        state.setHealth(Math.max(0, state.getHealth() - healthMalus));

        List<DisasterObserver> copy = new ArrayList<>(observers);
        for (DisasterObserver obs : copy) {
            obs.onEarthquake(damageToInflict);
        }
    }
}
