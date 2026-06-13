package it.citylife.model;

import it.citylife.model.core.GameController;
import it.citylife.model.structures.Structure;
import it.citylife.model.structures.StructureType;
import it.citylife.model.structures.upgrades.StructureDecorator;
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
 * API usata attraverso GameController (che espone saveManualGame/loadGame):
 *   Path  saveManualGame(int tick) → delega a SaveLoadManager.saveManual()
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

    // ── saveManualGame ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("saveManualGame crea un file JSON nella cartella saves/")
    void testSaveCreatesFile() throws IOException {
        GameController gc = new GameController();
        savedPath = gc.saveManualGame(0);
        assertTrue(Files.exists(savedPath), "Il file di salvataggio deve esistere");
        assertTrue(savedPath.getFileName().toString().endsWith(".json"));
    }

    // ── round-trip save/load ──────────────────────────────────────────────────

    @Test
    @DisplayName("Round-trip: edificio piazzato viene ritrovato dopo load")
    void testSaveLoadRoundTrip() throws IOException {
        GameController gc = new GameController();
        gc.placeBuilding("ROAD", 3, 7);

        savedPath = gc.saveManualGame(5);

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

        savedPath = gc.saveManualGame(0);

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

        savedPath = gc.saveManualGame(0);

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

        savedPath = gc.saveManualGame(0);

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
        savedPath = gc.saveManualGame(0);

        List<Path> saves = gc.listSaves();
        assertTrue(saves.contains(savedPath),
                "listSaves deve contenere il file appena salvato");
    }

    @Test
    @DisplayName("load con JSON malformato lancia IOException (AC-08.3)")
    void testLoadCorruptedFileThrowsIOException() throws IOException {
        Path corrotto = Files.createTempFile("corrupt_save", ".json");
        Files.writeString(corrotto, "{ json non valido !!! }");
        GameController gc = new GameController();
        assertThrows(IOException.class, () -> gc.loadGame(corrotto));
        Files.deleteIfExists(corrotto);
    }

    // ── autosave (AC-08.5) ────────────────────────────────────────────────────

    @Test
    @DisplayName("autosaveGame produce un file con prefisso 'autosave_' (AC-08.5)")
    void testAutosaveCreatesFile() throws IOException {
        GameController gc = new GameController();
        savedPath = gc.autosaveGame(5);
        assertTrue(Files.exists(savedPath), "Il file di autosave deve esistere su disco");
        assertTrue(savedPath.getFileName().toString().startsWith("autosave_"),
                "Il file di autosave deve avere prefisso 'autosave_'");
    }

    @Test
    @DisplayName("autosaveGame preserva tick e metriche (AC-08.5)")
    void testAutosavePreservesState() throws IOException {
        GameController gc = new GameController();
        gc.getState().setBudget(2500.0);
        savedPath = gc.autosaveGame(10);

        GameController gc2 = new GameController();
        int loadedTick = gc2.loadGame(savedPath);

        assertEquals(10, loadedTick);
        assertEquals(2500.0, gc2.getState().getBudget(), 0.001);
    }

    @Test
    @DisplayName("Autosave multipli nella stessa sessione sovrascrivono il file precedente")
    void testAutosaveOverwritesSameSession() throws IOException {
        GameController gc = new GameController();
        savedPath = gc.autosaveGame(5);
        Path secondPath = gc.autosaveGame(10);

        // Stesso controller → stessa sessionId → stesso file
        assertEquals(savedPath.getFileName().toString(),
                secondPath.getFileName().toString(),
                "Due autosave della stessa sessione devono usare lo stesso file");

        // Il file deve contenere il tick più recente
        GameController gc2 = new GameController();
        int loadedTick = gc2.loadGame(savedPath);
        assertEquals(10, loadedTick);

        Files.deleteIfExists(secondPath);
    }

    // ── Decorator doppio round-trip (B4, AC-16.1 SCRUM-24) ──────────────────

    @Test
    @DisplayName("Round-trip: Waste Center con SEISMIC + WASTE_THERMAL viene ripristinato con 2 upgrade")
    void testSaveLoadDoubleDecoratorRoundTrip() throws IOException {
        GameController gc = new GameController();
        gc.placeBuilding("WASTE_CENTER", 0, 0);       // budget 4100
        gc.upgradeBuilding(0, 0, "SEISMIC");           // budget 3600
        gc.upgradeBuilding(0, 0, "WASTE_THERMAL");     // budget 2900

        Structure beforeSave = (Structure) gc.getGrid().getCell(0, 0).getStructure();
        assertTrue(beforeSave instanceof StructureDecorator);
        assertEquals(2, ((StructureDecorator) beforeSave).getUpgradeLevel(),
                "Devono esserci 2 livelli di Decorator prima del salvataggio");

        savedPath = gc.saveManualGame(0);

        GameController gc2 = new GameController();
        gc2.loadGame(savedPath);

        Structure afterLoad = (Structure) gc2.getGrid().getCell(0, 0).getStructure();
        assertNotNull(afterLoad);
        assertInstanceOf(StructureDecorator.class, afterLoad,
                "Dopo il caricamento la struttura deve ancora essere un StructureDecorator");
        assertEquals(2, ((StructureDecorator) afterLoad).getUpgradeLevel(),
                "Il numero di upgrade deve essere 2 anche dopo il caricamento");
        assertEquals(StructureType.WASTE_CENTER, afterLoad.getType(),
                "Il tipo deve rimanere WASTE_CENTER lungo tutta la catena di Decorator");
    }

    @Test
    @DisplayName("Round-trip: gli HP vengono preservati correttamente dopo Decorator doppio")
    void testSaveLoadDoubleDecoratorPreservesHp() throws IOException {
        GameController gc = new GameController();
        gc.placeBuilding("WASTE_CENTER", 0, 0);
        gc.upgradeBuilding(0, 0, "SEISMIC");
        gc.upgradeBuilding(0, 0, "WASTE_THERMAL");

        // Infliggo danni per avere HP < maxHp
        Structure s = (Structure) gc.getGrid().getCell(0, 0).getStructure();
        s.takeDamage(50);
        int hpBeforeSave = s.getHp();

        savedPath = gc.saveManualGame(0);

        GameController gc2 = new GameController();
        gc2.loadGame(savedPath);

        Structure afterLoad = (Structure) gc2.getGrid().getCell(0, 0).getStructure();
        assertEquals(hpBeforeSave, afterLoad.getHp(),
                "Gli HP devono essere preservati dopo save/load con Decorator doppio");
    }
}
