package it.citylife.model.core;

import it.citylife.model.policies.PolicyModifiers;

/**
 * POJO che rappresenta le soddisfazioni demografiche del gruppo di popolazione (AC-25.1).
 *
 * Contiene tre indici di soddisfazione, ciascuno nel range [0, 100]:
 *   - jobSatisfaction:    soddisfazione lavorativa, dipende dal rapporto tra
 *                         edifici produttivi (industrial + commercial) e residenziali
 *   - healthSatisfaction: soddisfazione sanitaria, dipende dal numero di ospedali
 *                         relativamente alla popolazione residente
 *   - safetySatisfaction: soddisfazione sulla sicurezza, penalizzata da inquinamento,
 *                         edifici danneggiati ed eventi catastrofici.
 *
 * I valori vengono aggiornati ogni tick da {@link PopulationManager#updateDemographics}
 * e letti da {@link CityState#resolveTick} per calcolare il malus di felicità (AC-25.1).
 * Sono inoltre esposti alla UI tramite {@link CityState#getPopulationGroup()}.
 *
 * Tutti i setter applicano il clamping [0, 100] automaticamente.
 *
 * @see PopulationManager
 * @see CityState#resolveTick(PolicyModifiers)
 */
public class PopulationGroup {

    // Valore di partenza neutro (50/100) per tutte le soddisfazioni
    private double jobSatisfaction    = 50.0;
    private double healthSatisfaction = 50.0;
    private double safetySatisfaction = 50.0;

    /** Restituisce la soddisfazione lavorativa corrente [0, 100]. */
    public double getJobSatisfaction()    { return jobSatisfaction; }

    /** Restituisce la soddisfazione sanitaria corrente [0, 100]. */
    public double getHealthSatisfaction() { return healthSatisfaction; }

    /** Restituisce la soddisfazione sulla sicurezza corrente [0, 100]. */
    public double getSafetySatisfaction() { return safetySatisfaction; }

    /** Imposta la soddisfazione lavorativa, clampata in [0, 100]. */
    public void setJobSatisfaction(double v)    { jobSatisfaction    = Math.max(0, Math.min(100, v)); }

    /** Imposta la soddisfazione sanitaria, clampata in [0, 100]. */
    public void setHealthSatisfaction(double v) { healthSatisfaction = Math.max(0, Math.min(100, v)); }

    /** Imposta la soddisfazione sulla sicurezza, clampata in [0, 100]. */
    public void setSafetySatisfaction(double v) { safetySatisfaction = Math.max(0, Math.min(100, v)); }
}
