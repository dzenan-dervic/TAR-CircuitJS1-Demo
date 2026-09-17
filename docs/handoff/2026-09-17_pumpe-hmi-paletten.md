# Handoff: Pumpen-HMI Farbpaletten

## Letztes Update

| Feld | Wert |
|---|---|
| **Datum** | 2026-09-17 |
| **Branch** | TAR-Dervic `main` uncommittet; Veröffentlichung in `TAR-CircuitJS1-Demo` |
| **Commit** | Demo `7604e36` auf `TAR-CircuitJS1-Demo` main, gepusht |
| **Git-Status** | Öffentliche Dateien in `.tmp_pages_publish/docs/` |
| **Session** | Professionelle Farbvarianten für Pumpen-HMI, verlinkt im Lernhub |

---

## Ziel und erreichtes Ergebnis

**Ziel:** Die Pumpensteuerung soll nicht mehr nach generischem Dunkelblau aussehen. Hub und HMI brauchen klare, professionelle Paletten. Die neuen Farben liegen unter Punkt 03 Pumpensteuerung.

**Ergebnis:** Vier Paletten, nur Farben, gleicher Aufbau:

| Parameter | Name auf dem Hub | Charakter |
|---|---|---|
| `palette=nacht` | Dunkel | Schaltschrank-Nacht, Anthrazit |
| `palette=tag` | Hell | Tageslicht, hellgraues Bedienpanel |
| `palette=graphit` | Graphit | Warmes Anthrazit, leicht grünlicher Metallton |
| `palette=kontrast` | Kontrast | Beamer/Tafel, hoher Kontrast |

Kopfleiste: Schalter **Hell / Kontrast** wechselt ohne Reload zwischen `tag-feld` und `kontrast`. Hub-Punkt 03 führt auf Hell Instrument; darunter nur noch die zwei Favoriten.

Alte Links `graphite` / `steel` / `light` werden intern auf `graphit` / `nacht` / `tag` umgebogen.

Helle I/O-Zeilen (nicht Grau auf Grau):

| Parameter | Name | I/O-Karten |
|---|---|---|
| `palette=tag` | Hell Panel | weiße Zeilen auf grauer Karte |
| `palette=tag-rahmen` | Hell Rahmen | weiße Zeilen, dunkler Haarstrich |
| `palette=tag-feld` | Hell Instrument | weiße Zeilen, stahlblauer Rahmen |

---

## Anforderungs- und Modulbezug

| Bereich | IDs / Referenzen |
|---|---|
| Lastenheft | VIS-02 |
| Module | HTML-Anlagenvisualisierung |
| Schaltungskatalog | LOGIK-08 Pumpensteuerung |
| Abnahmetests | Hub-Links, vier Paletten visuell, HMI-Alleinmodus |

---

## Geänderte Dateien und Komponenten

| Datei / Komponente | Änderung und Begründung |
|---|---|
| `.tmp_pages_publish/docs/egt-hmi-pumpe.html` | Chrome-Farben auf CSS-Variablen; vier Paletten |
| `.tmp_pages_publish/docs/hub.html` | Links Dunkel / Hell / Graphit / Kontrast unter Pumpensteuerung |
| `.tmp_pages_publish/docs/handoff/2026-09-17_lernhub.md` | Paletten nachgetragen |

---

## Technische Entscheidungen

| Entscheidung | Begründung | Auswirkung |
|---|---|---|
| Nur Farben, kein Layoutumbau | Nutzerauftrag | Bedienung unverändert |
| IEC-Rot/Grün/Gelb nicht entsättigen | Alarmfarben müssen lesbar bleiben | Paletten ändern Gehäuse, nicht die Bedeutung der Melder |
| Default = Dunkel (`:root` = nacht) | Bisheriges Tailwind-Slate wirkte unprofessionell | Auch ohne Query-Parameter neues Aussehen |
| Öffentlich in `TAR-CircuitJS1-Demo` | Nutzer: nicht privat | GitHub Pages nach Push |

```css
html.palette-tag { color-scheme: light; }
```

---

## Projekttagebuch für die TAR-Dokumentation

### Ausgangslage

Die Live-Pumpe nutzte ein dunkles Slate-Blau. Frühere Varianten Graphit/Stahlblau/Hellgrau änderten nur wenige Variablen; Karten, Header und Szene blieben blau.

### Vorgehen

1. Flächen, Knöpfe und Tags auf CSS-Variablen gelegt.
2. Vier Paletten als Token-Sätze definiert.
3. Hub-Punkt 03 um die vier Links ergänzt.
4. Lokal unter `127.0.0.1:8767` visuell geprüft.

### Problem und Lösung

| Problem / Beobachtung | Ursache | Lösung / Entscheidung | Ergebnis |
|---|---|---|---|
| Paletten wirkten fast gleich | Hardcodiertes Slate in der Zeichnung | Variablen für Szene, Karten, Knöpfe | Jede Palette färbt die ganze HMI |

### Abweichungen vom Plan

- Kein Layout-Redesign, keine Tor-Paletten in diesem Schritt.

### Zeitnachweis

| Datum / Zeitraum | Ist-Dauer | Schätzung Mensch (min) | Tätigkeit |
|---|---|---:|---|
| 2026-09-17 | nicht erfasst | 45 | Pumpen-HMI Paletten + Hub-Links |

---

## Build, Start und Prüfung

| Kommando / Prüfung | Ergebnis |
|---|---|
| Lokal `hub.html` | Links Dunkel/Hell/Graphit/Kontrast unter 03 |
| `egt-hmi-pumpe.html?hmiOnly=1&palette=nacht` | Anthrazit, Buttons lesbar |
| `…&palette=tag` | Hellgraues Panel, dunkle Schrift |
| `…&palette=graphit` | Warmes Anthrazit, grünlicher EIN-Knopf |
| `…&palette=kontrast` | Reines Schwarz, kräftigere Prozessfarben |

---

## Nachweise und Quellen

| Art | Repository-Pfad / Referenz | Verwendungszweck |
|---|---|---|
| Live | https://dzenan-dervic.github.io/TAR-CircuitJS1-Demo/hub.html | Öffentlicher Einstieg |
| HMI | `docs/egt-hmi-pumpe.html` im Demo-Repo | Paletten |

---

## Dokumentationsbaustein

Für die öffentliche Pumpen-HMI wurden vier Farbpaletten eingeführt. Sie ändern nur die Darstellung, nicht die Bedienung. Die Alarm- und Zustandfarben bleiben als Rot, Grün und Gelb erkennbar. Der Lernhub verlinkt die Varianten unter der Pumpensteuerung.

---

## Bekannte Probleme / Bugs

| ID | Beschreibung | Schwere | Status |
|---|---|---|---|
| — | Tor-HMI übernimmt Hell/Kontrast | — | erledigt, siehe `2026-09-17_tor-hmi-paletten.md` |

---

## Risiken, offene Fragen und bewusst nicht erledigt

- Arbeitsrepo `TAR-Dervic` wurde für die Paletten nicht committed.

---

## Direkter Wiedereinstieg / Nächste Schritte

1. Tor-Paletten: `docs/handoff/2026-09-17_tor-hmi-paletten.md`.

**Zum Weiterarbeiten zuerst öffnen:**

- `docs/handoff/2026-09-17_tor-hmi-paletten.md`
- `.tmp_pages_publish/docs/egt-hmi-tor.html`
- `.tmp_pages_publish/docs/hub.html`

---

## Änderungslog (kurz)

| Datum | Branch | Änderung |
|---|---|---|
| 2026-09-17 | Demo main | Vier Pumpen-Paletten, Hub-Links |

---

## Qualitätscheck vor Abschluss

- [x] Ergebnis und verbleibender Umfang sind eindeutig getrennt.
- [x] Betroffene IDs und Dateien sind genannt.
- [x] Tests enthalten Kommando und Ergebnis.
- [x] Ausgangslage, Vorgehen und Problem/Lösung sind für die TAR-Dokumentation nachvollziehbar.
- [x] Nachweise besitzen stabile Repository-Pfade.
- [x] Ist-Dauer nicht erfasst; Schätzung Mensch getrennt.
- [x] Dokumentationsbaustein vorhanden.
- [x] Risiken dokumentiert.
- [x] Branch und Git-Status aktuell.
- [x] Wiedereinstieg ohne Chatverlauf möglich.
