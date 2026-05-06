package it.citylife.model;

/**
 * Strategy Pattern per le leggi cittadine.
 * Ogni policy non applica direttamente le modifiche, ma restituisce un
 * oggetto contenente i moltiplicatori che il motore di gioco (advanceTick)
 * userà per calcolare lo stato aggiornato della città.
 */
public interface PolicyStrategy {
    /**
     * Restituisce i modificatori associati a questa specifica policy.
     * @return Un oggetto PolicyModifiers con i valori da applicare.
     */
    PolicyModifiers getModifiers();
}