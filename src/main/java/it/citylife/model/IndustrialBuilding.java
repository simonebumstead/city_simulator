package it.citylife.model;

/**
 * Edificio industriale: fabbriche e impianti produttivi della città.
 *
 * È la struttura con la maggiore produzione di budget per tick, ma anche
 * la più inquinante e quella con l'impatto negativo più alto su felicità e salute.
 * Richiede alimentazione elettrica e connessione stradale per generare revenue
 * (le merci devono poter essere trasportate).
 *
 * La quota di budget generata viene tracciata separatamente in CityState
 * tramite addIndustrialBudgetDelta(), in modo che politiche come FossilFuelPolicy
 * possano applicarle un moltiplicatore dedicato (×1.5) in resolveTick().
 *
 * Effetti per tick (quando alimentato):
 *   - Budget:    +30 (solo se connesso a una strada)
 *   - Pollution: +2.5
 *   - Happiness: −1.0
 *   - Health:    −0.8
 *   - Consumo:   25 unità di energia
 *
 * Costo di costruzione: 1000. HP massimi: 400.
 *
 * @see Structure
 * @see CityState#addIndustrialBudgetDelta(double)
 * @see FossilFuelPolicy
 */
public class IndustrialBuilding extends Structure {

    /**
     * Crea un edificio industriale con 400 HP massimi.
     * Gli HP elevati riflettono la robustezza strutturale degli impianti produttivi.
     */
    public IndustrialBuilding() {
        super(400);
    }

    /**
     * Applica gli effetti dell'edificio industriale allo stato della città per il tick corrente.
     *
     * Se l'edificio non è alimentato la fabbrica è ferma e non produce alcun effetto.
     * La revenue (+30) viene registrata anche nel delta industrial separato,
     * così resolveTick() può applicarle il moltiplicatore della politica attiva.
     *
     * @param state lo stato della città su cui accumulare i delta
     * @param power la rete elettrica a cui dichiarare il consumo energetico
     */
    @Override
    public void applyEffects(CityState state, PowerNetwork power) {
        // Fabbrica ferma: senza corrente non c'è produzione né effetti collaterali
        if (!this.powered) {
            return;
        }

        // Revenue generata solo se la strada permette il trasporto delle merci prodotte
        if (this.connectedToRoad) {
            state.updateBudget(30);
            // Tracciato separatamente per permettere a FossilFuelPolicy di applicare il moltiplicatore industrial
            state.addIndustrialBudgetDelta(30);
        }

        state.updatePollution(2.5);             // Emissioni industriali: la fonte principale di inquinamento
        state.updateHappiness(-1.0);            // Rumore, traffico pesante e smog riducono il benessere
        state.updateHealth(-0.8);               // Esposizione cronica alle emissioni danneggia la salute
        power.addConsumption(25);               // Consumo energetico elevato: macchinari e impianti H24
        state.addIndustrialPollutionDelta(2.5); // Tracciato separatamente per statistiche (attualmente non usato in resolveTick)
    }

    /** Restituisce il tipo INDUSTRIAL, usato da City e PopulationManager per il conteggio demografico. */
    @Override
    public StructureType getType() {
        return StructureType.INDUSTRIAL;
    }

    /** Costo di costruzione in budget: 1000. */
    @Override
    public int getConstructionCost() { return 1000; }
}
