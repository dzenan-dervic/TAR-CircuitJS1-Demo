// Bewusst einfache Projekt-Richtwerte; keine allgemeine Hardware- oder Unterrichtsfreigabe.
export const criteria={version:1,loadMs:{goodMax:2000,warnMax:5000},reactionMs:{goodMax:100,warnMax:300},solver:{goodMin:.95,warnMin:.75},note:'Projektbezogene Orientierung, keine Norm. Solver: 5 % Messtoleranz um das Echtzeitziel. API-/Frame-Reaktion ist kein INP.'};
const labels={good:'Gut',warn:'Eingeschränkt',bad:'Deutlich langsam',error:'Fehler',unknown:'Nicht bewertet',incomplete:'Nicht vollständig'};
const rank={unknown:0,good:1,warn:2,bad:3,error:4};
function result(level,reasons=[]){return {level,label:labels[level],reasons};}
export function assessResult(row){
  if(row.status==='fehlgeschlagen'||row.error||row.events?.length)return result('error',[row.error||'Fehlerereignis im Test.']);
  const checks=[];
  if(row.kind==='load'&&Number.isFinite(row.readyMs)&&row.readyMs>=0){
    checks.push(result(row.readyMs<=criteria.loadMs.goodMax?'good':row.readyMs<=criteria.loadMs.warnMax?'warn':'bad',['Ladezeit: bis 2 s gut, über 2 bis 5 s eingeschränkt, über 5 s deutlich langsam.']));
  }
  const ratio=row.solver?.summary?.median;
  if(Number.isFinite(ratio)&&ratio>=0){
    checks.push(result(ratio>=criteria.solver.goodMin?'good':ratio>=criteria.solver.warnMin?'warn':'bad',[ratio>=.95?'Echtzeitziel innerhalb der 5-%-Messtoleranz erreicht.':'Simulation bleibt hinter der Echtzeit zurück.']));
  }
  const reaction=row.interactionMs?.median;
  if(Number.isFinite(reaction)&&reaction>=0){
    checks.push(result(reaction<=100?'good':reaction<=300?'warn':'bad',['Reaktionsstichprobe: bis 100 ms gut, über 100 bis 300 ms eingeschränkt, darüber deutlich langsam; kein INP.']));
  }
  if(!checks.length)return result('unknown',['Keine passende Leistungsmessung vorhanden.']);
  const worst=checks.reduce((a,b)=>rank[a.level]>=rank[b.level]?a:b);
  return result(worst.level,checks.flatMap(c=>c.reasons));
}
export function assessReport(report){
  if(report.status==='error')return {...result('error',[report.error||'Test nicht ausführbar.']),title:'Test nicht erfolgreich abgeschlossen'};
  if(!['complete','completed-with-findings'].includes(report.status))return {...result('incomplete'),title:report.status==='running'?'Messung läuft – noch keine Gesamtbewertung':'Test unvollständig – keine Gesamtbewertung'};
  const rows=report.results.map(row=>({test:row.test,...assessResult(row)}));
  if(rows.some(r=>r.level==='unknown'&&report.results.find(x=>x.test===r.test)?.kind!=='limits'))return {...result('unknown'),title:'Messwerte nicht vollständig bewertbar',rows};
  const rated=rows.filter(r=>r.level!=='unknown');
  if(!rated.length)return {...result('unknown'),title:'Keine bewertbaren Messwerte',rows};
  const worst=rated.reduce((a,b)=>rank[a.level]>=rank[b.level]?a:b);
  const titles={good:'Gut im gemessenen Test',warn:'Test mit Leistungseinschränkungen',bad:'Deutliche Leistungseinschränkungen',error:'Test mit Fehlern – Ergebnis prüfen'};
  return {...result(worst.level),title:titles[worst.level],rows,issues:rated.filter(r=>['warn','bad','error'].includes(r.level)),criteriaVersion:criteria.version};
}
