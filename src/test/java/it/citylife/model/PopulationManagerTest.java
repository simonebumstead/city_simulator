package it.citylife.model;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Testa PopulationManager: crescita/decrescita popolazione e soddisfazioni del gruppo.
 *
 * Firma del metodo:
 *   updateDemographics(CityState state, boolean hasPowerNearby, int maxCapacity,
 *                      int industrialCount, int commercialCount,
 *                      int hospitalCount,   int residentialCount)
 *
 * Formula crescita:
 *   deltaPop = clamp(1 + (happiness-50)*0.15 + (health-50)*0.10 + (pollution-50)*(-0.10),
 *              -15, +8)
 *   newPop   = max(10, currentPop + deltaPop)
 *
 * Soddisfazioni (se residentialCount > 0):
 *   jobSatisfaction    = min(100, (ind+com)*100.0/res)
 *   healthSatisfaction = min(100, hosp*200.0/res)
 *   safetySatisfaction = max(0, 100 - pollution)
 *
 * Se residentialCount == 0: tutte le soddisfazioni = 100 (nessuna domanda).
 */
class PopulationManagerTest {

    private CityState       state;
    private PopulationManager manager;

    @BeforeEach
    void setUp() {
        state   = new CityState();
        manager = new PopulationManager();
    }

    // ── Crescita/decrescita popolazione ──────────────────────────────────────

    @Test
    @DisplayName("Alta happiness e health → popolazione cresce")
    void testPopulationGrowsWithHighHappiness() {
        state.setHappiness(80.0);
        state.setHealth(80.0);
        // pollution = 0 (default)
        // deltaPop = clamp(1 + (80-50)*0.15 + (80-50)*0.10 + (0-50)*(-0.10), -15, 8)
        //          = clamp(1 + 4.5 + 3.0 + 5.0) = clamp(13.5) = 8
        manager.updateDemographics(state, true, 1000, 0, 0, 0, 1);
        assertTrue(state.getPopulation() > 10);
    }

    @Test
    @DisplayName("Bassa happiness, bassa health e alta pollution → popolazione decresce")
    void testPopulationDeclinesWithLowHappiness() {
        state.setPopulation(50); // partiamo da 50 così c'è spazio per scendere
        state.setHappiness(20.0);
        state.setHealth(20.0);
        state.setPollution(80.0);
        // deltaPop = clamp(1 + (20-50)*0.15 + (20-50)*0.10 + (80-50)*(-0.10))
        //          = clamp(1 - 4.5 - 3.0 - 3.0) = clamp(-9.5) = -9 (troncamento Java)
        // newPop = max(10, 50 - 9) = 41
        manager.updateDemographics(state, true, 1000, 0, 0, 0, 1);
        assertTrue(state.getPopulation() < 50);
    }

    @Test
    @DisplayName("Popolazione non scende sotto il minimo di 10")
    void testPopulationDoesNotGoBelowMinimum() {
        // Anche con pessime condizioni, il minimo è 10
        state.setHappiness(0.0);
        state.setHealth(0.0);
        state.setPollution(100.0);
        manager.updateDemographics(state, true, 1000, 0, 0, 0, 1);
        assertTrue(state.getPopulation() >= 10);
    }

    // ── Soddisfazioni del gruppo demografico (AC-19.2/19.3) ─────────────────

    @Test
    @DisplayName("jobSatisfaction = min(100, (ind+com)*100/res)")
    void testJobSatisfactionCalculation() {
        // 2 industrial + 0 commercial, 2 residential → (2+0)*100/2 = 100
        manager.updateDemographics(state, true, 1000, 2, 0, 0, 2);
        assertEquals(100.0, state.getPopulationGroup().getJobSatisfaction(), 0.001);
    }

    @Test
    @DisplayName("jobSatisfaction < 100 se i posti lavoro sono insufficienti")
    void testJobSatisfactionBelowMax() {
        // 1 industrial, 4 residential → (1+0)*100/4 = 25
        manager.updateDemographics(state, true, 1000, 1, 0, 0, 4);
        assertEquals(25.0, state.getPopulationGroup().getJobSatisfaction(), 0.001);
    }

    @Test
    @DisplayName("healthSatisfaction = min(100, hospitalCount*200/residentialCount)")
    void testHealthSatisfactionCalculation() {
        // 1 hospital, 2 residential → 1*200/2 = 100
        manager.updateDemographics(state, true, 1000, 0, 0, 1, 2);
        assertEquals(100.0, state.getPopulationGroup().getHealthSatisfaction(), 0.001);
    }

    @Test
    @DisplayName("safetySatisfaction = max(0, 100 - pollution)")
    void testSafetySatisfactionCalculation() {
        state.setPollution(30.0);
        manager.updateDemographics(state, true, 1000, 0, 0, 0, 1);
        assertEquals(70.0, state.getPopulationGroup().getSafetySatisfaction(), 0.001);
    }

    @Test
    @DisplayName("safetySatisfaction clamped a 0 con pollution altissima")
    void testSafetySatisfactionClampedToZero() {
        state.setPollution(100.0);
        manager.updateDemographics(state, true, 1000, 0, 0, 0, 1);
        assertEquals(0.0, state.getPopulationGroup().getSafetySatisfaction(), 0.001);
    }

    @Test
    @DisplayName("Health critica (< 20): happiness non compensa, popolazione declina comunque")
    void testPopulationDeclinesWhenHealthCriticalDespiteHighHappiness() {
        state.setPopulation(50);
        state.setHappiness(100.0); // happiness massima
        state.setHealth(0.0);      // salute critica
        state.setPollution(90.0);
        // effectiveHappiness = 0 (health < 20 → azzerato)
        // healthEffect    = (0-50)*0.10  = -5.0
        // pollutionEffect = (90-50)*(-0.10) = -4.0
        // deltaPop = clamp(1 + 0 - 5 - 4) = -8
        manager.updateDemographics(state, true, 1000, 0, 0, 0, 1);
        assertTrue(state.getPopulation() < 50);
    }

    // ── Caso residentialCount == 0 (AC-19.3 bug fix) ─────────────────────────

    @Test
    @DisplayName("Se residentialCount == 0, tutte le soddisfazioni restano a 100 (nessuna domanda)")
    void testAllSatisfactionsAt100WhenNoResidential() {
        manager.updateDemographics(state, true, 1000, 0, 0, 0, 0);
        PopulationGroup pg = state.getPopulationGroup();
        assertEquals(100.0, pg.getJobSatisfaction(),    0.001);
        assertEquals(100.0, pg.getHealthSatisfaction(), 0.001);
        assertEquals(100.0, pg.getSafetySatisfaction(), 0.001);
    }
}
