package it.citylife.model;

/**
 * Decorator che aggiunge il recupero energetico dai rifiuti al WasteManagementCenter (AC-18.4).
 *
 * Estende il comportamento del centro di gestione rifiuti avvolto chiamando prima
 * il suo applyEffects() (che applica la riduzione base di −10 waste/tick),
 * poi aggiunge gli effetti del recupero termico: ulteriori −15 waste/tick
 * e +50 budget/tick come entrata da vendita di energia termica.
 *
 * Gli effetti aggiuntivi sono condizionati all'alimentazione elettrica del centro:
 * se il WasteManagementCenter interno non è powered, l'impianto è fermo
 * e il recupero termico non avviene.
 *
 * Tutti gli altri comportamenti (HP, tipo, costo) sono delegati alla struttura avvolta.
 *
 * Costo di applicazione: 700 (detratti dal budget in GameController.upgradeBuilding).
 *
 * @see StructureDecorator
 * @see WasteManagementCenter
 * @see GameController#upgradeBuilding(int, int, String)
 */
public class WasteThermalUpgrade extends StructureDecorator {

    // Costo di applicazione dell'upgrade, referenziato da GameController
    public static final int COST = 700;

    // Riduzione aggiuntiva di rifiuti per tick grazie al trattamento termico
    private static final double EXTRA_WASTE_REDUCTION = 5.0;

    // Entrata da vendita di energia termica prodotta dalla combustione dei rifiuti
    private static final double THERMAL_BUDGET_BONUS  = 50.0;

    /**
     * Crea un WasteThermalUpgrade che avvolge la struttura indicata.
     *
     * @param wrapped la struttura da potenziare con il recupero termico (tipicamente WasteManagementCenter)
     */
    public WasteThermalUpgrade(Structure wrapped) {
        super(wrapped);
    }

    /**
     * Applica gli effetti base della struttura avvolta, poi aggiunge gli effetti del recupero termico.
     *
     * Gli effetti aggiuntivi (−15 waste, +50 budget) si attivano solo se la struttura
     * interna è alimentata: senza corrente l'impianto termico non è operativo.
     *
     * @param state lo stato della città su cui accumulare i delta
     * @param power la rete elettrica (passata alla struttura avvolta)
     */
    @Override
    public void applyEffects(CityState state, PowerNetwork power) {
        // Applica prima gli effetti base del WasteManagementCenter (−10 waste, −20 budget, −10 power)
        wrapped.applyEffects(state, power);

        if (wrapped.isPowered()) {
            // Trattamento termico attivo: brucia i rifiuti per produrre energia vendibile
            state.updateWaste(-EXTRA_WASTE_REDUCTION); // Riduzione aggiuntiva per combustione
            state.updateBudget(THERMAL_BUDGET_BONUS);  // Entrata da vendita di energia termica alla rete
        }
    }

    /**
     * Restituisce il nome dell'upgrade per la serializzazione nel file di salvataggio.
     *
     * @return "WASTE_THERMAL"
     */
    @Override
    public String getUpgradeName() {
        return "WASTE_THERMAL";
    }
}
