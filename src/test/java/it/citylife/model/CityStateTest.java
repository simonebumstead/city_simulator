package it.citylife.model;

import it.citylife.model.core.CityState;
import it.citylife.model.policies.DefaultPolicy;
import it.citylife.model.policies.GreenPolicy;
import it.citylife.model.policies.PolicyModifiers;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Testa CityState: accumulo dei delta, resolveTick con varie policy, clamping.
 *
 * Come funziona:
 *  - @BeforeEach crea un nuovo CityState prima di ogni @Test (oggetto "fresco").
 *  - I metodi updateX() accumulano delta; resolveTick() li applica davvero.
 *  - Per i double si usa assertEquals(atteso, reale, delta) con delta = tolleranza (es. 0.001).
 */
class CityStateTest {

    private CityState state;
    private PolicyModifiers defaultMod;

    @BeforeEach
    void setUp() {
        state = new CityState();
        defaultMod = new DefaultPolicy().getModifiers();
    }

    // ── Valori iniziali ──────────────────────────────────────────────────────

    @Test
    @DisplayName("Budget iniziale deve essere 5000")
    void testInitialBudget() {
        assertEquals(5000.0, state.getBudget(), 0.001);
    }

    @Test
    @DisplayName("Happiness iniziale deve essere 67")
    void testInitialHappiness() {
        assertEquals(67.0, state.getHappiness(), 0.001);
    }

    @Test
    @DisplayName("Health iniziale deve essere 100")
    void testInitialHealth() {
        assertEquals(100.0, state.getHealth(), 0.001);
    }

    @Test
    @DisplayName("Pollution iniziale deve essere 0")
    void testInitialPollution() {
        assertEquals(0.0, state.getPollution(), 0.001);
    }

    @Test
    @DisplayName("WasteLevel iniziale deve essere 0")
    void testInitialWasteLevel() {
        assertEquals(0, state.getWasteLevel());
    }

    // ── Accumulo delta ───────────────────────────────────────────────────────

    @Test
    @DisplayName("updateBudget accumula il delta e resolveTick lo applica")
    void testDeltaAccumulation() {
        state.updateBudget(100);
        state.resolveTick(defaultMod);
        // budget = 5000 + 100 = 5100
        assertEquals(5100.0, state.getBudget(), 0.001);
    }

    @Test
    @DisplayName("I delta vengono azzerati dopo resolveTick (doppio resolve non accumula)")
    void testDeltaResetAfterResolve() {
        state.updateBudget(100);
        state.resolveTick(defaultMod);
        // Secondo resolve senza nuovi delta: budget non deve cambiare
        double budgetAfterFirst = state.getBudget();
        state.resolveTick(defaultMod);
        assertEquals(budgetAfterFirst, state.getBudget(), 0.001);
    }

    // ── resolveTick con DefaultPolicy ────────────────────────────────────────

    @Test
    @DisplayName("DefaultPolicy: moltiplicatori neutri, happiness += delta")
    void testResolveTick_DefaultPolicy() {
        state.updateHappiness(5.0);
        state.resolveTick(defaultMod);
        // happiness = 67 + 5.0 = 72.0 (nessun malus: pollution=0, group ok)
        assertEquals(72.0, state.getHappiness(), 0.001);
    }

    @Test
    @DisplayName("DefaultPolicy: decadimento naturale pollution di -2 per tick")
    void testNaturalPollutionDecay() {
        state.setPollution(10.0);
        state.resolveTick(defaultMod);
        // finalDeltaPollution = 0 * 1.0 + 0 - 2.0 = -2.0
        // pollution = max(0, 10 - 2) = 8.0
        assertEquals(8.0, state.getPollution(), 0.001);
    }

    // ── resolveTick con GreenPolicy ──────────────────────────────────────────

    @Test
    @DisplayName("GreenPolicy: pollution generata dimezzata (x0.50) e fixed -2")
    void testResolveTick_GreenPolicy_ReducesPollution() {
        PolicyModifiers greenMod = new GreenPolicy().getModifiers();
        state.updatePollution(10.0);
        state.resolveTick(greenMod);
        // finalDeltaPollution = 10 * 0.50 + (-2) - 2.0 (decay) = 5 - 2 - 2 = 1.0
        assertEquals(1.0, state.getPollution(), 0.001);
    }

    @Test
    @DisplayName("GreenPolicy: budget ridotto di 200 a tick (fixed budget change)")
    void testResolveTick_GreenPolicy_ReducesBudget() {
        PolicyModifiers greenMod = new GreenPolicy().getModifiers();
        state.resolveTick(greenMod);
        // budget = 5000 + 0 + (-200) = 4800
        assertEquals(4800.0, state.getBudget(), 0.001);
    }

    // ── Clamping ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Happiness non supera 100 (clamping superiore)")
    void testClampingHappinessMax() {
        state.updateHappiness(500.0);
        state.resolveTick(defaultMod);
        assertEquals(100.0, state.getHappiness(), 0.001);
    }

    @Test
    @DisplayName("Pollution non scende sotto 0 (clamping inferiore)")
    void testClampingPollutionMin() {
        state.updatePollution(-200.0);
        state.resolveTick(defaultMod);
        assertEquals(0.0, state.getPollution(), 0.001);
    }

    @Test
    @DisplayName("Health non supera 100 (clamping superiore)")
    void testClampingHealthMax() {
        state.updateHealth(500.0);
        state.resolveTick(defaultMod);
        assertEquals(100.0, state.getHealth(), 0.001);
    }

    // ── Waste penalty (AC-18.2) ──────────────────────────────────────────────

    @Test
    @DisplayName("WasteLevel > 50 aumenta pollution e riduce happiness (AC-18.2)")
    void testWastePenaltyIncreasesPollutionAndReducesHappiness() {
        state.setWasteLevel(60);         // threshold è 50, excess = 10
        state.updatePollution(5.0);      // delta base per poter misurare l'effetto
        state.resolveTick(defaultMod);

        // wastePenalty = (60 - 50) * 0.10 = 1.0
        // finalDeltaPollution = 5 * 1.0 + 0 + 1.0 - 2.0 (decay) = 4.0
        assertEquals(4.0, state.getPollution(), 0.001);

        // finalDeltaHappiness = 0 + 0 - 1.0 * 0.5 (waste) = -0.5
        // happiness = 67 - 0.5 = 66.5
        assertEquals(66.5, state.getHappiness(), 0.001);
    }
}
