# TAR CircuitJS1 – öffentliche Demo

Dieses Repository enthält die statische, im Browser ausführbare Demo der CircuitJS1-Erweiterung für Energie- und Gebäudetechnik. Das private TAR-Projektrepository mit Pflichtenheft und Arbeitsdokumentation ist **nicht** Bestandteil dieser Veröffentlichung.

- [CircuitJS1 starten](https://dzenan-dervic.github.io/TAR-CircuitJS1-Demo/)
- [Torsteuerung mit HMI](https://dzenan-dervic.github.io/TAR-CircuitJS1-Demo/egt-hmi-tor.html)
- [Pumpensteuerung mit HMI](https://dzenan-dervic.github.io/TAR-CircuitJS1-Demo/egt-hmi-pumpe.html)

Die Website liegt in `docs/`. Der zugehörige bearbeitbare Java-/GWT-Quellcode, die Build-Konfiguration und die statischen Ausgangsdateien liegen in `source/Circuitjs1/`. Die kompilierten JavaScript-Dateien unter `docs/circuitjs1/` wurden aus diesem Quellcode erstellt. Die Demo-Dateien wurden für GitHub Pages um die lokale PWA-Registrierung gekürzt; die Schaltungssimulation selbst ist unverändert.

Zum erneuten Bauen: JDK 11 und Gradle 8.7 verwenden, dann in `source/Circuitjs1` `gradlew compileGwt` ausführen. Die GWT-Ausgabe liegt anschließend unter `build/gwt/out/circuitjs1/`. Die fertige GitHub-Pages-Seite besteht aus dieser Ausgabe und den statischen Dateien unter `war/`. `shortrelay.php` ist bei GitHub Pages nicht verfügbar; die Simulatorfunktion benötigt es nicht.

CircuitJS1 ist freie Software unter der [GNU GPL, Version 2 oder neuer](COPYING.txt). Urheber- und Lizenzhinweise des ursprünglichen Projekts stehen auch in `source/Circuitjs1/README.md`.
