# Schützwerkstatt auf den Live-Seiten

2026-09-22. Öffentliche Demo `TAR-CircuitJS1-Demo`.

Stand aus TAR-Dervic Branch `cursor/reihenklemme-projektdatei-a208` (Reihenklemme, Potenzialschiene, Projektdatei). Der Lehrer-Link bleibt `hub.html` ohne diesen Eintrag.

Einstieg intern: [Hub intern](../hub-intern.html) · [Schützwerkstatt](../egt-hmi-stecken.html)

## Was auf Pages liegt

- `docs/egt-hmi-stecken.html` — aktuelle Werkstatt (Klemmen, Schiene, Projekt speichern/öffnen)
- `source/Circuitjs1/war/egt-hmi-stecken.html` — dieselbe HTML-Datei
- `EGTWorkbenchBridge.java` — Solver-Typen `terminal` / `rail`
- `docs/circuitjs1/*.cache.js` — frischer GWT-Compile, sonst kennt das Iframe die neuen Typen nicht

Hub, Tor-HMI, Pumpe-HMI und Workbench-HTML sind unverändert. Der GWT-Cache ist gemeinsam; CircuitJS-Seiten laden denselben Compile.
