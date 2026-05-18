package it.citylife.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

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
 *   Hospital:  constructionCost = 1200, maxHp = 350
 *
 *   demolish: demolitionCost = cost/10, refund = cost * 6 / 10
 *             netto = refund - demolitionCost (= 50% costo originale, AC-21.2)
 *   repair:   repairCost = (maxHp - hp) / 2
 *
 * Nota: Road è usata come edificio "neutro" nei test perché non richiede
 * adiacenza stradale (solo i RESIDENTIAL hanno questo vincolo).
 * I test di repair usano Hospital perché Road è immune ai danni (takeDamage no-op).
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

        // demolitionCost = 100/10 = 10, refund = 100*6/10 = 60, netto = +50 (AC-21.2)
        assertEquals(budgetAfterPlace + 50, controller.getState().getBudget(), 0.001);
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
        // Usa Hospital (maxHp=350, cost=1200): Road è immune ai danni (takeDamage no-op)
        controller.placeBuilding("HOSPITAL", 0, 0); // budget = 5000 - 1200 = 3800
        Structure hospital = (Structure) controller.getGrid().getCell(0, 0).getStructure();
        hospital.takeDamage(50); // hp = 300

        double budgetBeforeRepair = controller.getState().getBudget();
        boolean result = controller.repair(0, 0);

        assertTrue(result);
        assertEquals(350, hospital.getHp()); // fullRepair → maxHp
        // repairCost = (350 - 300) / 2 = 25
        assertEquals(budgetBeforeRepair - 25, controller.getState().getBudget(), 0.001);
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
        // Usa Hospital (maxHp=350): Road è immune ai danni (takeDamage no-op)
        controller.placeBuilding("HOSPITAL", 0, 0); // maxHp=350
        Structure hospital = (Structure) controller.getGrid().getCell(0, 0).getStructure();
        hospital.takeDamage(200); // hp=150, repairCost=(350-150)/2=100
        controller.getState().setBudget(50.0); // sotto il costo di 100
        boolean result = controller.repair(0, 0);
        assertFalse(result);
        assertEquals(150, hospital.getHp()); // hp non cambiato
    }

    // ── upgradeBuilding ──────────────────────────────────────────────────────

    @Test
    @DisplayName("Waste Thermal Upgrade non può essere applicato a un edificio diverso da Waste Center")
    void testWasteThermalUpgradeOnlyOnWasteCenter() {
        controller.placeBuilding("ROAD", 0, 0); // Piazziamo una strada
        boolean result = controller.upgradeBuilding(0, 0, "WASTE_THERMAL");
        assertFalse(result);
        assertEquals("Waste Thermal Upgrade can only be applied to a Waste Center.", controller.getLastError());
    }

    @Test
    @DisplayName("Seismic Upgrade viene applicato correttamente a un edificio valido")
    void testSeismicUpgradeSuccess() {
        controller.placeBuilding("ROAD", 0, 0); // cost = 100, budget = 4900
        boolean result = controller.upgradeBuilding(0, 0, "SEISMIC"); // cost = 500
        assertTrue(result);
        assertEquals(4400.0, controller.getState().getBudget(), 0.001);

        Structure s = (Structure) controller.getGrid().getCell(0, 0).getStructure();
        assertTrue(s instanceof SeismicUpgrade);
    }

    @Test
    @DisplayName("Waste Thermal Upgrade viene applicato correttamente a un Waste Center")
    void testWasteThermalUpgradeSuccess() {
        controller.placeBuilding("WASTE_CENTER", 0, 0); // cost = 900, budget = 4100
        boolean result = controller.upgradeBuilding(0, 0, "WASTE_THERMAL"); // cost = 700
        assertTrue(result);
        assertEquals(3400.0, controller.getState().getBudget(), 0.001);

        Structure s = (Structure) controller.getGrid().getCell(0, 0).getStructure();
        assertTrue(s instanceof WasteThermalUpgrade);
    }
}
