package it.citylife.model;

/**
 * PowerNetwork gestisce la rete elettrica della città.
 * Tiene traccia dell'energia totale prodotta (dalle centrali) 
 * e di quella consumata (dagli edifici).
 */
public class PowerNetwork {
    private int totalProduction;
    private int totalConsumption;

    public PowerNetwork() {
        this.totalProduction = 0;
        this.totalConsumption = 0;
    }

    // --- METODI PER GESTIRE LA RETE ---

    /**
     * Aggiunge energia alla rete 
     */
    public void addProduction(int amount) {
        this.totalProduction += amount;
    }

    /**
     * Aggiunge carico alla rete 
     */
    public void addConsumption(int amount) {
        this.totalConsumption += amount;
    }

    /**
     * Verifica se la città è in blackout (consumo supera la produzione).
     * @return true se c'è abbastanza energia, false se c'è un blackout.
     */
    public boolean hasEnoughPower() {
        return totalProduction >= totalConsumption;
    }

    public void reset() {
        this.totalProduction = 0;
        this.totalConsumption = 0;
    }

    // Getters per la UI
    public int getTotalProduction() { return totalProduction; }
    public int getTotalConsumption() { return totalConsumption; }
}