package it.citylife.model;

/**
 * Centro di gestione dei rifiuti: riduce il livello di rifiuti accumulati in città (AC-18.3).
 *
 * Richiede alimentazione elettrica per funzionare; se spento non produce alcun effetto.
 * Ogni centro attivo riduce il deltaWaste di WASTE_REDUCTION_PER_TICK (−10/tick),
 * contrastando i rifiuti generati dagli edifici residenziali (+1/tick ciascuno).
 *
 * Può essere potenziato con {@link WasteThermalUpgrade} (AC-18.4), che aggiunge
 * ulteriori −15 waste/tick e un bonus di +50 budget/tick grazie al recupero
 * energetico dai rifiuti trattati.
 *
 * Effetti per tick (quando alimentato):
 *   - Waste:   −10.0 (smaltimento rifiuti)
 *   - Budget:  −20 (costi operativi: personale e manutenzione degli impianti)
 *   - Consumo: 10 unità di energia
 *
 * Costo di costruzione: 900. HP massimi: 350.
 *
 * @see Structure
 * @see WasteThermalUpgrade
 * @see CityState#updateWaste(double)
 */
public class WasteManagementCenter extends Structure {

    // Riduzione di rifiuti applicata ogni tick quando il centro è alimentato
    private static final double WASTE_REDUCTION_PER_TICK = 10.0;

    /**
     * Crea un centro di gestione rifiuti con 350 HP massimi.
     */
    public WasteManagementCenter() {
        super(350);
    }

    /**
     * Applica gli effetti del centro di gestione rifiuti allo stato della città per il tick corrente.
     *
     * Se il centro non è alimentato non produce alcun effetto: gli impianti di
     * smaltimento richiedono energia per funzionare.
     *
     * @param state lo stato della città su cui accumulare i delta
     * @param power la rete elettrica a cui dichiarare il consumo energetico
     */
    @Override
    public void applyEffects(CityState state, PowerNetwork power) {
        // Senza corrente gli impianti di smaltimento sono fermi: nessun effetto
        if (!this.powered) return;

        state.updateWaste(-WASTE_REDUCTION_PER_TICK); // Smaltimento attivo: riduce i rifiuti accumulati
        state.updateBudget(-20);                       // Costi operativi: personale e manutenzione degli impianti
        power.addConsumption(10);                      // Consumo energetico dei macchinari di trattamento
    }

    /** Restituisce il tipo WASTE_CENTER, usato da City e GameController per identificare il centro. */
    @Override
    public StructureType getType() { return StructureType.WASTE_CENTER; }

    /** Costo di costruzione in budget: 900. */
    @Override
    public int getConstructionCost() { return 900; }
}
