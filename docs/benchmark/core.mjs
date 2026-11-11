import { assessReport, assessResult, criteria } from './assessment.mjs';
// Messwerte bleiben getrennt von der fachlichen Bewertung.
export function stats(values) {
  if (!values.length) return null;
  if (values.some(v => !Number.isFinite(v))) throw new Error('Ungültiger Messwert');
  const a = [...values].sort((x,y)=>x-y), n=a.length;
  return {count:n,min:a[0],max:a[n-1],mean:a.reduce((x,y)=>x+y,0)/n,
    median:n%2?a[(n-1)/2]:(a[n/2-1]+a[n/2])/2,p95:a[Math.ceil(n*.95)-1]};
}
export function safeError(error) {
  return String(error?.message ?? error).replace(/https?:\/\/[^\s<>"']+/g,'[Ressource]')
    .replace(/[A-Za-z]:[\\/][^\n"']+/g,'[lokaler Pfad]').slice(0,400);
}
export function environment(nav, screen, win) {
  const ua=nav.userAgent||'', match=ua.match(/Edg\/([\d.]+)/)||ua.match(/Firefox\/([\d.]+)/)||ua.match(/Chrome\/([\d.]+)/)||ua.match(/Version\/([\d.]+).*Safari/);
  return {browser:/Edg\//.test(ua)?'Edge':/Firefox\//.test(ua)?'Firefox':/Chrome\//.test(ua)?'Chrome':/Safari\//.test(ua)?'Safari':'nicht ermittelbar',
    version:match?.[1]??null, os:/Windows/.test(ua)?'Windows (Version nicht zuverlässig)':/Android/.test(ua)?'Android':/Macintosh/.test(ua)?'macOS':/Linux/.test(ua)?'Linux':'nicht ermittelbar',
    screen:{width:screen.width??null,height:screen.height??null},viewport:{width:win.innerWidth??null,height:win.innerHeight??null},pixelRatio:win.devicePixelRatio??null,
    logicalCoresHint:Number.isFinite(nav.hardwareConcurrency)?nav.hardwareConcurrency:null,
    memoryGiBHint:Number.isFinite(nav.deviceMemory)?nav.deviceMemory:null,
    cpuUtilization:'nicht messbar',ramUsage:'nicht messbar',hardwareNote:'Browserhinweise können gerundet oder aus Datenschutzgründen begrenzt sein; keine Hardwareinventur.'};
}
export function solverSample(t0,t1,w0,w1) {
  if (![t0,t1,w0,w1].every(Number.isFinite)||t1<t0||w1<=w0) throw new Error('Solverzeit zurückgesetzt oder Messung ungültig');
  return {solverSeconds:t1-t0,wallSeconds:(w1-w0)/1000,ratio:(t1-t0)/((w1-w0)/1000)};
}
export function csv(rows) {
  const keys=[...new Set(rows.flatMap(r=>Object.keys(r)))];
  const cell=value=>{let s=typeof value==='object'?JSON.stringify(value):String(value??'');if(/^[=+@\-]/.test(s)&&typeof value!=='number')s="'"+s;return '"'+s.replaceAll('"','""')+'"';};
  return keys.map(cell).join(',')+'\r\n'+rows.map(r=>keys.map(k=>cell(r[k])).join(',')).join('\r\n');
}
export function download(report,format) {
  const exported={...report,assessment:assessReport(report),assessmentCriteria:criteria};
  const content=format==='json'?JSON.stringify(exported,null,2):'\uFEFF'+csv(report.results.map(r=>({startedAt:report.startedAt,build:report.build?.buildId,browser:report.environment.browser,version:report.environment.version,runStatus:report.status,assessment:assessResult(r),assessmentCriteria:criteria,...r})));
  const blob=new Blob([content],{type:format==='json'?'application/json':'text/csv;charset=utf-8'}), url=URL.createObjectURL(blob),a=document.createElement('a');
  a.href=url;a.download='circuitjs-benchmark-'+report.startedAt.replace(/[:.]/g,'-')+'.'+format;a.click();setTimeout(()=>URL.revokeObjectURL(url),1000);
}
