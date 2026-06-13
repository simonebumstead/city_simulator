package it.citylife.model.structures;

import it.citylife.model.core.GameController;
import it.citylife.model.save.SaveLoadManager;
import it.citylife.model.structures.upgrades.SeismicUpgrade;
import it.citylife.model.structures.upgrades.StructureDecorator;
import it.citylife.model.structures.upgrades.WasteThermalUpgrade;

/**
 * Factory per la creazione e il potenziamento delle strutture della città.
 *
 * Centralizza tutta la logica di istanziazione: il resto del codice non
 * dipende mai dai costruttori concreti delle strutture, ma passa sempre
 * attraverso questa classe (Pattern Factory).
 *
 * Viene usata in due contesti principali:
 *   - GameController.placeBuilding(): per creare una nuova struttura da piazzare sulla griglia.
 *   - SaveLoadManager: per ricostruire la griglia al caricamento di un salvataggio,
 *     re-applicando anche gli upgrade in ordine tramite applyUpgrade().
 *
 * @see Structure
 * @see StructureDecorator
 * @see GameController
 * @see SaveLoadManager
 */
public class BuildingFactory {

    /**
     * Crea e restituisce una nuova istanza della struttura corrispondente al tipo indicato.
     *
     * Il confronto sul tipo è case-insensitive (il valore viene convertito in maiuscolo).
     * I tipi riconosciuti sono: RESIDENTIAL, INDUSTRIAL, COMMERCIAL, POWER_PLANT,
     * PARK, ROAD, HOSPITAL, WASTE_CENTER.
     *
     * @param type stringa che identifica il tipo di struttura da creare
     * @return una nuova istanza della struttura corrispondente
     * @throws IllegalArgumentException se il tipo non corrisponde ad alcuna struttura nota
     */
    public static Structure createBuilding(String type) {
        return switch (type.toUpperCase()) {
            // Edifici abitativi, produttivi e commerciali
            case "RESIDENTIAL" -> new ResidentialBuilding();
            case "INDUSTRIAL"  -> new IndustrialBuilding();
            case "COMMERCIAL"  -> new CommercialBuilding();

            // Infrastrutture energetiche e viarie
            case "POWER_PLANT" -> new PowerPlant();
            case "ROAD"        -> new Road();

            // Servizi pubblici
            case "PARK"        -> new Park();
            case "HOSPITAL"      -> new Hospital();
            case "WASTE_CENTER"  -> new WasteManagementCenter();

            // Tipo non riconosciuto: errore esplicito per segnalare bug nel codice chiamante
            default -> throw new IllegalArgumentException("Unknown building type: " + type);
        };
    }

    /**
     * Avvolge la struttura base in un Decorator che applica il potenziamento richiesto.
     *
     * Se il nome dell'upgrade non è riconosciuto, restituisce la struttura originale
     * senza modifiche (comportamento silenzioso, non lancia eccezione).
     * Gli upgrade riconosciuti sono: SEISMIC, WASTE_THERMAL.
     *
     * Il metodo è usato sia da GameController.upgradeBuilding() al momento del potenziamento,
     * sia da SaveLoadManager al caricamento di un salvataggio per riapplicare
     * gli upgrade in ordine (ogni chiamata aggiunge un livello di decorazione).
     *
     * @param base        la struttura da potenziare (può essere già un StructureDecorator)
     * @param upgradeName stringa che identifica il tipo di upgrade (case-insensitive)
     * @return la struttura avvolta nel Decorator corrispondente, oppure base invariata
     * @see SeismicUpgrade
     * @see WasteThermalUpgrade
     * @see StructureDecorator#getUpgradeLevel()
     */
    public static Structure applyUpgrade(Structure base, String upgradeName) {
        return switch (upgradeName.toUpperCase()) {
            // Riduce i danni ricevuti di metà (inclusi i terremoti) — costo 500
            case "SEISMIC"       -> new SeismicUpgrade(base);

            // Aggiunge riduzione rifiuti e bonus budget al WasteManagementCenter — costo 700
            case "WASTE_THERMAL" -> base.getType() == StructureType.WASTE_CENTER ? new WasteThermalUpgrade(base) : base;

            // Upgrade sconosciuto: restituisce la struttura invariata senza errori
            default -> base;
        };
    }
}
