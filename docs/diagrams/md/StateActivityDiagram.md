# State & Activity Diagram - City Simulator

Questo documento raccoglie due diagrammi comportamentali complementari ai diagrammi di sequenza:

1. un **diagramma di stato** che descrive il ciclo di vita di una `Structure` in funzione dei suoi HP;
2. un **diagramma di attività** che descrive il flusso interno di `advanceTick()`, ovvero il singolo
   passo di simulazione.

---

## 1. Diagramma di Stato — Ciclo di vita di una `Structure`

Ogni struttura nasce **Integra** (HP = maxHp). A ogni tick `decayTick()` ne riduce gli HP di 1
(le `Road` fanno eccezione: sovrascrivono `decayTick()` e non si deteriorano). Quando gli HP scendono
sotto il 20% di `maxHp` la struttura è segnalata come **Critica** (AC-23.2); quando raggiungono 0 è
**Distrutta** e viene rimossa dalla griglia. La riparazione (`repair()` / `fullRepair()`) riporta la
struttura allo stato Integra. Un terremoto (`onEarthquake()`) può ridurre bruscamente gli HP e far
transitare la struttura direttamente in stato Critica o Distrutta.

```mermaid
stateDiagram-v2
    direction LR

    [*] --> Integra : costruzione (HP = maxHp)

    Integra --> Critica : decayTick() / onEarthquake()<br/>HP < 20% maxHp
    Critica --> Integra : repair() / fullRepair()
    Integra --> Integra : decayTick() (HP ≥ 20% maxHp)

    Integra --> Distrutta : onEarthquake() porta HP a 0
    Critica --> Distrutta : HP = 0

    Distrutta --> [*] : rimozione dalla griglia<br/>e dagli observer

    note right of Critica
        Conteggiata in
        criticalBuildingCount
        ed evidenziata nell'UI
    end note
    note right of Distrutta
        Non applica più effetti
        (AC-23.4)
    end note
```

---

## 2. Diagramma di Attività — `advanceTick()`

Il diagramma descrive l'ordine delle fasi di un singolo tick, coerente con
[InternalSequenceDiagram1](InternalSequenceDiagram1.md). La **pre-pass** aggiorna i flag `powered` e
`connectedToRoad` di ogni struttura *prima* che `City.updateState()` li legga; l'ordine delle fasi
successive determina che i delta accumulati dagli edifici vengano risolti dalla politica attiva prima
dell'aggiornamento demografico e della notifica agli observer.

```mermaid
flowchart TD
    Start([Avvio tick]) --> PrePass["Pre-pass: per ogni struttura<br/>aggiorna powered e connectedToRoad<br/>(GridQueries)"]
    PrePass --> Structures["tickStructuresPhase():<br/>decayTick() + applyEffects()<br/>per ogni struttura"]
    Structures --> Parks["applyParkEffects():<br/>bonus felicità di prossimità (AC-28.2)<br/>e riduzione inquinamento (AC-28.3)"]
    Parks --> Capacity["tickCapacityPhase():<br/>popolazione &gt; capacità massima?"]
    Capacity -->|Sì| Over["setOverpopulated(true)<br/>(dimezza i bonus)"]
    Capacity -->|No| Policy
    Over --> Policy["activePolicy.getModifiers()"]
    Policy --> Resolve["CityState.resolveTick(modifiers):<br/>applica moltiplicatori, soglie,<br/>decadimento inquinamento, commit delta"]
    Resolve --> Quake{"Probabilità terremoto<br/>raggiunta? (1%)"}
    Quake -->|Sì| Trigger["triggerEarthquake():<br/>danno = 5 × magnitudo²"]
    Trigger --> Remove["Rimuove le strutture distrutte<br/>dalla griglia e dagli observer<br/>+ updateRoadConnections()"]
    Quake -->|No| Demo
    Remove --> Demo["tickDemographicsPhase():<br/>PopulationManager.updateDemographics()"]
    Demo --> Log["logTickSummary()"]
    Log --> Notify["notifyObservers():<br/>StateObserver.onStateChanged(state)"]
    Notify --> End([Fine tick: UI aggiornata])
```
