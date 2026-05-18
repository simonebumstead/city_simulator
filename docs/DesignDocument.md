# Documento di Design — CityLogic City Simulator

**Versione:** 1.1 — 2026-05-18

---

## 1. Domain Model

Il dominio di CityLogic ruota attorno alla classe `City`, che orchestra tutti i sottosistemi della simulazione. La `City` possiede una `Grid` 20×20 di celle (`Cell`), ognuna delle quali può contenere al massimo una struttura (`Structure`). Lo stato globale della città — budget, popolazione, happiness, health, pollution, waste — è raccolto in `CityState`, che accumula i delta prodotti dagli edifici ad ogni tick e li risolve applicando i modificatori della `PolicyStrategy` attiva.

Le strutture concrete (8 tipi: `ResidentialBuilding`, `IndustrialBuilding`, `CommercialBuilding`, `PowerPlant`, `Park`, `Road`, `Hospital`, `WasteManagementCenter`) ereditano da `Structure` (classe astratta) e implementano `applyEffects()` secondo il **Template Method Pattern**. I potenziamenti (`SeismicUpgrade`, `WasteThermalUpgrade`) avvolgono le strutture con il **Decorator Pattern** senza modificarne il codice sorgente.

La `PowerNetwork` tiene traccia del bilancio energetico produzione/consumo. Il `DisasterManager` gestisce i terremoti con probabilità 1%/tick, notificando tutte le strutture registrate tramite il **Observer Pattern** (`DisasterObserver`). Le politiche cittadine (Default, Green, FossilFuel, Austerity) implementano la `PolicyStrategy` (**Strategy Pattern**) e alterano i moltiplicatori di `CityState.resolveTick()`. Il `SaveLoadManager` serializza e deserializza lo stato completo in JSON tramite Jackson (**Pure Fabrication**). Il `PopulationManager` calcola ogni tick la crescita demografica e le tre soddisfazioni del `PopulationGroup` (lavoro, salute, sicurezza).

```mermaid
classDiagram
    direction TB

    class City {
        -grid: Grid
        -state: CityState
        -powerNet: PowerNetwork
        -activePolicy: PolicyStrategy
        -disasterManager: DisasterManager
        +advanceTick()
        +setPolicy(policy: PolicyStrategy)
    }

    class Grid {
        -matrix: Cell[][] 20x20
        +getCell(x, y) Cell
        +placeStructure(s, x, y)
        +removeStructure(x, y)
    }

    class Cell {
        -structure: Placeable
        +isEmpty() boolean
    }

    class CityState {
        -budget: double
        -population: int
        -happiness: double
        -health: double
        -pollution: double
        -wasteLevel: int
        +resolveTick(modifiers: PolicyModifiers)
    }

    class PowerNetwork {
        -totalProduction: int
        -totalConsumption: int
        +hasEnoughPower() boolean
    }

    class PopulationManager {
        +updateDemographics(state, hasPower, capacity, ...)
    }

    class PopulationGroup {
        -jobSatisfaction: double
        -healthSatisfaction: double
        -safetySatisfaction: double
    }

    class DisasterManager {
        -observers: List~DisasterObserver~
        +triggerEarthquake(state: CityState)
    }

    class SaveLoadManager {
        +saveManual(city, tick) Path
        +saveAuto(city, tick, sessionId) Path
        +load(city, path) int
    }

    class Structure {
        <<abstract>>
        #hp: int
        #maxHp: int
        +applyEffects(state, power)*
        +takeDamage(amount) int
        +decayTick()
    }

    class StructureDecorator {
        <<abstract>>
        #wrapped: Structure
        +collectUpgrades() List~String~
    }

    class PolicyStrategy {
        <<interface>>
        +getModifiers() PolicyModifiers
    }

    class PolicyModifiers {
        -pollutionMultiplier: double
        -industrialBudgetMultiplier: double
        -fixedBudgetChange: double
    }

    City "1" o-- "1" Grid
    City "1" o-- "1" CityState
    City "1" o-- "1" PowerNetwork
    City "1" o-- "1" PolicyStrategy
    City "1" o-- "1" DisasterManager
    Grid "1" *-- "400" Cell
    Cell "1" o-- "0..1" Structure
    CityState "1" *-- "1" PopulationGroup
    PopulationManager ..> CityState : aggiorna
    Structure <|-- StructureDecorator
    StructureDecorator "1" o-- "1" Structure : wraps
    PolicyStrategy ..> PolicyModifiers : crea
    CityState ..> PolicyModifiers : usa
    SaveLoadManager ..> City : serializza
```

---

## 2. System Sequence Diagrams

Il diagramma seguente mostra le interazioni tra l'attore esterno (Player) e il Sistema visto come scatola nera. Documenta le 4 categorie di operazioni disponibili: costruzione/potenziamento, gestione simulazione, interrogazione dati e persistenza.

```plantuml
@startuml
!theme vibrant

title System Sequence Diagram (SSD) — CityLogic

actor Player
participant ":System" as System

group Costruzione e Potenziamento
    Player -> System: placeBuilding(type, x, y)
    activate System
    note right of System: Valida posizione, controlla budget,\ncrea struttura, aggiorna stato.
    System --> Player: status: "OK" / "Error: [motivo]"
    deactivate System

    Player -> System: demolish(x, y)
    activate System
    note right of System: Valida posizione, rimuove struttura,\nrimborsa il 50% netto del costo.
    System --> Player: status: "OK"
    deactivate System

    Player -> System: upgradeBuilding(x, y, upgradeType)
    activate System
    note right of System: Applica Decorator (SEISMIC o WASTE_THERMAL)\nse budget sufficiente e livello < 3.
    System --> Player: status: "OK" / "Error: [motivo]"
    deactivate System

    Player -> System: repairBuilding(x, y)
    activate System
    note right of System: Ripara edificio al costo (maxHp−hp)/2.
    System --> Player: status: "OK" / "Error: budget insufficiente"
    deactivate System
end

group Gestione Simulazione
    Player -> System: advanceTick()
    activate System
    note right of System: Aggiorna flag powered/connectedToRoad,\napplica effetti di tutti gli edifici,\nricalcola policy, demografia e disastri.
    System --> Player: updatedCityMetrics
    deactivate System

    Player -> System: setPolicy(policyType)
    activate System
    note right of System: Sostituisce la politica attiva;\nla precedente viene dismessa e l'utente notificato.
    System --> Player: status: "OK"
    deactivate System
end

group Interrogazione Dati (Query)
    Player -> System: getCityMetrics()
    activate System
    System --> Player: {budget, popolazione, happiness, health, pollution, waste}
    deactivate System

    Player -> System: getStructureDetails(x, y)
    activate System
    System --> Player: {tipo, hp, maxHp, powered, connectedToRoad} / "Cella vuota"
    deactivate System
end

group Persistenza Dati
    Player -> System: saveGame()
    activate System
    note right of System: Serializza griglia + metriche in JSON\nnella cartella saves/.
    System --> Player: status: "OK" / "Error: I/O"
    deactivate System

    Player -> System: loadGame(fileName)
    activate System
    note right of System: Deserializza da JSON e ricostruisce\nla griglia con tutti gli upgrade.
    System --> Player: tickCaricato
    deactivate System
end

@enduml
```

---

## 3. Design Class Model

Il modello di design riflette la struttura tecnica completa, incluso il layer UI e il facade. I pattern architetturali chiave sono:

- **Facade** — `SimulationController` è un wrapper leggero attorno a `GameController`; la UI non tocca mai il dominio direttamente.
- **GRASP Controller** — `GameController` è l'unico punto di ingresso per le operazioni che mutano lo stato (place, demolish, repair, repairAll, upgrade, policy change, save/load).
- **Strategy** — `PolicyStrategy` con 4 implementazioni; `CityState.resolveTick(PolicyModifiers)` applica i modificatori senza conoscere la policy concreta.
- **Observer** — `City` notifica `StateObserver` (la UI) ad ogni tick; `DisasterManager` notifica `DisasterObserver` (le strutture) ad ogni terremoto.
- **Decorator** — `StructureDecorator` avvolge `Structure` per aggiungere comportamento (dimezzamento danni, recupero termico) senza modificare le classi base.
- **Factory** — `BuildingFactory` centralizza la costruzione di tutte le istanze `Structure` e la riapplicazione degli upgrade al caricamento del salvataggio.
- **Template Method** — `Structure` definisce lo scheletro del ciclo di vita (decayTick, applyEffects, takeDamage); ogni sottoclasse sovrascrive solo `applyEffects()` e `getType()`.

```mermaid
classDiagram
    direction TB

    %% --- LAYER UI ---
    class DashboardView {
        +onStateChanged(state: CityState)
        +launch()
    }
    class SimulationController {
        +tick()
        +placeBuilding(type, x, y) boolean
        +demolish(x, y) boolean
        +repair(x, y) boolean
        +repairAll() boolean
        +upgrade(x, y, type) boolean
        +setPolicy(policy)
        +saveManual(tick) Path
        +load(path) int
    }

    %% --- DOMAIN CONTROLLER ---
    class GameController {
        -city: City
        +advanceTick()
        +placeBuilding(type, x, y) boolean
        +demolish(x, y) boolean
        +repair(x, y) boolean
        +repairAll() boolean
        +upgradeBuilding(x, y, type) boolean
        +changePolicy(policy)
        +saveManualGame(tick) Path
        +loadGame(path) int
    }

    %% --- CORE DOMAIN ---
    class City {
        -grid: Grid
        -state: CityState
        -powerNet: PowerNetwork
        -activePolicy: PolicyStrategy
        -disasterManager: DisasterManager
        +advanceTick()
        +setPolicy(PolicyStrategy)
        +addObserver(StateObserver)
        +notifyObserversPublic()
    }

    class Grid {
        -matrix: Cell[][] 20x20
        +getCell(x, y) Cell
        +placeStructure(s, x, y)
        +removeStructure(x, y)
        +isCellEmpty(x, y) boolean
    }

    class Cell {
        -structure: Placeable
        +getStructure() Placeable
        +isEmpty() boolean
    }

    class CityState {
        -budget: double
        -population: int
        -happiness: double
        -health: double
        -pollution: double
        -wasteLevel: int
        -populationGroup: PopulationGroup
        +resolveTick(PolicyModifiers)
        +updateBudget(double)
        +setBudget(double)
    }

    class PopulationGroup {
        -jobSatisfaction: double
        -healthSatisfaction: double
        -safetySatisfaction: double
    }

    class PopulationManager {
        +updateDemographics(state, hasPower, maxCap, ...)
    }

    class PowerNetwork {
        +addProduction(int)
        +addConsumption(int)
        +hasEnoughPower() boolean
        +reset()
    }

    class DisasterManager {
        -observers: List~DisasterObserver~
        +addObserver(DisasterObserver)
        +removeObserver(DisasterObserver)
        +triggerEarthquake(CityState)
    }

    class SaveLoadManager {
        +saveManual(City, tick) Path
        +saveAuto(City, tick, sessionId) Path
        +load(City, Path) int
    }

    class BuildingFactory {
        <<utility>>
        +createBuilding(type)$ Structure
    }

    class GridQueries {
        <<utility>>
        +isPoweredAt(grid, x, y)$ boolean
        +hasAdjacentRoad(grid, x, y)$ boolean
        POWER_RADIUS = 5
    }

    %% --- INTERFACES ---
    class Placeable {
        <<interface>>
        +getType() StructureType
    }
    class PolicyStrategy {
        <<interface>>
        +getModifiers() PolicyModifiers
    }
    class StateObserver {
        <<interface>>
        +onStateChanged(CityState)
    }
    class DisasterObserver {
        <<interface>>
        +onEarthquake(int)
    }

    %% --- STRUCTURES (Template Method) ---
    class Structure {
        <<abstract>>
        #hp: int
        #maxHp: int
        #powered: boolean
        #connectedToRoad: boolean
        +applyEffects(CityState, PowerNetwork)*
        +getType() StructureType*
        +takeDamage(int) int
        +decayTick()
        +fullRepair()
    }

    class ResidentialBuilding
    class IndustrialBuilding
    class CommercialBuilding
    class PowerPlant
    class Park
    class Road
    class Hospital
    class WasteManagementCenter

    %% --- DECORATORS ---
    class StructureDecorator {
        <<abstract>>
        #wrapped: Structure
        +getUpgradeLevel() int
        +collectUpgrades() List~String~
    }
    class SeismicUpgrade {
        COST = 500
        +takeDamage(int) int
    }
    class WasteThermalUpgrade {
        COST = 700
        +applyEffects(CityState, PowerNetwork)
    }

    %% --- STRATEGIES ---
    class PolicyModifiers {
        -pollutionMultiplier: double
        -industrialBudgetMultiplier: double
        -fixedBudgetChange: double
        -fixedHappinessChange: double
    }
    class DefaultPolicy
    class GreenPolicy
    class AusterityPolicy
    class FossilFuelPolicy

    %% --- RELATIONSHIPS ---
    DashboardView ..|> StateObserver
    DashboardView --> SimulationController : usa
    SimulationController --> GameController : delega

    GameController "1" o-- "1" City
    GameController ..> BuildingFactory : usa
    GameController ..> GridQueries : usa

    City "1" o-- "1" Grid
    City "1" o-- "1" CityState
    City "1" o-- "1" PowerNetwork
    City "1" o-- "1" PolicyStrategy
    City "1" o-- "1" DisasterManager
    City "1" o-- "*" StateObserver : notifica
    City ..> PopulationManager : usa

    Grid "1" *-- "400" Cell
    Cell "1" o-- "0..1" Placeable

    CityState "1" *-- "1" PopulationGroup
    CityState ..> PolicyModifiers : usa
    PolicyStrategy ..> PolicyModifiers : crea

    Structure ..|> Placeable
    Structure ..|> DisasterObserver
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

    DefaultPolicy ..|> PolicyStrategy
    GreenPolicy ..|> PolicyStrategy
    AusterityPolicy ..|> PolicyStrategy
    FossilFuelPolicy ..|> PolicyStrategy

    DisasterManager ..> DisasterObserver : notifica
    SaveLoadManager ..> BuildingFactory : usa
```

---

## 4. Internal Sequence Diagrams

I tre diagrammi interni documentano le interazioni tra i componenti del dominio per le operazioni più significative, annotando i pattern GRASP e GoF applicati.

### 4.1 advanceTick() — Flusso tick completo

La UI delega al `SimulationController` (Facade), che delega al `GameController` (GRASP Controller). Il controller esegue una **pre-pass** per aggiornare i flag `powered` e `connectedToRoad` su ogni struttura, poi delega a `City` (Information Expert). `City` itera sugli edifici applicando gli effetti via polimorfismo (Template Method / GRASP Polymorphism), poi applica i modificatori della policy attiva (GoF Strategy) e aggiorna la demografia (`PopulationManager`). Infine notifica la UI via Observer.

```mermaid
sequenceDiagram
    participant UI as :DashboardView
    participant SC as :SimulationController
    participant GC as :GameController
    participant City as :City
    participant Grid as :Grid
    participant S as :CityState
    participant B as :Structure
    participant P as activePolicy:PolicyStrategy
    participant PM as :PopulationManager
    participant DM as :DisasterManager

    UI->>SC: tick()
    SC->>GC: advanceTick()
    activate GC
    Note over GC: Pre-pass: aggiorna\npowered e connectedToRoad\nsu ogni struttura

    GC->>Grid: itera celle
    Grid-->>GC: strutture
    GC->>B: setPowered(GridQueries.isPoweredAt(...))
    GC->>B: setConnectedToRoad(GridQueries.hasAdjacentRoad(...))

    GC->>City: advanceTick()
    activate City

    City->>S: reset delta accumulatori
    City->>Grid: itera strutture
    loop Per ogni struttura
        City->>B: decayTick()
        City->>B: applyEffects(S, powerNet)
        activate B
        Note right of B: Template Method Pattern
        B->>S: updateBudget / updatePollution / ...
        deactivate B
    end

    City->>City: applicaEffettiParchi()
    Note right of City: Bonus happiness ai Residenziali\nentro raggio Chebyshev 3

    Note over City,P: Strategy Pattern
    City->>P: getModifiers()
    P-->>City: PolicyModifiers
    City->>S: resolveTick(modifiers)
    activate S
    Note right of S: Applica moltiplicatori,\npenalità soglia, commit delta
    deactivate S

    City->>PM: updateDemographics(S, hasPower, ...)
    activate PM
    PM->>S: setPopulation(newPop)
    deactivate PM

    City->>DM: (1% prob) triggerEarthquake(S)
    activate DM
    Note right of DM: Observer Pattern:\nnotifica tutte le strutture
    DM->>B: onEarthquake(damage)
    deactivate DM

    City->>UI: onStateChanged(S)
    Note right of City: Observer Pattern:\naggiorna la dashboard
    deactivate City
    deactivate GC
```

### 4.2 placeBuilding(type, x, y)

Il `GameController` riceve la richiesta dalla UI tramite facade, usa `BuildingFactory` per istanziare la struttura (GoF Factory), valida le precondizioni (budget, cella libera, strada per i Residential) e piazza. In caso di successo notifica gli observer.

```mermaid
sequenceDiagram
    participant UI as :DashboardView
    participant SC as :SimulationController
    participant GC as :GameController
    participant F as :BuildingFactory
    participant City as :City
    participant Grid as :Grid
    participant S as :CityState

    UI->>SC: placeBuilding(type, x, y)
    SC->>GC: placeBuilding(type, x, y)
    activate GC
    Note over GC: GRASP Controller

    GC->>F: createBuilding(type)
    activate F
    Note right of F: Factory Pattern
    F-->>GC: structure
    deactivate F

    GC->>Grid: getCell(x, y)
    Grid-->>GC: cell

    alt cella occupata o fuori griglia
        GC-->>SC: false (lastError = "Cell occupied")
    else tipo RESIDENTIAL e nessuna Road adiacente
        GC-->>SC: false (lastError = "Must build next to a Road")
    else budget < constructionCost
        GC-->>SC: false (lastError = "Insufficient budget")
    else OK
        GC->>Grid: placeStructure(structure, x, y)
        GC->>S: setBudget(budget − cost)
        GC->>City: addDisasterObserver(structure)
        GC->>City: updateRoadConnections()
        GC->>City: notifyObserversPublic()
        Note right of City: Observer Pattern
        City->>UI: onStateChanged(S)
        GC-->>SC: true
    end
    deactivate GC
```

### 4.3 saveGame() / loadGame()

Il `GameController` delega I/O al `SaveLoadManager` (GRASP Pure Fabrication). Al salvataggio, `SaveDataMapper` estrae i DTO dal dominio. Al caricamento, `SaveDataApplier` ricostruisce la griglia usando `BuildingFactory` per ogni struttura e riapplica gli upgrade in ordine.

```mermaid
sequenceDiagram
    participant UI as :DashboardView
    participant SC as :SimulationController
    participant GC as :GameController
    participant IO as :SaveLoadManager
    participant Mapper as :SaveDataMapper
    participant Applier as :SaveDataApplier
    participant F as :BuildingFactory
    participant City as :City

    Note over UI,City: === SALVATAGGIO ===
    UI->>SC: saveManual(tick)
    SC->>GC: saveManualGame(tick)
    GC->>IO: saveManual(city, tick)
    activate IO
    IO->>Mapper: toSaveData(city, tick)
    activate Mapper
    Note right of Mapper: GRASP Information Expert:\nestrae stato da City, Grid,\nCityState e decoratori
    Mapper-->>IO: SaveData (DTO)
    deactivate Mapper
    Note over IO: Jackson serializza SaveData → JSON\nnella cartella saves/
    IO-->>GC: Path del file creato
    deactivate IO

    Note over UI,City: === CARICAMENTO ===
    UI->>SC: load(path)
    SC->>GC: loadGame(path)
    GC->>IO: load(city, path)
    activate IO
    Note over IO: Jackson deserializza JSON → SaveData
    IO->>Applier: apply(city, saveData)
    activate Applier
    Note right of Applier: GRASP Pure Fabrication:\nricostruisce il dominio dal DTO
    loop Per ogni BuildingEntry in saveData.buildings
        Applier->>F: createBuilding(entry.type)
        F-->>Applier: structure
        loop Per ogni upgrade in entry.upgrades
            Applier->>F: applyUpgrade(structure, upgradeType)
        end
        Applier->>City: placeStructure(structure, x, y)
        Applier->>City: restoreHp(structure, entry.hp)
    end
    Applier->>City: setCityState(saveData metrics)
    Applier->>City: setPolicy(saveData.activePolicy)
    deactivate Applier
    IO-->>GC: tick caricato
    deactivate IO
    GC->>City: updateRoadConnections()
    GC->>City: notifyObserversPublic()
    City->>UI: onStateChanged(state)
```
