package it.citylife.model;

import it.citylife.model.core.CityState;
import it.citylife.model.core.PowerNetwork;
import it.citylife.model.policies.DefaultPolicy;
import it.citylife.model.policies.PolicyModifiers;
import it.citylife.model.structures.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Testa applyEffects() dei singoli edifici.
 *
 * Tecnica: per ogni test creo CityState e PowerNetwork freschi, chiamo
 * applyEffects(), poi risolvo con DefaultPolicy e confronto i valori finali
 * con quelli iniziali noti (budget=5000, happiness=67, health=100, pollution=0, waste=0).
 *
 * Tutti gli edifici partono in stato "powered=true, connectedToRoad=true" (default di Structure).
 *
 * Valori delta di riferimento (prima del resolve):
 *   ResidentialBuilding powered:  budget+2, happiness+0.2, waste+1.0, consumption+5
 *   IndustrialBuilding powered+road: budget+30, pollution+2.5, happiness-1.0, health-0.8, consumption+25
 *   CommercialBuilding powered+road: budget+15, pollution+0.3, happiness+1.0, consumption+10
 *   PowerPlant:                   budget-20, pollution+3.5, happiness-1.0, health-1.0, production+250
 *   Park (no power needed):       budget-10, pollution-0.5, happiness+1.5, health+1.0
 *   Hospital powered:             health+2.0, happiness+0.5, budget-25, consumption+15
 *   WasteManagementCenter powered: waste-10, budget-20, consumption+10
 *
 * Nota: dopo resolveTick con DefaultPolicy:
 *   - pollution subisce decay naturale -2 (clamped a 0 se negativo)
 *   - waste è cast a int
 */
class BuildingTest {

    private CityState state;
    private PowerNetwork power;
    private PolicyModifiers defaultMod;

    @BeforeEach
    void setUp() {
        state      = new CityState();
        power      = new PowerNetwork();
        defaultMod = new DefaultPolicy().getModifiers();
    }

    // ── ResidentialBuilding ──────────────────────────────────────────────────

    @Test
    @DisplayName("ResidentialBuilding powered: genera budget, happiness e waste")
    void testResidentialPowered() {
        ResidentialBuilding r = new ResidentialBuilding();
        r.applyEffects(state, power);
        state.resolveTick(defaultMod);

        assertEquals(5002.0, state.getBudget(),    0.001); // 5000 + 2
        assertEquals(67.2,   state.getHappiness(), 0.001); // 67 + 0.2
        assertEquals(1,      state.getWasteLevel());       // 0 + 1 (int)
        assertEquals(5,      power.getTotalConsumption());
    }

    @Test
    @DisplayName("ResidentialBuilding non powered: nessun effetto")
    void testResidentialUnpowered() {
        ResidentialBuilding r = new ResidentialBuilding();
        r.setPowered(false);
        r.applyEffects(state, power);
        state.resolveTick(defaultMod);

        assertEquals(5000.0, state.getBudget(),    0.001); // invariato
        assertEquals(67.0,   state.getHappiness(), 0.001); // invariato
        assertEquals(0,      state.getWasteLevel());       // invariato
    }

    // ── IndustrialBuilding ───────────────────────────────────────────────────

    @Test
    @DisplayName("IndustrialBuilding powered+road: genera budget e pollution, malus happiness/health")
    void testIndustrialPoweredWithRoad() {
        IndustrialBuilding ind = new IndustrialBuilding();
        // connectedToRoad e powered sono già true per default
        ind.applyEffects(state, power);
        state.resolveTick(defaultMod);

        assertEquals(5030.0, state.getBudget(),    0.001); // 5000 + 30
        assertEquals(0.5,    state.getPollution(),  0.001); // 0 + 2.5 - 2.0 (decay)
        assertEquals(66.0,   state.getHappiness(), 0.001); // 67 - 1.0
        assertEquals(99.2,   state.getHealth(),    0.001); // 100 - 0.8
        assertEquals(25,     power.getTotalConsumption());
    }

    @Test
    @DisplayName("IndustrialBuilding powered ma senza road: nessun budget, ma pollution c'è")
    void testIndustrialPoweredNoRoad() {
        IndustrialBuilding ind = new IndustrialBuilding();
        ind.setConnectedToRoad(false);
        ind.applyEffects(state, power);
        state.resolveTick(defaultMod);

        assertEquals(5000.0, state.getBudget(),    0.001); // nessun budget senza road
        assertEquals(0.5,    state.getPollution(),  0.001); // pollution c'è comunque
    }

    // ── CommercialBuilding ───────────────────────────────────────────────────

    @Test
    @DisplayName("CommercialBuilding powered+road: genera budget e happiness")
    void testCommercialPoweredWithRoad() {
        CommercialBuilding c = new CommercialBuilding();
        c.applyEffects(state, power);
        state.resolveTick(defaultMod);

        assertEquals(5015.0, state.getBudget(),    0.001); // 5000 + 15
        assertEquals(68.0,   state.getHappiness(), 0.001); // 67 + 1.0
        // pollution: 0 + 0.3 - 2.0 = -1.7 → clamped a 0
        assertEquals(0.0,    state.getPollution(),  0.001);
        assertEquals(10,     power.getTotalConsumption());
    }

    // ── PowerPlant ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("PowerPlant: produce energia, causa pollution e malus budget/happiness/health")
    void testPowerPlant() {
        PowerPlant pp = new PowerPlant();
        pp.applyEffects(state, power);
        state.resolveTick(defaultMod);

        assertEquals(4980.0, state.getBudget(),    0.001); // 5000 - 20
        assertEquals(1.5,    state.getPollution(),  0.001); // 0 + 3.5 - 2.0 (decay)
        assertEquals(66.0,   state.getHappiness(), 0.001); // 67 - 1.0
        assertEquals(99.0,   state.getHealth(),    0.001); // 100 - 1.0
        assertEquals(250,    power.getTotalProduction());
    }

    // ── Park ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Park: riduce pollution, aumenta happiness e health, costo budget")
    void testPark() {
        Park park = new Park();
        park.applyEffects(state, power);
        state.resolveTick(defaultMod);

        assertEquals(4990.0, state.getBudget(),    0.001); // 5000 - 10
        // pollution: 0 + (-0.5) - 2.0 = -2.5 → clamped a 0
        assertEquals(0.0,    state.getPollution(),  0.001);
        assertEquals(68.5,   state.getHappiness(), 0.001); // 67 + 1.5
        // health: 100 + 1.0 = 101 → clamped a 100
        assertEquals(100.0,  state.getHealth(),    0.001);
    }

    // ── Hospital ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Hospital powered: aumenta health e happiness, costa budget")
    void testHospitalPowered() {
        Hospital h = new Hospital();
        h.applyEffects(state, power);
        state.resolveTick(defaultMod);

        assertEquals(4975.0, state.getBudget(),    0.001); // 5000 - 25
        // health: 100 + 5.0 = 105 → clamped a 100
        assertEquals(100.0,  state.getHealth(),    0.001);
        assertEquals(67.5,   state.getHappiness(), 0.001); // 67 + 0.5
        assertEquals(15,     power.getTotalConsumption());
    }

    @Test
    @DisplayName("Hospital non powered: nessun effetto")
    void testHospitalUnpowered() {
        Hospital h = new Hospital();
        h.setPowered(false);
        h.applyEffects(state, power);
        state.resolveTick(defaultMod);

        assertEquals(5000.0, state.getBudget(),    0.001); // invariato
        assertEquals(100.0,  state.getHealth(),    0.001); // invariato
    }

    // ── WasteManagementCenter ────────────────────────────────────────────────

    @Test
    @DisplayName("WasteManagementCenter powered: riduce waste di 10 e costa budget")
    void testWasteManagementCenterPowered() {
        // Partiamo con wasteLevel > 0 così la riduzione è visibile
        state.setWasteLevel(20);
        WasteManagementCenter wmc = new WasteManagementCenter();
        wmc.applyEffects(state, power);
        state.resolveTick(defaultMod);

        // wasteLevel = max(0, 20 + (-10)) = 10
        assertEquals(10, state.getWasteLevel());
        assertEquals(4980.0, state.getBudget(), 0.001); // 5000 - 20
        assertEquals(10, power.getTotalConsumption());
    }

    @Test
    @DisplayName("WasteManagementCenter non powered: nessun effetto")
    void testWasteManagementCenterUnpowered() {
        state.setWasteLevel(20);
        WasteManagementCenter wmc = new WasteManagementCenter();
        wmc.setPowered(false);
        wmc.applyEffects(state, power);
        state.resolveTick(defaultMod);

        assertEquals(20,     state.getWasteLevel()); // invariato
        assertEquals(5000.0, state.getBudget(), 0.001); // invariato
    }
}
