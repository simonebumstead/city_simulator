```mermaid
classDiagram
%% --- PRESENTATION LAYER (MVC) ---
    class DashboardUI {
        +updateView(CityState state)
    }
    class StateObserver {
        <<interface>>
        +updateView(CityState state)
    }

%% --- CONTROLLER LAYER (GRASP) ---
    class GameController {
        -City city
        -SaveLoadManager ioManager
        +advanceTick()
        +placeBuilding(String type, int x, int y)
        +changePolicy(PolicyStrategy newPolicy)
        +saveGame(String filename)
    }

%% --- DOMAIN CORE (Information Expert) ---
    class City {
        -Grid grid
        -CityState state
        -PolicyStrategy activePolicy
        -List~StateObserver~ observers
        +updateState()
        +addObserver(StateObserver o)
        -notifyObservers()
    }

    class CityState {
        +int budget
        +int population
        +int pollution
        +int happiness
    }

    class Grid {
        -Cell[][] cells
        +getCell(int x, int y) Cell
        +placeStructure(Structure s, int x, int y)
    }

%% --- PERSISTENCE (File I/O) ---
    class SaveLoadManager {
        +save(City city, String path)
        +load(String path) City
    }

%% --- GoF: FACTORY PATTERN ---
    class BuildingFactory {
        +createBuilding(String type) Structure
    }

%% --- POLYMORPHISM ---
    class Structure {
        <<interface>>
        +applyEffect(CityState state)
        +getConstructionCost() int
    }
    class Residential {
        +applyEffect(CityState state)
        +getConstructionCost() int
    }
    class PowerPlant {
        +applyEffect(CityState state)
        +getConstructionCost() int
    }

%% --- GoF: STRATEGY PATTERN ---
    class PolicyStrategy {
        <<interface>>
        +applyPolicy(CityState state)
    }
    class GreenPolicy {
        +applyPolicy(CityState state)
    }
    class FossilFuelPolicy {
        +applyPolicy(CityState state)
    }

%% --- RELATIONS & DEPENDENCIES ---
    DashboardUI ..|> StateObserver : implements
    City o-- StateObserver : notifies (Observer Pattern)

    GameController --> City : manages
    GameController --> BuildingFactory : uses
    GameController --> SaveLoadManager : uses

    City *-- Grid : contains
    City *-- CityState : tracks
    City o-- PolicyStrategy : executes

    BuildingFactory ..> Structure : instantiates
    Structure <|.. Residential : implements
    Structure <|.. PowerPlant : implements

    PolicyStrategy <|.. GreenPolicy : implements
    PolicyStrategy <|.. FossilFuelPolicy : implements
```

Architettura MVC e Pattern Observer: Abbiamo separato strettamente la UI (DashboardUI) dalla logica.
La UI implementa l'interfaccia StateObserver. La classe City non sa nulla dell'interfaccia grafica: quando il suo stato cambia, si limita a chiamare notifyObservers().
Questo garantisce un Basso Accoppiamento (Low Coupling).Gestione dei Dati (File I/O): 
Come richiesto, abbiamo evitato i Database. Abbiamo applicato il principio di Alta Coesione (High Cohesion) creando una classe dedicata SaveLoadManager (che userà Jackson o Gson) , 
impedendo così alla classe City di violare il suo ruolo diventando una God Class che gestisce sia la simulazione che il File System .Pattern GoF Fondamentali: L'uso della BuildingFactory e della PolicyStrategy  assicura che il sistema sia estensibile (Open-Closed Principle): 
potremo aggiungere nuovi edifici o nuove politiche senza dover toccare il codice del motore principale.