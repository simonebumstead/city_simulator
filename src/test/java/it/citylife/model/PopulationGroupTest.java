package it.citylife.model;

import it.citylife.model.core.CityState;
import it.citylife.model.core.PopulationGroup;
import it.citylife.model.policies.DefaultPolicy;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Testa PopulationGroup: valori iniziali, setter con clamping, integrazione con CityState.resolveTick.
 */
class PopulationGroupTest {

    private PopulationGroup group;

    @BeforeEach
    void setUp() {
        group = new PopulationGroup();
    }

    // ── Valori iniziali ──────────────────────────────────────────────────────

    @Test
    @DisplayName("Valori iniziali di soddisfazione sono tutti 50.0 (neutro)")
    void testInitialValues() {
        assertEquals(50.0, group.getJobSatisfaction(),    0.001);
        assertEquals(50.0, group.getHealthSatisfaction(), 0.001);
        assertEquals(50.0, group.getSafetySatisfaction(), 0.001);
    }

    // ── Setter normali ───────────────────────────────────────────────────────

    @Test
    @DisplayName("setJobSatisfaction imposta il valore correttamente")
    void testSetJobSatisfaction() {
        group.setJobSatisfaction(75.0);
        assertEquals(75.0, group.getJobSatisfaction(), 0.001);
    }

    @Test
    @DisplayName("setHealthSatisfaction imposta il valore correttamente")
    void testSetHealthSatisfaction() {
        group.setHealthSatisfaction(30.0);
        assertEquals(30.0, group.getHealthSatisfaction(), 0.001);
    }

    @Test
    @DisplayName("setSafetySatisfaction imposta il valore correttamente")
    void testSetSafetySatisfaction() {
        group.setSafetySatisfaction(90.0);
        assertEquals(90.0, group.getSafetySatisfaction(), 0.001);
    }

    // ── Clamping superiore ───────────────────────────────────────────────────

    @Test
    @DisplayName("setJobSatisfaction clampata a 100 se supera il massimo")
    void testJobSatisfactionClampedMax() {
        group.setJobSatisfaction(150.0);
        assertEquals(100.0, group.getJobSatisfaction(), 0.001);
    }

    @Test
    @DisplayName("setHealthSatisfaction clampata a 100 se supera il massimo")
    void testHealthSatisfactionClampedMax() {
        group.setHealthSatisfaction(200.0);
        assertEquals(100.0, group.getHealthSatisfaction(), 0.001);
    }

    @Test
    @DisplayName("setSafetySatisfaction clampata a 100 se supera il massimo")
    void testSafetySatisfactionClampedMax() {
        group.setSafetySatisfaction(999.0);
        assertEquals(100.0, group.getSafetySatisfaction(), 0.001);
    }

    // ── Clamping inferiore ───────────────────────────────────────────────────

    @Test
    @DisplayName("setJobSatisfaction clampata a 0 se negativa")
    void testJobSatisfactionClampedMin() {
        group.setJobSatisfaction(-10.0);
        assertEquals(0.0, group.getJobSatisfaction(), 0.001);
    }

    @Test
    @DisplayName("setHealthSatisfaction clampata a 0 se negativa")
    void testHealthSatisfactionClampedMin() {
        group.setHealthSatisfaction(-50.0);
        assertEquals(0.0, group.getHealthSatisfaction(), 0.001);
    }

    @Test
    @DisplayName("setSafetySatisfaction clampata a 0 se negativa")
    void testSafetySatisfactionClampedMin() {
        group.setSafetySatisfaction(-1.0);
        assertEquals(0.0, group.getSafetySatisfaction(), 0.001);
    }

    // ── Integrazione con CityState (AC-19.4) ────────────────────────────────

    @Test
    @DisplayName("Soddisfazioni basse generano malus happiness in resolveTick (AC-19.4)")
    void testLowSatisfactionCausesHappinessMalus() {
        CityState state = new CityState();
        // Tutte le soddisfazioni al minimo (0) → malus massimo
        state.getPopulationGroup().setJobSatisfaction(0.0);
        state.getPopulationGroup().setHealthSatisfaction(0.0);
        state.getPopulationGroup().setSafetySatisfaction(0.0);

        double happinessBefore = state.getHappiness();
        state.resolveTick(new DefaultPolicy().getModifiers());

        // Malus = (50−0)*0.04 * 3 = 6.0; happiness scende di 6 unità
        assertTrue(state.getHappiness() < happinessBefore,
                "Soddisfazioni basse devono ridurre la felicità");
    }

    @Test
    @DisplayName("Soddisfazioni al 50 (neutro) non generano malus happiness")
    void testNeutralSatisfactionNoMalus() {
        CityState state = new CityState();
        // Valori di default (50) → nessun malus
        double happinessBefore = state.getHappiness();
        state.resolveTick(new DefaultPolicy().getModifiers());

        // Nessun delta happiness accumulato → happiness invariata
        assertEquals(happinessBefore, state.getHappiness(), 0.001);
    }
}
