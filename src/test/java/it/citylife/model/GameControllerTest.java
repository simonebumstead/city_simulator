package it.citylife.model;

import static org.junit.jupiter.api.Assertions.*;
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

    // ── Demolizione strada → isolamento (AC3 SCRUM-20) ─────────────────────

    @Test
    @DisplayName("Commerciale con road genera più reddito rispetto a uno isolato nel tick")
    void testCommercialWithRoadGeneratesMoreIncomeThanIsolated() {
        // Scenario A: PowerPlant (5,0), Road (5,1), Commercial (5,2) — connesso e alimentato
        // Distanza Chebyshev (5,0)→(5,2) = max(0,2) = 2 ≤ 5 → alimentato ✓
        GameController c1 = new GameController();
        c1.placeBuilding("POWER_PLANT", 5, 0);
        c1.placeBuilding("ROAD", 5, 1);
        c1.placeBuilding("COMMERCIAL", 5, 2);
        double before1 = c1.getState().getBudget();
        c1.advanceTick();
        double deltaWithRoad = c1.getState().getBudget() - before1;

        // Scenario B: stessa topologia ma senza road — commercial alimentato ma non connesso
        GameController c2 = new GameController();
        c2.placeBuilding("POWER_PLANT", 5, 0);
        // nessuna road
        c2.placeBuilding("COMMERCIAL", 5, 2);
        double before2 = c2.getState().getBudget();
        c2.advanceTick();
        double deltaNoRoad = c2.getState().getBudget() - before2;

        // Con road il commerciale aggiunge +15: il delta deve essere +15 migliore
        assertTrue(deltaWithRoad > deltaNoRoad,
                "Un commerciale con road deve generare più reddito rispetto a uno isolato");
    }

    @Test
    @DisplayName("Commerciale isolato dopo demolizione della road: non genera reddito (AC3 SCRUM-20)")
    void testCommercialIsolatedAfterRoadDemolitionNoIncome() {
        // Layout: PowerPlant (5,0), Road (5,1), Commercial (5,2)
        controller.placeBuilding("POWER_PLANT", 5, 0);
        controller.placeBuilding("ROAD", 5, 1);
        controller.placeBuilding("COMMERCIAL", 5, 2);

        // Demolisco la road: il commerciale resta isolato
        controller.demolish(5, 1);

        double budgetBefore = controller.getState().getBudget();
        controller.advanceTick();

        // Senza road il commerciale non deve generare reddito positivo netto
        // (può esserci la PowerPlant che genera +250 ma non il commerciale)
        // Confronto: se il budget è aumentato solo per effetti di altri edifici, ok;
        // ma il commerciale non deve contribuire. Verifichiamo via flag connectedToRoad.
        Cell cell = controller.getGrid().getCell(5, 2);
        assertNotNull(cell);
        Structure commercial = (Structure) cell.getStructure();
        assertFalse(commercial.isConnectedToRoad(),
                "Dopo la demolizione della road il commerciale deve risultare non connesso");
    }

    // ── repairAll (B2) ───────────────────────────────────────────────────────

    @Test
    @DisplayName("repairAll ripara tutti gli edifici danneggiati e deduce il costo totale")
    void testRepairAllSuccess() {
        controller.placeBuilding("HOSPITAL", 0, 0); // maxHp=350, budget=3800
        controller.placeBuilding("HOSPITAL", 1, 1); // maxHp=350, budget=2600

        Structure h1 = (Structure) controller.getGrid().getCell(0, 0).getStructure();
        Structure h2 = (Structure) controller.getGrid().getCell(1, 1).getStructure();
        h1.takeDamage(100); // hp=250, repairCost=(350-250)/2=50
        h2.takeDamage(200); // hp=150, repairCost=(350-150)/2=100

        double budgetBefore = controller.getState().getBudget();
        boolean result = controller.repairAll();

        assertTrue(result);
        assertEquals(350, h1.getHp(), "Ospedale 1 deve essere riparato a maxHp");
        assertEquals(350, h2.getHp(), "Ospedale 2 deve essere riparato a maxHp");
        assertEquals(budgetBefore - 150, controller.getState().getBudget(), 0.001,
                "Il costo totale (50+100=150) deve essere detratto dal budget");
    }

    @Test
    @DisplayName("repairAll restituisce false se non ci sono edifici danneggiati")
    void testRepairAllNothingToDo() {
        controller.placeBuilding("ROAD", 0, 0); // HP pieno
        boolean result = controller.repairAll();
        assertFalse(result);
    }

    @Test
    @DisplayName("repairAll restituisce false se il budget è insufficiente")
    void testRepairAllInsufficientBudget() {
        controller.placeBuilding("HOSPITAL", 0, 0); // maxHp=350
        Structure h = (Structure) controller.getGrid().getCell(0, 0).getStructure();
        h.takeDamage(200); // repairCost = (350-150)/2 = 100
        controller.getState().setBudget(50.0); // sotto il costo

        boolean result = controller.repairAll();
        assertFalse(result);
        assertEquals(150, h.getHp(), "Gli HP non devono cambiare se il budget è insufficiente");
    }

    // ── changePolicy AC2 SCRUM-18 ────────────────────────────────────────────

    @Test
    @DisplayName("changePolicy sostituisce la politica attiva con quella nuova (AC2 SCRUM-18)")
    void testChangePolicyReplacesActive() {
        controller.changePolicy(new GreenPolicy());
        assertInstanceOf(GreenPolicy.class, controller.getActivePolicy(),
                "La politica attiva deve essere GreenPolicy dopo il cambio");

        controller.changePolicy(new FossilFuelPolicy());
        assertInstanceOf(FossilFuelPolicy.class, controller.getActivePolicy(),
                "FossilFuelPolicy deve sostituire GreenPolicy (politiche mutualmente esclusive)");
    }

    @Test
    @DisplayName("changePolicy(null) ripristina la DefaultPolicy")
    void testChangePolicyNullRestoresDefault() {
        controller.changePolicy(new AusterityPolicy());
        controller.changePolicy(null);
        assertInstanceOf(DefaultPolicy.class, controller.getActivePolicy(),
                "changePolicy(null) deve ripristinare la DefaultPolicy neutrale");
    }
}
