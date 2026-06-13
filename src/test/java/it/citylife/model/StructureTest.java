package it.citylife.model;

import it.citylife.model.structures.ResidentialBuilding;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Testa la logica HP di Structure (classe astratta).
 *
 * Poiché Structure è astratta, usiamo ResidentialBuilding come concretizzazione:
 * - maxHp = 300
 * - HP_DECAY_PER_TICK = 1 (costante privata in Structure, usata da decayTick())
 *
 * Ogni test parte da un edificio fresco (hp = maxHp = 300).
 */
class StructureTest {

    private ResidentialBuilding building;

    @BeforeEach
    void setUp() {
        building = new ResidentialBuilding(); // maxHp = 300, hp = 300
    }

    // ── HP iniziali ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("HP iniziali devono essere uguali a maxHp (300)")
    void testInitialHp() {
        assertEquals(300, building.getHp());
        assertEquals(300, building.getMaxHp());
    }

    @Test
    @DisplayName("Edificio nuovo non è distrutto")
    void testIsNotDestroyedWhenHpPositive() {
        assertFalse(building.isDestroyed());
    }

    // ── takeDamage ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("takeDamage riduce gli HP del valore indicato")
    void testTakeDamage() {
        building.takeDamage(50);
        assertEquals(250, building.getHp());
    }

    @Test
    @DisplayName("takeDamage non porta gli HP sotto 0")
    void testTakeDamageCannotGoBelowZero() {
        building.takeDamage(1000);
        assertEquals(0, building.getHp());
    }

    @Test
    @DisplayName("isDestroyed ritorna true quando HP = 0")
    void testIsDestroyedWhenHpZero() {
        building.takeDamage(300); // distrugge l'edificio
        assertTrue(building.isDestroyed());
    }

    // ── repair e fullRepair ──────────────────────────────────────────────────

    @Test
    @DisplayName("repair aggiunge HP (senza superare maxHp)")
    void testRepairIncreasesHp() {
        building.takeDamage(100);   // hp = 200
        building.repair(50);        // hp = 250
        assertEquals(250, building.getHp());
    }

    @Test
    @DisplayName("repair non supera maxHp")
    void testRepairDoesNotExceedMaxHp() {
        building.takeDamage(10);    // hp = 290
        building.repair(500);       // hp clamped a 300
        assertEquals(300, building.getHp());
    }

    @Test
    @DisplayName("fullRepair ripristina maxHp")
    void testFullRepairRestoresMaxHp() {
        building.takeDamage(100);
        building.fullRepair();
        assertEquals(300, building.getHp());
    }

    @Test
    @DisplayName("fullRepair NON funziona se l'edificio è distrutto (hp = 0)")
    void testFullRepairDoesNotWorkIfDestroyed() {
        building.takeDamage(300);   // hp = 0, isDestroyed() = true
        building.fullRepair();      // non deve fare nulla
        assertEquals(0, building.getHp());
    }

    // ── decayTick ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("decayTick riduce gli HP di 1 per tick")
    void testDecayTickReducesHp() {
        building.decayTick();
        assertEquals(299, building.getHp()); // 300 - 1
    }

    @Test
    @DisplayName("decayTick non porta gli HP sotto 0")
    void testDecayTickDoesNotGoBelowZero() {
        building.takeDamage(299);   // hp = 1
        building.decayTick();       // hp = max(0, 1 - 2) = 0
        assertEquals(0, building.getHp());
    }

    @Test
    @DisplayName("decayTick non agisce su un edificio già a 0 HP")
    void testDecayTickSkipsDestroyedBuilding() {
        building.takeDamage(300);   // hp = 0
        building.decayTick();       // non deve toccare hp
        assertEquals(0, building.getHp());
    }

    // ── onEarthquake ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("onEarthquake riduce gli HP delegando a takeDamage")
    void testOnEarthquake() {
        building.onEarthquake(50);
        assertEquals(250, building.getHp());
    }
}
