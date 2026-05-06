package it.citylife.model;

import java.util.ArrayList;
import java.util.List;

public class City {
    // Componenti principali definiti nel Domain Model
    private Grid grid;               // La mappa 20x20
    private CityState state;         // I contatori (budget, pop, ecc.)
    private PowerNetwork powerNet;   // La gestione dell'energia
        
    // Questa è una lista che contiene tutti i "soggetti" interessati ai cambiamenti della città.
    // Usiamo l'interfaccia 'StateObserver' invece di una classe specifica (es. DashboardUI)
    // per mantenere il codice flessibile (Disaccoppiamento).
    private List<StateObserver> observers;

    public City() {
        this.grid = new Grid();
        this.state = new CityState();
        this.powerNet = new PowerNetwork();
        this.observers = new ArrayList<>();
    }

    // --- METODI CORE  ---

    //Coordina l'aggiornamento di tutti i sistemi.
    public void advanceTick() {
        // 1. Aggiorna lo stato interno (Information Expert)
        updateState();
        
        // 2. Notifica la UI che i dati sono cambiati (Observer Pattern)
        notifyObservers();
    }

    private void updateState() {
        
    }

    // Metodo per registrare la UI come osservatore
    public void addObserver(StateObserver observer) {
        this.observers.add(observer);
    }

    private void notifyObservers() {
        for (StateObserver obs : observers) {
            obs.onStateChanged(this.state);
        }
    }

    // Getters per permettere alla UI di leggere (ma non modificare direttamente)
    public CityState getState() { return state; }
    public Grid getGrid() { return grid; }
    public PowerNetwork getPowerNet() {return powerNet; }
}