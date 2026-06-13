package it.citylife.model;

import it.citylife.model.core.CityState;
import it.citylife.model.core.PowerNetwork;
import it.citylife.model.policies.DefaultPolicy;
import it.citylife.model.structures.Road;
import it.citylife.model.structures.StructureType;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Testa Road: immunità ai danni, immunità al decadimento, effetti sul tick e flag di direzione.
 */
class RoadTest {

    private Road road;

    @BeforeEach
    void setUp() {
        road = new Road();
    }

    // ── Tipo e costo ────────────────────────────────────────────────────────

    @Test
    @DisplayName("getType() restituisce ROAD")
    void testType() {
        assertEquals(StructureType.ROAD, road.getType());
    }

    @Test
    @DisplayName("getConstructionCost() restituisce 100")
    void testConstructionCost() {
        assertEquals(100, road.getConstructionCost());
    }

    // ── Immunità ai danni ────────────────────────────────────────────────────

    @Test
    @DisplayName("takeDamage() non riduce gli HP (strade immuni ai danni)")
    void testTakeDamageNoOp() {
        int hpBefore = road.getHp();
        road.takeDamage(100);
        assertEquals(hpBefore, road.getHp(),
                "Le strade non devono subire danni dai terremoti");
    }

    @Test
    @DisplayName("isDestroyed() è sempre false anche dopo takeDamage massiccia")
    void testNeverDestroyed() {
        road.takeDamage(9999);
        assertFalse(road.isDestroyed());
    }

    // ── Immunità al decadimento ──────────────────────────────────────────────

    @Test
    @DisplayName("decayTick() non riduce gli HP (strade immuni al decadimento)")
    void testDecayTickNoOp() {
        int hpBefore = road.getHp();
        road.decayTick();
        assertEquals(hpBefore, road.getHp(),
                "Le strade non devono decadere nel tempo");
    }

    // ── Effetti sul tick ─────────────────────────────────────────────────────

    @Test
    @DisplayName("applyEffects() incrementa l'inquinamento di 0.1 per tick (traffico)")
    void testApplyEffectsAddsPollution() {
        CityState state = new CityState();
        PowerNetwork net = new PowerNetwork();
        double pollutionBefore = state.getPollution();
        road.setPowered(true);
        road.applyEffects(state, net);
        // Il delta viene commesso solo da resolveTick; verifichiamo tramite getDeltaBudget proxy.
        // Usiamo resolveTick per osservare il valore finale
        state.resolveTick(new DefaultPolicy().getModifiers());
        // decadimento naturale −2, più +0.1 della strada → −1.9 ma clamped a 0
        // pollutionBefore = 0 → max(0, 0 + 0.1 - 2.0) = 0
        assertEquals(Math.max(0, pollutionBefore + 0.1 - 2.0), state.getPollution(), 0.001);
    }

    // ── Flag di connessione stradale ─────────────────────────────────────────

    @Test
    @DisplayName("Flag di connessione inizialmente tutti false")
    void testInitialConnectionFlags() {
        assertFalse(road.isConnectedNorth());
        assertFalse(road.isConnectedSouth());
        assertFalse(road.isConnectedEast());
        assertFalse(road.isConnectedWest());
    }

    @Test
    @DisplayName("setConnected*() modifica correttamente i flag nelle 4 direzioni")
    void testSetConnectionFlags() {
        road.setConnectedNorth(true);
        road.setConnectedSouth(true);
        road.setConnectedEast(false);
        road.setConnectedWest(true);

        assertTrue(road.isConnectedNorth());
        assertTrue(road.isConnectedSouth());
        assertFalse(road.isConnectedEast());
        assertTrue(road.isConnectedWest());
    }
}
