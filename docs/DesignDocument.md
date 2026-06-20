# Documento di Design — CityLogic City Simulator

**Versione:** 2.0 (Aggiornata con i nuovi diagrammi dettagliati)

---

## 1. Domain Model

Il dominio di CityLogic ruota attorno alla classe `City`, che orchestra tutti i sottosistemi della simulazione. La `City` possiede una `Grid` 20x20 di celle (`Cell`), ognuna delle quali può contenere al massimo una struttura (`Structure`). Lo stato globale della città è raccolto in `CityState`.

```mermaid
classDiagram
    direction TB

    %% --- DOMAIN ENTITIES ---
    class City {
        +advanceTick()
    }

    class CityState {
        -budget: double
        -population: int
        -happiness: double
        -pollution: double
        -health: double
        -wasteLevel: int
    }

    class Grid {
        -width: int
        -height: int
    }

    class Cell {
        -x: int
        -y: int
    }

    class Structure {
        <<abstract>>
        -hp: int
        -constructionCost: int
        -powered: boolean
        -connectedToRoad: boolean
        +applyEffects()
    }

    class ResidentialBuilding {
        -capacity: int
        -residents: int
    }

    class CommercialBuilding
    class IndustrialBuilding
    class PowerPlant
    class Park
    class Hospital
    class WasteManagementCenter
    class Road

    class PowerNetwork {
        -totalGenerated: int
        -totalConsumed: int
    }

    class PolicyStrategy {
        <<interface>>
        +getModifiers()
    }

    class DisasterManager {
        +triggerEarthquake()
    }

    %% --- LOGICAL RELATIONSHIPS ---
    City "1" *-- "1" Grid : contains
    City "1" *-- "1" CityState : tracks
    City "1" *-- "1" PowerNetwork : manages
    City "1" *-- "1" DisasterManager : handles events
    
    City "1" o-- "1" PolicyStrategy : applies policy
    
    Grid "1" *-- "400" Cell : composed of
    
    Cell "1" o-- "0..1" Structure : holds
    
    Structure <|-- ResidentialBuilding
    Structure <|-- CommercialBuilding
    Structure <|-- IndustrialBuilding
    Structure <|-- PowerPlant
    Structure <|-- Park
    Structure <|-- Hospital
    Structure <|-- WasteManagementCenter
    Structure <|-- Road

    Structure ..> PowerNetwork : consumes / produces
    Structure ..> CityState : alters
```

---

## 2. System Sequence Diagrams

I diagrammi seguenti mostra le interazioni tra l'attore esterno (Player) e il Sistema senza classi interne.

```mermaid
sequenceDiagram
    actor Player
    participant System as :System

    Note over Player, System: 1. Costruzione, Demolizione e Potenziamento

    opt Costruisci
        Player->>System: placeBuilding(type, x, y)
        alt Budget sufficiente e posizione valida
            System-->>Player: status: "OK"
            Note right of System: Il sistema crea la struttura (tramite BuildingFactory),<br/>aggiorna budget e mappa.
        else Errore di validazione o budget
            System-->>Player: status: "Error: [reason]"
        end
    end
    
    opt Demolisci
        Player->>System: demolish(x, y)
        alt Cella occupata e demolibile
            System-->>Player: status: "OK"
            Note right of System: Rimuove la struttura e<br/>rimborsa una percentuale dei costi.
        else Cella non valida
            System-->>Player: status: "Error: Impossibile demolire"
        end
    end
    
    opt Potenzia
        Player->>System: upgradeBuilding(x, y, upgradeType)
        alt Requisiti soddisfatti
            System-->>Player: status: "OK"
            Note right of System: Applica l'upgrade (es. SeismicUpgrade)<br/>alla struttura.
        else Requisiti mancanti (es. Max livello)
            System-->>Player: status: "Error: [reason]"
        end
    end
```

```mermaid
sequenceDiagram
    actor Player
    participant System as :System

    Note over Player, System: 2. Gestione e Avanzamento Simulazione

    opt Avanza Tempo (Tick)
        Player->>System: advanceTick()
        System-->>Player: tickResult (metrics, disasters/events)
        Note right of System: Ricalcola i parametri, applica Policy,<br/>gestisce consumi, produzioni ed eventi (DisasterManager).
    end
    
    opt Imposta Politica
        Player->>System: setPolicy(policyType)
        alt Cooldown passato
            System-->>Player: status: "OK"
        else Policy non applicabile / In cooldown
            System-->>Player: status: "Error: [reason]"
        end
    end
```



```mermaid
sequenceDiagram
    actor Player
    participant System as :System

    Note over Player, System: 3. Persistenza Dati

    opt Salva Partita
        Player->>System: saveGame(fileName)
        alt Scrittura I/O OK
            System-->>Player: status: "OK"
            Note right of System: Usa SaveDataMapper per<br/>serializzare lo stato.
        else Errore I/O
            System-->>Player: status: "Error: [reason]"
        end
    end
    
    opt Carica Partita
        Player->>System: loadGame(fileName)
        alt File valido e integrità OK
            System-->>Player: loadedCityState
            Note right of System: Distrugge stato corrente e ripristina<br/>il salvataggio via SaveDataApplier.
        else File corrotto o assente
            System-->>Player: status: "Error: File non valido"
        end
    end
```

---

## 3. Design Class Model

Il modello di design riflette la struttura tecnica completa, inclusi i layer di controllo. Per rendere più agevole la consultazione e il rendering in Mermaid, questo sistema complesso è stato **suddiviso in tre moduli separati**. I pattern architetturali e di design chiave implementati e visibili in questi diagrammi sono:

- **Facade** *(Modulo 1)* — `SimulationController` è un wrapper leggero attorno a `GameController`; la UI non tocca mai il dominio direttamente.
- **GRASP Controller** *(Modulo 1)* — `GameController` è l'unico punto di ingresso per le operazioni che mutano lo stato (place, demolish, repair, repairAll, upgrade, policy change, save/load).
- **Strategy** *(Modulo 3)* — `PolicyStrategy` con 4 implementazioni; `CityState.resolveTick(PolicyModifiers)` applica i modificatori senza conoscere la policy concreta.
- **Observer** *(Modulo 3)* — `City` notifica gli `StateObserver` (la UI) ad ogni tick; `DisasterManager` notifica i `DisasterObserver` (le strutture) ad ogni terremoto.
- **Decorator** *(Modulo 2)* — `StructureDecorator` avvolge `Structure` per aggiungere comportamento (dimezzamento danni, recupero termico) senza modificare le classi base.
- **Factory** *(Modulo 2)* — `BuildingFactory` centralizza la costruzione di tutte le istanze `Structure` e la riapplicazione degli upgrade al caricamento del salvataggio.
- **Template Method** *(Modulo 2)* — `Structure` definisce lo scheletro del ciclo di vita (decayTick, applyEffects, takeDamage); ogni sottoclasse sovrascrive solo `applyEffects()` e `getType()`.

### 3.1 Architettura Core (Controller e Mondo)

```mermaid
classDiagram
    direction TB

    class GameController {
        -city: City
        -ioManager: SaveLoadManager
        +placeBuilding(type: String, x: int, y: int) boolean
        +demolish(x: int, y: int) boolean
        +repair(x: int, y: int) boolean
        +upgradeBuilding(x: int, y: int, upgradeType: String) boolean
        +changePolicy(policy: PolicyStrategy)
        +advanceTick()
    }
    
    class City {
        -grid: Grid
        -state: CityState
        -powerNet: PowerNetwork
        -activePolicy: PolicyStrategy
        -disasterManager: DisasterManager
        -observers: List~StateObserver~
        +advanceTick()
        +setPolicy(policy: PolicyStrategy)
        +addObserver(obs: StateObserver)
        -updateState()
    }

    class Grid {
        -width: int
        -height: int
        -matrix: Cell[][]
        +getCell(x: int, y: int) Cell
        +placeStructure(s: Structure, x: int, y: int)
        +removeStructure(x: int, y: int)
    }

    class Cell {
        -x: int
        -y: int
        -structure: Placeable
        +getStructure() Placeable
        +setStructure(structure: Placeable)
        +isEmpty() boolean
    }

    class Placeable {
        <<interface>>
    }

    class CityState {
        -budget: double
        -population: int
        -happiness: double
        -pollution: double
        -health: double
        -wasteLevel: int
        +resolveTick(modifiers: PolicyModifiers)
        +updateBudget(amount: double)
    }

    class PowerNetwork {
        -totalGenerated: int
        -totalConsumed: int
        +registerProducer(amount: int)
        +registerConsumer(amount: int)
        +hasEnoughPower() boolean
        +reset()
    }

    class SaveLoadManager {
        -mapper: ObjectMapper
        +saveManual(city: City, tick: int) Path
        +load(city: City, path: Path) int
    }
    
    class DisasterManager {
        -observers: List~DisasterObserver~
        +triggerEarthquake(state: CityState)
    }

    class PopulationManager {
        +updateDemographics()
    }

    class GridQueries {
        <<utility>>
        +hasAdjacentRoad(grid: Grid, x: int, y: int)$ boolean
        +isPoweredAt(grid: Grid, x: int, y: int)$ boolean
    }

    GameController "1" *-- "1" City : owns
    City "1" *-- "1" Grid : owns
    City "1" *-- "1" CityState : owns
    City "1" *-- "1" PowerNetwork : owns
    City "1" *-- "1" DisasterManager : owns
    Grid "1" *-- "400" Cell : composed of
    
    GameController ..> SaveLoadManager : uses
    GameController ..> GridQueries : uses
    City ..> PopulationManager : uses
    City ..> GridQueries : uses
    SaveLoadManager ..> City : reads/writes
    Cell o-- Placeable : holds
```

### 3.2 Strutture e Gerarchia degli Edifici (Template Method & Decorator)

```mermaid
classDiagram
    direction TB

    class Placeable {
        <<interface>>
        +getType() StructureType
    }

    class StructureType {
        <<enumeration>>
        RESIDENTIAL
        INDUSTRIAL
        COMMERCIAL
        POWER_PLANT
        PARK
        ROAD
        HOSPITAL
        WASTE_CENTER
    }

    class Structure {
        <<abstract>>
        #hp: int
        #maxHp: int
        #constructionCost: int
        #powered: boolean
        #connectedToRoad: boolean
        +applyEffects(state: CityState, powerNet: PowerNetwork)*
        +getType() StructureType*
        +takeDamage(damage: int)
        +fullRepair()
    }

    class ResidentialBuilding {
        -capacity: int
        -residents: int
    }
    class IndustrialBuilding {
        -jobsProvided: int
    }
    class CommercialBuilding
    class PowerPlant
    class Park
    class Road
    class Hospital
    class WasteManagementCenter

    class StructureDecorator {
        <<abstract>>
        #wrapped: Structure
    }
    class SeismicUpgrade
    class WasteThermalUpgrade

    class BuildingFactory {
        <<utility>>
        +createBuilding(type: String)$ Structure
    }

    Structure ..|> Placeable
    
    Structure <|-- ResidentialBuilding
    Structure <|-- IndustrialBuilding
    Structure <|-- CommercialBuilding
    Structure <|-- PowerPlant
    Structure <|-- Park
    Structure <|-- Road
    Structure <|-- Hospital
    Structure <|-- WasteManagementCenter
    
    Structure <|-- StructureDecorator
    StructureDecorator "1" o-- "1" Structure : wraps
    StructureDecorator <|-- SeismicUpgrade
    StructureDecorator <|-- WasteThermalUpgrade

    BuildingFactory ..> Structure : instantiates
    Placeable ..> StructureType : returns
    BuildingFactory ..> StructureType : uses
```

### 3.3 Politiche Economiche e Observer Pattern

```mermaid
classDiagram
    direction TB

    class PolicyStrategy {
        <<interface>>
        +getModifiers() PolicyModifiers
    }
    class StateObserver {
        <<interface>>
        +onStateChanged(state: CityState)
    }
    class DisasterObserver {
        <<interface>>
        +onEarthquake(magnitude: int)
    }

    class PolicyModifiers {
        -pollutionMultiplier: double
        -fixedBudgetChange: int
        -happinessBonus: double
    }

    class DefaultPolicy
    class GreenPolicy
    class AusterityPolicy
    class FossilFuelPolicy

    class City {
        -activePolicy: PolicyStrategy
        -observers: List~StateObserver~
        +setPolicy(policy: PolicyStrategy)
        +addObserver(obs: StateObserver)
    }

    class DisasterManager {
        -observers: List~DisasterObserver~
        +addObserver(obs: DisasterObserver)
    }

    DefaultPolicy ..|> PolicyStrategy
    GreenPolicy ..|> PolicyStrategy
    AusterityPolicy ..|> PolicyStrategy
    FossilFuelPolicy ..|> PolicyStrategy

    PolicyStrategy ..> PolicyModifiers : creates

    City "1" o-- "1" PolicyStrategy : current policy
    City "1" o-- "*" StateObserver : updates
    DisasterManager "1" o-- "*" DisasterObserver : manages
```

### 3.4 Livello UI (User Interface e JavaFX)

```mermaid
classDiagram
    direction TB

    %% Classi esterne per evidenziare l'integrazione
    class GameController {
        <<Controller>>
    }
    class StateObserver {
        <<interface>>
    }
    class CityState {
        <<Domain>>
    }

    class SimulationController {
        -view: DashboardView
        -gameController: GameController
    }

    class DashboardView {
        -mapGridView: MapGridView
        -metricsPanel: MetricsPanel
        -controlsBar: SimulationControlsBar
        -buildToolbar: BuildToolbar
        -chart: DashboardChart
        +showEarthquakeWarning()
        +onStateChanged(state: CityState)
    }

    class MapGridView {
        -canvas: Canvas
        +drawGrid()
        +highlightCell()
    }

    class MetricsPanel {
        +updateMetrics(state: CityState)
        +log(message: String)
    }

    class SimulationControlsBar {
        +setPlayPauseHandler()
        +setPolicyHandler()
    }

    class BuildToolbar {
        +setToolSelectionHandler()
    }

    class DashboardChart {
        +addDataPoint(tick: int, state: CityState)
    }

    class DialogHelper {
        <<utility>>
        +showError()
        +showSaveDialog()
    }
    
    class IconCatalog {
        <<utility>>
    }

    %% Relazioni interne UI
    SimulationController "1" *-- "1" DashboardView : manages
    DashboardView "1" *-- "1" MapGridView : contains
    DashboardView "1" *-- "1" MetricsPanel : contains
    DashboardView "1" *-- "1" SimulationControlsBar : contains
    DashboardView "1" *-- "1" BuildToolbar : contains
    DashboardView "1" *-- "1" DashboardChart : contains
    DashboardView ..> DialogHelper : uses
    DashboardView ..> IconCatalog : uses

    %% Relazioni con il Core
    SimulationController --> GameController : delegates commands
    DashboardView ..|> StateObserver : implements
    DashboardView ..> CityState : receives
    MetricsPanel ..> CityState : reads data
    DashboardChart ..> CityState : reads data
```

---


## 4. Internal Sequence Diagrams

I diagrammi interni documentano le interazioni tra i componenti del dominio per le operazioni più significative, annotando i pattern GRASP e GoF applicati.

### 4.1 advanceTick() — Flusso tick completo

```mermaid
sequenceDiagram
    actor User
    participant UI as SimulationController
    participant GC as GameController
    participant City as City
    participant Grid as Grid
    participant Queries as <<static>><br/>GridQueries
    participant Structure as Structure
    participant State as CityState
    participant PowerNet as PowerNetwork
    participant Policy as PolicyStrategy
    participant Disaster as DisasterManager
    participant PopManager as PopulationManager
    participant Observer as StateObserver

    User->>UI: Avanza la simulazione<br/>(timer automatico o manuale)
    activate UI
    UI->>GC: advanceTick()
    activate GC

    %% Fase Pre-Pass
    GC->>Grid: getCells()
    activate Grid
    Grid-->>GC: cells
    deactivate Grid

    loop per ogni cella non vuota
        GC->>Structure: setPowered(GridQueries.isPoweredAt(...))
        activate Structure
        Structure-->>GC: ok
        deactivate Structure

        GC->>Structure: setConnectedToRoad(GridQueries.hasAdjacentRoad(...))
        activate Structure
        Structure-->>GC: ok
        deactivate Structure
    end

    %% Fase Simulazione Dominio
    GC->>City: advanceTick()
    activate City

    City->>City: tickStructuresPhase()
    activate City
    loop per ogni cella non vuota
        City->>Structure: decayTick()
        activate Structure
        Structure-->>City: ok
        deactivate Structure

        City->>Structure: applyEffects(state, powerNet)
        activate Structure
        Structure->>State: modifica stato
        Structure->>PowerNet: consuma/produce
        Structure-->>City: ok
        deactivate Structure
    end
    deactivate City

    City->>City: tickCapacityPhase(residentialCount)
    activate City
    opt popolazione > maxCapacity
        City->>State: setOverpopulated(true)
    end
    deactivate City

    City->>Policy: getModifiers()
    activate Policy
    Policy-->>City: modifiers
    deactivate Policy

    City->>State: resolveTick(modifiers)
    activate State
    State-->>City: ok
    deactivate State

    City->>City: tickDisastersPhase()
    activate City
    opt probabilità di terremoto raggiunta
        City->>Disaster: triggerEarthquake(state)
        activate Disaster
        Disaster-->>City: ok
        deactivate Disaster

        City->>State: setEarthquakeOccurred(true)

        loop per ogni Structure s distrutta dal sisma
            City->>Grid: removeStructure(x, y)
            City->>Disaster: removeObserver(s)
        end

        opt se almeno un edificio è crollato nel sisma
            City->>City: updateRoadConnections()
        end
    end
    deactivate City

    City->>City: tickDemographicsPhase(...)
    activate City
    City->>PopManager: new PopulationManager()
    City->>PopManager: updateDemographics(...)
    activate PopManager
    PopManager-->>City: ok
    deactivate PopManager
    deactivate City

    City->>City: logTickSummary(...)

    %% Notifica UI
    City->>City: notifyObservers()
    activate City
    loop per ogni StateObserver registrato
        City->>Observer: onStateChanged(state)
        activate Observer
        Observer-->>City: ok
        deactivate Observer
    end
    deactivate City

    City-->>GC: ok
    deactivate City

    GC-->>UI: ok
    deactivate GC

    UI-->>User: Refresh della dashboard e mappa
    deactivate UI
```

### 4.2 placeBuilding(type, x, y)

```mermaid
sequenceDiagram
    actor User
    participant UI as SimulationController
    participant GC as GameController
    participant City as City
    participant State as CityState
    participant Factory as <<static>><br/>BuildingFactory
    participant Grid as Grid
    participant Queries as <<static>><br/>GridQueries
    participant Structure as Structure

    User->>UI: Richiesta posizionamento<br/>(click su cella vuota)
    activate UI
    UI->>GC: placeBuilding(type, x, y)
    activate GC

    GC->>GC: consumeOneShotEvents()
    activate GC
    GC->>City: getState()
    activate City
    City-->>GC: state
    deactivate City
    GC->>State: isEarthquakeOccurred()
    activate State
    State-->>GC: occurred
    deactivate State

    opt occurred == true
        GC->>State: setEarthquakeOccurred(false)
        activate State
        State-->>GC: ok
        deactivate State
    end
    deactivate GC

    GC->>Factory: createBuilding(type)
    activate Factory
    Factory-->>GC: building
    deactivate Factory

    GC->>City: getGrid()
    activate City
    City-->>GC: grid
    deactivate City

    GC->>Grid: getCell(x, y)
    activate Grid
    Grid-->>GC: cell
    deactivate Grid

    alt cell == null OR !cell.isEmpty()
        GC->>GC: lastError = "Cell occupied or invalid."
        GC-->>UI: false
    else cell is valid and empty
        GC->>Structure: getType()
        activate Structure
        Structure-->>GC: structureType
        deactivate Structure

        opt structureType == StructureType.RESIDENTIAL
            GC->>City: getGrid()
            activate City
            City-->>GC: grid
            deactivate City

            GC->>Queries: hasAdjacentRoad(grid, x, y)
            activate Queries
            Queries-->>GC: hasRoad
            deactivate Queries
        end

        alt structureType == StructureType.RESIDENTIAL AND !hasRoad
            GC->>GC: lastError = "Must build next to a road!"
            GC-->>UI: false
        else road condition satisfied
            GC->>City: getState()
            activate City
            City-->>GC: state
            deactivate City

            GC->>State: getBudget()
            activate State
            State-->>GC: budget
            deactivate State

            GC->>Structure: getConstructionCost()
            activate Structure
            Structure-->>GC: cost
            deactivate Structure

            alt budget < cost
                GC->>GC: lastError = "Insufficient budget..."
                GC-->>UI: false
            else budget sufficient
                GC->>Grid: placeStructure(building, x, y)
                activate Grid
                Grid-->>GC: ok
                deactivate Grid

                GC->>City: addDisasterObserver(building)
                activate City
                City-->>GC: ok
                deactivate City

                GC->>City: getState()
                activate City
                City-->>GC: state
                deactivate City

                GC->>State: getBudget()
                activate State
                State-->>GC: currentBudget
                deactivate State

                GC->>Structure: getConstructionCost()
                activate Structure
                Structure-->>GC: cost
                deactivate Structure

                GC->>State: setBudget(currentBudget - cost)
                activate State
                State-->>GC: ok
                deactivate State

                GC->>City: updateRoadConnections()
                activate City
                City-->>GC: ok
                deactivate City

                GC->>City: notifyObserversPublic()
                activate City
                City-->>GC: ok
                deactivate City

                GC-->>UI: true
            end
        end
    end

    deactivate GC
    UI-->>User: Aggiornamento interfaccia<br/>e feedback visivo
    deactivate UI
```

### 4.3 changePolicy(policy)

```mermaid
sequenceDiagram
    actor User
    participant UI as SimulationController
    participant GC as GameController
    participant City as City
    participant DefaultPolicy as DefaultPolicy

    User->>UI: Seleziona una politica<br/>(o nessuna)
    activate UI
    UI->>GC: changePolicy(policy)
    activate GC

    alt policy == null
        GC->>DefaultPolicy: new DefaultPolicy()
        GC->>City: setPolicy(defaultPolicy)
        activate City
        City-->>GC: ok
        deactivate City
    else policy != null
        GC->>City: setPolicy(policy)
        activate City
        City-->>GC: ok
        deactivate City
    end

    GC-->>UI: ok
    deactivate GC
    UI-->>User: Aggiornamento interfaccia
    deactivate UI
```

### 4.4 saveManualGame(tick)

```mermaid
sequenceDiagram
    actor User
    participant UI as SimulationController
    participant GC as GameController
    participant IO as SaveLoadManager
    participant Mapper as <<static>><br/>SaveDataMapper
    participant ObjMapper as ObjectMapper

    User->>UI: Richiesta salvataggio manuale
    activate UI
    UI->>GC: saveManualGame(tick)
    activate GC

    GC->>IO: saveManual(city, tick)
    activate IO

    IO->>IO: filename = "save_" + timestamp + ".json"

    IO->>IO: saveToFile(city, tick, filename)
    activate IO

    IO->>Mapper: toSaveData(city, tick)
    activate Mapper
    Mapper-->>IO: saveData
    deactivate Mapper

    IO->>ObjMapper: writeValue(file, saveData)
    activate ObjMapper
    ObjMapper-->>IO: ok
    deactivate ObjMapper

    IO-->>IO: file (Path)
    deactivate IO

    IO-->>GC: file (Path)
    deactivate IO

    GC-->>UI: file (Path)
    deactivate GC

    UI-->>User: Notifica salvataggio completato
    deactivate UI
```

### 4.5 loadGame(path)

```mermaid
sequenceDiagram
    actor User
    participant UI as SimulationController
    participant GC as GameController
    participant IO as SaveLoadManager
    participant ObjMapper as ObjectMapper
    participant Applier as <<static>><br/>SaveDataApplier
    participant City as City

    User->>UI: Richiesta caricamento partita
    activate UI
    UI->>GC: loadGame(path)
    activate GC

    GC->>IO: load(city, path)
    activate IO

    IO->>ObjMapper: readValue(file, SaveData.class)
    activate ObjMapper
    ObjMapper-->>IO: saveData
    deactivate ObjMapper

    IO->>Applier: apply(city, saveData)
    activate Applier
    Applier-->>IO: ok
    deactivate Applier

    IO-->>GC: tick
    deactivate IO

    GC->>City: updateRoadConnections()
    activate City
    City-->>GC: ok
    deactivate City

    GC->>City: notifyObserversPublic()
    activate City
    City-->>GC: ok
    deactivate City

    GC-->>UI: tick
    deactivate GC

    UI-->>User: Ripristino partita e interfaccia
    deactivate UI
```
