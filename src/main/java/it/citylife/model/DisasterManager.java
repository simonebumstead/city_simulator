package it.citylife.model;

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
 * Il danno è proporzionale al quadrato della magnitudo (formula quadratica),
 * garantendo che terremoti forti siano significativamente più devastanti di quelli lievi.
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

    public void addObserver(DisasterObserver obs) {
        if (!observers.contains(obs)) observers.add(obs);
    }

    public void removeObserver(DisasterObserver obs) {
        observers.remove(obs);
    }

    public void clearObservers() {
        observers.clear();
    }

    /**
     * Scatena un terremoto che colpisce l'intera griglia della città.
     *
     * Sequenza di esecuzione:
     *   1. Genera una magnitudo casuale in [1.0, 7.0)
     *   2. Calcola il danno agli edifici: 1 × magnitudo²
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

        // Danno quadratico: una magnitudo 7 infligge ~49 danni, una magnitudo 1 solo ~1
        int damageToInflict = (int) (1 * Math.pow(magnitude, 2));

        // Malus happiness più pesante del malus health: il panico sociale supera il danno fisico
        int happinessMalus = (int) (1.5 * Math.pow(magnitude, 2));
        int healthMalus    = (int) (0.5 * Math.pow(magnitude, 2));

        System.out.println(String.format("--- Earthquake of magnitude %.1f has struck the city! ---", magnitude));
        System.out.println(String.format("Each building will suffer %d damage.", damageToInflict));
        System.out.println(String.format("Citizens' happiness dropped by %d points and health by %d.", happinessMalus, healthMalus));

        // Bypass del delta: happiness e health calano immediatamente, non a fine tick
        state.setHappiness(Math.max(0, state.getHappiness() - happinessMalus));
        state.setHealth(Math.max(0, state.getHealth() - healthMalus));

        List<DisasterObserver> copy = new ArrayList<>(observers);
        for (DisasterObserver obs : copy) {
            obs.onEarthquake(damageToInflict);
        }
    }
}
