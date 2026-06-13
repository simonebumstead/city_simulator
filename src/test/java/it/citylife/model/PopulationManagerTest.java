package it.citylife.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.citylife.model.core.CityState;
import it.citylife.model.core.PopulationGroup;
import it.citylife.model.core.PopulationManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

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
 * Soddisfazioni (se currentPop > 0):
 *   jobSatisfaction    = min(100, (ind*200 + com*50)*100.0 / currentPop)
 *   healthSatisfaction = min(100, hosp*400*100.0 / currentPop)
 *   safetySatisfaction = max(0, 100 - pollution/4 - criticalBuildingCount*5)
 *
 * Se currentPop == 0: tutte a 100 (non raggiungibile in gioco, min pop = 10).
 */
class PopulationManagerTest {

    private CityState state;
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
        // Assicuriamo che le soddisfazioni non siano critiche (almeno 1 industria e 1 ospedale)
        manager.updateDemographics(state, true, 1000, 1, 0, 1, 1);
        assertTrue(state.getPopulation() > 10);
    }

    @Test
    @DisplayName("Bassa happiness, bassa health e alta pollution → popolazione decresce")
    void testPopulationDeclinesWithLowHappiness() {
        state.setPopulation(50); // partiamo da 50 così c'è spazio per scendere
        state.setHappiness(20.0);
        state.setHealth(20.0);
        state.setPollution(80.0);
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
    @DisplayName("jobSatisfaction < 100 se i posti lavoro sono insufficienti rispetto alla popolazione")
    void testJobSatisfactionBelowMax() {
        // pop=400, 1 industrial (200 posti), 0 commercial → (200+0)*100/400 = 50
        state.setPopulation(400);
        manager.updateDemographics(state, true, 1000, 1, 0, 0, 4);
        assertEquals(50.0, state.getPopulationGroup().getJobSatisfaction(), 0.001);
    }

    @Test
    @DisplayName("healthSatisfaction = min(100, hospitalCount*200/residentialCount)")
    void testHealthSatisfactionCalculation() {
        // 1 hospital, 2 residential → 1*200/2 = 100
        manager.updateDemographics(state, true, 1000, 0, 0, 1, 2);
        assertEquals(100.0, state.getPopulationGroup().getHealthSatisfaction(), 0.001);
    }

    @Test
    @DisplayName("safetySatisfaction = max(0, 100 - pollution/4 - criticalBuildingCount*5)")
    void testSafetySatisfactionCalculation() {
        // pollution=30, criticalBuildingCount=0 → 100 - 30/4 - 0 = 92.5
        state.setPollution(30.0);
        manager.updateDemographics(state, true, 1000, 0, 0, 0, 1);
        assertEquals(92.5, state.getPopulationGroup().getSafetySatisfaction(), 0.001);
    }

    @Test
    @DisplayName("safetySatisfaction si riduce con pollution massima")
    void testSafetySatisfactionClampedToZero() {
        // pollution=100, criticalBuildingCount=0 → 100 - 100/4 - 0 = 75
        state.setPollution(100.0);
        manager.updateDemographics(state, true, 1000, 0, 0, 0, 1);
        assertEquals(75.0, state.getPopulationGroup().getSafetySatisfaction(), 0.001);
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
    @DisplayName("Offerta sufficiente → tutte le soddisfazioni a 100")
    void testAllSatisfactionsAt100WhenNoResidential() {
        // pop=10 (default), 1 industrial (200 posti), 1 hospital (400 cap), pollution=0
        // jobSat    = min(100, 200*100/10) = 100
        // healthSat = min(100, 400*100/10) = 100
        // safetySat = 100 - 0/4 = 100
        manager.updateDemographics(state, true, 1000, 1, 0, 1, 1);
        PopulationGroup pg = state.getPopulationGroup();
        assertEquals(100.0, pg.getJobSatisfaction(),    0.001);
        assertEquals(100.0, pg.getHealthSatisfaction(), 0.001);
        assertEquals(100.0, pg.getSafetySatisfaction(), 0.001);
    }
}
