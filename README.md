## CityLogic
### Sviluppatori: Alessandro Piano, Simone Iantosca, Michele Coppari, Paolo Muraro.

Citylogic è un simulatore gestionale di città sviluppato in Java, in cui il giocatore veste i panni del Sindaco. L'obiettivo principale è costruire, espandere e mantenere in salute una metropoli virtuale, bilanciando attentamente la crescita economica con il benessere dei cittadini e la tutela dell'ambiente.

Ogni scelta ha un peso: costruire una nuova area industriale aumenterà le entrate cittadine e i posti di lavoro, ma farà crollare la Felicità e schizzare alle stelle l'Inquinamento. La sfida sta nel trovare la strategia perfetta per far prosperare la città turno dopo turno.

Funzionalità Principali (Gameplay):
Pianificazione Urbana su Griglia: Costruisci la tua metropoli su una mappa tattica 20x20. Posiziona strategicamente Strade per le connessioni, Edifici (Residenziali, Commerciali, Industriali) per l'economia e Infrastrutture (Centrali Elettriche, Parchi) per il supporto.

Motore di Simulazione Dinamico: Un sistema a "Tick" (avanzamento temporale) ricalcola costantemente lo stato della città (Budget, Popolazione, Inquinamento e Felicità) in base agli edifici attivi.

Sistema di Politiche (Ordinanze): Guida lo sviluppo strategico attivando leggi cittadine. Adotta una "Politica Verde" per abbattere le emissioni a discapito delle tasse, o spingi sull' "Espansione Industriale" per massimizzare i profitti.

Eventi Casuali: Testa la resilienza della tua pianificazione urbana affrontando disastri improvvisi, come i terremoti, che possono danneggiare gli edifici e avere ripercussioni sulle statistiche globali.

Persistenza dello Stato: Salva e carica le tue partite localmente per riprendere la simulazione in qualsiasi momento.

## Documentazione
Tutta la documentazione è nella cartella [docs](docs).

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
| **JUnit Jupiter** (`org.junit.jupiter`) | 5.10.0 | Framework per i test automatizzati (scope `test`); 110 test, 0 failure |

---

## API esterne

Il progetto non utilizza API HTTP/REST esterne né servizi di rete.

Tutte le funzionalità si basano su:
- **File system locale** — i salvataggi vengono scritti nella cartella `saves/` come file JSON (`save_<timestamp>.json`)
- **Librerie Maven locali** — le dipendenze sono risolte dal repository Maven Central al momento del build
