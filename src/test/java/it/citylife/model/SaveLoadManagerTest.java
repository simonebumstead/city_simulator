package it.citylife.model;

import org.junit.jupiter.api.*;
import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Testa SaveLoadManager: salvataggio su disco, round-trip load, metriche, upgrade.
 *
 * I file di save vengono creati nella cartella saves/ del progetto.
 * @AfterEach cancella i file prodotti durante il test per non sporcare il repository.
 *
 * API usata attraverso GameController (che espone saveGame/loadGame):
 *   Path  saveGame(int tick)     → delega a SaveLoadManager.saveAuto()
 *   int   loadGame(Path path)    → delega a SaveLoadManager.load(), ritorna il tick
 *
 * Test del caricamento di file inesistente: si aspetta IOException (AC-11.3).
 */
class SaveLoadManagerTest {

    private Path savedPath; // traccia il file creato, per poterlo cancellare

    @AfterEach
    void cleanup() throws IOException {
        if (savedPath != null) {
            Files.deleteIfExists(savedPath);
        }
    }

    // ── saveGame ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("saveGame crea un file JSON nella cartella saves/")
    void testSaveCreatesFile() throws IOException {
        GameController gc = new GameController();
        savedPath = gc.saveGame(0);
        assertTrue(Files.exists(savedPath), "Il file di salvataggio deve esistere");
        assertTrue(savedPath.getFileName().toString().endsWith(".json"));
    }

    // ── round-trip save/load ──────────────────────────────────────────────────

    @Test
    @DisplayName("Round-trip: edificio piazzato viene ritrovato dopo load")
    void testSaveLoadRoundTrip() throws IOException {
        GameController gc = new GameController();
        gc.placeBuilding("ROAD", 3, 7);

        savedPath = gc.saveGame(5);

        GameController gc2 = new GameController();
        int loadedTick = gc2.loadGame(savedPath);

        assertEquals(5, loadedTick);
        assertFalse(gc2.getGrid().isCellEmpty(3, 7), "La Road deve essere in (3,7) dopo load");
        assertEquals(StructureType.ROAD, gc2.getGrid().getCell(3, 7).getStructure().getType());
    }

    @Test
    @DisplayName("Round-trip: metriche (budget e happiness) vengono preservate")
    void testSaveLoadPreservesMetrics() throws IOException {
        GameController gc = new GameController();
        gc.getState().setBudget(3333.0);
        gc.getState().setHappiness(55.0);

        savedPath = gc.saveGame(0);

        GameController gc2 = new GameController();
        gc2.loadGame(savedPath);

        assertEquals(3333.0, gc2.getState().getBudget(),    0.001);
        assertEquals(55.0,   gc2.getState().getHappiness(), 0.001);
    }

    @Test
    @DisplayName("Round-trip: edificio con upgrade Seismic viene ripristinato con il decorator")
    void testSaveLoadPreservesUpgrades() throws IOException {
        GameController gc = new GameController();
        // WasteManagementCenter: cost=900 (budget rimane 4100), SeismicUpgrade: cost=500
        gc.placeBuilding("WASTE_CENTER", 0, 0);
        gc.upgradeBuilding(0, 0, "SEISMIC");

        savedPath = gc.saveGame(0);

        GameController gc2 = new GameController();
        gc2.loadGame(savedPath);

        Structure s = (Structure) gc2.getGrid().getCell(0, 0).getStructure();
        assertNotNull(s);
        assertInstanceOf(StructureDecorator.class, s,
                "Dopo load la struttura deve essere un StructureDecorator (upgrade applicato)");
    }

    @Test
    @DisplayName("Round-trip: cella vuota rimane vuota dopo load")
    void testSaveLoadEmptyCellRemainsEmpty() throws IOException {
        GameController gc = new GameController();
        gc.placeBuilding("ROAD", 0, 0); // solo cella (0,0) occupata

        savedPath = gc.saveGame(0);

        GameController gc2 = new GameController();
        gc2.loadGame(savedPath);

        assertTrue(gc2.getGrid().isCellEmpty(5, 5), "Celle non salvate devono restare vuote");
    }

    // ── caricamento file inesistente (AC-11.3) ────────────────────────────────

    @Test
    @DisplayName("loadGame con file inesistente lancia IOException (AC-11.3)")
    void testLoadNonexistentFileThrows() {
        GameController gc = new GameController();
        Path fake = Path.of("saves", "questo_file_non_esiste_12345.json");
        assertThrows(IOException.class, () -> gc.loadGame(fake));
    }

    // ── listSaves ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Dopo un salvataggio, listSaves include il file appena creato")
    void testListSavesIncludesSavedFile() throws IOException {
        GameController gc = new GameController();
        savedPath = gc.saveGame(0);

        List<Path> saves = gc.listSaves();
        assertTrue(saves.contains(savedPath),
                "listSaves deve contenere il file appena salvato");
    }
}
