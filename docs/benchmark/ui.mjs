import '../vendor/webawesome/components/button/button.js';
import '../vendor/webawesome/components/badge/badge.js';
import '../vendor/webawesome/components/progress-bar/progress-bar.js';
import { Benchmark } from './runner.mjs?v=20261111modules1';
import { download, safeError } from './core.mjs';
import { assessResult, assessReport } from './assessment.mjs';
const $=id=>document.getElementById(id),number=(v,d=0)=>Number.isFinite(v)?v.toFixed(d):'nicht messbar';
const names={running:'Läuft',complete:'Lauf abgeschlossen','completed-with-findings':'Mit Hinweisen beendet',aborted:'Abgebrochen',error:'Nicht ausführbar'};
function describe(r){
  if(r.error)return r.error;
  if(r.kind==='load')return 'DOM '+number(r.visibleMs)+' ms · bereit '+number(r.readyMs)+' ms'+(r.events?.length?' · '+r.events.length+' Fehlerereignisse':'');
  if(r.kind==='limits')return 'CPU / RAM: nicht messbar · Schul-PC und Sichtprüfung offen';
  if(r.solver)return 'Solver-Median '+number(r.solver.summary?.median,3)+' s/s'+(r.interactionMs?' · Reaktion '+number(r.interactionMs.median)+' ms':'')+(r.actualSeconds?' · '+number(r.actualSeconds,1)+' s Dauerlauf':'')+(r.events?.length?' · '+r.events.length+' Fehlerereignisse':'');
  return 'Messung erfasst';
}
export function render(report){
  $('current').textContent=report.current||'Start';$('percent').textContent=number(report.progress||0)+' %';$('progress').value=report.progress||0;
  $('status').textContent=names[report.status]||report.status;
  $('identity').textContent='Build '+(report.build?.buildId||'wird geladen')+' · '+report.environment.browser+' '+(report.environment.version||'')+' · '+report.environment.viewport.width+' × '+report.environment.viewport.height;
  const env=report.environment;
  const entries=[['Browser',env.browser+' '+(env.version||'')],['Betriebssystem',env.os],['Bildschirm',env.screen.width+' × '+env.screen.height],['Fenster',env.viewport.width+' × '+env.viewport.height],['Logische Prozessoren (Browserhinweis)',env.logicalCoresHint??'Nicht verfügbar'],['Gerätespeicher (grober Browserhinweis)',env.memoryGiBHint===null?'Nicht verfügbar':env.memoryGiBHint+' GiB'],['CPU-Auslastung / RAM-Verbrauch','Nicht messbar']];
  $('environment').replaceChildren(...entries.flatMap(([label,value])=>{const dt=document.createElement('dt'),dd=document.createElement('dd');dt.textContent=label;dd.textContent=value;return [dt,dd];}));
  const assessment=assessReport(report);$('assessment').hidden=false;$('assessment').dataset.level=assessment.level;
  $('assessmentTitle').textContent=assessment.title;
  $('assessmentText').textContent=assessment.level==='good'?'Die erfassten Werte liegen im grünen Orientierungsbereich.':assessment.level==='incomplete'?'Abwarten oder den vollständigen Test erneut starten.':assessment.level==='unknown'?'Es fehlen passende Messwerte.':'Die folgenden Messreihen brauchen Aufmerksamkeit:';
  const issues=[...new Set((assessment.issues||[]).map(r=>r.test.replace(/ · (Erstaufruf|erneutes Laden \d+|Solver \/ Bedienung)$/,'')))];
  $('assessmentIssues').replaceChildren(...issues.map(name=>{const li=document.createElement('li');li.textContent=name;return li;}));
  $('failure').hidden=!report.error;$('failure').textContent=report.error||'';
  $('empty').hidden=!!report.results.length;$('tableWrap').hidden=!report.results.length;
  $('results').replaceChildren(...report.results.map(r=>{
    const rating=assessResult(r),tr=document.createElement('tr');tr.dataset.level=rating.level;tr.title=rating.reasons.join(' ');for(const text of [r.test,describe(r),rating.label]){const td=document.createElement('td');td.textContent=text;td.dataset.fail=String(r.status==='fehlgeschlagen');tr.append(td);}return tr;
  }));
  if(report.status!=='running'&&!$('preview').querySelector('iframe')){const note=document.createElement('p');note.textContent=report.status==='aborted'?'Test abgebrochen. Die Testinstanz wurde geschlossen.':'Test beendet. Die Testinstanz wurde geschlossen; die Ergebnisse bleiben links sichtbar.';$('preview').replaceChildren(note);}
  $('json').disabled=$('csv').disabled=!report.results.length;
}
const bench=new Benchmark($('preview'),render);
$('start').addEventListener('click',async()=>{
  if(bench.active)return;
  document.body.classList.add('is-running');$('start').disabled=true;$('cancel').disabled=false;$('duration').disabled=true;$('failure').hidden=true;
  try{await bench.run({stabilitySeconds:Number($('duration').value)});}
  catch(error){$('failure').hidden=false;$('failure').textContent=safeError(error);}
  finally{document.body.classList.remove('is-running');$('start').disabled=false;$('cancel').disabled=true;$('duration').disabled=false;$('start').textContent='Erneut testen';}
});
$('cancel').addEventListener('click',()=>bench.cancel());
for(const format of ['json','csv'])$(format).addEventListener('click',()=>{if(bench.report)download(bench.report,format);});
addEventListener('pagehide',()=>bench.cancel('Seite verlassen'));
// Read-only status for local verification; no measurements leave this page.
Object.defineProperty(window,'benchmarkStatus',{get:()=>bench.report});

window.__benchmarkReady=true;$('start').disabled=false;$('current').textContent='Bereit für den ersten Lauf';
