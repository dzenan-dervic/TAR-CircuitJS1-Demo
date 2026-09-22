# Schützwerkstatt auf den Live-Seiten

2026-09-22. Stand TAR-Dervic `cursor/drehstrommotor-werkstatt-a208` (`56aa6c6`). Lehrer-Hub bleibt ohne Schützwerkstatt.

Einstieg intern: [Hub intern](../hub-intern.html) · [Schützwerkstatt](../egt-hmi-stecken.html)

## Was neu ist

- Toolbar **Drehstrommotor** (`acmotor`, Kennbuchstabe M, U1 V1 W1 / U2 V2 W2 / PE)
- `EGTWorkbenchBridge` Typ `acmotor` → Dump 459, Stern/Dreieck durch Adern
- Frischer GWT-Cache (`582070CE…` Safari), sonst kennt das Iframe `acmotor` nicht

400 V Einspeisung, 24-V-Selbsthaltung, Reihenklemme und Potenzialschiene bleiben.

## Bridge

Stecken darf das CircuitJS-Iframe nicht nach `left:-12000px` schieben (Safari lädt es dann nicht). `circuitjs.html?v=20260922motor` plus `nocache.js?v=…`. Alte `*.cache.js` bleiben liegen, falls ein Telefon noch den vorigen nocache hat.

iPhone: **neuer Tab**, Pull-to-refresh reicht nicht.
