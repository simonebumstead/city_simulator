package it.citylife.model;

import it.citylife.model.core.GameController;
import it.citylife.model.structures.Structure;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test d'integrazione del flusso di tick orchestrato da City.advanceTick(),
 * pilotato tramite GameController (unico entry point, boundary GRASP).
 *
 * Verifica due proprietà end-to-end non coperte dagli unit test isolati:
 *  - AC-06.2: ogni tick aggiorna le metriche della città e fa decadere le strutture.
 *  - AC-23.4: un edificio con HP = 0 non applica effetti durante il tick (e viene rimosso).
 *
 * Scenario minimo: una Road, una PowerPlant nel raggio di copertura e un Residential
 * adiacente alla strada e alimentato, così da produrre effetti misurabili (rifiuti).
 */
class CityAdvanceTickTest {

    private GameController gc;

    // Coordinate del residenziale sotto osservazione
    private static final int RES_X = 5, RES_Y = 4;

    @BeforeEach
    void setUp() {
        gc = new GameController();
        assertTrue(gc.placeBuilding("ROAD", 5, 5));
        assertTrue(gc.placeBuilding("POWER_PLANT", 6, 6));
        // Residential adiacente alla road (5,5) ed entro il raggio della PowerPlant (6,6)
        assertTrue(gc.placeBuilding("RESIDENTIAL", RES_X, RES_Y));
    }

    private Structure residential() {
        return (Structure) gc.getGrid().getCell(RES_X, RES_Y).getStructure();
    }

    @Test
    @DisplayName("advanceTick aggiorna le metriche e fa decadere le strutture (AC-06.2)")
    void testTickUpdatesMetricsAndDecaysStructures() {
        Structure res = residential();
        int hpPrima = res.getHp();
        int wastePrima = gc.getState().getWasteLevel();

        gc.advanceTick();

        // La struttura ha subito il decadimento naturale (AC-23.1): il tick ha processato gli edifici
        assertTrue(res.getHp() < hpPrima, "Gli HP della struttura devono calare dopo un tick");
        // Il residenziale alimentato genera rifiuti: le metriche sono state aggiornate (AC-06.2)
        assertTrue(gc.getState().getWasteLevel() > wastePrima, "Il wasteLevel deve aumentare dopo un tick");
    }

    @Test
    @DisplayName("Un edificio con HP = 0 non applica effetti durante il tick e viene rimosso (AC-23.4)")
    void testDestroyedBuildingAppliesNoEffects() {
        // Porta il residenziale a 0 HP: diventa "distrutto"
        residential().takeDamage(100_000);
        assertTrue(residential().isDestroyed());

        int wastePrima = gc.getState().getWasteLevel();
        gc.advanceTick();

        // Il residenziale distrutto non ha generato rifiuti (effetti saltati dal guard isDestroyed())
        assertEquals(wastePrima, gc.getState().getWasteLevel(), "Un edificio a HP=0 non deve generare effetti");
        // Dopo il tick l'edificio crollato è stato rimosso dalla griglia
        assertTrue(gc.getGrid().getCell(RES_X, RES_Y).isEmpty(), "L'edificio distrutto deve essere rimosso dalla griglia");
    }
}
