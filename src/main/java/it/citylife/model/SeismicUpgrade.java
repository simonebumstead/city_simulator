package it.citylife.model;

/**
 * Decorator che applica un rinforzo antisismico alla struttura avvolta (AC-16.2).
 *
 * Sovrascrive takeDamage() dimezzando qualsiasi danno ricevuto, con un minimo
 * di 1 per garantire che la struttura possa comunque deteriorarsi nel tempo.
 * Il dimezzamento si applica anche ai danni da terremoto grazie al dispatch
 * virtuale su Structure.onEarthquake() → takeDamage() (AC-14.2).
 *
 * Tutti gli altri comportamenti (applyEffects, HP, tipo, costo) sono delegati
 * alla struttura avvolta tramite StructureDecorator.
 *
 * Costo di applicazione: 500 (detratti dal budget in GameController.upgradeBuilding).
 *
 * @see StructureDecorator
 * @see Structure#onEarthquake(int)
 * @see GameController#upgradeBuilding(int, int, String)
 */
public class SeismicUpgrade extends StructureDecorator {

    // Costo di applicazione dell'upgrade, referenziato da GameController
    public static final int COST = 500;

    /**
     * Crea un SeismicUpgrade che avvolge la struttura indicata.
     *
     * @param wrapped la struttura da proteggere con il rinforzo antisismico
     */
    public SeismicUpgrade(Structure wrapped) {
        super(wrapped);
    }

    /**
     * Applica il danno dimezzato alla struttura avvolta.
     *
     * Il danno viene diviso per 2 prima di essere passato alla struttura interna;
     * il minimo garantito è 1 per evitare che la struttura diventi indistruttibile.
     *
     * @param amount il danno originale da infliggere
     * @return gli HP rimanenti dopo il danno dimezzato
     */
    @Override
    public int takeDamage(int amount) {
        return wrapped.takeDamage(Math.max(1, amount / 2));
    }

    /**
     * Restituisce il nome dell'upgrade per la serializzazione nel file di salvataggio.
     *
     * @return "SEISMIC"
     */
    @Override
    public String getUpgradeName() {
        return "SEISMIC";
    }
}
