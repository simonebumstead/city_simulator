# Domain Model - City Simulator

Il Domain Model illustra i concetti fondamentali del dominio applicativo, omettendo i dettagli implementativi per concentrarsi esclusivamente sulla logica di business, le entità principali del gioco e le loro relazioni concettuali.

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
        -maxHp: int
        -powered: boolean
        -connectedToRoad: boolean
        +applyEffects()
        +getConstructionCost()
    }

    class ResidentialBuilding
    class CommercialBuilding
    class IndustrialBuilding
    class PowerPlant
    class Park
    class Hospital
    class WasteManagementCenter
    class Road

    class PowerNetwork {
        -totalProduction: int
        -totalConsumption: int
    }

    class PolicyStrategy {
        <<interface>>
        +getModifiers()
    }

    class DisasterManager {
        +triggerEarthquake()
    }

    class PopulationManager {
        +updateDemographics()
    }

    class PopulationGroup {
        -jobSatisfaction: double
        -healthSatisfaction: double
        -safetySatisfaction: double
    }

    %% --- LOGICAL RELATIONSHIPS ---
    City "1" *-- "1" Grid : contains
    City "1" *-- "1" CityState : tracks
    City "1" *-- "1" PowerNetwork : manages
    City "1" *-- "1" DisasterManager : handles events
    
    City "1" o-- "1" PolicyStrategy : applies policy
    City ..> PopulationManager : updates demographics

    CityState "1" *-- "1" PopulationGroup : measures
    PopulationManager ..> PopulationGroup : updates
    
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