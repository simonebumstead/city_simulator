# Class Diagram - City Simulator

Essendo un sistema complesso, il Class Diagram è stato suddiviso in **quattro moduli separati** per renderne più agevole la consultazione e il rendering in Mermaid:

1. **Architettura Core**: I controller principali e i sistemi di simulazione del mondo.
2. **Strutture ed Edifici**: La gerarchia delle costruzioni, le interfacce e i Decorator.
3. **Politiche e Observer**: Il sistema di policy governative e la notifica degli eventi.
4. **Livello UI**: I componenti dell'interfaccia JavaFX e il controller di simulazione.

---

## 1. Architettura Core (Controller e Mondo)

```mermaid
classDiagram
    direction TB

    class GameController {
        -city: City
        -ioManager: SaveLoadManager
        +placeBuilding(type: String, x: int, y: int) boolean
        +demolish(x: int, y: int) boolean
        +repair(x: int, y: int) boolean
        +repairAll() boolean
        +upgradeBuilding(x: int, y: int, upgradeType: String) boolean
        +changePolicy(policy: PolicyStrategy)
        +advanceTick()
        +saveManualGame(tick: int) Path
        +loadGame(path: Path) int
        +listSaves() List~Path~
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
        -WIDTH: int$
        -HEIGHT: int$
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
        -populationGroup: PopulationGroup
        +resolveTick(modifiers: PolicyModifiers)
        +updateBudget(amount: double)
    }

    class PopulationGroup {
        -jobSatisfaction: double
        -healthSatisfaction: double
        -safetySatisfaction: double
    }

    class PowerNetwork {
        -totalProduction: int
        -totalConsumption: int
        +addProduction(amount: int)
        +addConsumption(amount: int)
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
        +updateDemographics(state: CityState, hasPowerNearby: boolean, maxCapacity: int, industrialCount: int, commercialCount: int, hospitalCount: int, residentialCount: int)
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
    CityState "1" *-- "1" PopulationGroup : measures
    
    GameController ..> SaveLoadManager : uses
    GameController ..> GridQueries : uses
    City ..> PopulationManager : uses
    City ..> GridQueries : uses
    PopulationManager ..> PopulationGroup : updates
    SaveLoadManager ..> City : reads/writes
    Cell o-- Placeable : holds
```

---

## 2. Strutture e Gerarchia degli Edifici (Template Method & Decorator)

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
        #powered: boolean
        #connectedToRoad: boolean
        +applyEffects(state: CityState, powerNet: PowerNetwork)*
        +getType() StructureType*
        +getConstructionCost() int*
        +decayTick()
        +takeDamage(damage: int) int
        +repair(amount: int)
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

    class StructureDecorator {
        <<abstract>>
        #wrapped: Structure
    }
    class SeismicUpgrade
    class WasteThermalUpgrade

    class BuildingFactory {
        <<utility>>
        +createBuilding(type: String)$ Structure
        +applyUpgrade(base: Structure, upgradeName: String)$ Structure
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

---

## 3. Politiche Economiche e Observer Pattern

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
        +onEarthquake(damage: int)
    }

    class PolicyModifiers {
        -pollutionGenerationMultiplier: double
        -happinessGenerationMultiplier: double
        -healthGenerationMultiplier: double
        -wasteGenerationMultiplier: double
        -industrialBudgetMultiplier: double
        -industrialPollutionMultiplier: double
        -fixedHappinessChange: double
        -fixedHealthChange: double
        -fixedPollutionChange: double
        -fixedBudgetChange: int
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

---

## 4. Livello UI (User Interface e JavaFX)

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
        <<Facade>>
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
    DashboardView "1" *-- "1" SimulationController : drives
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