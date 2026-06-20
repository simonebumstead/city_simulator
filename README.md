# CityLogic — City Simulator

**Sviluppatori:** Alessandro Piano, Simone Iantosca, Michele Coppari, Paolo Muraro  
**Tecnologie:** Java 17 · JavaFX 17 · Maven · Jackson · Ikonli

---

## Descrizione del progetto

CityLogic è un simulatore gestionale a turni sviluppato in Java con interfaccia grafica JavaFX, in cui il giocatore veste i panni del Sindaco di una città in crescita su una griglia 20×20. L'obiettivo è costruire, espandere e mantenere in salute una metropoli virtuale, bilanciando crescita economica, benessere dei cittadini e sostenibilità ambientale turno dopo turno.

Ogni decisione ha conseguenze concrete e durature: un'area industriale aumenta il budget e i posti di lavoro, ma fa crollare la felicità dei residenti e schizzare l'inquinamento. Un ospedale migliora la salute e favorisce la crescita demografica, ma pesa sul bilancio ogni tick. La sfida sta nel trovare la strategia giusta e nel saper reagire ai disastri imprevisti.

---

## Edifici e infrastrutture

La città si costruisce posizionando sulla griglia 8 tipi di struttura, ognuna con effetti specifici sulle metriche:

| Edificio | Costo | Effetti principali | Note |
|----------|------:|--------------------|------|
| **Strada** | 100 | Connette gli edifici alla rete viaria | Richiesta da Residenziali per funzionare |
| **Residenziale** | 500 | +budget (tasse), +felicità, +rifiuti/tick | Richiede strada adiacente e alimentazione |
| **Commerciale** | 750 | +budget, +felicità | Richiede strada adiacente e alimentazione |
| **Industriale** | 1.000 | +budget (elevato), +inquinamento, −felicità | Richiede strada adiacente e alimentazione |
| **Centrale Elettrica** | 2.000 | Alimenta tutti gli edifici entro raggio 5 | +inquinamento, −felicità |
| **Parco** | 300 | −inquinamento, +felicità, +salute | Bonus +2 felicità ai Residenziali nel raggio 3 |
| **Ospedale** | 1.200 | +salute, +felicità | Copre fino a 400 residenti; richiede alimentazione |
| **Centro Raccolta Rifiuti** | 900 | −rifiuti/tick | Controbilancia i rifiuti prodotti dai Residenziali |

Gli edifici non alimentati da una Centrale Elettrica non producono effetti e vengono evidenziati visivamente sulla mappa. Ogni struttura (eccetto le Strade) perde 1 HP per tick: se scende sotto il 20% dell'HP massimo viene segnalata come critica.

---

## Metriche della città

Lo stato della città è monitorato in tempo reale attraverso 6 metriche, tutte visibili nella dashboard con grafici time-series:

| Metrica | Range | Impatto |
|---------|-------|---------|
| **Budget** | illimitato | Necessario per costruire, riparare e demolire; negativo = bancarotta |
| **Popolazione** | min 10 | Cresce se felicità, salute e soddisfazioni sono alte; diminuisce altrimenti |
| **Felicità** | 0 – 100 | Influenzata da edifici, parchi, politiche e disastri |
| **Salute** | 0 – 100 | Dipende da ospedali, inquinamento e livello di rifiuti |
| **Inquinamento** | 0 – 100 | Generato da industrie e centrali; ridotto da parchi e Politica Verde |
| **Rifiuti** | 0+ | Prodotti da Residenziali; sopra 50 unità penalizzano salute e felicità |

La popolazione è ulteriormente descritta da tre soddisfazioni interne — **lavoro**, **salute** e **sicurezza** — che pesano sulla crescita demografica tick per tick.

---

## Politiche cittadine

Il giocatore può attivare in qualsiasi momento una delle 4 ordinanze disponibili, che alterano i moltiplicatori globali di tutte le metriche:

| Politica | Effetto principale | Trade-off |
|----------|--------------------|-----------|
| **Default** | Nessun modificatore | — |
| **Verde** | −50% inquinamento, +salute, +felicità | −200 budget/tick |
| **Combustibili Fossili** | +300 budget/tick, ×1.5 ricavi industriali | ×2 inquinamento, −salute |
| **Austerità** | +500 budget/tick | −15 felicità/tick, −2 salute/tick |

---

## Sistemi speciali

**Deterioramento e manutenzione** — Tutti gli edifici (eccetto le Strade) si deteriorano di 1 HP per tick. Il giocatore può ripararli al costo di `(maxHP − HP attuali) / 2`. Un edificio a 0 HP è distrutto e non produce effetti.

**Potenziamenti (Decorator Pattern)** — Ogni struttura può ricevere fino a 3 livelli di upgrade:
- *SeismicUpgrade* — dimezza i danni ricevuti dai terremoti
- *WasteThermalUpgrade* — applicabile solo ai Centri Raccolta; aggiunge −5 rifiuti e +50 budget/tick

**Terremoti** — Con probabilità 1% per tick si scatena un terremoto di magnitudo casuale. Il danno agli edifici è proporzionale a magnitudine², e si traduce in un calo immediato di felicità e salute. Gli edifici con SeismicUpgrade subiscono la metà del danno.

**Demolizione** — Il giocatore può demolire qualsiasi struttura pagando il 10% del suo costo; riceve in cambio il 60% (rimborso netto: +50% del costo originale).


---

## Installazione e avvio

### Prerequisiti
- **Java Development Kit (JDK) 17** o superiore
- **Apache Maven 3.6** o superiore

Le dipendenze JavaFX e le librerie grafiche vengono scaricate automaticamente da Maven al primo build; non è necessaria alcuna installazione manuale di JavaFX.

### Comandi

| Comando | Descrizione |
|---------|-------------|
| `mvn compile` | Compila il progetto |
| `mvn javafx:run` | Avvia il gioco |
| `mvn test` | Esegue tutti i test JUnit |
| `mvn -Dtest=NomeTest test` | Esegue una singola classe di test |
| `mvn clean package` | Build completo (compilazione + test + JAR) |
| `mvn surefire-report:report` | Genera il report HTML dei test in `target/site/` |

### Avvio rapido
```bash
git clone <url-repository>
cd city_simulator
mvn javafx:run
```

---

## Vincoli sull'ambiente

| Requisito | Versione minima | Note |
|-----------|-----------------|------|
| JDK | 17 | Versioni superiori compatibili; non testato con JDK < 17 |
| Apache Maven | 3.6 | Necessario per la gestione delle dipendenze e il plugin JavaFX |
| JavaFX | 17.0.8 | Incluso automaticamente via Maven (`org.openjfx`); non richiede installazione separata |
| Sistema operativo | Windows / macOS / Linux | Qualsiasi OS con supporto Java 17 |

**Nota tecnica:** il plugin Surefire è configurato con `useModulePath=false` nel `pom.xml` per garantire la compatibilità tra i moduli JavaFX e l'esecuzione classpath-based dei test JUnit.

---

## Librerie esterne utilizzate

| Libreria | Versione | Utilizzo nel progetto |
|----------|----------|-----------------------|
| **JavaFX Controls** (`org.openjfx:javafx-controls`) | 17.0.8 | Framework UI: layout, controlli (Button, Slider, ComboBox), grafici LineChart per la dashboard |
| **JavaFX FXML** (`org.openjfx:javafx-fxml`) | 17.0.8 | Supporto al caricamento di file FXML per la definizione delle viste |
| **Jackson Databind** (`com.fasterxml.jackson.core:jackson-databind`) | 2.15.2 | Serializzazione e deserializzazione JSON per il salvataggio e caricamento delle partite (`SaveLoadManager`) |
| **Ikonli JavaFX** (`org.kordamp.ikonli:ikonli-javafx`) | 12.3.1 | Integrazione di icone vettoriali SVG in componenti JavaFX (`FontIcon`) |
| **Ikonli FontAwesome 5** (`org.kordamp.ikonli:ikonli-fontawesome5-pack`) | 12.3.1 | Pack di icone FontAwesome 5 Solid usate nella mappa, toolbar e pannelli (`IconCatalog`) |
| **JUnit Jupiter** (`org.junit.jupiter`) | 5.10.0 | Framework per i test automatizzati (scope `test`); 151 test, 0 failure |

---

## API esterne

Il progetto non utilizza API HTTP/REST esterne né servizi di rete.

Tutte le funzionalità si basano su:
- **File system locale** — i salvataggi vengono scritti nella cartella `saves/` come file JSON (`save_<timestamp>.json`)
- **Librerie Maven locali** — le dipendenze sono risolte dal repository Maven Central al momento del build


---
## Documentazione
Tutta la documentazione è nella cartella [docs](docs).

- [**Design Document**](docs/DesignDocument.md): Architettura completa del sistema, diagrammi delle classi e diagrammi di sequenza.
- [**System Test Report**](docs/SystemTestReport.md): Report dettagliato dell'esecuzione dei test di sistema e verifica della copertura degli Acceptance Criteria.


