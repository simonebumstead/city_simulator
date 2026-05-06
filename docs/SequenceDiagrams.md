## **advanceTick()**
```mermaid
sequenceDiagram
    participant UI as :DashboardUI
    participant C as :GameController
    participant City as :City
    participant G as :Grid
    participant B as :Building
    participant P as activePolicy:PolicyStrategy
    participant S as :CityState

    UI->>C: advanceTick()
    activate C
    Note right of UI: GRASP: Controller
    
    C->>City: updateState()
    activate City
    Note right of C: GRASP: Information Expert
    
    City->>G: buildings = getActiveBuildings()
    activate G
    G-->>City: List~Building~
    deactivate G
    
    loop Per ogni edificio (b in buildings)
        City->>B: applyBaseEffect(S)
        activate B
        Note right of City: GRASP: Polymorphism
        
        B->>S: updateMetrics(budgetDelta, pollutionDelta)
        activate S
        S-->>B: ack
        deactivate S
        
        B-->>City: ack
        deactivate B
    end

    Note right of City: GoF: Strategy Pattern
    City->>P: applyPolicyModifier(S)
    activate P
    
    P->>S: modifyMetrics(taxRate, pollutionModifier)
    activate S
    S-->>P: ack
    deactivate S
    
    P-->>City: ack
    deactivate P

    City-->>C: tickCompleted
    deactivate City
    
    C-->>UI: refreshData(S)
    deactivate C
```
### GRASP - Controller: Il messaggio iniziale advanceTick() parte dalla DashboardUI (livello di presentazione) e viene ricevuto dal GameController. Il Controller agisce come intermediario, disaccoppiando l'interfaccia grafica dalla logica di dominio (Low Coupling). Non esegue i calcoli, ma delega l'operazione.

### GRASP - Information Expert: Il GameController delega l'aggiornamento alla classe City invocando updateState(). La classe City è l'Esperto dell'Informazione per questa operazione, poiché possiede i riferimenti alla Grid (dove si trovano gli edifici), allo stato globale CityState e all'ordinanza attiva PolicyStrategy.

### GRASP - Polymorphism: Nel ciclo loop, la classe City invoca applyBaseEffect(S) sull'interfaccia/classe astratta Building. Il sistema usa il polimorfismo per calcolare gli effetti: la classe City non ha bisogno di sapere se sta parlando con una centrale elettrica o una zona residenziale (non ci sono costrutti switch o if-else). Ogni specifica sottoclasse di Building sa come aggiornare le proprie metriche.

### GoF - Strategy Pattern: Dopo aver calcolato gli effetti base di tutti gli edifici, la City delega il calcolo delle tasse o delle riduzioni dell'inquinamento all'oggetto activePolicy(istanza dell'interfaccia PolicyStrategy). La City è il Context del pattern:non conosce l'algoritmo specifico della politica attiva (es. Tassa Ambientale vs Espansione Industriale), ma invoca semplicemente applyPolicyModifier(S). Questo garantisce un design aperto all'estensione ma chiuso alla modifica (Open-Closed Principle).

## **placeBuilding("PowerPlant", x, y)**
```mermaid
sequenceDiagram
    participant UI as :DashboardUI
    participant C as :GameController
    participant F as :BuildingFactory
    participant City as :City
    participant B as b:Structure
    participant G as :Grid
    participant S as :CityState

    UI->>C: placeBuilding("PowerPlant", x, y)
    activate C
    Note right of UI: GRASP: Controller
    
    C->>F: createBuilding("PowerPlant")
    activate F
    Note right of C: GoF: Factory Pattern
    F-->>C: b (PowerPlant instanziato)
    deactivate F
    
    C->>City: requestPlacement(b, x, y)
    activate City
    Note right of City: GRASP: Information Expert
    
    City->>B: cost = getConstructionCost()
    activate B
    B-->>City: cost
    deactivate B
    
    City->>G: cell = getCell(x, y)
    activate G
    G-->>City: cell
    deactivate G
    
    alt Cella vuota E Budget >= cost
        City->>S: deductBudget(cost)
        activate S
        S-->>City: ack
        deactivate S
        
        City->>G: placeStructure(b, x, y)
        activate G
        G-->>City: ack
        deactivate G
        
        Note right of City: GoF: Observer Pattern
        City->>City: notifyObservers()
        City->>UI: updateView(S)
        
        City-->>C: success
    else Spazio occupato o Fondi insufficienti
        City-->>C: error("Impossibile costruire")
        C-->>UI: showErrorMsg("Fondi o spazio insufficienti")
    end
    
    deactivate City
    C-->>UI: ack
    deactivate C
```

### GRASP - Controller: Il GameController intercetta l'input dell'utente dalla DashboardUI. Non calcola nulla, ma coordina il lavoro delle altre classi (Low Coupling).

### GoF - Factory Pattern: Il GameController usa la BuildingFactory per creare l'oggetto b. In questo modo, né il Controller né la Città devono usare la parola chiave new PowerPlant(). Il sistema dipenderà dall'astrazione (Structure), rispettando il principio di Inversione delle Dipendenze.

### GRASP - Information Expert: Il GameController passa l'oggetto appena creato alla City. La City è l'esperto dell'informazione incaricato di validare la richiesta, poiché è l'unica classe che ha accesso sia allo stato finanziario (CityState) sia alla mappa topografica (Grid).

### Polimorfismo: Quando la City interroga l'oggetto b chiamando getConstructionCost(), non sa (e non le interessa sapere) che si tratta di una centrale elettrica. Usa l'interfaccia generale per ottenere il costo.

### GoF - Observer Pattern: Nel ramo di successo dell' alt, dopo aver aggiornato i dati, la City chiama notifyObservers(). La UI, che è in ascolto, viene notificata e aggiorna i contatori a schermo, mantenendo la separazione netta tra Modello (dati) e Vista (grafica).


## Save()
```mermaid
sequenceDiagram
    participant UI as :ConsoleView
    participant C as :GameController
    participant IO as :SaveLoadManager
    participant City as :City

    UI->>C: saveGame("salvataggio1.json")
    activate C
    Note right of UI: GRASP: Controller
    
    C->>IO: save(City, "salvataggio1.json")
    activate IO
    Note right of C: GRASP: Pure Fabrication<br/>(Alta Coesione)
    
    IO->>City: getStateData()
    activate City
    Note right of City: GRASP: Information Expert
    City-->>IO: dataToSerialize
    deactivate City
    
    Note over IO: Scrittura dati su File System
    
    IO-->>C: success
    deactivate IO
    
    C-->>UI: showMessage("Partita salvata con successo")
    deactivate C
```