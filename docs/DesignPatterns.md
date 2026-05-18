# Analisi Architetturale e Design Pattern

Il progetto *CityLogic* fa un uso intensivo e metodico dei pattern di progettazione del software. Di seguito l'analisi dettagliata di come i pattern GoF e i principi GRASP si riflettono nel Class Diagram attuale.

## Pattern GoF (Gang of Four)

### 1. Strategy Pattern
* **Contesto**: Gestione delle politiche economiche della città.
* **Implementazione**: L'interfaccia `PolicyStrategy` definisce il contratto comune (es. `getModifiers()`). La classe `City` mantiene un riferimento all'interfaccia. Classi concrete come `DefaultPolicy`, `GreenPolicy` e `AusterityPolicy` incapsulano i diversi algoritmi e bonus fiscali/ambientali.
* **Vantaggi**: Rispetta il principio Open/Closed. Aggiungere una nuova politica economica richiede solo di creare una nuova classe che implementi `PolicyStrategy`, senza alterare il motore di simulazione.

### 2. Observer Pattern
* **Contesto**: Notifica di cambiamenti di stato e gestione dei disastri.
* **Implementazione**:
  * `StateObserver`: La UI (es. `DashboardView`) si registra presso `City` per ricevere aggiornamenti (`onStateChanged`) sul `CityState`.
  * `DisasterObserver`: Le classi `Structure` si registrano al `DisasterManager` per reagire agli eventi catastrofici (`onEarthquake`).
* **Vantaggi**: Permette una comunicazione uno-a-molti disaccoppiata. Il soggetto (`City` o `DisasterManager`) non conosce i dettagli implementativi dei suoi osservatori, garantendo alta flessibilità.

### 3. Decorator Pattern
* **Contesto**: Potenziamento fisico e tecnologico delle strutture.
* **Implementazione**: La classe astratta `StructureDecorator` estende `Structure` ma contiene anche un riferimento all'edificio stesso (`#wrapped: Structure`). Sottoclassi come `SeismicUpgrade` o `WasteThermalUpgrade` intercettano e alterano metodi (come `takeDamage`) prima di delegare le restanti chiamate alla struttura originale.
* **Vantaggi**: Permette di combinare potenziamenti dinamicamente a runtime, evitando una gerarchia di classi esplosiva basata sull'ereditarietà (es. evitando di dover creare classi statiche come `ResidentialBuildingAntisismico`).

### 4. Factory Pattern (Simple Factory)
* **Contesto**: Creazione delle strutture e applicazione dei potenziamenti.
* **Implementazione**: La classe `BuildingFactory` centralizza la logica di istanziazione. Espone metodi statici come `createBuilding(String)` e `applyUpgrade(Structure, String)`.
* **Vantaggi**: Il client (`GameController` o `SaveLoadManager`) è sgravato dalla complessità di creazione e non ha bisogno di conoscere le classi concrete (es. `new ResidentialBuilding()`), basandosi solo su stringhe identificative.

### 5. Template Method Pattern
* **Contesto**: Comportamento di base delle strutture.
* **Implementazione**: La classe astratta `Structure` definisce il "template" del comportamento. Contiene metodi concreti condivisi (es. `takeDamage()`) e definisce metodi astratti (`applyEffects()`) che le sottoclassi devono obbligatoriamente implementare.
* **Vantaggi**: Evita la duplicazione del codice di base, imponendo un contratto strutturale rigido per il calcolo specifico degli effetti di ogni entità.

---

## Principi GRASP (General Responsibility Assignment Software Patterns)

### 1. Controller
* **Implementazione**: `GameController` agisce da punto di ingresso tra la UI e il modello. Intercetta gli input (es. `placeBuilding`, `advanceTick`) e orchestra le operazioni delegandole a `City`, `Grid` e `BuildingFactory`, senza eseguire in proprio la logica di business.

### 2. Information Expert (Esperto delle Informazioni)
* **Implementazione**: Le responsabilità sono assegnate alle classi che possiedono i dati necessari per assolverle:
  * `CityState` è l'esperto per elaborare le metriche a fine turno (`resolveTick`) in base a budget e popolazione.
  * Ogni `Structure` è l'esperta per applicare i propri effetti specifici (`applyEffects`).
  * `Grid` gestisce la matrice spaziale ed espone la logica di interrogazione delle coordinate.

### 3. Creator
* **Implementazione**: `BuildingFactory` è il creatore logico delle strutture (`Structure`). La classe `City`, invece, funge da creatore per i propri componenti vitali (istanzia ad esempio la `Grid`, il `CityState` e il manager `PowerNetwork`).

### 4. High Cohesion (Alta Coesione)
* **Implementazione**: Le classi hanno un focus ben definito. `DisasterManager` gestisce unicamente le catastrofi, `PowerNetwork` gestisce unicamente la rete elettrica, `SaveLoadManager` si occupa unicamente della persistenza. Nessuna classe assume il ruolo di "God Object" in grado di fare tutto.

### 5. Low Coupling (Basso Accoppiamento)
* **Implementazione**: L'accoppiamento tra le entità è ridotto al minimo grazie all'uso di interfacce (`PolicyStrategy`, `StateObserver`, `Placeable`, `DisasterObserver`). `City` dipende da astrazioni e non da classi concrete, permettendo di alterare le politiche o i disastri senza ripercussioni.

### 6. Polymorphism (Polimorfismo)
* **Implementazione**: Ampiamente utilizzato nel cuore della simulazione. Quando `City` avanza il tempo, itera sulle entità e invoca `s.applyEffects(...)` su ogni singola struttura polimorficamente. Lo stesso avviene per le politiche invocando `activePolicy.getModifiers()`.

### 7. Pure Fabrication (Fabbricazione Pura)
* **Implementazione**: Classi di supporto e utilità come `GridQueries`, `BuildingFactory`, `SaveLoadManager`, `SaveDataMapper` e `SaveDataApplier` non esistono nel dominio logico di un ecosistema urbano reale. Sono state create appositamente dai progettisti per raggruppare comportamenti tecnici e preservare la coesione del modello.