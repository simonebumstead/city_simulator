package it.citylife.model;

public class BuildingFactory {
    public static Structure createBuilding(String type) {
        return switch (type.toUpperCase()) {
            case "RESIDENTIAL" -> new ResidentialBuilding();
            case "INDUSTRIAL"  -> new IndustrialBuilding();
            case "COMMERCIAL"  -> new CommercialBuilding();
            case "POWER_PLANT" -> new PowerPlant();
            case "PARK"        -> new Park();
            case "ROAD"        -> new Road();
            case "HOSPITAL"      -> new Hospital();
            case "WASTE_CENTER"  -> new WasteManagementCenter();
            default -> throw new IllegalArgumentException("Unknown building type: " + type);
        };
    }

    public static Structure applyUpgrade(Structure base, String upgradeName) {
        return switch (upgradeName.toUpperCase()) {
            case "SEISMIC"       -> new SeismicUpgrade(base);
            case "WASTE_THERMAL" -> new WasteThermalUpgrade(base);
            default -> base;
        };
    }
}
