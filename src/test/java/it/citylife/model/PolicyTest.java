package it.citylife.model;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Testa le quattro Policy tramite i loro effetti su CityState.
 *
 * Approccio: creare CityState, accumulare un delta noto, chiamare resolveTick()
 * con i modifiers della policy da testare, poi verificare il valore finale.
 *
 * Valori di riferimento (iniziali):
 *   budget = 5000, happiness = 67, health = 100, pollution = 0
 *
 * Decadimento naturale pollution: -2 per tick (sempre, indipendente dalla policy).
 */
class PolicyTest {

    private CityState state;

    @BeforeEach
    void setUp() {
        state = new CityState();
    }

    // ── DefaultPolicy ────────────────────────────────────────────────────────

    @Test
    @DisplayName("DefaultPolicy: tutti i moltiplicatori sono 1.0")
    void testDefaultPolicyNeutralMultipliers() {
        PolicyModifiers mod = new DefaultPolicy().getModifiers();
        assertEquals(1.0, mod.getPollutionGenerationMultiplier(), 0.001);
        assertEquals(1.0, mod.getHappinessGenerationMultiplier(), 0.001);
        assertEquals(1.0, mod.getHealthGenerationMultiplier(), 0.001);
    }

    @Test
    @DisplayName("DefaultPolicy: nessuna variazione fissa al budget")
    void testDefaultPolicyNoBudgetChange() {
        PolicyModifiers mod = new DefaultPolicy().getModifiers();
        assertEquals(0, mod.getFixedBudgetChange());
    }

    @Test
    @DisplayName("DefaultPolicy: nessuna variazione fissa a happiness, health, pollution")
    void testDefaultPolicyNoFlatChanges() {
        PolicyModifiers mod = new DefaultPolicy().getModifiers();
        assertEquals(0.0, mod.getFixedHappinessChange(), 0.001);
        assertEquals(0.0, mod.getFixedHealthChange(), 0.001);
        assertEquals(0.0, mod.getFixedPollutionChange(), 0.001);
    }

    // ── GreenPolicy ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("GreenPolicy: pollution generata dimezzata rispetto a DefaultPolicy")
    void testGreenPolicyReducesPollution() {
        // Con GreenPolicy: finalDeltaPollution = 10 * 0.50 + (-2) - 2.0 = 1.0
        state.updatePollution(10.0);
        state.resolveTick(new GreenPolicy().getModifiers());
        assertEquals(1.0, state.getPollution(), 0.001);
    }

    @Test
    @DisplayName("GreenPolicy: budget ridotto di 200 per tick (costo politica verde)")
    void testGreenPolicyReducesBudget() {
        state.resolveTick(new GreenPolicy().getModifiers());
        // budget = 5000 + 0 + (-200) = 4800
        assertEquals(4800.0, state.getBudget(), 0.001);
    }

    @Test
    @DisplayName("GreenPolicy: happiness aumenta più che con DefaultPolicy a parità di delta")
    void testGreenPolicyBoostsHappiness() {
        // GreenPolicy: happiness x1.20 + flat +1.0
        // Con delta happiness = 5:
        //   Green:   5 * 1.20 + 1.0 = 7.0  → happiness = 67 + 7 = 74.0
        //   Default: 5 * 1.00 + 0.0 = 5.0  → happiness = 67 + 5 = 72.0
        CityState greenState   = new CityState();
        CityState defaultState = new CityState();
        greenState.updateHappiness(5.0);
        defaultState.updateHappiness(5.0);
        greenState.resolveTick(new GreenPolicy().getModifiers());
        defaultState.resolveTick(new DefaultPolicy().getModifiers());
        assertTrue(greenState.getHappiness() > defaultState.getHappiness());
    }

    // ── FossilFuelPolicy ─────────────────────────────────────────────────────

    @Test
    @DisplayName("FossilFuelPolicy: pollution generata raddoppiata rispetto a DefaultPolicy")
    void testFossilFuelPolicyIncreasesPollution() {
        // Fossil: finalDeltaPollution = 10 * 2.0 + 3.0 - 2.0 = 21.0
        // Default: finalDeltaPollution = 10 * 1.0 + 0 - 2.0 = 8.0
        CityState fossilState   = new CityState();
        CityState defaultState  = new CityState();
        fossilState.updatePollution(10.0);
        defaultState.updatePollution(10.0);
        fossilState.resolveTick(new FossilFuelPolicy().getModifiers());
        defaultState.resolveTick(new DefaultPolicy().getModifiers());
        assertTrue(fossilState.getPollution() > defaultState.getPollution());
    }

    @Test
    @DisplayName("FossilFuelPolicy: fixedPollutionChange = 3.0 (emissioni base senza industria)")
    void testFossilFuelPolicyHasFixedPollutionChange() {
        assertEquals(3.0, new FossilFuelPolicy().getModifiers().getFixedPollutionChange(), 0.001);
    }

    @Test
    @DisplayName("FossilFuelPolicy: industrialBudgetMultiplier amplifica i ricavi industriali (1.5x)")
    void testFossilFuelIndustrialBudgetMultiplier() {
        // Scenario: un edificio industriale genera 30 budget/tick
        // FossilFuel:  (30 - 30) + 30 * 1.5 + 300 (flat) = 345  → budget = 5000 + 345 = 5345
        // Default:     (30 - 30) + 30 * 1.0 + 0          = 30   → budget = 5000 + 30  = 5030
        CityState fossilState  = new CityState();
        CityState defaultState = new CityState();

        fossilState.updateBudget(30.0);
        fossilState.addIndustrialBudgetDelta(30.0);
        defaultState.updateBudget(30.0);
        defaultState.addIndustrialBudgetDelta(30.0);

        fossilState.resolveTick(new FossilFuelPolicy().getModifiers());
        defaultState.resolveTick(new DefaultPolicy().getModifiers());

        assertEquals(5345.0, fossilState.getBudget(), 0.001);
        assertEquals(5030.0, defaultState.getBudget(), 0.001);
        assertTrue(fossilState.getBudget() > defaultState.getBudget());
    }

    @Test
    @DisplayName("FossilFuelPolicy: budget aumenta di 300 per tick (rendita petrolifera)")
    void testFossilFuelPolicyIncreasesBudget() {
        state.resolveTick(new FossilFuelPolicy().getModifiers());
        // budget = 5000 + 0 + 300 = 5300
        assertEquals(5300.0, state.getBudget(), 0.001);
    }

    // ── AusterityPolicy ──────────────────────────────────────────────────────

    @Test
    @DisplayName("AusterityPolicy: budget aumenta di 500 per tick (tasse elevate)")
    void testAusterityPolicyIncreasesBudget() {
        state.resolveTick(new AusterityPolicy().getModifiers());
        // budget = 5000 + 0 + 500 = 5500
        assertEquals(5500.0, state.getBudget(), 0.001);
    }

    @Test
    @DisplayName("AusterityPolicy: happiness ridotta di 15 per tick (malus flat)")
    void testAusterityPolicyReducesHappiness() {
        state.resolveTick(new AusterityPolicy().getModifiers());
        // happiness = 67 + 0 + (-15.0) = 52.0
        assertEquals(52.0, state.getHappiness(), 0.001);
    }

    @Test
    @DisplayName("AusterityPolicy: health ridotta di 2 per tick (malus flat)")
    void testAusterityPolicyReducesHealth() {
        state.resolveTick(new AusterityPolicy().getModifiers());
        // health = 100 + 0 + (-2.0) = 98.0
        assertEquals(98.0, state.getHealth(), 0.001);
    }

    // ── wasteGenerationMultiplier ────────────────────────────────────────────

    @Test
    @DisplayName("GreenPolicy: wasteGenerationMultiplier = 0.8 (meno rifiuti)")
    void testGreenPolicyWasteMultiplier() {
        assertEquals(0.8, new GreenPolicy().getModifiers().getWasteGenerationMultiplier(), 0.001);
    }

    @Test
    @DisplayName("FossilFuelPolicy: wasteGenerationMultiplier = 1.2 (più rifiuti)")
    void testFossilFuelPolicyWasteMultiplier() {
        assertEquals(1.2, new FossilFuelPolicy().getModifiers().getWasteGenerationMultiplier(), 0.001);
    }

    @Test
    @DisplayName("DefaultPolicy: wasteGenerationMultiplier = 1.0 (neutro)")
    void testDefaultPolicyWasteMultiplier() {
        assertEquals(1.0, new DefaultPolicy().getModifiers().getWasteGenerationMultiplier(), 0.001);
    }
}
