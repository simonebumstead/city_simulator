package it.citylife.model;

/**
 * Enumerazione dei tipi di struttura piazzabili sulla griglia della città.
 *
 * Usata da City, GameController, PopulationManager e SaveLoadManager per
 * identificare il tipo di una struttura senza dipendere dalle classi concrete,
 * garantendo il corretto funzionamento anche attraverso la catena di Decorator
 * (StructureDecorator.getType() delega alla struttura avvolta).
 *
 * Ogni valore corrisponde a una classe concreta di {@link Structure}:
 *   - RESIDENTIAL  → {@link ResidentialBuilding}
 *   - INDUSTRIAL   → {@link IndustrialBuilding}
 *   - COMMERCIAL   → {@link CommercialBuilding}
 *   - POWER_PLANT  → {@link PowerPlant}
 *   - PARK         → {@link Park}
 *   - ROAD         → {@link Road}
 *   - HOSPITAL     → {@link Hospital}
 *   - WASTE_CENTER → {@link WasteManagementCenter}
 */
public enum StructureType {
    RESIDENTIAL,
    INDUSTRIAL,
    COMMERCIAL,
    POWER_PLANT,
    PARK,
    ROAD,
    HOSPITAL,
    WASTE_CENTER
}
