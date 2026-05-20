package it.citylife.ui.components;

import javafx.scene.control.Tooltip;
import javafx.util.Duration;

/** Tooltip statici per i pulsanti della UI (toolbar e barra controlli). */
final class ToolTooltips {

    private ToolTooltips() {}

    static Tooltip forTool(String tool) {
        if (tool == null) return null;
        String text = switch (tool) {
            case "RESIDENTIAL" -> "🏠 Residential\nCost: 500 $ | Max HP: 300\nCapacity: 200\n"
                    + "Effects: +2 Budget, +0.2 Happiness, +1 Waste\n"
                    + "Consumes: 5 Power\nRequires: Power, Adjacent Road";
            case "INDUSTRIAL" -> "🏭 Industrial\nCost: 1000 $ | Max HP: 400\n"
                    + "Effects: +30 Budget, +2.5 Pollution, -1 Happiness, -0.8 Health\n"
                    + "Consumes: 25 Power\nRequires: Power, Adjacent Road";
            case "COMMERCIAL" -> "🏬 Commercial\nCost: 750 $ | Max HP: 300\n"
                    + "Effects: +15 Budget, +0.3 Pollution, +1 Happiness\n"
                    + "Consumes: 10 Power\nRequires: Power, Adjacent Road";
            case "POWER_PLANT" -> "⚡ Power Plant\nCost: 2000 $ | Max HP: 500\n"
                    + "Produces: 250 Power (radius 5)\n"
                    + "Effects: -20 Budget, +3.5 Pollution, -1 Happiness, -1 Health";
            case "PARK" -> "🌳 Park\nCost: 300 $ | Max HP: 200\n"
                    + "Effects: -10 Budget, -0.5 Pollution, +1.5 Happiness, +1 Health\n"
                    + "Radius: +2 Happiness (radius 3), -3 Global Pollution";
            case "HOSPITAL" -> "🏥 Hospital\nCost: 1200 $ | Max HP: 350\n"
                    + "Effects: -25 Budget, +5 Health, +0.5 Happiness\n"
                    + "Consumes: 15 Power\nRequires: Power";
            case "WASTE_CENTER" -> "🗑️ Waste Center\nCost: 900 $ | Max HP: 350\n"
                    + "Effects: -20 Budget, -10 Waste\n"
                    + "Consumes: 10 Power\nRequires: Power";
            case "ROAD" -> "🛣️ Road\nCost: 100 $ | Can't take damage\nEffects: +0.1 Pollution";
            case "REPAIR" -> "🔧 Repair\nClick or drag to repair damaged structures.";
            case "DEMOLISH" -> "🔨 Demolish\nClick or drag to destroy structures.";
            case "UPGRADE_SEISMIC" -> "🛡️ SEISMIC UPGRADE\n💵 Cost: 500 $\n"
                    + "✨ Effect: Halves earthquake damage\n📌 Max level: 3 per building";
            case "UPGRADE_WASTE_THERMAL" -> "🔥 WASTE THERMAL UPGRADE\n💵 Cost: 700 $\n"
                    + "✨ Effect: -15 Waste, +50 Budget\n⚡ Requires: Power\n📌 Max level: 3 per building";
            default -> null;
        };
        if (text == null) return null;
        Tooltip tt = new Tooltip(text);
        tt.setShowDelay(Duration.millis(150));
        tt.setShowDuration(Duration.INDEFINITE);
        return tt;
    }

    static Tooltip forPolicy(String policyName) {
        if (policyName == null) return null;
        String text = switch (policyName) {
            case "DEFAULT" -> "📜 Default Policy\nNo special modifiers.\nThe city runs on its own merits.";
            case "GREEN" -> "🌿 Green Policy\nCost: -200 Budget/tick\nEffects:\n • -50% Pollution\n • + Health & Happiness";
            case "FOSSIL_FUEL" -> "⛽ Fossil Fuel Policy\nEffects:\n • +300 Budget/tick\n • x1.5 Industrial revenue\n • x2 Pollution\n • - Health";
            case "AUSTERITY" -> "💰 Austerity Policy\nEffects:\n • +500 Budget/tick\n • -15 Happiness/tick\n • -2 Health/tick";
            default -> null;
        };
        if (text == null) return null;
        Tooltip tt = new Tooltip(text);
        tt.setShowDelay(Duration.millis(150));
        tt.setShowDuration(Duration.INDEFINITE);
        return tt;
    }
}
