# Epic 1: Governance and policies

## Story 1: Cambio di politica cittadina
**Come giocatore, voglio poter attivare una politica cittadina specifica, in modo da poter guidare lo sviluppo strategico della mia città.**

**Acceptance Criteria:**
- Il sistema deve permettere all'utente di avere a disposizione almeno due diverse politiche cittadine (ad esempio, Tassa Ambientale o Espansione Industriale o Tassazione Estrema).
- Il giocatore deve poter cambiare la politica attiva durante la partita, passando facilmente da una "Fossil Fuel Policy" a una "Green Policy" o una “Austerity Policy“.
- Il cambio di strategia deve modificare i calcoli effettuati dal motore di simulazione a partire dal "Tick" successivo.

**Classi collegate:**
- `PolicyStrategy`, `DefaultPolicy`, `GreenPolicy`, `FossilFuelPolicy`, `AusterityPolicy`
- `PolicyModifiers`
- `CityState`
- `GameController`
- `SimulationControlsBar`

## Story 2: Esclusività delle politiche
**Come giocatore, voglio che il sistema mi impedisca di mantenere attive contemporaneamente politiche tra loro contraddittorie, in modo che la simulazione rimanga coerente.**

**Acceptance Criteria:**
- Se l'utente seleziona una politica mutualmente esclusiva rispetto a quella attualmente attiva (es. Tassa Ambientale vs. Espansione Industriale), il sistema deve disattivare la precedente prima di applicare la nuova.
- Il sistema deve avvisare l'utente dell'avvenuta sostituzione dell'ordinanza attiva.

**Classi collegate:**
- `City`
- `GameController`
- `SimulationControlsBar`
- `MetricsPanel` (per il log della notifica)

---

# Epic 2: Urban Management

## Story 1: Visualizzazione della griglia
**Come Sindaco, voglio visualizzare una griglia urbana 20x20 per poter pianificare la costruzione degli edifici.**

**Acceptance Criteria:**
- All'avvio, il sistema mostra una griglia composta da 20x20 celle.
- Ogni cella della griglia deve essere inizialmente vuota.
- L'interfaccia deve permettere di identificare le coordinate di ogni cella.

**Classi collegate:**
- `Grid`, `Cell`
- `MapGridView`

## Story 2: Posizionamento edifici
**Come Sindaco, voglio poter scegliere e posizionare diversi tipi di edifici (Residenziali, Industriali, Commerciali) sulla griglia.**

**Acceptance Criteria:**
- L'utente può selezionare da un menu il tipo di edificio desiderato tra residenziale, industriale o commerciale.
- Ogni edificio ha un costo di costruzione specifico che viene detratto dal budget totale della città.
- Il sistema deve impedire il posizionamento di un edificio se il budget residuo è insufficiente.
- Non è possibile costruire sopra una cella della griglia che risulta già occupata da un altro oggetto.

**Classi collegate:**
- `GameController`, `SimulationController`
- `Structure`, `StructureType`, `BuildingFactory`
- `ResidentialBuilding`, `IndustrialBuilding`, `CommercialBuilding`
- `BuildToolbar`, `MapGridView`

## Story 3: Posizionamento strade
**Come Sindaco, voglio poter posizionare delle strade per connettere gli edifici, così da permettere il loro corretto funzionamento.**

**Acceptance Criteria:**
- L'utente può posizionare celle di tipo "Road" sulla griglia ad un certo costo che deve essere detratto dal budget generale.
- Un edificio residenziale può essere costruito solo se adiacente (orizzontalmente o verticalmente) a una strada.
- Se una strada viene demolita, gli edifici che rimangono isolati smettono di generare tasse al tick successivo.

**Classi collegate:**
- `Road`
- `GameController`
- `GridQueries`
- `City`

## Story 4: Demolizione strutture
**Come Sindaco, voglio poter demolire edifici o strade esistenti per riorganizzare lo spazio urbano.**

**Acceptance Criteria:**
- L'utente può selezionare lo strumento demolizione e cliccare su una cella occupata.
- La demolizione libera la cella e restituisce il 50% del costo originale dell'edificio al budget generale.

**Classi collegate:**
- `GameController` (metodo `demolish`)
- `MapGridView`
- `BuildToolbar`

## Story 5: Potenziamento edifici (Upgrade)
**Come Sindaco, voglio poter spendere fondi per potenziare gli edifici esistenti (es. capienza residenziale, output energetico o resistenza sismica), affinché io possa ottimizzare lo spazio sulla griglia senza dover costruire strutture da zero.**

**Acceptance Criteria:**
- Dato un edificio base e fondi sufficienti, Quando invio il comando di potenziamento, Allora l'edificio cambia stato (tramite Decorator Pattern), i fondi vengono scalati e le sue statistiche migliorano.
- Dato un edificio con potenziamento "Antisismico", Quando si verifica l'evento "Terremoto", Allora i danni calcolati (HP persi) per quell'edificio vengono ridotti del 50% rispetto a un edificio normale.

**Classi collegate:**
- `StructureDecorator`
- `SeismicUpgrade`, `WasteThermalUpgrade`
- `GameController`
- `BuildingFactory`

## Story 6: Gestione salute e Ospedali
**Come Sindaco, voglio poter costruire un Ospedale per gestire il nuovo parametro "Salute", affinché la mortalità o l'esodo cittadino siano tenuti sotto controllo.**

**Acceptance Criteria:**
- Dato il parametro Salute nel CityState, Quando non ci sono ospedali attivi, Allora la Salute diminuisce ad ogni tick.
- Dato un Ospedale appena costruito sulla griglia, Quando il tick avanza, Allora l'Ospedale applica un modificatore positivo al parametro Salute, prevenendo il calo della Popolazione.

**Classi collegate:**
- `Hospital`
- `CityState`
- `PopulationManager`

## Story 7: Gestione energetica
**Come Sindaco, voglio poter costruire Centrali Elettriche e gestire la copertura energetica della rete, affinché il posizionamento delle infrastrutture richieda vera pianificazione strategica.**

**Acceptance Criteria:**
- Dato che l'utente seleziona la Centrale Elettrica dal menu, Quando la posiziona su una cella libera con budget sufficiente, Allora la cella risulta occupata e il costo viene detratto dal CityState.
- Dato una Centrale posizionata sulla griglia, Quando il tick avanza, Allora tutte le celle entro il raggio configurabile risultano "Alimentate".
- Dato un edificio fuori dal raggio di copertura di qualsiasi Centrale, Quando il tick avanza, Allora l'edificio entra in stato "Non Alimentato" e non contribuisce ai calcoli di entrate né di crescita della popolazione.
- Dato il CityState aggiornato, Quando la dashboard si aggiorna dopo il tick, Allora le celle non alimentate vengono segnalate visivamente con un indicatore di allerta.

**Classi collegate:**
- `PowerPlant`
- `PowerNetwork`
- `GridQueries`
- `MapGridView`

## Story 8: Parchi e impatto ambientale/sociale
**Come Sindaco, voglio poter posizionare Parchi sulla griglia, così da aumentare la Felicità dei cittadini e contrastare gli effetti dell'inquinamento.**

**Acceptance Criteria:**
- Dato che l'utente seleziona il Parco dal menu, Quando lo posiziona su una cella libera con budget sufficiente, Allora la cella risulta occupata e il costo viene detratto dal CityState.
- Dato un Parco attivo sulla griglia, Quando avanza il tick, Allora il parametro Felicità nel CityState viene incrementato di un valore fisso configurabile.
- Dato una zona Residenziale adiacente a un Parco, Quando avanza il tick, Allora quella zona riceve un bonus di Felicità aggiuntivo rispetto alle zone non adiacenti.
- Dato un Parco attivo sulla griglia, Quando avanza il tick, Allora il parametro Inquinamento delle celle nel raggio del parco viene ridotto di un valore fisso.

**Classi collegate:**
- `Park`
- `City` (metodo `applyParkEffects`)
- `CityState`

## Story 9: Rifiuti e Centri di Raccolta
**Come Sindaco, voglio gestire la produzione di rifiuti tramite Centri di Raccolta e potenziamenti, così da evitare picchi di inquinamento e convertire gli scarti in energia.**

**Acceptance Criteria:**
- Dato il CityState, Quando avanza il tick, Allora ogni zona Residenziale attiva incrementa il parametro Rifiuti di una quantità fissa configurabile.
- Dato che il parametro Rifiuti supera la soglia configurabile, Quando avanza il tick, Allora l'Inquinamento globale subisce un incremento aggiuntivo significativo.
- Dato un Centro di Raccolta attivo sulla griglia, Quando avanza il tick, Allora il parametro Rifiuti viene ridotto di una quantità fissa.
- Dato un Centro di Raccolta con l'upgrade "Termovalorizzatore", Quando avanza il tick, Allora i Rifiuti vengono azzerati e vengono generati automaticamente bonus energetici extra nel CityState.

**Classi collegate:**
- `WasteManagementCenter`
- `WasteThermalUpgrade`
- `ResidentialBuilding`
- `CityState`

---

# Epic 3: Simulation Engine

## Story 1: Avanzamento temporale (Tick)
**Come Sindaco, voglio poter far avanzare il tempo nella simulazione tramite un 'Tick', così da vedere gli effetti delle mie scelte su popolazione, budget, felicità, salute e inquinamento.**

**Acceptance Criteria:**
- Deve essere presente un comando (es. un bottone "Prossimo Mese") che attiva il calcolo del nuovo stato della città.
- A ogni "Tick", il sistema deve sommare le entrate degli edifici commerciali/industriali al budget totale.
- A ogni "Tick", il sistema deve calcolare l'incremento dell'inquinamento globale basandosi sulle fabbriche presenti.
- Se non sono presenti centrali elettriche, gli edifici residenziali non devono generare crescita della popolazione.

**Classi collegate:**
- `City`, `CityState`
- `GameController`
- `SimulationControlsBar`

## Story 2: Monitoraggio metriche globali
**Come Sindaco, voglio che il sistema calcoli automaticamente Felicità, Inquinamento e salute in modo da monitorare lo stato di salute della città.**

**Acceptance Criteria:**
- Il sistema deve calcolare un punteggio di Felicità basato sulla vicinanza tra case e parchi.
- Il sistema deve calcolare l'Inquinamento totale basato sul numero di fabbriche attive.
- Se l'Inquinamento supera una certa soglia, la Felicità dei cittadini deve diminuire proporzionalmente.

**Classi collegate:**
- `CityState`
- `City`
- `PopulationGroup`

## Story 3: Terremoti e Disastri Casuali
**Come Utente, voglio che il sistema possa generare terremoti casuali per testare la resistenza strutturale degli edifici e la capacità di gestione dell'emergenza.**

**Acceptance Criteria:**
- Il sistema deve avere una probabilità configurabile di generare un evento Terremoto durante il passaggio di un Tick.
- All'attivazione del terremoto, il sistema deve notificare tutti gli edifici presenti sulla griglia affinché calcolino i danni subiti.
- Gli edifici con poca resistenza o con manutenzione assente devono poter passare allo stato Danneggiato o Distrutto a seguito dell'evento.
- L'utente deve visualizzare un avviso immediato sulla dashboard quando si verifica un sisma, con un riepilogo dei dettagli del terremoto e dei danni totali causati alla città.

**Classi collegate:**
- `DisasterManager`, `DisasterObserver`
- `Structure`
- `DashboardView`
- `City`

## Story 4: Deterioramento degli edifici
**Come Sindaco, voglio che gli edifici si deteriorino nel tempo e mi notifichino quando necessitano di manutenzione, affinché il gioco offra una sfida gestionale a lungo termine.**

**Acceptance Criteria:**
- Dato un edificio costruito sulla mappa, Quando il gioco avanza di un tick, Allora l'edificio perde una quantità fissa di HP (Punti Vita).
- Dato un edificio, Quando i suoi HP scendono sotto il 20%, Allora il sistema di log/UI mostra un avviso testuale: "Attenzione: [Nome Edificio] necessita di manutenzione".
- Dato un edificio danneggiato, Quando il giocatore seleziona l'azione "Ripara", Allora vengono scalati i fondi dal budget e gli HP tornano al 100%.

**Classi collegate:**
- `Structure`
- `GameController`
- `MetricsPanel`

## Story 5: Dinamiche Demografiche
**Come Sindaco, voglio che il numero di abitanti vari in base al livello di felicità e ai servizi disponibili, affinché le mie decisioni urbanistiche abbiano un impatto diretto sulla demografia.**

**Acceptance Criteria:**
- Dato un livello di Felicità > 70% e spazio residenziale disponibile, Quando avanza il tick, Allora la popolazione totale aumenta.
- Dato un livello di Felicità < 30% o l'assenza di servizi base, Quando avanza il tick, Allora una percentuale di popolazione abbandona la città e la Felicità subisce un ulteriore malus.

**Classi collegate:**
- `PopulationManager`
- `CityState`
- `PopulationGroup`

---

# Epic 4: Infrastructure and persistence

## Story 1: Salvataggio e Caricamento
**Come Utente, voglio salvare lo stato della mia città in un file locale per poter riprendere la simulazione in un secondo momento.**

**Acceptance Criteria:**
- Il sistema deve permettere di salvare la configurazione attuale della griglia e le statistiche globali in un file JSON.
- La funzione di caricamento deve ripristinare correttamente tutti gli oggetti sulla griglia partendo dal file salvato.
- In caso di file di salvataggio corrotto o mancante, il sistema deve gestire l'errore senza interrompersi bruscamente.
- Il salvataggio deve includere lo stato di avanzamento temporale corrente (numero di tick).
- (Autosave) Come giocatore, voglio che il sistema esegua un salvataggio automatico ogni "X" Tick della simulazione, in modo da avere un backup recente nel caso in cui dimentichi di salvare manualmente.

**Classi collegate:**
- `SaveLoadManager`
- `SaveDataMapper`, `SaveDataApplier`
- `GameController`
- `SimulationControlsBar`

## Story 2: Dashboard real-time (MVC)
**Come Utente, voglio visualizzare grafici e statistiche in tempo reale sulla crescita della mia città.**

**Acceptance Criteria:**
- Obiettivo: Implementare una dashboard visiva separata dalla logica interna (Pattern MVC).
- La dashboard deve mostrare grafici in tempo reale sull'andamento del budget e della popolazione.
- L'interfaccia deve aggiornarsi automaticamente ogni volta che avviene un "Tick" (Pattern Observer).
- La UI deve essere chiaramente distinta dalle classi che gestiscono i dati della simulazione.

**Classi collegate:**
- `StateObserver`
- `DashboardView`
- `DashboardChart`
- `MetricsPanel`
- `SimulationController`