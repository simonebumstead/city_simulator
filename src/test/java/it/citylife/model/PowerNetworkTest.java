package it.citylife.model;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Testa PowerNetwork: bilancio produzione/consumo, reset tra tick.
 */
class PowerNetworkTest {

    private PowerNetwork net;

    @BeforeEach
    void setUp() {
        net = new PowerNetwork();
    }

    @Test
    @DisplayName("Rete vuota: produzione e consumo sono entrambi zero")
    void testInitialState() {
        assertEquals(0, net.getTotalProduction());
        assertEquals(0, net.getTotalConsumption());
    }

    @Test
    @DisplayName("hasEnoughPower è true se produzione >= consumo")
    void testHasEnoughPower_productionEqualsConsumption() {
        net.addProduction(100);
        net.addConsumption(100);
        assertTrue(net.hasEnoughPower());
    }

    @Test
    @DisplayName("hasEnoughPower è true se produzione supera il consumo")
    void testHasEnoughPower_surplusProduction() {
        net.addProduction(200);
        net.addConsumption(100);
        assertTrue(net.hasEnoughPower());
    }

    @Test
    @DisplayName("hasEnoughPower è false se il consumo supera la produzione (blackout)")
    void testHasEnoughPower_deficit() {
        net.addProduction(50);
        net.addConsumption(100);
        assertFalse(net.hasEnoughPower());
    }

    @Test
    @DisplayName("hasEnoughPower è true su rete completamente vuota (0 == 0)")
    void testHasEnoughPower_emptyNetwork() {
        assertTrue(net.hasEnoughPower());
    }

    @Test
    @DisplayName("addProduction accumula correttamente più PowerPlant")
    void testAddProduction_accumulation() {
        net.addProduction(250);
        net.addProduction(250);
        assertEquals(500, net.getTotalProduction());
    }

    @Test
    @DisplayName("addConsumption accumula correttamente più edifici")
    void testAddConsumption_accumulation() {
        net.addConsumption(25);
        net.addConsumption(15);
        net.addConsumption(10);
        assertEquals(50, net.getTotalConsumption());
    }

    @Test
    @DisplayName("reset() azzera produzione e consumo per il tick successivo")
    void testReset() {
        net.addProduction(300);
        net.addConsumption(200);
        net.reset();
        assertEquals(0, net.getTotalProduction());
        assertEquals(0, net.getTotalConsumption());
    }

    @Test
    @DisplayName("Dopo reset, hasEnoughPower torna true (rete neutra)")
    void testHasEnoughPower_afterReset() {
        net.addConsumption(500);
        assertFalse(net.hasEnoughPower());
        net.reset();
        assertTrue(net.hasEnoughPower());
    }
}
