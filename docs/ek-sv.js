import {referenceCircuits, wiringWorkshopUrl} from './schuetz-katalog.js';

const storageKey = 'egt.ek-sv.v1.';
const assessments = new Map([
  ['fits', 'Passt'],
  ['revise', 'Muss bearbeitet werden'],
  ['does-not-fit', 'Passt nicht'],
]);
const catalogGroups = {
  'EGT Installation': 'Installation',
  'EGT Contactor Circuits': 'Schütztechnik',
  'EGT Motor Circuits': 'Motoren und Antriebe',
  'EGT Function Plan': 'Logik und FUP',
  'ANH-07 Thermal Overload Relay': 'Thermisches Überlastrelais',
};
const workshopIds = new Map([...referenceCircuits].map(([id, file]) => [file, id]));
const rows = document.getElementById('reviewRows');
const search = document.getElementById('reviewSearch');
const status = document.getElementById('reviewStatus');
const count = document.getElementById('reviewCount');
const progress = document.getElementById('reviewProgress');
const error = document.getElementById('reviewError');
const sections = [...document.querySelectorAll('[data-kind]')];
let kind = 'circuit';
let rendered = [];

function circuitUrl(file) {
  return 'circuitjs.html?' + new URLSearchParams({lang:'de', IECGates:'true', startCircuit:file});
}

function readCircuits(text) {
  const groups = [];
  const entries = [];
  for (const line of text.split(/\r?\n/)) {
    if (line.startsWith('+')) groups.push(line.slice(1));
    else if (line === '-') groups.pop();
    else {
      const match = /^>?(egt-[a-z0-9-]+\.txt) (.+)$/.exec(line);
      if (!match || !Object.hasOwn(catalogGroups, groups[0])) continue;
      const [,file,title] = match;
      const links = [['Schaltplan', circuitUrl(file)]];
      if (workshopIds.has(file)) links.push(['Werkstatt', wiringWorkshopUrl(workshopIds.get(file))]);
      entries.push({id:file,title,group:catalogGroups[groups[0]],links});
    }
  }
  return entries.concat([
    {id:'tor', title:'Torsteuerung', group:'Anlagen', links:[['Übung','egt-hmi-tor.html?unterricht=1']]},
    {id:'pumpe', title:'Pumpensteuerung', group:'Anlagen', links:[['Übung','egt-hmi-pumpe.html?unterricht=1']]},
    {id:'werkstatt', title:'Verdrahtungswerkstatt', group:'Anlagen', links:[['Montageplatte','verdrahtungswerkstatt.html']]},
  ]);
}

function readProgress(key) {
  try {
    return JSON.parse(localStorage.getItem(key)) || {};
  } catch {
    error.textContent = 'Ein gespeicherter Eintrag konnte nicht gelesen werden.';
    error.hidden = false;
    return {};
  }
}

function cell(row) {
  const td = row.insertCell();
  td.setAttribute('role','cell');
  return td;
}

function makeRow(entry) {
  const key = storageKey + entry.id;
  const row = rows.insertRow();
  row.setAttribute('role','row');
  const check = document.createElement('input');
  check.type = 'checkbox';
  check.setAttribute('aria-label', entry.title + ' als besprochen markieren');
  cell(row).append(check);

  const title = document.createElement('strong');
  title.textContent = entry.title;
  const group = document.createElement('small');
  group.textContent = entry.group;
  cell(row).append(title, group);

  const links = document.createElement('div');
  links.className = 'review-links';
  cell(row).append(links);
  if (!entry.links.length) {
    const missing = document.createElement('small');
    missing.textContent = 'Kein Beispiel verlinkt';
    links.append(missing);
  }
  for (const [label,url] of entry.links) {
    const link = document.createElement('a');
    link.textContent = label;
    link.href = url;
    link.target = '_blank';
    link.rel = 'noopener';
    link.setAttribute('aria-label', label + ' zu ' + entry.title + ' öffnen');
    links.append(link);
  }

  const assessment = document.createElement('select');
  assessment.setAttribute('aria-label', 'Bewertung zu ' + entry.title);
  assessment.add(new Option('Nicht bewertet', ''));
  for (const [value,label] of assessments) assessment.add(new Option(label,value));
  cell(row).append(assessment);

  const notes = document.createElement('textarea');
  notes.rows = 2;
  notes.placeholder = 'Verbesserung notieren';
  notes.setAttribute('aria-label', 'Verbesserungsvorschläge zu ' + entry.title);
  cell(row).append(notes);

  function loadSaved() {
    const saved = readProgress(key);
    check.checked = saved.done === true;
    assessment.value = assessments.has(saved.assessment) ? saved.assessment : '';
    const text = typeof saved.notes === 'string' ? saved.notes : '';
    if (notes.value !== text) notes.value = text;
  }

  function save(change) {
    try {
      localStorage.setItem(key, JSON.stringify({...readProgress(key), ...change}));
    } catch {
      error.textContent = 'Der Browser konnte die Änderung nicht speichern.';
      error.hidden = false;
    }
  }
  check.addEventListener('change', () => { save({done:check.checked}); refresh(); });
  assessment.addEventListener('change', () => { save({assessment:assessment.value}); refresh(); });
  notes.addEventListener('input', () => save({notes:notes.value}));
  loadSaved();
  return {key,row,check,assessment,loadSaved,filterText:(entry.title + ' ' + entry.group).toLocaleLowerCase('de')};
}

function refresh() {
  const term = search.value.trim().toLocaleLowerCase('de');
  const filter = status.value;
  let shown = 0;
  let done = 0;
  for (const {row,check,assessment,filterText} of rendered) {
    if (check.checked) done++;
    row.classList.toggle('review-done',check.checked);
    assessment.dataset.result = assessment.value;
    const matchesStatus = assessments.has(filter)
      ? assessment.value === filter
      : filter === 'all' || check.checked === (filter === 'done');
    row.hidden = !filterText.includes(term) || !matchesStatus;
    if (!row.hidden) shown++;
  }
  progress.max = Math.max(rendered.length,1);
  progress.value = done;
  const total = done + ' von ' + rendered.length + ' besprochen';
  count.textContent = shown === rendered.length ? total : total + ' · ' + shown + ' angezeigt';
  document.getElementById('reviewEmpty').hidden = shown !== 0;
}

async function load(url,format) {
  const response = await fetch(url);
  if (!response.ok) throw new Error(url);
  return response[format]();
}

function bindBackup(entries, refreshRows) {
  const keys = new Map(entries.map(entry => [entry.id,storageKey + entry.id]));
  const fileInput = document.getElementById('reviewFile');
  const message = document.getElementById('reviewBackupStatus');
  const report = (text) => { message.textContent = text; message.hidden = false; };
  const normalize = saved => JSON.stringify({
    done:saved.done === true,
    assessment:assessments.has(saved.assessment) ? saved.assessment : '',
    notes:typeof saved.notes === 'string' ? saved.notes : '',
  });

  document.getElementById('exportReview').addEventListener('click', () => {
    try {
      const records = [];
      for (const [id,key] of keys) {
        const data = localStorage.getItem(key);
        if (data !== null) records.push([id,JSON.parse(data)]);
      }
      const blob = new Blob([JSON.stringify({kind:'egt-ek-sv',version:1,records},null,2)], {type:'application/json'});
      const url = URL.createObjectURL(blob);
      const anchor = document.createElement('a');
      anchor.href = url;
      anchor.download = 'ek-sv-sicherung.json';
      anchor.click();
      setTimeout(() => URL.revokeObjectURL(url),1000);
      report(records.length + ' gespeicherte Einträge in der Sicherungsdatei.');
    } catch {
      report('Die Sicherungsdatei konnte nicht erstellt werden.');
    }
  });

  document.getElementById('importReview').addEventListener('click', () => { fileInput.click(); });
  fileInput.addEventListener('change', async () => {
    const file = fileInput.files[0];
    fileInput.value = '';
    if (!file) return;
    let updates;
    try {
      const data = JSON.parse(await file.text());
      if (data?.kind !== 'egt-ek-sv' || data.version !== 1 || !Array.isArray(data.records))
        throw new Error();
      updates = new Map();
      for (const record of data.records) {
        if (!Array.isArray(record) || typeof record[0] !== 'string' || !record[1] || typeof record[1] !== 'object')
          throw new Error();
        const [id,saved] = record;
        if (!keys.has(id)) continue;
        updates.set(keys.get(id),normalize(saved));
      }
    } catch {
      report('Diese Datei ist keine gültige EK-SV-Sicherung.');
      return;
    }
    if (!updates.size) {
      report('Die Datei enthält keine passenden gespeicherten Einträge.');
      return;
    }
    let restored = 0;
    try {
      const conflicts = [...updates].some(([key,value]) => {
        const existing = JSON.parse(localStorage.getItem(key));
        return existing !== null && normalize(existing) !== value;
      });
      if (conflicts && !confirm('Die Sicherung ersetzt die vorhandenen Häkchen, Bewertungen und Notizen der enthaltenen Einträge. Sicherung laden?')) return;
      for (const [key,value] of updates) {
        localStorage.setItem(key,value);
        restored++;
      }
      report(restored + (restored === 1 ? ' Eintrag' : ' Einträge') + ' aus der Sicherung übernommen.');
    } catch {
      report('Der Browser konnte die Sicherung nicht vollständig speichern. Übernommen: ' + restored + ' von ' + updates.size + '.');
    }
    refreshRows();
  });
  document.getElementById('reviewBackup').hidden = false;
}

try {
  const [catalog,parts] = await Promise.all([load('circuitjs1/setuplist.txt','text'),load('ek-sv-bauteile.json','json')]);
  const entries = {
    circuit:readCircuits(catalog),
    part:parts.map(([id,title,group,example]) => ({
      id,title,group,links:Array.isArray(example) ? [example] : example ? [['Beispiel',circuitUrl(example)]] :
        group === 'Verdrahtungswerkstatt' ? [['Montageplatte','verdrahtungswerkstatt.html']] : [],
    })),
  };
  function show() {
    rows.replaceChildren();
    rendered = entries[kind].map(makeRow);
    refresh();
  }
  for (const section of sections) section.addEventListener('click', () => {
    if (kind === section.dataset.kind) return;
    kind = section.dataset.kind;
    for (const button of sections) button.setAttribute('aria-pressed',String(button === section));
    search.value = '';
    search.placeholder = kind === 'part' ? 'z. B. Leitungsschutz' : 'z. B. Stern-Dreieck';
    show();
  });
  search.addEventListener('input',refresh);
  const assessmentFilters = document.getElementById('assessmentFilters');
  for (const [value,label] of assessments) assessmentFilters.append(new Option(label,value));
  status.addEventListener('change',refresh);
  window.addEventListener('storage', event => {
    if (event.storageArea !== localStorage || (event.key && !event.key.startsWith(storageKey))) return;
    for (const entry of rendered) {
      if (!event.key || event.key === entry.key) entry.loadSaved();
    }
    refresh();
  });
  document.getElementById('reviewTable').hidden = false;
  progress.hidden = false;
  bindBackup([...entries.circuit,...entries.part], show);
  show();
} catch {
  count.textContent = '';
  error.textContent = 'Der Schaltungs- und Bauteilkatalog konnte nicht geladen werden. Lade die Seite erneut.';
  error.hidden = false;
}
