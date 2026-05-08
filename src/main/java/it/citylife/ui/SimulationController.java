package it.citylife.ui;

import it.citylife.model.GameController;
import it.citylife.model.PolicyStrategy;
import it.citylife.model.StateObserver;

public class SimulationController {

    private final GameController controller;

    public SimulationController() {
        this.controller = new GameController();
    }

    public void tick() {
        controller.advanceTick();
    }

    public void setPolicy(PolicyStrategy policy) {
        controller.changePolicy(policy);
    }

    public void addObserver(StateObserver observer) {
        controller.addObserver(observer);
    }

    public boolean hasPower() {
        return controller.getPowerNet().hasEnoughPower();
    }

    public void placeBuilding(String type, int x, int y) {
        controller.placeBuilding(type, x, y);
    }

    public boolean demolish(int x, int y) {
        return controller.demolish(x, y);
    }

    public it.citylife.model.Grid getGrid() {
        return controller.getGrid();
    }

    public it.citylife.model.CityState getState() {
        return controller.getState();
    }
}
