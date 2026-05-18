# System Test Report — CityLogic City Simulator

**Data:** 2026-05-18  
**Versione:** 1.0  
**Metodo di verifica:** suite JUnit automatizzata (110 test, 0 failure) + ispezione del codice sorgente + verifiche manuali condotte durante le fasi di fix.

---

## Legenda

| Metodo | Descrizione |
|--------|-------------|
| JUnit | Verificato da un test automatizzato nella suite `src/test/` |
| Ispezione codice | Verificato tramite lettura del codice sorgente |
| Ispezione UI | Verificato tramite avvio del gioco (`mvn javafx:run`) |

---

## SCRUM-5 — Griglia 20×20

| ID AC | Acceptance Criteria | Metodo | Risultato | Note |
|-------|---------------------|--------|:---------:|------|
| AC-05.1 | La griglia è composta da 20×20 celle | GridTest | **OK** | |
| AC-05.2 | Ogni cella contiene al massimo una struttura | GridTest | **OK** | |
| AC-05.3 | Coordinate fuori da [0,19] restituiscono null | GridTest | **OK** | |

---

## SCRUM-6 — Tick / Avanzamento temporale

| ID AC | Acceptance Criteria | Metodo | Risultato | Note |
|-------|---------------------|--------|:---------:|------|
| AC-06.1 | Il gioco avanza di un tick alla volta | Ispezione codice | **OK** | `City.advanceTick()` |
| AC-06.2 | Ogni tick aggiorna budget, popolazione, happiness, health, pollution, waste | CityStateTest | **OK** | |
| AC-06.3 | La simulazione può essere avviata, messa in pausa e avanzata manualmente | Ispezione UI | **OK** | `SimulationControlsBar` |

---

## SCRUM-7 — Posizionamento edifici + Factory Pattern

| ID AC | Acceptance Criteria | Metodo | Risultato | Note |
|-------|---------------------|--------|:---------:|------|
| AC-07.1 | Si piazza un edificio in una cella libera | GameControllerTest | **OK** | |
| AC-07.2 | Non è possibile piazzare in cella già occupata | GameControllerTest | **OK** | |
| AC-07.3 | Il budget si riduce del costo di costruzione | GameControllerTest | **OK** | |
| AC-07.4 | I residenziali richiedono una Road adiacente | GameControllerTest | **OK** | |

---

## SCRUM-8 — Salvataggio/caricamento JSON

| ID AC | Acceptance Criteria | Metodo | Risultato | Note |
|-------|---------------------|--------|:---------:|------|
| AC-08.1 | Autosave ogni 5 tick | Ispezione codice | **OK** | `City.advanceTick()` |
| AC-08.2 | Salvataggio/caricamento manuale funzionante (round-trip) | SaveLoadManagerTest | **OK** | |
| AC-08.3 | File JSON corrotto genera errore gestito, l'app non va in crash | SaveLoadManagerTest | **OK** | Aggiunto in Fase 2 |

---

## SCRUM-9 — Calcolo Felicità, Inquinamento, Salute

| ID AC | Acceptance Criteria | Metodo | Risultato | Note |
|-------|---------------------|--------|:---------:|------|
| AC-09.1 | Happiness influenzata da edifici, parchi e policy attiva | CityStateTest | **OK** | |
| AC-09.2 | Pollution generata da industrie/centrali, ridotta da parchi e GreenPolicy | CityStateTest | **OK** | |
| AC-09.3 | Health influenzata da ospedali e livello di inquinamento | BuildingTest | **OK** | |
| AC-09.4 | Tutte le metriche clampate nell'intervallo [0, 100] | CityStateTest | **OK** | |

---

## SCRUM-10 — Dashboard con grafici JavaFX

| ID AC | Acceptance Criteria | Metodo | Risultato | Note |
|-------|---------------------|--------|:---------:|------|
| AC-10.1 | La dashboard mostra grafici time-series per tutte le metriche | Ispezione UI | **OK** | `DashboardChart` |
| AC-10.2 | I grafici si aggiornano ad ogni tick | Ispezione UI | **OK** | Pattern Observer — `StateObserver` |

---

## SCRUM-11 — Terremoti + Observer Pattern

| ID AC | Acceptance Criteria | Metodo | Risultato | Note |
|-------|---------------------|--------|:---------:|------|
| AC-14.1 | Probabilità terremoto = 1% per tick | DisasterManagerTest | **OK** | `EARTHQUAKE_PROBABILITY = 0.01` |
| AC-14.2 | Danno inflitto = magnitudine² (formula quadratica) | DisasterManagerTest | **OK** | |
| AC-14.3 | Tutti gli edifici vengono notificati tramite Observer Pattern | DisasterManagerTest | **OK** | |

---

## SCRUM-15 — Politiche cittadine + Strategy Pattern

| ID AC | Acceptance Criteria | Metodo | Risultato | Note |
|-------|---------------------|--------|:---------:|------|
| AC-15-P.1 | Disponibili 4 politiche: Default, Green, FossilFuel, Austerity | PolicyTest | **OK** | |
| AC-15-P.2 | Ogni politica applica i propri moltiplicatori e modificatori flat | PolicyTest | **OK** | |
| AC-15-P.3 | La policy attiva viene applicata ad ogni tick tramite `resolveTick()` | CityStateTest | **OK** | |

---

## SCRUM-18 — Notifica cambio policy

| ID AC | Acceptance Criteria | Metodo | Risultato | Note |
|-------|---------------------|--------|:---------:|------|
| AC-18-P.1 | Il giocatore può cambiare policy durante la partita | Ispezione UI | **OK** | `SimulationControlsBar` |
| AC-18-P.2 | Viene mostrato un avviso testuale nel log al cambio policy | Ispezione codice | **OK** | Verificato Fase 1 — `SimulationControlsBar:165` |

---

## SCRUM-20 — Strade + adiacenza

| ID AC | Acceptance Criteria | Metodo | Risultato | Note |
|-------|---------------------|--------|:---------:|------|
| AC-20.1 | Le strade si piazzano su qualsiasi cella libera | GameControllerTest | **OK** | |
| AC-20.2 | Commerciali/industriali generano budget solo se adiacenti a una Road | BuildingTest | **OK** | |
| AC-20.3 | Le strade non subiscono decadimento né danni | StructureTest | **OK** | `Road.takeDamage()` è no-op |

---

## SCRUM-21 — Demolizione edifici

| ID AC | Acceptance Criteria | Metodo | Risultato | Note |
|-------|---------------------|--------|:---------:|------|
| AC-21.1 | Il giocatore demolisce pagando il 10% del costo originale | GameControllerTest | **OK** | |
| AC-21.2 | Il rimborso netto è il 50% del costo originale | GameControllerTest | **OK** | Fixato in Fase 1 |
| AC-21.3 | Demolizione impossibile se budget < costo di demolizione | GameControllerTest | **OK** | |

---

## SCRUM-23 — Deterioramento + manutenzione

| ID AC | Acceptance Criteria | Metodo | Risultato | Note |
|-------|---------------------|--------|:---------:|------|
| AC-15.1 | Ogni edificio perde 1 HP per tick | StructureTest | **OK** | |
| AC-15.2 | HP < 20% maxHp → edificio segnalato come critico nell'UI | Ispezione codice | **OK** | `City.java`, `MetricsPanel` |
| AC-15.3 | Riparazione costa (maxHp − hp) / 2 | GameControllerTest | **OK** | |
| AC-15.4 | Edificio con HP = 0 non applica effetti durante il tick | BuildingTest | **OK** | |

---

## SCRUM-24 — Potenziamento con Decorator Pattern

| ID AC | Acceptance Criteria | Metodo | Risultato | Note |
|-------|---------------------|--------|:---------:|------|
| AC-16.1 | SeismicUpgrade applicabile a qualsiasi edificio | GameControllerTest | **OK** | |
| AC-16.2 | SeismicUpgrade dimezza il danno subito dall'edificio | Ispezione codice | **OK** | `SeismicUpgrade.takeDamage()` |
| AC-16.3 | WasteThermalUpgrade applicabile solo a WasteManagementCenter | GameControllerTest | **OK** | |
| AC-16.4 | Massimo 3 livelli di decorator annidati per edificio | Ispezione codice | **OK** | `GameController.upgradeBuilding()` |

---

## SCRUM-25 — Variazione demografica

| ID AC | Acceptance Criteria | Metodo | Risultato | Note |
|-------|---------------------|--------|:---------:|------|
| AC-19.1 | La popolazione cresce/declina in base a happiness, health e soddisfazioni | PopulationManagerTest | **OK** | |
| AC-19.2 | Job satisfaction = min(100, (industriali+commerciali)×100/residenziali) | PopulationManagerTest | **OK** | |
| AC-19.3 | Health satisfaction = min(100, ospedali×200/residenziali) | PopulationManagerTest | **OK** | |
| AC-19.4 | La popolazione non scende sotto il minimo di 10 abitanti | PopulationManagerTest | **OK** | |

---

## SCRUM-26 — Ospedale + Salute

| ID AC | Acceptance Criteria | Metodo | Risultato | Note |
|-------|---------------------|--------|:---------:|------|
| AC-26.1 | Ospedale alimentato: +10 health, +0.5 happiness, −25 budget/tick | BuildingTest | **OK** | |
| AC-26.2 | Ospedale non alimentato: nessun effetto sulle metriche | BuildingTest | **OK** | |

---

## SCRUM-27 — Centrale Elettrica + copertura

| ID AC | Acceptance Criteria | Metodo | Risultato | Note |
|-------|---------------------|--------|:---------:|------|
| AC-27.1 | La centrale produce 250 unità di energia per tick | BuildingTest | **OK** | |
| AC-27.2 | Edifici entro raggio Chebyshev 5 ricevono alimentazione | GridQueriesTest | **OK** | |
| AC-27.3 | La dashboard segnala BLACKOUT se consumo > produzione | Ispezione UI | **OK** | `MetricsPanel` |
| AC-27.4 | Le celle non alimentate mostrano un indicatore visivo | Ispezione UI | **OK** | Verificato Fase 1 — `MapGridView:369` |

---

## SCRUM-28 — Parchi + Felicità

| ID AC | Acceptance Criteria | Metodo | Risultato | Note |
|-------|---------------------|--------|:---------:|------|
| AC-28.1 | Parco applica +1.5 happiness, +1.0 health, −0.5 pollution/tick | BuildingTest | **OK** | |
| AC-28.2 | Residenziali entro raggio Chebyshev 3 ricevono +2 happiness/tick | Ispezione codice | **OK** | `City.applyParkEffects()` |
| AC-28.3 | Parco riduce la pollution globale di −3/tick | Ispezione codice | **OK** | `City.java` |

---

## SCRUM-30 — Centri Raccolta + Rifiuti

| ID AC | Acceptance Criteria | Metodo | Risultato | Note |
|-------|---------------------|--------|:---------:|------|
| AC-18.1 | Ogni residenziale genera +1 unità di rifiuti per tick | BuildingTest | **OK** | |
| AC-18.2 | WasteLevel > 50 aumenta pollution e riduce happiness | CityStateTest | **OK** | |
| AC-18.3 | WasteManagementCenter alimentato riduce −10 rifiuti/tick | BuildingTest | **OK** | |
| AC-18.4 | WasteThermalUpgrade: −5 rifiuti aggiuntivi e +50 budget/tick | Ispezione codice | **OK** | `WasteThermalUpgrade.java` |

---

## Riepilogo

| Totale AC testate | OK | KO |
|:-----------------:|:--:|:--:|
| 57 | 57 | 0 |

Tutte le acceptance criteria risultano soddisfatte. Le uniche correzioni applicate durante il progetto riguardano SCRUM-21 AC-21.2 (rimborso demolizione portato al 50% netto, Fase 1) e SCRUM-8 AC-08.3 (gestione file corrotto verificata e coperta da test, Fase 2).
