package it.citylife.model.structures.upgrades;

import it.citylife.model.core.City;
import it.citylife.model.core.CityState;
import it.citylife.model.core.GameController;

/**
 * Interfaccia del Pattern Observer per la ricezione degli aggiornamenti di stato della città.
 *
 * Qualsiasi componente che vuole essere notificato al termine di ogni tick
 * deve implementare questa interfaccia e registrarsi tramite
 * {@link City#addObserver(StateObserver)} (o equivalentemente tramite
 * {@link GameController#addObserver(StateObserver)}).
 *
 * Attualmente l'unico osservatore registrato è {@code DashboardView} (la UI JavaFX),
 * ma l'interfaccia permette di aggiungere futuri osservatori (es. logger, AI avversaria)
 * senza modificare il codice del motore di simulazione.
 *
 * @see City#notifyObservers()
 * @see CityState
 */
public interface StateObserver {

    /**
     * Invocato da {@link City} al termine di ogni tick con lo stato aggiornato della città.
     *
     * L'implementazione deve limitarsi a leggere i dati da {@code newState} e aggiornare
     * la propria visualizzazione; non deve modificare lo stato della città.
     *
     * @param newState lo stato corrente della città dopo la risoluzione del tick
     */
    void onStateChanged(CityState newState);
}
