package it.citylife.model;

/**
 * CityState è una classe di tipo "Entity" (o Data Holder).
 * Il suo unico scopo è memorizzare i parametri numerici della simulazione.
 * Come Core Engineer, ho applicato l'incapsulamento per proteggere i dati.
 */
public class CityState {
    
    // COSTANTI DI BILANCIAMENTO (Semplificano il Game Design)
    private static final double INITIAL_BUDGET = 1000.0;
    private static final double INITIAL_HAPPINESS = 67.0; // Partenza al 67% come richiesto
    private static final double MAX_VAL = 100.0;
    private static final double MIN_VAL = 0.0;

    // ATTRIBUTI (Privati per sicurezza)
    private double budget;
    private int population;
    private double pollution;
    private double happiness;
    private double health;
    private int wasteLevel;

    /**
     * Costruttore: Inizializza lo stato della città con i valori di default.
     */
    public CityState() {
        this.budget = INITIAL_BUDGET;
        this.happiness = INITIAL_HAPPINESS;
        this.population = 0;
        this.pollution = 0.0;
        this.health = 100.0; // I cittadini partono sani
        this.wasteLevel = 0;
    }

    // --- METODI DI ACCESSO (Getter) ---

    public double getBudget() { return budget; }
    public int getPopulation() { return population; }
    public double getPollution() { return pollution; }
    public double getHappiness() { return happiness; }
    public double getHealth() { return health; }
    public int getWasteLevel() { return wasteLevel; }

    // --- METODI DI MODIFICA (Setter con Logica) ---

    /**
     * Aggiorna il budget. Può accettare valori negativi (spese) o positivi (tasse).
     */
    public void updateBudget(double amount) {
        this.budget += amount;
    }

    /**
     * Aggiorna la felicità applicando il "Clamping" (rimane tra 0 e 100).
     */
    public void updateHappiness(double amount) {
        this.happiness += amount;
        if (this.happiness > MAX_VAL) this.happiness = MAX_VAL;
        if (this.happiness < MIN_VAL) this.happiness = MIN_VAL;
    }

    public void setPopulation(int population) {
        this.population = population;
    }

    public void updateHealth(double amount) {
        this.health += amount;
        if (this.health > MAX_VAL) this.health = MAX_VAL;
        if (this.health < MIN_VAL) this.health = MIN_VAL;
    }

    public void updatePollution(double amount) {
        this.pollution += amount;
        if (this.pollution > MAX_VAL) this.pollution = MAX_VAL;
        if (this.pollution < MIN_VAL) this.pollution = MIN_VAL;
    }

    public void setWasteLevel(int value) {
        this.wasteLevel = (int) Math.max(MIN_VAL, Math.min(MAX_VAL, value));
    }
}