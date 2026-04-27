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

```mermaid
classDiagram
%% --- Pattern: Controller (GRASP) ---
class GameController {
-City logic
+advanceTick()
+placeBuilding(String type, int x, int y)
+changePolicy(PolicyStrategy newPolicy)
}

    %% --- Pattern: Factory (GoF) ---
    class BuildingFactory {
        +createBuilding(String type) Building
    }

    %% --- Core Domain ---
    class City {
        -Grid cityGrid
        -CityState currentState
        -PolicyStrategy activePolicy
        +updateState()
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
        +addBuilding(Building b, int x, int y)
    }

    %% --- Edifici ---
    class Building {
        <<interface>>
        +applyEffect(CityState state)
        +getCost() int
    }
    
    class PowerPlant {
        +applyEffect(CityState state)
        +getCost() int
    }
    class Residential {
        +applyEffect(CityState state)
        +getCost() int
    }

    %% --- Pattern: Strategy (GoF) ---
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

    %% Relazioni Architetturali
    GameController --> City : gestisce
    GameController --> BuildingFactory : usa per creare
    City *-- Grid : contiene
    City *-- CityState : possiede
    City o-- PolicyStrategy : adotta
    
    BuildingFactory ..> Building : istanzia
    Building <|.. PowerPlant : implementa
    Building <|.. Residential : implementa
    
    PolicyStrategy <|.. GreenPolicy : implementa
    PolicyStrategy <|.. FossilFuelPolicy : implementa
```

GRASP - Controller: Il messaggio iniziale advanceTick() parte dalla DashboardUI (livello di presentazione) e viene ricevuto dal GameController. Il Controller agisce come intermediario, disaccoppiando l'interfaccia grafica dalla logica di dominio (Low Coupling). Non esegue i calcoli, ma delega l'operazione.

GRASP - Information Expert: Il GameController delega l'aggiornamento alla classe City invocando updateState(). La classe City è l'Esperto dell'Informazione per questa operazione, poiché possiede i riferimenti alla Grid (dove si trovano gli edifici), allo stato globale CityState e all'ordinanza attiva PolicyStrategy.

GRASP - Polymorphism: Nel ciclo loop, la classe City invoca applyBaseEffect(S) sull'interfaccia/classe astratta Building. Il sistema usa il polimorfismo per calcolare gli effetti: la classe City non ha bisogno di sapere se sta parlando con una centrale elettrica o una zona residenziale (non ci sono costrutti switch o if-else). Ogni specifica sottoclasse di Building sa come aggiornare le proprie metriche.

GoF - Strategy Pattern: Dopo aver calcolato gli effetti base di tutti gli edifici, la City delega il calcolo delle tasse o delle riduzioni dell'inquinamento all'oggetto activePolicy (istanza dell'interfaccia PolicyStrategy). La City è il Context del pattern: non conosce l'algoritmo specifico della politica attiva (es. Tassa Ambientale vs Espansione Industriale), ma invoca semplicemente applyPolicyModifier(S). Questo garantisce un design aperto all'estensione ma chiuso alla modifica (Open-Closed Principle).