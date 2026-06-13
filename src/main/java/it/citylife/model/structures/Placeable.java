package it.citylife.model.structures;

import it.citylife.model.structures.upgrades.StructureDecorator;
import it.citylife.model.grid.Cell;

/**
 * Interfaccia marcatore per tutto ciò che può essere piazzato in una {@link Cell} della griglia.
 *
 * Definisce il contratto minimo: ogni oggetto piazzabile deve dichiarare il proprio
 * tipo tramite {@link StructureType}. Questo permette a City e GameController di
 * interrogare il tipo di una struttura senza dover fare cast a classi concrete,
 * e funziona correttamente anche quando la struttura è avvolta in un Decorator.
 *
 * Attualmente l'unica gerarchia che implementa Placeable è {@link Structure}
 * (e le sue sottoclassi e decoratori), ma l'interfaccia lascia aperta la possibilità
 * di aggiungere in futuro altri tipi di oggetti piazzabili (es. segnali, decorazioni).
 *
 * @see Structure
 * @see StructureDecorator
 * @see StructureType
 * @see Cell
 */
public interface Placeable {

    /**
     * Restituisce il tipo di struttura rappresentato da questo oggetto.
     *
     * @return il {@link StructureType} corrispondente
     */
    StructureType getType();
}
