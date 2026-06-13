# System Test & Acceptance Criteria Verification

Questo documento traccia la verifica degli Acceptance Criteria (AC) estratti dalle User Stories, mettendoli a confronto con l'effettiva implementazione nel codice sorgente del simulatore.

---

## Epic 1: Governance and policies

### Story 1: Cambio di politica cittadina
- **AC 1: Avere a disposizione almeno due diverse politiche cittadine.**
  - **Esito:** ✅ Sì
  - **Commento:** Implementato nel codice. Sono presenti `DefaultPolicy`, `GreenPolicy`, `FossilFuelPolicy` e `AusterityPolicy` (tramite Pattern Strategy).
- **AC 2: Il giocatore deve poter cambiare politica durante la partita.**
  - **Esito:** ✅ Sì
  - **Commento:** Implementato tramite i pulsanti nella `SimulationControlsBar` che delegano al `GameController.changePolicy()`.
- **AC 3: Il cambio modifica i calcoli a partire dal Tick successivo.**
  - **Esito:** ✅ Sì
  - **Commento:** `City.setPolicy()` aggiorna l'`activePolicy`. Al tick successivo, `City.updateState()` esegue `state.resolveTick(activePolicy.getModifiers())` applicando i nuovi modificatori sui delta appena calcolati.

### Story 2: Esclusività delle politiche
- **AC 1: Disattivare la precedente prima di applicare la nuova.**
  - **Esito:** ✅ Sì
  - **Commento:** Il metodo `City.setPolicy(PolicyStrategy)` sovrascrive il riferimento singolo alla politica attiva. È strutturalmente impossibile averne due contemporaneamente.
- **AC 2: Avvisare l'utente dell'avvenuta sostituzione.**
  - **Esito:** ✅ Sì
  - **Commento:** `SimulationControlsBar.setActivePolicy` invoca `metricsPanel.log(...)` stampando un messaggio a video che conferma il cambio.

---

## Epic 2: Urban Management

### Story 1: Visualizzazione della griglia
- **AC 1: Griglia urbana 20x20 all'avvio.**
  - **Esito:** ✅ Sì
  - **Commento:** Le costanti `Grid.WIDTH` e `HEIGHT` sono impostate a 20. `MapGridView` crea una griglia visiva corrispondente.
- **AC 2: Ogni cella inizialmente vuota.**
  - **Esito:** ✅ Sì
  - **Commento:** Il costruttore di `Grid` inizializza la matrice con oggetti `Cell` dove `structure = null`.
- **AC 3: Interfaccia permette di identificare le coordinate.**
  - **Esito:** ✅ Sì
  - **Commento:** Le coordinate sono tracciate logicamente nell'iterazione della UI (click e drag su `fx, fy`). Non ci sono etichette testuali esplicite (es. "A1", "B2") fisse sulla mappa, ma il sistema sa esattamente su quale coordinata l'utente interagisce ed eventuali log di costruzione riportano le coordinate `(x,y)`.

### Story 2: Posizionamento edifici
- **AC 1: Selezionare da un menu il tipo di edificio desiderato.**
  - **Esito:** ✅ Sì
  - **Commento:** Supportato tramite la `BuildToolbar` che espone i vari tipi (Residential, Industrial, Commercial, etc.).
- **AC 2: Costo di costruzione detratto dal budget.**
  - **Esito:** ✅ Sì
  - **Commento:** `GameController.placeBuilding` sottrae `building.getConstructionCost()` dal budget istantaneamente.
- **AC 3: Impedire il posizionamento se budget insufficiente.**
  - **Esito:** ✅ Sì
  - **Commento:** `GameController.placeBuilding` restituisce `false` (bloccando l'azione) se `budget < cost`.
- **AC 4: Impossibile costruire su cella occupata.**
  - **Esito:** ✅ Sì
  - **Commento:** Viene fatto un controllo esplicito `!cell.isEmpty()` prima del piazzamento in `GameController`.

### Story 3: Posizionamento strade
- **AC 1: Posizionare "Road" ad un certo costo scalato dal budget.**
  - **Esito:** ✅ Sì
  - **Commento:** `Road` ha un costo (100) e si piazza tramite il drag and drop dedicato o il click singolo in `MapGridView`.
- **AC 2: Edificio residenziale richiede strada adiacente.**
  - **Esito:** ✅ Sì
  - **Commento:** In `GameController.placeBuilding`, c'è un blocco `if` specifico per impedire la costruzione di `RESIDENTIAL` senza `GridQueries.hasAdjacentRoad`.
- **AC 3: Se strada demolita, gli edifici isolati smettono di generare tasse.**
  - **Esito:** ⚠️ Parzialmente
  - **Commento:** Il codice di `ResidentialBuilding` genera *sempre* +2 di tasse fintanto che c'è corrente. Il concetto di isolamento blocca la generazione di tasse per `CommercialBuilding` e `IndustrialBuilding` (grazie a `isRevenueBuilding(s)` in `City`), ma gli edifici residenziali continuano a pagare le tasse anche senza strada (sebbene non possano essere costruiti originariamente senza).

### Story 4: Demolizione strutture
- **AC 1: Selezionare strumento demolizione e cliccare su cella.**
  - **Esito:** ✅ Sì
  - **Commento:** Lo strumento `DEMOLISH` è presente in toolbar e gestito in `MapGridView`.
- **AC 2: Libera la cella e restituisce il 50% del costo netto.**
  - **Esito:** ✅ Sì
  - **Commento:** `GameController.demolish` calcola un costo del 10% e un rimborso del 60%, per un netto esatto del 50%.

### Story 5: Potenziamento edifici (Upgrade)
- **AC 1: Upgrade tramite Decorator, fondi scalati, statistiche migliorano.**
  - **Esito:** ✅ Sì
  - **Commento:** `GameController.upgradeBuilding` istanzia `SeismicUpgrade` o `WasteThermalUpgrade` (che estendono `StructureDecorator`), decurtando i fondi.
- **AC 2: Potenziamento Antisismico dimezza danni da terremoto.**
  - **Esito:** ✅ Sì
  - **Commento:** `SeismicUpgrade.takeDamage()` invoca `Math.max(1, amount / 2)`. Il dispatch virtuale fa sì che `onEarthquake()` utilizzi questa logica.

### Story 6: Gestione salute e Ospedali
- **AC 1: La Salute diminuisce ad ogni tick se non ci sono ospedali.**
  - **Esito:** ⚠️ Parzialmente
  - **Commento:** La metrica `health` della città non ha un calo automatico prefissato per la semplice *assenza* di ospedali. L'assenza di ospedali abbassa però la `healthSatisfaction` in `PopulationGroup`, il che a sua volta riduce la crescita della popolazione e applica malus alla Felicità. (La salute cala principalmente per l'inquinamento).
- **AC 2: L'Ospedale applica un modificatore positivo alla Salute.**
  - **Esito:** ✅ Sì
  - **Commento:** `Hospital.applyEffects` applica `+10.0` alla metrica `Health` ogni tick, che contrasta le malattie (inquinamento) e favorisce la demografia.

### Story 7: Gestione energetica
- **AC 1: Posizionare Centrale Elettrica deduce fondi e occupa cella.**
  - **Esito:** ✅ Sì
  - **Commento:** Utilizza il normale flusso di `GameController.placeBuilding`.
- **AC 2: Le celle nel raggio risultano "Alimentate".**
  - **Esito:** ✅ Sì
  - **Commento:** `GridQueries.isPoweredAt` controlla la presenza di una `PowerPlant` entro una distanza di Chebyshev pari a 5.
- **AC 3: Edificio non alimentato non contribuisce a entrate/popolazione.**
  - **Esito:** ✅ Sì
  - **Commento:** Gli edifici che richiedono corrente (Res, Com, Ind, Hosp, Waste) fanno un return immediato in `applyEffects` se `!powered`. `PopulationManager` blocca la crescita demografica se non c'è corrente.
- **AC 4: La UI segnala visivamente celle non alimentate.**
  - **Esito:** ✅ Sì
  - **Commento:** `MapGridView` renderizza un'icona di warning (triangolo giallo) e un bordo rosso `powerWarning`.

### Story 8: Parchi e impatto ambientale/sociale
- **AC 1: Posizionamento Parco.**
  - **Esito:** ✅ Sì
  - **Commento:** Flusso di posizionamento standard supportato.
- **AC 2: Incremento fisso Felicità globale.**
  - **Esito:** ✅ Sì
  - **Commento:** `Park.applyEffects()` fornisce `+1.5` Felicità globale ogni tick.
- **AC 3: Residenziale adiacente riceve bonus aggiuntivo Felicità.**
  - **Esito:** ✅ Sì
  - **Commento:** `City.applyParkEffects()` scansiona un raggio di 3 celle intorno al parco e aggiunge `+2.0` Felicità per ogni edificio residenziale trovato.
- **AC 4: Riduzione Inquinamento nel raggio del Parco.**
  - **Esito:** ⚠️ Parzialmente
  - **Commento:** Nel codice (`City.applyParkEffects`), la riduzione di inquinamento prodotta dal Parco (`-PARK_POLLUTION_REDUCTION`) viene applicata all'inquinamento **globale** della città (`state.updatePollution`), e non calcolata cella per cella all'interno di uno specifico raggio. L'obiettivo generale di riduzione dell'inquinamento è comunque soddisfatto.

### Story 9: Rifiuti e Centri di Raccolta
- **AC 1: Residenziale incrementa Rifiuti ogni tick.**
  - **Esito:** ✅ Sì
  - **Commento:** `ResidentialBuilding.applyEffects` incrementa il waste di `+1.0`.
- **AC 2: Rifiuti sopra soglia causano inquinamento extra.**
  - **Esito:** ✅ Sì
  - **Commento:** `CityState.resolveTick` controlla se `wasteLevel > 50.0` e in caso affermativo incrementa l'inquinamento proporzionalmente all'eccesso.
- **AC 3: Centro Raccolta riduce Rifiuti.**
  - **Esito:** ✅ Sì
  - **Commento:** `WasteManagementCenter.applyEffects` applica una riduzione di `-10.0` al `wasteLevel`.
- **AC 4: Termovalorizzatore azzera rifiuti e genera energia extra.**
  - **Esito:** ⚠️ Parzialmente
  - **Commento:** L'upgrade `WasteThermalUpgrade` applica una drastica riduzione extra (`-15.0`) e genera entrate finanziarie (`+50.0 Budget`), ma non "azzera" forzatamente la variabile a zero indipendentemente dalla mole, né produce energia (Power) utilizzabile dalla rete elettrica. Soddisfa lo spirito della story, ma meccanicamente agisce sul Budget.

---

## Epic 3: Simulation Engine

### Story 1: Avanzamento temporale (Tick)
- **AC 1: Bottone per avanzare calcolo di stato.**
  - **Esito:** ✅ Sì
  - **Commento:** Presenti i bottoni "Start/Stop" (tick automatico) e "Tick" (manuale) in `SimulationControlsBar`. Supportata anche la Barra Spaziatrice.
- **AC 2: Entrate commerciali/industriali sommate al budget.**
  - **Esito:** ✅ Sì
  - **Commento:** Il sistema di accumulo Delta di `CityState` viene risolto a fine tick committando il valore nel budget reale.
- **AC 3: Incremento inquinamento basato su fabbriche.**
  - **Esito:** ✅ Sì
  - **Commento:** Ogni `IndustrialBuilding` immette `+2.5` nel pool inquinamento.
- **AC 4: Popolazione ferma senza centrali elettriche.**
  - **Esito:** ✅ Sì
  - **Commento:** `PopulationManager` usa la flag `hasPowerNearby` per bloccare il calcolo demografico positivo se non c'è corrente.

### Story 2: Monitoraggio metriche globali
- **AC 1: Punteggio Felicità calcolato in base alla prossimità dei parchi.**
  - **Esito:** ✅ Sì
  - **Commento:** Logica presente in `City.applyParkEffects`.
- **AC 2: Inquinamento totale calcolato dalle fabbriche.**
  - **Esito:** ✅ Sì
  - **Commento:** L'inquinamento è un parametro globale gestito da `CityState` che scala ogni tick.
- **AC 3: Inquinamento sopra soglia riduce Felicità.**
  - **Esito:** ✅ Sì
  - **Commento:** `CityState.resolveTick` scala la Felicità proporzionalmente all'inquinamento in eccesso sopra il valore di `30.0`.

### Story 3: Terremoti e Disastri Casuali
- **AC 1: Probabilità di generazione configurabile.**
  - **Esito:** ✅ Sì
  - **Commento:** `DisasterManager.EARTHQUAKE_PROBABILITY` controlla la frequenza del sisma.
- **AC 2: Notifica a tutti gli edifici per calcolo danni.**
  - **Esito:** ✅ Sì
  - **Commento:** Il Pattern Observer su `DisasterObserver` fa invocare `onEarthquake` su tutti gli edifici.
- **AC 3: Edifici con scarsa resistenza vengono distrutti/danneggiati.**
  - **Esito:** ✅ Sì
  - **Commento:** Il calcolo scala quadraticamente sulla magnitudo. Edifici con HP che scendono a `<= 0` vengono distrutti e rimossi dalla griglia.
- **AC 4: Avviso UI sulla dashboard.**
  - **Esito:** ✅ Sì
  - **Commento:** `DashboardView.showEarthquakeWarning()` genera un grande avviso a schermo e il log mostra i dettagli nel `MetricsPanel`.

### Story 4: Deterioramento degli edifici
- **AC 1: Perdita quantità fissa HP a ogni tick.**
  - **Esito:** ✅ Sì
  - **Commento:** `Structure.decayTick()` sottrae 1 HP ad ogni avanzamento temporale.
- **AC 2: Avviso UI quando HP < 20%.**
  - **Esito:** ✅ Sì
  - **Commento:** Il `CityState` conta gli edifici critici e la `DashboardView` inserisce un log giallo nel `MetricsPanel`.
- **AC 3: Azione ripara ripristina 100% scalando fondi.**
  - **Esito:** ✅ Sì
  - **Commento:** `GameController.repair()` calcola il costo e invoca `fullRepair()` (riporta HP a `maxHp`).

### Story 5: Dinamiche Demografiche
- **AC 1: Felicità alta = popolazione aumenta.**
  - **Esito:** ✅ Sì
  - **Commento:** `PopulationManager` usa un complesso bilanciamento di indici. Una soddisfazione alta si traduce in `deltaPop` > 0.
- **AC 2: Felicità bassa / No servizi = abbandono città.**
  - **Esito:** ✅ Sì
  - **Commento:** Se le condizioni sono critiche (soddisfazione sotto la soglia), il delta demografico diviene negativo (fino a -15 per tick).

---

## Epic 4: Infrastructure and persistence

### Story 1: Salvataggio e Caricamento
- **AC 1: Salvataggio stato città in file JSON.**
  - **Esito:** ✅ Sì
  - **Commento:** `SaveLoadManager` utilizza Jackson ObjectMapper per scrivere su `saves/`.
- **AC 2: Caricamento ripristina oggetti su griglia.**
  - **Esito:** ✅ Sì
  - **Commento:** `SaveDataApplier` pulisce la mappa e ricostruisce la città blocco per blocco (tramite `BuildingFactory`), reinserendo i Decorator.
- **AC 3: Gestione errori (corruzione/file mancante).**
  - **Esito:** ✅ Sì
  - **Commento:** IOException intercettate. La UI in `DashboardView` e `SimulationControlsBar` mostra finestre di errore tramite `DialogHelper.showError()`.
- **AC 4: Salvataggio include numero di tick.**
  - **Esito:** ✅ Sì
  - **Commento:** La classe DTO `SaveData` possiede la variabile `public int tick;`.
- **AC 5: Autosave ogni X Tick.**
  - **Esito:** ✅ Sì
  - **Commento:** Implementato in `SimulationControlsBar.doTick()`, ogni 5 tick viene richiamato `controller.autosave()`. È presente anche un toggle UI.

### Story 2: Dashboard real-time (MVC)
- **AC 1: Dashboard separata dalla logica interna (MVC).**
  - **Esito:** ✅ Sì
  - **Commento:** Architettura eccellente. `DashboardView` e `SimulationController` non contengono logica di gioco, gestiscono unicamente i comandi verso `GameController`.
- **AC 2: Grafici in tempo reale (Budget e Popolazione).**
  - **Esito:** ⚠️ Parzialmente
  - **Commento:** Il grafico in `DashboardChart` disegna la Popolazione e vari indicatori di Felicità/Salute/Inquinamento e Soddisfazione. Tuttavia, l'andamento storico del **Budget** non viene tracciato come linea sul grafico (è visibile in tempo reale solo come numero testuale sul pannello laterale).
- **AC 3: Aggiornamento automatico ogni Tick.**
  - **Esito:** ✅ Sì
  - **Commento:** `City` estende il pattern Observer notificando `DashboardView` che a sua volta aggiorna `MapGridView`, `MetricsPanel` e `DashboardChart`.
- **AC 4: UI chiaramente distinta dai dati di simulazione.**
  - **Esito:** ✅ Sì
  - **Commento:** Separazione di package chiara (`it.citylife.model` e `it.citylife.ui`).