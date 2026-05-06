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
}
