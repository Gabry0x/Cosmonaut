# Cosmonaut

Plugin Paper per Minecraft 1.21 che aggiunge esplorazione spaziale: razzi craftabili, pianeti con terreno personalizzato, gravità ridotta e atmosfera aliena con danno da esposizione.

**Versione:** 1.4.0-Alpha — **Autore:** ItaliaRevenge

---

## Requisiti

- Java 21+
- Paper 1.21+

---

## Installazione

1. Scarica il `.jar` dalla sezione Releases
2. Copia nella cartella `plugins/` del server
3. Riavvia il server — i mondi `luna` e `marte` vengono creati automaticamente
4. Modifica `plugins/Cosmonaut/config.yml` se necessario

---

## Sistema Razzi

### Crafting

Il razzo si crafta con questa ricetta a forma (3x3):

```
 I
IGI
ILI
```

| Simbolo | Materiale |
|---------|-----------|
| `I` | Iron Block |
| `G` | Gunpowder |
| `L` | Lava Bucket |

Produce un **Firework Rocket** con tag NBT personalizzato (`cosmonaut:cosmonaut_rocket`). Gli item normali senza il tag non funzionano come razzi.

### Piazzamento

Con il razzo in mano, fai **click destro su un blocco**. Il plugin costruisce automaticamente la struttura fisica nella posizione sopra (14 blocchi di altezza, impronta 5×5 ai booster):

```
         [■]              ← punta nera (y+13)
       [■][■][■]          ← transizione naso (y+12)
     [▓][▓][■][▓][▓]      ← corpo superiore (y+10..11)
     [▓][▓][░][▓][▓]      ← banda metallica grigia (y+9)
     [▓][▓][■][▓][▓]      ← corpo (y+8)
     [▓][□][□][□][▓]      ← finestre arancio (y+6..7)
     [▓][▓][■][▓][▓]      ← corpo (y+5)
     [▓][▓][░][▓][▓]      ← banda metallica grigia (y+4)
[■]  [▓][▓][■][▓][▓]  [■] ← corpo + booster (y+0..3)
[■]  [▓][▓][■][▓][▓]  [■]
```

| Simbolo | Materiale |
|---------|-----------|
| `■` | Black Concrete |
| `▓` | Orange Concrete |
| `░` | Gray Concrete |
| `□` | Orange Stained Glass |

La colonna centrale (y+0 → y+13) deve essere completamente libera (aria), altrimenti il piazzamento viene bloccato. Il razzo viene rimosso dall'inventario al piazzamento.

Dopo il piazzamento viene mostrato il messaggio:
> *"Razzo posizionato! Usa `/cosmonaut setplanet <nome>` per impostare la destinazione, poi `/lancio` per partire."*

### Impostare la destinazione

```
/cosmonaut setplanet <nome_pianeta>
```

Associa al razzo più vicino (entro 15 blocchi) il pianeta di destinazione. Se non trova un razzo registrato, cerca automaticamente una struttura valida entro 10 blocchi in X/Z e 5 in Y e la registra. I razzi vengono salvati in `rockets.yml` con coordinate e destinazione.

### Avviare il lancio

```
/lancio
```

Prima di avviare, il plugin esegue questi controlli in ordine:

1. Il sender è un giocatore (non console)
2. Ha il permesso `cosmonaut.use`
3. Non è già in volo
4. Non è in cooldown
5. C'è un razzo registrato entro **15 blocchi**
6. Il razzo ha una destinazione configurata
7. La struttura fisica è ancora intatta (Orange Concrete + Gray Concrete + Black Concrete tip)

Se la struttura è stata distrutta o modificata, il razzo viene de-registrato automaticamente.

### Sequenza di volo

| Fase | Durata | Cosa succede |
|------|--------|-------------|
| **Countdown** | 10 secondi | Titolo a schermo con numeri (giallo → rosso negli ultimi 3s), suono pling a ogni secondo, particelle di fumo alla base del razzo |
| **Decollo** | ~5 secondi (100 tick) | Il giocatore passa in modalità Spectator + invulnerabile; la struttura fisica viene distrutta e il razzo de-registrato; il giocatore viene teleportato verso l'alto ogni tick con velocità crescente (`0.4 + tick × 0.015` blocchi/tick); particelle `FLAME` e `SMOKE` compaiono 3 blocchi sotto come scarico |
| **Transizione** | ~2.5 secondi | Schermata nera (title invisibile) |
| **Arrivo** | — | Teleport allo spawn del mondo di destinazione; ripristino del GameMode originale; titolo "Benvenuto su [Pianeta]"; applicazione del cooldown |

Se il mondo di destinazione non è caricato, il giocatore viene comunque resettato (GameMode + invulnerabilità) e riceve un messaggio di errore.

### Cooldown

Dopo ogni lancio riuscito viene applicato un cooldown configurabile (default 60 secondi). Il tempo rimanente viene mostrato se si tenta `/lancio` troppo presto. Il cooldown è in memoria e si azzera al riavvio del server.

### Persistenza

I razzi piazzati vengono salvati in `plugins/Cosmonaut/rockets.yml` e ricaricati ad ogni avvio. Se un mondo non è più disponibile al caricamento, il razzo corrispondente viene scartato.

---

## Pianeti

### Pianeti di default

| Pianeta | World | Gravità | Atmosfera |
|---------|-------|---------|-----------|
| Luna | `luna` | 0.4× | No |
| Marte | `marte` | 0.5× | No |

I mondi vengono creati automaticamente all'avvio con generatore personalizzato. Se il mondo esiste già su disco, viene caricato senza modificare i chunk esistenti. Spawn predefinito: `0, 70, 0`.

### Aggiungere pianeti

Aggiungi una sezione in `config.yml`:

```yaml
planets:
  mioPianeta:
    world-name: nome_cartella_mondo
    gravity-multiplier: 0.6   # 1.0 = Terra, < 1.0 = gravità ridotta
    has-atmosphere: false
```

I pianeti senza generatore dedicato (`luna` / `marte`) useranno il generatore vanilla.

---

## Sistema Atmosfera

Ogni secondo (ogni 20 tick) il plugin controlla tutti i giocatori online. Se un giocatore si trova in un pianeta con `has-atmosphere: false`, viene applicato **danno da esposizione** a meno che:

- Abbia il permesso `cosmonaut.bypass`
- Indossi il **Casco Spaziale** (slot testa)
- Si trovi dentro una **Zona Pressurizzata**

### Meccanica danno

- Se l'aria rimanente è > 0: viene sottratta di 15 unità per tick (ogni secondo)
- Se l'aria è 0: viene inflitto **1 cuore di danno** (2.0 danni) ogni secondo
- Se protetto: l'aria viene ripristinata di 20 unità per tick fino al massimo

### Casco Spaziale

Iron Helmet con tag NBT `cosmonaut:space_helmet`. Protegge completamente dall'assenza di atmosfera finché è nello slot testa. Ottenibile con `/cosmonaut give helmet`.

### Zone Pressurizzate

Il **Generatore di Pressione** (Beacon con tag `cosmonaut:pressurizer`) crea una bolla di aria respirabile quando piazzato. Il raggio è configurabile (`pressurized-radius`, default 10 blocchi). Distruggere il blocco rimuove la zona. Le zone sono persistenti in `zones.yml`.

---

## Sistema Gravità

Ogni tick (ogni singolo tick, non ogni secondo) il plugin applica una **forza contraria** alla caduta per i giocatori che si trovano in un pianeta con `gravity-multiplier < 1.0`:

```
forza_contraria = 0.08 × (1.0 - gravity_multiplier)
```

La forza viene sommata alla velocità Y del giocatore solo quando è in aria (non a terra, non in volo creativo). Con gravità 0.4 (Luna), la forza contraria è `0.08 × 0.6 = 0.048` unità/tick. I giocatori con permesso `cosmonaut.bypass` non sono influenzati.

---

## Oggetti Speciali

| Oggetto | Base | Comando give | Funzione |
|---------|------|--------------|---------|
| Razzo | Firework Rocket | `rocket` | Si crafta o si ottiene via comando; si piazza con click destro |
| Casco Spaziale | Iron Helmet | `helmet` | Protegge dall'atmosfera aliena |
| Generatore di Pressione | Beacon | `pressurizer` | Crea zona pressurizzata al piazzamento |

Tutti gli oggetti usano **PersistentDataContainer** (NBT) per essere identificati — non basta il materiale base.

---

## Comandi

### Comandi giocatore

| Comando | Permesso | Descrizione |
|---------|----------|-------------|
| `/lancio` | `cosmonaut.use` | Avvia la sequenza di lancio del razzo più vicino |

### Comandi admin

| Comando | Permesso | Descrizione |
|---------|----------|-------------|
| `/cosmonaut list` | `cosmonaut.use` | Lista pianeti con stato (caricato/non), gravità e atmosfera |
| `/cosmonaut setplanet <nome>` | `cosmonaut.admin` | Imposta/modifica la destinazione del razzo più vicino |
| `/cosmonaut tp <pianeta>` | `cosmonaut.admin` | Teletrasporto diretto allo spawn del pianeta |
| `/cosmonaut give <tipo>` | `cosmonaut.admin` | Dà un oggetto speciale (`rocket`, `helmet`, `pressurizer`) |
| `/cosmonaut reload` | `cosmonaut.admin` | Ricarica `config.yml` senza riavviare |

Tutti i sottocomandi di `/cosmonaut` hanno tab-completion.

---

## Permessi

| Nodo | Default | Descrizione |
|------|---------|-------------|
| `cosmonaut.use` | Tutti | Accesso a `/lancio` e `/cosmonaut list` |
| `cosmonaut.admin` | OP | Accesso a `setplanet`, `tp`, `give`, `reload` |
| `cosmonaut.bypass` | OP | Ignora gravità ridotta e danno atmosferico |

---

## Configurazione

### `config.yml`

```yaml
launch-cooldown: 60          # Secondi di attesa tra un lancio e l'altro
pressurized-radius: 10       # Raggio in blocchi delle zone pressurizzate

planets:
  luna:
    world-name: luna
    gravity-multiplier: 0.4  # 40% della gravità terrestre
    has-atmosphere: false
  marte:
    world-name: marte
    gravity-multiplier: 0.5  # 50% della gravità terrestre
    has-atmosphere: false
```

### File di dati (auto-generati)

| File | Contenuto |
|------|-----------|
| `rockets.yml` | Razzi piazzati (coordinate + destinazione) |
| `zones.yml` | Zone pressurizzate (coordinate + mondo) |

---

## Build

```bash
./gradlew build
```

Il `.jar` viene generato in `build/libs/`. Richiede Java 21.
