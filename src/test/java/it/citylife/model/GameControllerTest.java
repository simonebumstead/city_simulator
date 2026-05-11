package it.citylife.model;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Testa GameController: placeBuilding, demolish e repair.
 *
 * GameController crea internamente una City con Grid e CityState.
 * Si accede allo stato tramite controller.getState() e controller.getGrid().
 *
 * Costi di riferimento:
 *   Road:        constructionCost = 100
 *   Residential: constructionCost = 500
 *   Industrial:  constructionCost = 1000
 *
 *   demolish: demolitionCost = cost/10, refund = cost/2
 *             netto = refund - demolitionCost
 *   repair:   repairCost = (maxHp - hp) * 2
 *
 * Nota: Road è usata come edificio "neutro" nei test perché non richiede
 * adiacenza stradale (solo i RESIDENTIAL hanno questo vincolo).
 */
class GameControllerTest {

    private GameController controller;

    @BeforeEach
    void setUp() {
        controller = new GameController();
    }

    // ── placeBuilding ────────────────────────────────────────────────────────

    @Test
    @DisplayName("Piazzare una Road su cella libera ha successo")
    void testPlaceRoadSuccess() {
        boolean result = controller.placeBuilding("ROAD", 0, 0);
        assertTrue(result);
        assertFalse(controller.getGrid().isCellEmpty(0, 0));
    }

    @Test
    @DisplayName("Piazzare un edificio su cella già occupata fallisce")
    void testPlaceBuildingOnOccupiedCell() {
        controller.placeBuilding("ROAD", 5, 5);
        boolean secondPlace = controller.placeBuilding("ROAD", 5, 5);
        assertFalse(secondPlace);
    }

    @Test
    @DisplayName("Piazzare un edificio deduce il costo dal budget")
    void testPlaceBuildingDeductsBudget() {
        double budgetBefore = controller.getState().getBudget(); // 5000
        controller.placeBuilding("ROAD", 0, 0); // cost = 100
        assertEquals(budgetBefore - 100, controller.getState().getBudget(), 0.001);
    }

    @Test
    @DisplayName("Residential senza Road adiacente non può essere piazzato")
    void testPlaceResidentialWithoutRoad() {
        boolean result = controller.placeBuilding("RESIDENTIAL", 5, 5);
        assertFalse(result);
        // La cella deve rimanere vuota
        assertTrue(controller.getGrid().isCellEmpty(5, 5));
    }

    @Test
    @DisplayName("Residential con Road adiacente può essere piazzato")
    void testPlaceResidentialWithRoad() {
        controller.placeBuilding("ROAD", 5, 5);
        // (5,4) è adiacente a (5,5)
        boolean result = controller.placeBuilding("RESIDENTIAL", 5, 4);
        assertTrue(result);
        assertFalse(controller.getGrid().isCellEmpty(5, 4));
    }

    @Test
    @DisplayName("Budget insufficiente impedisce il piazzamento")
    void testPlaceBuildingInsufficientBudget() {
        // Abbasso il budget sotto il costo di una Road (100)
        controller.getState().setBudget(50.0);
        boolean result = controller.placeBuilding("ROAD", 0, 0);
        assertFalse(result);
        assertTrue(controller.getGrid().isCellEmpty(0, 0));
    }

    // ── demolish ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Demolire un edificio svuota la cella e aggiorna il budget")
    void testDemolishSuccess() {
        controller.placeBuilding("ROAD", 0, 0); // cost=100, budget=4900
        double budgetAfterPlace = controller.getState().getBudget();

        boolean result = controller.demolish(0, 0);
        assertTrue(result);
        assertTrue(controller.getGrid().isCellEmpty(0, 0));

        // demolitionCost = 100/10 = 10, refund = 100/2 = 50, netto = +40
        assertEquals(budgetAfterPlace + 40, controller.getState().getBudget(), 0.001);
    }

    @Test
    @DisplayName("Demolire una cella vuota restituisce false")
    void testDemolishEmptyCell() {
        boolean result = controller.demolish(10, 10);
        assertFalse(result);
    }

    @Test
    @DisplayName("Demolire con budget insufficiente per il costo di rimozione restituisce false")
    void testDemolishInsufficientBudget() {
        // Industrial: cost=1000, demolitionCost=100
        controller.placeBuilding("INDUSTRIAL", 0, 0); // budget=4000
        controller.getState().setBudget(5.0); // simulo budget esaurito
        boolean result = controller.demolish(0, 0);
        assertFalse(result);
        // L'edificio deve essere ancora lì
        assertFalse(controller.getGrid().isCellEmpty(0, 0));
    }

    // ── repair ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Riparare un edificio danneggiato ripristina HP e deduce il costo")
    void testRepairSuccess() {
        controller.placeBuilding("ROAD", 0, 0); // Road maxHp=250, cost=100, budget=4900
        Structure road = (Structure) controller.getGrid().getCell(0, 0).getStructure();
        road.takeDamage(50); // hp = 200

        double budgetBeforeRepair = controller.getState().getBudget();
        boolean result = controller.repair(0, 0);

        assertTrue(result);
        assertEquals(250, road.getHp()); // fullRepair → maxHp
        // repairCost = (250 - 200) * 2 = 100
        assertEquals(budgetBeforeRepair - 100, controller.getState().getBudget(), 0.001);
    }

    @Test
    @DisplayName("Riparare un edificio a HP pieni restituisce false")
    void testRepairFullHpBuilding() {
        controller.placeBuilding("ROAD", 0, 0);
        boolean result = controller.repair(0, 0); // hp già al massimo
        assertFalse(result);
    }

    @Test
    @DisplayName("Riparare una cella vuota restituisce false")
    void testRepairEmptyCell() {
        boolean result = controller.repair(10, 10);
        assertFalse(result);
    }

    @Test
    @DisplayName("Budget insufficiente per la riparazione: restituisce false")
    void testRepairInsufficientBudget() {
        controller.placeBuilding("ROAD", 0, 0); // maxHp=250
        Structure road = (Structure) controller.getGrid().getCell(0, 0).getStructure();
        road.takeDamage(100); // hp=150, repairCost=(250-150)*2=200
        controller.getState().setBudget(50.0); // sotto il costo
        boolean result = controller.repair(0, 0);
        assertFalse(result);
        assertEquals(150, road.getHp()); // hp non cambiato
    }
}
