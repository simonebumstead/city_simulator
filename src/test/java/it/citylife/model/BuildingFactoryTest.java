package it.citylife.model;

import it.citylife.model.structures.BuildingFactory;
import it.citylife.model.structures.ResidentialBuilding;
import it.citylife.model.structures.Structure;
import it.citylife.model.structures.StructureType;
import it.citylife.model.structures.WasteManagementCenter;
import it.citylife.model.structures.upgrades.SeismicUpgrade;
import it.citylife.model.structures.upgrades.StructureDecorator;
import it.citylife.model.structures.upgrades.WasteThermalUpgrade;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Testa BuildingFactory (Pattern Factory): creazione delle strutture per tipo
 * e applicazione degli upgrade (Decorator) con le relative restrizioni.
 *
 * Il confronto sul tipo è case-insensitive; un tipo sconosciuto deve sollevare
 * un'eccezione esplicita, mentre un upgrade sconosciuto deve restituire la
 * struttura invariata (fallimento silenzioso).
 */
class BuildingFactoryTest {

    // ── createBuilding: ogni tipo riconosciuto produce la struttura corretta ──

    @Test
    @DisplayName("createBuilding crea ogni tipo riconosciuto con il StructureType atteso")
    void testCreateBuildingAllTypes() {
        assertEquals(StructureType.RESIDENTIAL, BuildingFactory.createBuilding("RESIDENTIAL").getType());
        assertEquals(StructureType.INDUSTRIAL,  BuildingFactory.createBuilding("INDUSTRIAL").getType());
        assertEquals(StructureType.COMMERCIAL,  BuildingFactory.createBuilding("COMMERCIAL").getType());
        assertEquals(StructureType.POWER_PLANT, BuildingFactory.createBuilding("POWER_PLANT").getType());
        assertEquals(StructureType.ROAD,        BuildingFactory.createBuilding("ROAD").getType());
        assertEquals(StructureType.PARK,        BuildingFactory.createBuilding("PARK").getType());
        assertEquals(StructureType.HOSPITAL,    BuildingFactory.createBuilding("HOSPITAL").getType());
        assertEquals(StructureType.WASTE_CENTER, BuildingFactory.createBuilding("WASTE_CENTER").getType());
    }

    @Test
    @DisplayName("createBuilding è case-insensitive sul tipo")
    void testCreateBuildingCaseInsensitive() {
        assertEquals(StructureType.RESIDENTIAL, BuildingFactory.createBuilding("residential").getType());
    }

    @Test
    @DisplayName("createBuilding con tipo sconosciuto solleva IllegalArgumentException")
    void testCreateBuildingUnknownTypeThrows() {
        assertThrows(IllegalArgumentException.class, () -> BuildingFactory.createBuilding("UNKNOWN_TYPE"));
    }

    // ── applyUpgrade: avvolgimento nel Decorator e restrizioni ────────────────

    @Test
    @DisplayName("applyUpgrade SEISMIC avvolge la struttura in un SeismicUpgrade")
    void testApplyUpgradeSeismicWraps() {
        Structure base = new ResidentialBuilding();
        Structure upgraded = BuildingFactory.applyUpgrade(base, "SEISMIC");
        assertInstanceOf(SeismicUpgrade.class, upgraded);
        // Il Decorator preserva il tipo della struttura avvolta
        assertEquals(StructureType.RESIDENTIAL, upgraded.getType());
    }

    @Test
    @DisplayName("applyUpgrade WASTE_THERMAL avvolge solo un WasteManagementCenter")
    void testApplyUpgradeWasteThermalOnWasteCenter() {
        Structure wasteCenter = new WasteManagementCenter();
        Structure upgraded = BuildingFactory.applyUpgrade(wasteCenter, "WASTE_THERMAL");
        assertInstanceOf(WasteThermalUpgrade.class, upgraded);
    }

    @Test
    @DisplayName("applyUpgrade WASTE_THERMAL su struttura non idonea restituisce la base invariata")
    void testApplyUpgradeWasteThermalRejectedOnOtherTypes() {
        Structure base = new ResidentialBuilding();
        Structure result = BuildingFactory.applyUpgrade(base, "WASTE_THERMAL");
        assertSame(base, result);
        assertFalse(result instanceof StructureDecorator);
    }

    @Test
    @DisplayName("applyUpgrade con nome sconosciuto restituisce la struttura invariata")
    void testApplyUpgradeUnknownReturnsBase() {
        Structure base = new ResidentialBuilding();
        Structure result = BuildingFactory.applyUpgrade(base, "NON_ESISTE");
        assertSame(base, result);
    }
}
