```plantuml
@startuml
title Domain Model - City Simulator

' --- Entità Principali ---
class City
class Grid
class Cell
class CityState
class PowerNetwork
class DisasterManager
class PolicyStrategy

' --- Strutture ---
interface Placeable
class Structure
class ResidentialBuilding
class IndustrialBuilding
class PowerPlant
class Park
class Road

' --- Relazioni ---
City *-- Grid : contains
City *-- CityState : tracks
City *-- PowerNetwork : manages
City *-- DisasterManager : uses
City o-- PolicyStrategy : active policy

Grid *-- Cell : composed of
Cell o-- Placeable : holds

Placeable <|.. Structure
Structure <|-- ResidentialBuilding
Structure <|-- IndustrialBuilding
Structure <|-- PowerPlant
Structure <|-- Park
Structure <|-- Road

@enduml
```