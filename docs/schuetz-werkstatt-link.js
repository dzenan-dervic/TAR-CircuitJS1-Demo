import {referenceCircuits, wiringWorkshopUrl} from './schuetz-katalog.js';

const classroom = new URLSearchParams(location.search).get('unterricht') === '1';
const exampleByFile = new Map([...referenceCircuits].map(([id, file]) => [file, id]));

function update() {
  const link = document.getElementById('workshopCircuitLink');
  if (!link) return;
  const id = exampleByFile.get(link.dataset.circuitFile);
  link.hidden = !id;
  if (id) link.href = wiringWorkshopUrl(id, classroom);
  else link.removeAttribute('href');
}

new MutationObserver(update).observe(document.body, {
  subtree: true,
  attributes: true,
  attributeFilter: ['data-circuit-file']
});
update();
