# Schützwerkstatt auf den Live-Seiten

2026-09-22. Stand TAR-Dervic `cursor/reihenklemme-projektdatei-a208` (`75d17fe`). Lehrer-Hub bleibt ohne Schützwerkstatt.

Einstieg intern: [Hub intern](../hub-intern.html) · [Schützwerkstatt](../egt-hmi-stecken.html)

## Was neu ist

- Toolbar **Drehstrom 400 V** (`mains`, Kennbuchstabe T, L1/L2/L3/N/PE)
- `EGTWorkbenchBridge` Typ `mains` → vorhandenes Dump 439
- Frischer GWT-Cache, sonst kennt das Iframe `mains` nicht

24-V-Selbsthaltung, Reihenklemme und Potenzialschiene bleiben.

## Bridge (22.09. später)

Stecken darf das CircuitJS-Iframe nicht nach `left:-12000px` schieben (Safari lädt es dann nicht). `circuitjs.html?v=20260922bridge` plus `nocache.js?v=…`. Alte `*.cache.js` bleiben liegen, falls ein Telefon noch den vorigen nocache hat.

