package it.citylife.model;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Testa Grid: dimensioni, accesso alle celle, piazzamento e rimozione strutture.
 *
 * Grid è 20×20. getCell(x,y) ritorna null fuori dai bounds.
 * placeStructure ritorna false se la cella è già occupata.
 */
class GridTest {

    private Grid grid;

    @BeforeEach
    void setUp() {
        grid = new Grid();
    }

    // ── Dimensioni ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("La griglia ha larghezza 20")
    void testGridWidth() {
        assertEquals(20, grid.getWidth());
    }

    @Test
    @DisplayName("La griglia ha altezza 20")
    void testGridHeight() {
        assertEquals(20, grid.getHeight());
    }

    // ── getCell ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getCell(0,0) e getCell(19,19) restituiscono celle valide")
    void testGetCellInBounds() {
        assertNotNull(grid.getCell(0, 0));
        assertNotNull(grid.getCell(19, 19));
    }

    @Test
    @DisplayName("getCell con coordinate negative restituisce null")
    void testGetCellNegativeCoords() {
        assertNull(grid.getCell(-1, 0));
        assertNull(grid.getCell(0, -1));
    }

    @Test
    @DisplayName("getCell con coordinate uguali alla dimensione restituisce null")
    void testGetCellOutOfBounds() {
        assertNull(grid.getCell(20, 0));
        assertNull(grid.getCell(0, 20));
        assertNull(grid.getCell(20, 20));
    }

    // ── isCellEmpty ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("Una griglia appena creata ha tutte le celle vuote")
    void testIsCellEmptyOnNewGrid() {
        for (int x = 0; x < 20; x++) {
            for (int y = 0; y < 20; y++) {
                assertTrue(grid.isCellEmpty(x, y),
                        "La cella (" + x + "," + y + ") dovrebbe essere vuota");
            }
        }
    }

    // ── placeStructure ───────────────────────────────────────────────────────

    @Test
    @DisplayName("placeStructure piazza un edificio e la cella non è più vuota")
    void testPlaceStructure() {
        boolean result = grid.placeStructure(new Road(), 5, 5);
        assertTrue(result);
        assertFalse(grid.isCellEmpty(5, 5));
    }

    @Test
    @DisplayName("placeStructure su cella occupata restituisce false")
    void testPlaceStructureOnOccupiedCell() {
        grid.placeStructure(new Road(), 5, 5);
        boolean secondPlace = grid.placeStructure(new Road(), 5, 5);
        assertFalse(secondPlace);
    }

    @Test
    @DisplayName("La struttura piazzata è accessibile tramite getCell().getStructure()")
    void testPlacedStructureIsRetrievable() {
        Road road = new Road();
        grid.placeStructure(road, 3, 7);
        Structure retrieved = (Structure) grid.getCell(3, 7).getStructure();
        assertNotNull(retrieved);
        assertEquals(StructureType.ROAD, retrieved.getType());
    }

    // ── removeStructure ──────────────────────────────────────────────────────

    @Test
    @DisplayName("removeStructure svuota la cella")
    void testRemoveStructure() {
        grid.placeStructure(new Road(), 5, 5);
        grid.removeStructure(5, 5);
        assertTrue(grid.isCellEmpty(5, 5));
    }

    @Test
    @DisplayName("Dopo rimozione è possibile piazzare di nuovo nella stessa cella")
    void testPlaceAfterRemove() {
        grid.placeStructure(new Road(), 5, 5);
        grid.removeStructure(5, 5);
        boolean result = grid.placeStructure(new Road(), 5, 5);
        assertTrue(result);
    }
}
