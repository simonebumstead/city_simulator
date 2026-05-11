package it.citylife.ui;

import it.citylife.model.GameController;
import it.citylife.model.PolicyStrategy;
import it.citylife.model.StateObserver;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

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

    public boolean placeBuilding(String type, int x, int y) {
        return controller.placeBuilding(type, x, y);
    }

    public boolean demolish(int x, int y) {
        return controller.demolish(x, y);
    }

    public boolean repair(int x, int y) {
        return controller.repair(x, y);
    }

    public boolean upgrade(int x, int y, String upgradeType) {
        return controller.upgradeBuilding(x, y, upgradeType);
    }

    public it.citylife.model.Grid getGrid() {
        return controller.getGrid();
    }

    public it.citylife.model.CityState getState() {
        return controller.getState();
    }

    public Path save(int tick) throws IOException {
        return controller.saveGame(tick);
    }

    public List<Path> listSaves() throws IOException {
        return controller.listSaves();
    }

    public int load(Path path) throws IOException {
        return controller.loadGame(path);
    }
}
