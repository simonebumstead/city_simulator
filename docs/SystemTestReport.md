# System Test Report — CityLogic City Simulator

**Data:** 2026-05-18  
**Versione:** 1.2  
**Metodo di verifica:** suite JUnit automatizzata (151 test, 0 failure) + ispezione del codice sorgente + verifiche manuali condotte durante le fasi di fix.

---

## Legenda

| Metodo | Descrizione |
|--------|-------------|
| JUnit | Verificato da un test automatizzato nella suite `src/test/` |
| Ispezione codice | Verificato tramite lettura del codice sorgente |
| Ispezione UI | Verificato tramite avvio del gioco (`mvn javafx:run`) |

---

## SCRUM-5 — Griglia 20×20

| ID AC | Acceptance Criteria | Riferimento / Test | Risultato | Note |
|-------|---------------------|--------------------|:---------:|------|
| AC-05.1 | La griglia è composta da 20×20 celle | `GridTest.testGridDimensions()` | **OK** | |
| AC-05.2 | Ogni cella contiene al massimo una struttura | `GridTest.testMaxOneStructurePerCell()` | **OK** | |
| AC-05.3 | Coordinate fuori da [0,19] restituiscono null | `GridTest.testOutOfBoundsReturnsNull()` | **OK** | |

---

## SCRUM-6 — Tick / Avanzamento temporale

| ID AC | Acceptance Criteria | Riferimento / Test | Risultato | Note |
|-------|---------------------|--------------------|:---------:|------|
| AC-06.1 | Il gioco avanza di un tick alla volta | Ispezione codice | **OK** | `City.advanceTick()` |
| AC-06.2 | Ogni tick aggiorna budget, popolazione, happiness, health, pollution, waste | `CityStateTest.testMetricsUpdateOnTick()` | **OK** | |
| AC-06.3 | La simulazione può essere avviata, messa in pausa e avanzata manualmente | Ispezione UI | **OK** | `SimulationControlsBar` |

---

## SCRUM-7 — Posizionamento edifici + Factory Pattern

| ID AC | Acceptance Criteria | Riferimento / Test | Risultato | Note |
|-------|---------------------|--------------------|:---------:|------|
| AC-07.1 | Si piazza un edificio in una cella libera | `GameControllerTest.testPlaceBuildingOnEmptyCell()` | **OK** | |
| AC-07.2 | Non è possibile piazzare in cella già occupata | `GameControllerTest.testPlaceBuildingFailsOnOccupiedCell()` | **OK** | |
| AC-07.3 | Il budget si riduce del costo di costruzione | `GameControllerTest.testBudgetReducedOnPlacement()` | **OK** | |
| AC-07.4 | I residenziali richiedono una Road adiacente | `GameControllerTest.testResidentialRequiresAdjacentRoad()` | **OK** | |

---

## SCRUM-8 — Salvataggio/caricamento JSON

| ID AC | Acceptance Criteria | Riferimento / Test | Risultato | Note |
|-------|---------------------|--------------------|:---------:|------|
| AC-08.1 | Salvataggio/caricamento manuale funzionante (round-trip) | `SaveLoadManagerTest.testRoundTripSaveLoad()` | **OK** | |
| AC-08.2 | Save/load preserva metriche, edifici e upgrade | `SaveLoadManagerTest.testMetricsAndStructuresPreserved()` | **OK** | |
| AC-08.3 | File JSON corrotto genera errore gestito, l'app non va in crash | `SaveLoadManagerTest.testCorruptedJsonHandledGracefully()` | **OK** | Aggiunto in Fase B |
| AC-08.4 | Il tick corrente viene salvato e ripristinato | `SaveLoadManagerTest.testCurrentTickSavedAndRestored()` | **OK** | |
| AC-08.5 | Autosave ogni X tick con toggle on/off in UI | `SaveLoadManagerTest.testAutosaveToggle()` + UI | **OK** | Aggiunto in Fase B + Fase C; CheckBox in `SimulationControlsBar` |

---

## SCRUM-9 — Calcolo Felicità, Inquinamento, Salute

| ID AC | Acceptance Criteria | Riferimento / Test | Risultato | Note |
|-------|---------------------|--------------------|:---------:|------|
| AC-09.1 | Happiness influenzata da edifici, parchi e policy attiva | `CityStateTest.testHappinessInfluencedByFactors()` | **OK** | |
| AC-09.2 | Pollution generata da industrie/centrali, ridotta da parchi e GreenPolicy | `CityStateTest.testPollutionCalculations()` | **OK** | |
| AC-09.3 | Health influenzata da ospedali e livello di inquinamento | `BuildingTest.testHealthImpactFromPollutionAndHospitals()` | **OK** | |
| AC-09.4 | Tutte le metriche clampate nell'intervallo [0, 100] | `CityStateTest.testMetricsClampedBetweenZeroAndHundred()` | **OK** | |

---

## SCRUM-10 — Dashboard con grafici JavaFX

| ID AC | Acceptance Criteria | Riferimento / Test | Risultato | Note |
|-------|---------------------|--------------------|:---------:|------|
| AC-10.1 | La dashboard mostra grafici time-series per tutte le metriche | Ispezione UI | **OK** | `DashboardChart` |
| AC-10.2 | I grafici si aggiornano ad ogni tick | Ispezione UI | **OK** | Pattern Observer — `StateObserver` |

---

## SCRUM-11 — Terremoti + Observer Pattern

| ID AC | Acceptance Criteria | Riferimento / Test | Risultato | Note |
|-------|---------------------|--------------------|:---------:|------|
| AC-11.1 | Probabilità terremoto = 1% per tick | `DisasterManagerTest.testEarthquakeProbability()` | **OK** | `EARTHQUAKE_PROBABILITY = 0.01` |
| AC-11.2 | Danno inflitto = 5 × magnitudo² (formula calibrata in Fase C) | `DisasterManagerTest.testEarthquakeDamageFormula()` | **OK** | Moltiplicatore aggiornato da 1× a 5× in Fase C |
| AC-11.3 | Tutti gli edifici vengono notificati tramite Observer Pattern | `DisasterManagerTest.testObserverNotificationOnEarthquake()` | **OK** | |
| AC-11.4 | Avviso visivo in dashboard al verificarsi del terremoto | Ispezione UI | **OK** | `DashboardView.showEarthquakeWarning()` |

---

## SCRUM-15 — Politiche cittadine + Strategy Pattern

| ID AC | Acceptance Criteria | Riferimento / Test | Risultato | Note |
|-------|---------------------|--------------------|:---------:|------|
| AC-15.1 | Disponibili 4 politiche: Default, Green, FossilFuel, Austerity | `PolicyTest.testAvailablePolicies()` | **OK** | |
| AC-15.2 | Ogni politica applica i propri moltiplicatori e modificatori flat | `PolicyTest.testPolicyModifiersApplied()` | **OK** | |
| AC-15.3 | La policy attiva viene applicata ad ogni tick tramite `resolveTick()` | `CityStateTest.testActivePolicyResolvedOnTick()` | **OK** | |

---

## SCRUM-18 — Notifica cambio policy

| ID AC | Acceptance Criteria | Riferimento / Test | Risultato | Note |
|-------|---------------------|--------------------|:---------:|------|
| AC-18.1 | Il giocatore può cambiare policy durante la partita | `GameControllerTest.testChangePolicyReplacesActive()` | **OK** | Aggiunto in Fase B |
| AC-18.2 | Viene mostrato un avviso testuale nel log al cambio policy | Ispezione codice | **OK** | `SimulationControlsBar.setActivePolicy()` — `metricsPanel.log(...)` |
| AC-18.3 | `changePolicy(null)` ripristina DefaultPolicy | `GameControllerTest.testChangePolicyNullRestoresDefault()` | **OK** | Aggiunto in Fase B |

---

## SCRUM-20 — Strade + adiacenza

| ID AC | Acceptance Criteria | Riferimento / Test | Risultato | Note |
|-------|---------------------|--------------------|:---------:|------|
| AC-20.1 | Le strade si piazzano su qualsiasi cella libera | `GameControllerTest.testRoadPlacement()` | **OK** | |
| AC-20.2 | Commerciali con road generano più reddito di quelli isolati | `GameControllerTest.testCommercialWithRoadGeneratesMoreIncomeThanIsolated()` | **OK** | Aggiunto in Fase B |
| AC-20.3 | Dopo demolizione della road, edifici adiacenti risultano non connessi al tick successivo | `GameControllerTest.testCommercialIsolatedAfterRoadDemolitionNoIncome()` | **OK** | Aggiunto in Fase B |
| AC-20.4 | Le strade non subiscono decadimento né danni | `RoadTest.testRoadHasNoDecayOrDamage()` | **OK** | Aggiunto in Fase B |

---

## SCRUM-21 — Demolizione edifici

| ID AC | Acceptance Criteria | Riferimento / Test | Risultato | Note |
|-------|---------------------|--------------------|:---------:|------|
| AC-21.1 | Il giocatore demolisce pagando il 10% del costo originale | `GameControllerTest.testDemolishCostTenPercent()` | **OK** | |
| AC-21.2 | Il rimborso netto è il 50% del costo originale | `GameControllerTest.testDemolishNetRefundFiftyPercent()` | **OK** | Fix Javadoc allineato in Fase A |
| AC-21.3 | Demolizione impossibile se budget < costo di demolizione | `GameControllerTest.testDemolishFailsIfBudgetInsufficient()` | **OK** | |

---

## SCRUM-23 — Deterioramento + manutenzione

| ID AC | Acceptance Criteria | Riferimento / Test | Risultato | Note |
|-------|---------------------|--------------------|:---------:|------|
| AC-23.1 | Ogni edificio perde 1 HP per tick | `StructureTest.testHpDecayPerTick()` | **OK** | |
| AC-23.2 | HP < 20% maxHp → edificio segnalato come critico nell'UI | Ispezione codice | **OK** | `City.java`, `MetricsPanel` |
| AC-23.3 | Riparazione singola costa (maxHp − hp) / 2 | `GameControllerTest.testSingleRepairCost()` | **OK** | |
| AC-23.4 | Edificio con HP = 0 non applica effetti durante il tick | `BuildingTest.testZeroHpAppliesNoEffects()` | **OK** | |
| AC-23.5 | Riparazione globale (`repairAll`) scala costo totale dal budget | `GameControllerTest.testRepairAllSuccess()` | **OK** | Aggiunto in Fase B |

---

## SCRUM-24 — Potenziamento con Decorator Pattern

| ID AC | Acceptance Criteria | Riferimento / Test | Risultato | Note |
|-------|---------------------|--------------------|:---------:|------|
| AC-24.1 | SeismicUpgrade applicabile a qualsiasi edificio | `GameControllerTest.testSeismicUpgradeApplicability()` | **OK** | |
| AC-24.2 | SeismicUpgrade dimezza il danno subito dall'edificio | Ispezione codice | **OK** | `SeismicUpgrade.takeDamage()` |
| AC-24.3 | WasteThermalUpgrade applicabile solo a WasteManagementCenter | `GameControllerTest.testWasteThermalUpgradeApplicability()` | **OK** | |
| AC-24.4 | Massimo 3 livelli di decorator annidati per edificio | Ispezione codice | **OK** | `GameController.upgradeBuilding()` |
| AC-24.5 | Round-trip save/load con 2 upgrade impilati preserva livello e HP | `SaveLoadManagerTest.testSaveLoadDoubleDecoratorRoundTrip()` | **OK** | Aggiunto in Fase B |

---

## SCRUM-25 — Variazione demografica

| ID AC | Acceptance Criteria | Riferimento / Test | Risultato | Note |
|-------|---------------------|--------------------|:---------:|------|
| AC-25.1 | La popolazione cresce/declina in base a happiness, health e soddisfazioni | `PopulationManagerTest.testPopulationGrowthAndDecline()` | **OK** | |
| AC-25.2 | Job satisfaction = min(100, (industriali+commerciali)×100/residenziali) | `PopulationManagerTest.testJobSatisfactionFormula()` | **OK** | |
| AC-25.3 | Health satisfaction = min(100, ospedali×400/residenziali) | `PopulationManagerTest.testHealthSatisfactionFormula()` | **OK** | |
| AC-25.4 | La popolazione non scende sotto il minimo di 10 abitanti | `PopulationManagerTest.testPopulationMinimumLimit()` | **OK** | |
| AC-25.5 | Soddisfazioni clampate in [0, 100] | `PopulationGroupTest.testSatisfactionsClamped()` | **OK** | Aggiunto in Fase B |

---

## SCRUM-26 — Ospedale + Salute

| ID AC | Acceptance Criteria | Riferimento / Test | Risultato | Note |
|-------|---------------------|--------------------|:---------:|------|
| AC-26.1 | Ospedale alimentato: +10 health, +0.5 happiness, −25 budget/tick | `BuildingTest.testPoweredHospitalEffects()` | **OK** | |
| AC-26.2 | Ospedale non alimentato: nessun effetto sulle metriche | `BuildingTest.testUnpoweredHospitalNoEffects()` | **OK** | |

---

## SCRUM-27 — Centrale Elettrica + copertura

| ID AC | Acceptance Criteria | Riferimento / Test | Risultato | Note |
|-------|---------------------|--------------------|:---------:|------|
| AC-27.1 | La centrale produce 250 unità di energia per tick | `BuildingTest.testPowerPlantEnergyProduction()` | **OK** | |
| AC-27.2 | Edifici entro raggio Chebyshev 5 ricevono alimentazione | `GridQueriesTest.testPowerRangeCoverage()` | **OK** | |
| AC-27.3 | La dashboard segnala BLACKOUT se consumo > produzione | Ispezione UI | **OK** | `MetricsPanel` |
| AC-27.4 | Le celle non alimentate mostrano un indicatore visivo | Ispezione UI | **OK** | `MapGridView` |
| AC-27.5 | PowerNetwork accumula produzione/consumo e si azzera ad ogni tick | `PowerNetworkTest.testNetworkAccumulationAndReset()` | **OK** | Aggiunto in Fase B |

---

## SCRUM-28 — Parchi + Felicità

| ID AC | Acceptance Criteria | Riferimento / Test | Risultato | Note |
|-------|---------------------|--------------------|:---------:|------|
| AC-28.1 | Parco applica +1.5 happiness, +1.0 health, −0.5 pollution/tick | `BuildingTest.testParkEffects()` | **OK** | |
| AC-28.2 | Residenziali entro raggio Chebyshev 3 ricevono +2 happiness/tick | Ispezione codice | **OK** | `City.applyParkEffects()` |
| AC-28.3 | Parco riduce la pollution globale di −3/tick | Ispezione codice | **OK** | `City.java` |

---

## SCRUM-30 — Centri Raccolta + Rifiuti

| ID AC | Acceptance Criteria | Riferimento / Test | Risultato | Note |
|-------|---------------------|--------------------|:---------:|------|
| AC-30.1 | Ogni residenziale genera +1 unità di rifiuti per tick | `BuildingTest.testResidentialWasteGeneration()` | **OK** | |
| AC-30.2 | WasteLevel > 50 aumenta pollution e riduce happiness | `CityStateTest.testHighWasteIncreasesPollution()` | **OK** | |
| AC-30.3 | WasteManagementCenter alimentato riduce −10 rifiuti/tick | `BuildingTest.testPoweredWasteCenterReducesWaste()` | **OK** | |
| AC-30.4 | WasteThermalUpgrade: −5 rifiuti aggiuntivi e +50 budget/tick | Ispezione codice | **OK** | `WasteThermalUpgrade.java`; costante rinominata in Fase A |

---

## Riepilogo

| Totale AC testate | OK | KO |
|:-----------------:|:--:|:--:|
|        67         | 67 | 0 |

Tutte le acceptance criteria risultano soddisfatte.

### Modifiche rispetto alla versione 1.0

| Fase | Modifica                                                                                 |
|------|------------------------------------------------------------------------------------------|
| Fase A | Fix Javadoc demolizione (AC-21.2): allineamento commento con formula `60%−10%=50% netto` |
| Fase A | Rimosso codice morto `addIndustrialPollutionDelta` / `getLastIndustrialPollutionDelta`   |
| Fase A | `repairAll()` spostato da UI a `GameController` (fix violazione MVC)                     |
| Fase B | Aggiunto test AC-08.3 (save corrotto) e AC-08.5 (autosave round-trip)                    |
| Fase B | Aggiunto test AC-24.5 (Decorator doppio round-trip save/load)                            |
| Fase B | Aggiunto test AC-20.2/AC-20.3 (road → income, demolizione → isolamento)                  |
| Fase B | Aggiunto test AC-23.5 (`repairAll` con verifica costo)                                   |
| Fase B | Aggiunte classi `PowerNetworkTest`, `RoadTest`, `PopulationGroupTest`                    |
| Fase B | Aggiunto test AC-18.1 / AC-18.3 (cambio policy e ripristino Default)                 |
| Fase C | Danno terremoto calibrato: moltiplicatore da 1× a 5× (AC-11.2)                           |
| Fase C | Toggle autosave on/off aggiunto in UI (AC-08.5)                                          |
| Fase C | Documentate soglie asimmetriche `PopulationManager` (design intent)                      |
