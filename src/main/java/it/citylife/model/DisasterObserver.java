package it.citylife.model;

/**
 * Interfaccia del Pattern Observer per la gestione degli eventi sismici.
 * Implementato dalle strutture per ricevere i danni causati dal terremoto.
 */
public interface DisasterObserver {
    void onEarthquake(int damage);
}