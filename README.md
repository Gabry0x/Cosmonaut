# Cosmonaut

Un plugin Minecraft (Paper) che aggiunge l'esplorazione spaziale: razzi, pianeti, gravità ridotta e atmosfera aliena.

## Caratteristiche

- **Razzi** — Crafta e lancia razzi per viaggiare tra i pianeti
- **Pianeti** — Luna e Marte con generazione del terreno personalizzata
- **Gravità** — Gravità ridotta sui pianeti alieni
- **Atmosfera** — Effetti atmosferici e danno da esposizione
- **Comandi** — Gestione completa tramite comandi in-game

## Requisiti

- Java 21+
- Paper 1.21+

## Installazione

1. Scarica il `.jar` dalla sezione Releases
2. Copia il file nella cartella `plugins/` del server
3. Riavvia il server
4. Modifica `plugins/Cosmonaut/config.yml` se necessario

## Comandi

| Comando | Descrizione |
|---------|-------------|
| `/cosmonaut setplanet <pianeta>` | Imposta il mondo come pianeta |
| `/cosmonaut list` | Lista dei pianeti registrati |
| `/cosmonaut tp <pianeta>` | Teletrasportati su un pianeta |
| `/cosmonaut give <item>` | Ottieni un oggetto speciale |
| `/cosmonaut reload` | Ricarica la configurazione |
| `/lancio` | Avvia la sequenza di lancio del razzo |

## Configurazione

`config.yml` — Multipicatori di gravità, impostazioni atmosferiche e cooldown di lancio.

`zones.yml` — Definizione delle zone spaziali.

## Build

```bash
./gradlew build
```

Il `.jar` verrà generato in `build/libs/`.

## Versione

`1.0.0-Alpha`
