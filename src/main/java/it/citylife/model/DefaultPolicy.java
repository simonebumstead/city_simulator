package it.citylife.model;

/**
 * La Default Policy è una politica neutrale.
 * Non applica nessun bonus o malus (tutti i moltiplicatori a 1.0 e gli additivi a 0).
 * Ideale per l'inizio del gioco o quando il sindaco non vuole interferire.
 */
public class DefaultPolicy implements PolicyStrategy {
    
    @Override
    public PolicyModifiers getModifiers() {
        // Il costruttore di base di PolicyModifiers ha già tutti i parametri 
        // impostati su valori neutri (1.0 per la generazione, 0 per i cambiamenti fissi)
        return new PolicyModifiers(); 
    }
}