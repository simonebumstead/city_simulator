package it.citylife.model;

/**
 * Politica economica neutrale: nessun bonus né malus applicato alla città.
 *
 * Tutti i moltiplicatori restano a 1.0 e tutti i modificatori flat a 0,
 * quindi resolveTick() applica i delta degli edifici senza alcuna alterazione.
 *
 * È la politica attiva all'avvio della simulazione e può essere riselezionata
 * dal giocatore in qualsiasi momento per rimuovere gli effetti delle altre politiche.
 *
 * @see PolicyStrategy
 * @see PolicyModifiers
 * @see CityState#resolveTick(PolicyModifiers)
 */
public class DefaultPolicy implements PolicyStrategy {

    /**
     * Restituisce un PolicyModifiers con tutti i valori neutri.
     *
     * Il costruttore di default di PolicyModifiers inizializza già tutti i
     * moltiplicatori a 1.0 e tutti i modificatori flat a 0, quindi non è
     * necessaria alcuna configurazione aggiuntiva.
     *
     * @return un PolicyModifiers neutro, senza alcun effetto sui delta
     */
    @Override
    public PolicyModifiers getModifiers() {
        return new PolicyModifiers();
    }
}
