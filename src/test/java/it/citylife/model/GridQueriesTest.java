package it.citylife.model;

import it.citylife.model.grid.Grid;
import it.citylife.model.grid.GridQueries;
import it.citylife.model.structures.BuildingFactory;
import it.citylife.model.structures.Structure;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Testa GridQueries: adiacenza stradale e copertura elettrica.
 *
 * Usa Grid + BuildingFactory per piazzare strutture di test senza passare
 * da GameController, così da isolare la logica topologica.
 */
class GridQueriesTest {

    private Grid grid;

    @BeforeEach
    void setUp() {
        grid = new Grid();
    }

    @Test
    @DisplayName("hasAdjacentRoad: cella con Road adiacente a nord → true")
    void testAdjacentRoadTrue() {
        grid.placeStructure((Structure) BuildingFactory.createBuilding("ROAD"), 0, 1);
        assertTrue(GridQueries.hasAdjacentRoad(grid, 0, 0));
    }

    @Test
    @DisplayName("hasAdjacentRoad: nessuna Road adiacente → false")
    void testAdjacentRoadFalse() {
        assertFalse(GridQueries.hasAdjacentRoad(grid, 0, 0));
    }

    @Test
    @DisplayName("isPoweredAt: PowerPlant entro raggio → true")
    void testPoweredAtTrue() {
        grid.placeStructure((Structure) BuildingFactory.createBuilding("POWER_PLANT"), 5, 5);
        // Chebyshev(5,5 → 3,3) = max(2,2) = 2 ≤ 5
        assertTrue(GridQueries.isPoweredAt(grid, 3, 3));
    }

    @Test
    @DisplayName("isPoweredAt: nessuna PowerPlant → false")
    void testPoweredAtFalse() {
        assertFalse(GridQueries.isPoweredAt(grid, 0, 0));
    }

    @Test
    @DisplayName("isPoweredAt: cella esattamente al bordo del raggio (5) → true")
    void testPoweredAtEsattamenteAlRaggio() {
        grid.placeStructure((Structure) BuildingFactory.createBuilding("POWER_PLANT"), 0, 0);
        // Chebyshev(0,0 → 5,0) = 5 ≤ POWER_RADIUS(5)
        assertTrue(GridQueries.isPoweredAt(grid, 5, 0));
    }

    @Test
    @DisplayName("isPoweredAt: cella oltre il raggio (6) → false")
    void testPoweredAtOltreIlRaggio() {
        grid.placeStructure((Structure) BuildingFactory.createBuilding("POWER_PLANT"), 0, 0);
        // Chebyshev(0,0 → 6,0) = 6 > POWER_RADIUS(5)
        assertFalse(GridQueries.isPoweredAt(grid, 6, 0));
    }
}
