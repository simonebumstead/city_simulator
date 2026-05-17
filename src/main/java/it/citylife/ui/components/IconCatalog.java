package it.citylife.ui.components;

import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;

import it.citylife.model.StructureType;
import javafx.scene.paint.Color;

/** Mapping tra {@link StructureType} e relativa icona / colore / etichetta UI. */
public final class IconCatalog {

    private IconCatalog() {}

    public static FontAwesomeSolid iconFor(StructureType type) {
        return switch (type) {
            case RESIDENTIAL  -> FontAwesomeSolid.HOME;
            case INDUSTRIAL   -> FontAwesomeSolid.INDUSTRY;
            case COMMERCIAL   -> FontAwesomeSolid.STORE;
            case POWER_PLANT  -> FontAwesomeSolid.BOLT;
            case PARK         -> FontAwesomeSolid.TREE;
            case ROAD         -> FontAwesomeSolid.ROAD;
            case HOSPITAL     -> FontAwesomeSolid.HOSPITAL;
            case WASTE_CENTER -> FontAwesomeSolid.TRASH;
        };
    }

    public static Color colorFor(StructureType type) {
        return switch (type) {
            case RESIDENTIAL  -> Color.web("#60a5fa");
            case INDUSTRIAL   -> Color.web("#fb923c");
            case COMMERCIAL   -> Color.web("#facc15");
            case POWER_PLANT  -> Color.web("#f472b6");
            case PARK         -> Color.web("#4ade80");
            case ROAD         -> Color.web("#94a3b8");
            case HOSPITAL     -> Color.web("#f87171");
            case WASTE_CENTER -> Color.web("#a78bfa");
        };
    }

    public static String labelFor(StructureType type) {
        return switch (type) {
            case RESIDENTIAL  -> "Residential";
            case INDUSTRIAL   -> "Industrial";
            case COMMERCIAL   -> "Commercial";
            case POWER_PLANT  -> "Power Plant";
            case PARK         -> "Park";
            case ROAD         -> "Road";
            case HOSPITAL     -> "Hospital";
            case WASTE_CENTER -> "Waste Center";
        };
    }
}
