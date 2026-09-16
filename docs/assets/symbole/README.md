# Zielsymbole (Planarten)

PNG-Dateien hier ablegen — die Seite `docs/symbole-vergleich.html` lädt sie automatisch.

| Ordner / Datei | Planart |
|---|---|
| `zusammenhaengend/` | Zusammenhängende Darstellung (allpolig, Installationsoptik) |
| `uebersicht/` | Übersichtsschaltplan (einpolig / Übersicht) |
| `progress.json` | Fortschritt ZH / ÜS / **CJS** (gilt nach Commit in jedem Browser) |

**Dateiname** = Symbol-`id` aus der HTML-Tabelle, z. B.:

- `Switch2Elm.png` (vorhandene CircuitJS-Klasse)
- `Abzweigdose.png` (neues EGT-Bauteil)

## Upload in der HTML-Seite

1. Seite in **Chrome/Edge** öffnen  
2. Oben **„Ordner Zusammenhängend…“** / **„Ordner Übersicht…“** → jeweiligen Ordner wählen  
3. Pro Zelle **Hochladen** → Bild wird mit richtigem Namen gespeichert  
4. Ohne Ordner-Verknüpfung: Datei wird heruntergeladen → manuell hierhin verschieben  
5. Nach Abhaken ZH / ÜS / **CJS**: **Fortschritt speichern** → `progress.json` aktualisieren und committen  

**CJS** = in CircuitJS erledigt (EGT-Zeichnung importiert, Klasse angepasst oder getestet). Unabhängig von den Planart-PNGs.

Nach dem Ablegen die HTML-Seite neu laden (bei Ordner-Write oft sofort sichtbar in der Vorschau).
