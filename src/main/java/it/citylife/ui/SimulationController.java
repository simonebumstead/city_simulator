package it.citylife.ui;

import it.citylife.model.City;
import it.citylife.model.CityState;
import it.citylife.model.GreenPolicy;
import it.citylife.model.PolicyStrategy;
import it.citylife.model.StateObserver;

import java.util.Random;

public class SimulationController {

    private final City city;
    private PolicyStrategy activePolicy;
    private final Random random = new Random();

    public SimulationController() {
        this.city = new City();
        this.activePolicy = new GreenPolicy();
    }

    public void tick() {
        CityState state = city.getState();

        state.updateBudget(100);
        state.setPopulation(Math.max(0, state.getPopulation() + (random.nextInt(11) - 5)));
        state.updateHappiness(random.nextInt(5) - 2);
        state.updatePollution(random.nextInt(7) - 3);
        state.updateHealth(random.nextInt(3) - 1);

        city.advanceTick();
    }

    public void setPolicy(PolicyStrategy policy) {
        this.activePolicy = policy;
    }

    public void addObserver(StateObserver observer) {
        city.addObserver(observer);
    }

    public boolean hasPower() {
        return city.getPowerNet().hasEnoughPower();
    }
}
