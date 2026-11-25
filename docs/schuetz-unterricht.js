import {schuetzExamples} from './verdrahtungswerkstatt/schuetztechnik.js?v=20261125schuetzexport1';
import {referenceCircuits, wiringWorkshopUrl} from './schuetz-katalog.js';

function link(text, href, className) {
  const anchor = document.createElement('a');
  anchor.textContent = text;
  anchor.href = href;
  anchor.className = className;
  return anchor;
}

const list = document.getElementById('lessons');
for (const [id, title] of schuetzExamples) {
  const row = document.createElement('li');
  const query = new URLSearchParams({
    lang: 'de', IECGates: 'true', unterricht: '1', startCircuit: referenceCircuits.get(id)
  });
  const workshop = link('Zur Werkstatt', wiringWorkshopUrl(id, true), 'workshop-link');
  workshop.setAttribute('aria-label', title + ' in der Verdrahtungswerkstatt öffnen');
  row.append(link(title, 'circuitjs.html?' + query, 'lesson-title'), workshop);
  list.append(row);
}
