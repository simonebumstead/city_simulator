package it.citylife.model;

/**
 * StateObserver è l'interfaccia alla base dell'Observer Pattern per questo progetto.
 * Funziona come un "contratto": qualsiasi componente (es. la UI JavaFX)
 * che vuole ricevere aggiornamenti dalla simulazione DEVE implementare questa interfaccia.
 */
public interface StateObserver {
    
    /**
     * Questo metodo viene invocato automaticamente dalla classe City 
     * alla fine di ogni turno (tick).
     * 
     * @param newState Una copia o il riferimento allo stato attuale della città (budget, felicità, ecc.)
     */
    void onStateChanged(CityState newState);
}