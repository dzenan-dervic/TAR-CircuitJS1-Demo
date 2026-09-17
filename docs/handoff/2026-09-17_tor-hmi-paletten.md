# Handoff: Tor-HMI Farbpaletten

## Letztes Update

| Feld | Wert |
|---|---|
| **Datum** | 2026-09-17 |
| **Branch** | TAR-Dervic `main` uncommittet; Veröffentlichung in `TAR-CircuitJS1-Demo` |
| **Commit** | Demo-Repo, nach diesem Handoff |
| **Git-Status** | Öffentliche Dateien in `.tmp_pages_publish/docs/`; lokale Kopie `Circuitjs1/war/egt-hmi-tor.html` |
| **Session** | Hell Instrument / Kontrast und Kopfleisten-Schalter für die SPS-Rolltorsteuerung |

---

## Ziel und erreichtes Ergebnis

**Ziel:** Dieselbe Farbbehandlung wie bei der Pumpen-HMI auf die SPS-Rolltorsteuerung übertragen: Tokens, Hub-Links unter Punkt 02, Schalter Hell/Kontrast in der Kopfleiste.

**Ergebnis:** Chrome der Tor-HMI läuft über dieselben Tokens wie die Pumpe. Favoriten am Hub und im Header:

| Parameter | Name | Charakter |
|---|---|---|
| `palette=tag-feld` | Hell Instrument | weiße I/O-Zeilen, stahlblauer Rahmen |
| `palette=kontrast` | Kontrast | Beamer/Tafel, hoher Kontrast |

Default ohne Query bleibt `nacht` (Anthrazit). Tor-Portal, Lamellen, Endschalter-Gelb, Sicherheitsleiste und Ampel bleiben Anlagenfarben und folgen nicht der Palette.

---

## Anforderungs- und Modulbezug

| Bereich | IDs / Referenzen |
|---|---|
| Lastenheft | VIS-02 |
| Module | HTML-Anlagenvisualisierung |
| Schaltungskatalog | LOGIK-07 Torsteuerung |
| Abnahmetests | Hub 02, Header-Schalter, Hell/Kontrast visuell, AUF im Testbetrieb |

---

## Geänderte Dateien und Komponenten

| Datei / Komponente | Änderung und Begründung |
|---|---|
| `.tmp_pages_publish/docs/egt-hmi-tor.html` | CSS-Tokens, Paletten nacht/tag-feld/kontrast, Header-Schalter |
| `.tmp_pages_publish/docs/hub.html` | Punkt 02 wie Pumpe: Hauptlink Hell Instrument plus zwei Varianten |
| `Circuitjs1/war/egt-hmi-tor.html` | gleiche Datei für den lokalen Simulator-Start |
| `docs/handoff/2026-09-17_tor-hmi-paletten.md` | dieser Handoff |

---

## Technische Entscheidungen

| Entscheidung | Begründung | Auswirkung |
|---|---|---|
| Gleiche Tokens wie Pumpe | Nutzer: ähnliches Farbenkonzept | Hub und Header wirken einheitlich |
| Nur nacht / tag-feld / kontrast | Favoriten der Pumpe, kein Extra-Graphit am Tor | URL `palette=tag-feld` und `palette=kontrast` |
| Anlagenfarben hart | Tor muss als Tor lesbar bleiben | Himmel/Rasen, Lamellen, LS, Leiste, Ampel unverändert |
| `history.replaceState` ohne Reload | wie Pumpe | Schalter ändert URL, Simulation läuft weiter |

```css
html.palette-tag-feld { --row: #ffffff; --row-line: #2f6f88; }
html.palette-kontrast { --bg: #0e0e0e; --row: #000000; }
```

---

## Projekttagebuch für die TAR-Dokumentation

### Ausgangslage

Die öffentliche Tor-HMI nutzte noch Tailwind-Slate (`#0b0f19` / `#1e293b`). Die Pumpe hatte bereits Paletten und den Hell/Kontrast-Schalter.

### Vorgehen

1. `:root` durch nacht/tag-feld/kontrast ersetzt, Chrome-Hex auf Variablen gelegt.
2. Header-Schalter Hell/Kontrast wie in der Pumpe eingebaut.
3. Hub-Punkt 02 auf `palette=tag-feld` gelegt, darunter Hell Instrument und Kontrast.
4. Lokal unter `127.0.0.1:8770` geprüft: Hub-Links, Hell, Kontrast, AUF im Testbetrieb.

### Problem und Lösung

| Problem / Beobachtung | Ursache | Lösung / Entscheidung | Ergebnis |
|---|---|---|---|
| Slate blieb in Karten und I/O | Hardcodierte Hex-Werte | Chrome auf `--card` / `--row` / `--row-line` | Paletten färben Gehäuse, nicht die Anlage |

### Abweichungen vom Plan

- Keine zusätzlichen Paletten tag/graphit am Tor; nur die zwei Favoriten plus Default nacht.

### Zeitnachweis

| Datum / Zeitraum | Ist-Dauer | Schätzung Mensch (min) | Tätigkeit |
|---|---|---:|---|
| 2026-09-17 | nicht erfasst | 45 | Tor-HMI Paletten + Hub-Links + Header-Schalter |

---

## Build, Start und Prüfung

| Kommando / Prüfung | Ergebnis |
|---|---|
| `py -3 -m http.server 8770` in `.tmp_pages_publish/docs` | lokal erreichbar |
| `hub.html` Punkt 02 | Hell Instrument und Kontrast unter Torsteuerung |
| `egt-hmi-tor.html?hmiOnly=1&palette=tag-feld` | Hell, Schalter Hell gedrückt, I/O weiß mit stahlblauem Rahmen |
| Klick Kontrast | URL `palette=kontrast`, dunkles Gehäuse, Anlage unverändert |
| Klick AUF im Testbetrieb | Tor fährt auf (51 % geprüft), I/O-LEDs folgen |

---

## Nachweise und Quellen

| Art | Repository-Pfad / Referenz | Verwendungszweck |
|---|---|---|
| Live nach Pages-Build | https://dzenan-dervic.github.io/TAR-CircuitJS1-Demo/hub.html | Öffentlicher Einstieg |
| HMI | `docs/egt-hmi-tor.html` im Demo-Repo | Paletten und Schalter |
| Vorbild | `docs/handoff/2026-09-17_pumpe-hmi-paletten.md` | gleiche Tokens |

---

## Dokumentationsbaustein

Für die öffentliche SPS-Rolltorsteuerung wurden dieselben Gehäuse-Paletten wie bei der Pumpe eingeführt. Hell Instrument und Kontrast sind über den Lernhub und über einen Schalter in der Kopfleiste erreichbar. Die Anlagenfarben von Tor, Endschaltern, Sicherheitsleiste und Ampel bleiben fest.

---

## Bekannte Probleme / Bugs

| ID | Beschreibung | Schwere | Status |
|---|---|---|---|
| — | Arbeitsrepo TAR-Dervic für diese HTML-Dateien nicht committed | niedrig | bewusst nicht |

---

## Risiken, offene Fragen und bewusst nicht erledigt

- Kein Graphit/tag-rahmen am Tor.
- TAR-Dervic-Commit nicht angefordert.

---

## Direkter Wiedereinstieg / Nächste Schritte

1. Demo-Repo pushen und GitHub Pages abwarten.
2. Bei Bedarf dieselben Tokens auf weitere HMIs.

**Zum Weiterarbeiten zuerst öffnen:**

- `docs/handoff/2026-09-17_tor-hmi-paletten.md`
- `.tmp_pages_publish/docs/egt-hmi-tor.html`
- `.tmp_pages_publish/docs/hub.html`

---

## Änderungslog (kurz)

| Datum | Branch | Änderung |
|---|---|---|
| 2026-09-17 | Demo main | Tor-Paletten Hell/Kontrast, Hub-Links, Header-Schalter |

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
