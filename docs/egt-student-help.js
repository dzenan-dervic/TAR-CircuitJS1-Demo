(function (root) {
  'use strict';

  const steps = {
    tor: [
      ['Dein Arbeitsplatz', 'Links baust du die Schaltung und das FUP-Programm. Rechts siehst du das Rolltor und bedienst die Anlage.', 'I bezeichnet einen SPS-Eingang, Q einen SPS-Ausgang. Die Tabelle folgt deiner Verdrahtung.'],
      ['Schaltung aufbauen', 'Wähle oben die Übung oder das vollständig verdrahtete Beispiel. Im EGT-Menü findest du Taster, Sensoren, Schütze und SPS/FUP.', 'Drücke W und ziehe eine Leitung von Anschluss zu Anschluss. Ein Doppelklick auf ein Bauteil öffnet seine Einstellungen.'],
      ['Tor bedienen', 'Halte Taster AUF oder ZU gedrückt und beobachte Schütze, Motor und Tor. Prüfe auch STOP, Not-Aus und „Hindernis stellen“.', 'Automatisch / Anlage: Die Anlage betätigt den Sensor. Handtest: Du hältst ihn betätigt oder unbetätigt, unabhängig von der Anlage. Danach wieder Automatisch wählen.'],
      ['Signale verstehen', 'Grün in der I/O-Tabelle bedeutet Signal EIN, Grau bedeutet AUS. Eine Sensorbetätigung ist noch kein elektrisches SPS-Signal.', 'Ein Schütz kann angezogen sein, während der Motor steht. Prüfe Versorgung und Verdrahtung. Strg+Z macht eine Bearbeitung rückgängig. Gelöschte Sensoren findest du unter EGT → Sensoren.']
    ],
    pumpe: [
      ['Dein Arbeitsplatz', 'Links baust du die Schaltung und das FUP-Programm. Rechts siehst du Brunnen, Pumpe und Behälter.', 'I bezeichnet einen SPS-Eingang, Q einen SPS-Ausgang. Die Tabelle folgt deiner Verdrahtung.'],
      ['Schaltung aufbauen', 'Wähle oben die Übung „Verdrahtung + FUP“ oder das vollständige Beispiel. Im EGT-Menü findest du Bauteile und SPS/FUP.', 'Drücke W und ziehe eine Leitung von Anschluss zu Anschluss. Ein Doppelklick auf ein Bauteil öffnet seine Einstellungen.'],
      ['Pumpe ausprobieren', 'Halte den Taster EIN gedrückt. Beobachte Pumpe und Füllstand. Mit Förderleistung änderst du das Fülltempo; „Zapfen“ gedrückt halten entnimmt Wasser.', 'Automatisch / Anlage: Der Wasserstand betätigt den Sensor. Handtest hält ihn betätigt oder unbetätigt, unabhängig vom Wasserstand. Danach wieder Automatisch wählen.'],
      ['Signale und Fehler prüfen', 'Grün in der I/O-Tabelle bedeutet Signal EIN, Grau bedeutet AUS. Sensorbetätigung, Kontaktstellung und SPS-Signal sind unterschiedliche Zustände.', 'Bei Stillstand Versorgung, Motorschutz und Brunnen prüfen. Strg+Z macht eine Bearbeitung rückgängig. Gelöschte Schwimmerschalter findest du unter EGT → Sensoren.']
    ],
    werkstatt: [
      ['Im Aufbau beginnen', 'Im Aufbau ist die Spannung aus. Wähle links ein Bauteil und platziere es auf der Montageplatte.', 'Klicke auf das Bauteil, um seine Eigenschaften zu bearbeiten. Am Gehäuse oder Namen kannst du es verschieben.'],
      ['Klemmen verbinden', 'Klicke zwei Anschlussklemmen nacheinander an oder ziehe eine Leitung zwischen ihnen. Die ausgewählte Aderfarbe gilt für neue Leitungen.', 'Esc bricht eine begonnene Verbindung ab. Jede Verbindungsklemme verbindet alle ihre Anschlüsse; separate Klemmen bleiben getrennt.'],
      ['Betrieb einschalten', 'Betrieb schaltet die Versorgung ein. Halte Taster gedrückt und beobachte die Reaktion. Ein rastender Schalter behält seine Stellung.', 'Einfrieren hält die Simulationszeit an. Die Spannung bleibt bestehen. Erneutes Betätigen lässt die Simulation weiterlaufen.'],
      ['Korrigieren und speichern', 'Rückgängig oder Strg+Z nimmt die letzte Bearbeitung zurück. Bei Kurzschluss: Fehlerweg prüfen, Leitung korrigieren und Reset betätigen.', 'Speichern lädt eine Projektdatei herunter. Über Projekt → Projekt öffnen kannst du sie wieder laden. Die ausführliche Werkstatthilfe erklärt weitere Funktionen.']
    ]
  };
  const circuitLegend = [
    ['signal-on', 'Signal EIN', 'Grüne Punkte in der I/O-Tabelle zeigen ein aktives elektrisches SPS-Signal.'],
    ['signal-off', 'Signal AUS', 'Graue Punkte in der I/O-Tabelle zeigen ein inaktives SPS-Signal.'],
    ['sensor', 'Sensor betätigt', 'Eine grüne Markierung am Sensor zeigt seine Betätigung. Öffner öffnen dabei, Schließer schließen. Das SPS-Signal hängt zusätzlich von der Verdrahtung ab.'],
    ['contactor', 'Schütz angezogen', 'Ein grüner Rahmen zeigt die angesteuerte Schützspule. Er bestätigt noch keine Motorversorgung oder Bewegung.'],
    ['fup-on', 'FUP: 1 / EIN', 'Grüne FUP-Signalleitungen und grüne I/Q-Beschriftungen zeigen ein aktives Programmsignal. Grau an der Signalleitung bedeutet 0 / AUS.'],
    ['selection', 'Auswahl im Simulator', 'Türkis markiert ausgewählte Bauteile oder Leitungen. Diese Farbe ist kein EIN-Signal.'],
    ['junction', 'Leitungsverbindung', 'Ein Punkt an einer Kreuzung bedeutet verbunden. Eine Kreuzung ohne Punkt bedeutet nicht verbunden.'],
    ['named', 'Gleicher Signalname', 'Gleichnamige FUP-Signalverbinder verbinden Netzwerke miteinander, wie eine Leitung. Sie erzeugen selbst kein Signal.'],
    ['current', 'Elektrischer Strom', 'Wandernde gelbe Punkte auf elektrischen Leitungen zeigen Stromfluss. Die Farben elektrischer Leitungen zeigen das Potenzial; grün heißt dort nicht pauschal EIN.']
  ];
  const workshopLegend = [
    ['selection-workshop', 'Bauteil oder Klemme ausgewählt', 'Blaue Ringe markieren die Auswahl. Das ist keine Spannungs- oder Durchgangsprüfung.'],
    ['target', 'Mögliches Leitungsziel', 'Ein grüner Klemmenring zeigt beim Verdrahten ein mögliches Ziel für die neue Ader.'],
    ['fault', 'Kurzschluss erkannt', 'Rote Klemmenringe und die rote Kontur zeigen den gespeicherten Fehlerweg. Verbindungen korrigieren, danach Reset betätigen.'],
    ['voltage', 'Spannungsanzeige', 'Ein heller Reflex markiert Potenzial gegenüber dem Bezugspunkt. Das Leuchten beweist keinen Stromfluss.'],
    ['current', 'Stromanzeige', 'Gelbe Würfel bewegen sich in der angezeigten Stromrichtung. Stromtempo verändert nur die Darstellung, nicht die Simulation.']
  ];
  const conductors = [
    ['l1', 'Braun · L1'], ['l2', 'Schwarz · L2'], ['l3', 'Grau · L3'], ['n', 'Blau · N'], ['pe', 'Grün-Gelb · PE']
  ];

  function legendSymbol(kind) {
    if (kind === 'voltage' || kind === 'current') {
      const live = kind === 'voltage' ? ' volt-live' : '';
      const conductor = '<path class="wire-shadow" d="M14 24H114"/><path class="wire-volt' + live + '" d="M14 24H114"/><path class="wire-line" d="M14 24H114" stroke="#6b3f24"/><path class="wire-highlight' + live + '" d="M14 24H114"/>';
      const flow = kind === 'current' ? '<defs><clipPath id="studentHelpCurrentClip"><rect x="14" y="16" width="100" height="16"/></clipPath></defs><g clip-path="url(#studentHelpCurrentClip)"><g class="student-help-flow">' + [-38,10,58,106,154].map(x => '<rect class="wire-dot" x="' + x + '" y="20" width="8" height="8"/>').join('') + '</g></g>' : '';
      return '<svg class="student-help-wire-demo" viewBox="0 0 128 48" aria-hidden="true">' + conductor + flow + '</svg>';
    }
    const label = {'fup-on':'1','junction':'●','named':'FUP_AUF','fault':'!','contactor':'Q','sensor':'B'}[kind] || '';
    return '<span class="student-help-symbol ' + kind + '" aria-hidden="true">' + label + '</span>';
  }

  function createState(area, storage) {
    if (!steps[area]) throw new Error('Unbekannter Hilfebereich: ' + area);
    const key = 'egt.student-help.v1.' + area;
    let seen = false;
    try { seen = storage?.getItem(key) === 'seen'; } catch (_) { /* Help also works with blocked storage. */ }
    let step = 0, view = 'start';
    return {
      get step() { return step; }, get view() { return view; }, get seen() { return seen; },
      open(nextView = 'start') { view = nextView === 'legend' ? 'legend' : 'start'; step = 0; },
      next() { step = Math.min(steps[area].length - 1, step + 1); },
      back() { step = Math.max(0, step - 1); },
      dismiss() { seen = true; try { storage?.setItem(key, 'seen'); } catch (_) {} }
    };
  }

  function mount(doc) {
    const entry = doc.querySelector('[data-student-help]');
    if (!entry || typeof doc.createElement('dialog').showModal !== 'function') return;
    const area = entry.dataset.studentHelp;
    let storage;
    try { storage = root.localStorage; } catch (_) {}
    const state = createState(area, storage);
    const dialog = doc.createElement('dialog');
    dialog.className = 'student-help';
    dialog.id = 'studentHelp';
    dialog.setAttribute('aria-labelledby', 'studentHelpTitle');
    entry.setAttribute('aria-controls', dialog.id);
    entry.setAttribute('aria-haspopup', 'dialog');
    dialog.innerHTML = '<div class="student-help-head"><h2 id="studentHelpTitle">Hilfe</h2><button type="button" data-help-close aria-label="Hilfe schließen">Schließen</button></div>' +
      '<div class="student-help-nav" role="group" aria-label="Hilfethema"><button type="button" data-help-view="start">Schnellstart</button><button type="button" data-help-view="legend">Legende</button></div>' +
      '<div class="student-help-content" id="studentHelpContent"></div><div class="student-help-footer"><button type="button" data-help-skip>Überspringen</button><div><button type="button" data-help-back>Zurück</button><button type="button" class="student-help-primary" data-help-next>Weiter</button></div></div>';
    doc.body.append(dialog);
    dialog.addEventListener('keydown', event => event.stopPropagation());
    const content = dialog.querySelector('#studentHelpContent');
    const next = dialog.querySelector('[data-help-next]');
    const back = dialog.querySelector('[data-help-back]');
    const skip = dialog.querySelector('[data-help-skip]');
    let returnFocus = entry;
    function render(focusHeading) {
      dialog.querySelectorAll('[data-help-view]').forEach(button => button.setAttribute('aria-pressed', String(button.dataset.helpView === state.view)));
      const start = state.view === 'start';
      dialog.querySelector('#studentHelpTitle').textContent = start ? 'Schnellstart' : 'Farben & Symbole';
      if (start) {
        const item = steps[area][state.step];
        content.innerHTML = '<p class="student-help-progress" role="status">Schritt ' + (state.step + 1) + ' von 4</p><div class="student-help-step" aria-hidden="true">' + (state.step + 1) + '</div><h3 tabindex="-1">' + item[0] + '</h3><p>' + item[1] + '</p><p class="student-help-note">' + item[2] + '</p><p class="student-help-reminder">Du kannst den Einstieg jederzeit über Hilfe erneut öffnen.</p>';
      } else {
        const list = area === 'werkstatt' ? workshopLegend : circuitLegend;
        content.innerHTML = '<p>Die Bedeutung hängt davon ab, wo die Markierung erscheint.</p><dl class="student-help-legend">' + list.map(item => '<div><dt>' + legendSymbol(item[0]) + item[1] + '</dt><dd>' + item[2] + '</dd></div>').join('') + '</dl><h3>Aderfarben</h3><p>Leiterkennzeichnungen, keine EIN/AUS-Anzeige. Eine gewählte Farbe verdrahtet den Leiter nicht automatisch.</p><ul class="student-help-conductors">' + conductors.map(item => '<li><span class="student-help-wire ' + item[0] + '" aria-hidden="true"></span>' + item[1] + '</li>').join('') + '</ul>';
      }
      if (area === 'werkstatt') {
        const more = doc.createElement('button');
        more.type = 'button'; more.className = 'student-help-more'; more.textContent = 'Ausführliche Werkstatthilfe';
        more.onclick = () => { dialog.close(); doc.querySelector('#helpDialog').showModal(); };
        content.append(more);
      }
      back.hidden = !start; back.disabled = state.step === 0;
      skip.hidden = !start;
      next.textContent = !start ? 'Schließen' : state.step === 3 ? 'Loslegen' : 'Weiter';
      content.scrollTop = 0;
      if (focusHeading) content.querySelector('h3')?.focus();
    }
    function open(view, opener = entry) {
      returnFocus = opener;
      state.open(view); render(false);
      if (!dialog.open) dialog.showModal();
      dialog.querySelector('[data-help-close]').focus();
    }
    entry.addEventListener('click', () => open('start'));
    dialog.querySelectorAll('[data-help-view]').forEach(button => button.addEventListener('click', () => { state.open(button.dataset.helpView); render(false); }));
    dialog.querySelector('[data-help-close]').onclick = () => dialog.close();
    skip.onclick = () => dialog.close();
    back.onclick = () => { state.back(); render(true); };
    next.onclick = () => { if (state.view === 'legend' || state.step === 3) dialog.close(); else { state.next(); render(true); } };
    dialog.addEventListener('close', () => {
      state.dismiss();
      if (!doc.querySelector('dialog[open]')) returnFocus.focus();
    });
    if (!state.seen && !doc.querySelector('dialog[open]')) open('start');
  }

  const api = {createState, steps};
  if (typeof module !== 'undefined' && module.exports) module.exports = api;
  if (root.document) {
    if (root.document.readyState === 'loading') root.document.addEventListener('DOMContentLoaded', () => mount(root.document), {once:true});
    else mount(root.document);
  }
})(typeof window !== 'undefined' ? window : {});
