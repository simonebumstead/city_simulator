package it.citylife.model;

import it.citylife.model.core.CityState;
import it.citylife.model.disasters.DisasterManager;
import it.citylife.model.disasters.DisasterObserver;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Testa DisasterManager: gestione observer e effetti del terremoto su CityState.
 *
 * Il danno è quadratico (magnitude²); la magnitudo è casuale in [1,7),
 * quindi i test verificano solo le proprietà qualitative (danno > 0,
 * happiness/health non aumentano) anziché valori esatti.
 */
class DisasterManagerTest {

    static class TestObserver implements DisasterObserver {
        int ricevuto = -1;
        boolean chiamato = false;
        @Override public void onEarthquake(int damage) { chiamato = true; ricevuto = damage; }
    }

    @Test
    @DisplayName("triggerEarthquake notifica l'observer con danno > 0")
    void testObserverRiceveIlDanno() {
        DisasterManager dm = new DisasterManager();
        CityState state = new CityState();
        TestObserver obs = new TestObserver();
        dm.addObserver(obs);
        dm.triggerEarthquake(state);
        assertTrue(obs.chiamato);
        assertTrue(obs.ricevuto > 0);
    }

    @Test
    @DisplayName("removeObserver: l'observer rimosso non viene più notificato")
    void testObserverRimossoNonNotificato() {
        DisasterManager dm = new DisasterManager();
        CityState state = new CityState();
        TestObserver obs = new TestObserver();
        dm.addObserver(obs);
        dm.removeObserver(obs);
        dm.triggerEarthquake(state);
        assertFalse(obs.chiamato);
    }

    @Test
    @DisplayName("triggerEarthquake riduce la happiness della città")
    void testTerremotoRiduceHappiness() {
        DisasterManager dm = new DisasterManager();
        CityState state = new CityState();
        double happinessPrima = state.getHappiness();
        dm.triggerEarthquake(state);
        assertTrue(state.getHappiness() <= happinessPrima);
    }

    @Test
    @DisplayName("triggerEarthquake riduce la health della città")
    void testTerremotoRiduceHealth() {
        DisasterManager dm = new DisasterManager();
        CityState state = new CityState();
        double healthPrima = state.getHealth();
        dm.triggerEarthquake(state);
        assertTrue(state.getHealth() <= healthPrima);
    }
}
