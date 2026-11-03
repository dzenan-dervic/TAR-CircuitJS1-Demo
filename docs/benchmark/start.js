// Anwendungsskripte erst nach erfolgreicher Speicherisolation freigeben.
(async()=>{
  if(!window.__benchmarkIsolation?.ready){document.body.textContent='Benchmark gestoppt: flüchtiger Testspeicher nicht verfügbar.';return;}
  try{
    for(const pending of document.querySelectorAll('script[type="application/x-benchmark-pending"]')){
      const script=document.createElement('script');
      if(pending.dataset.benchmarkType)script.type=pending.dataset.benchmarkType;
      if(pending.dataset.benchmarkSrc){script.src=pending.dataset.benchmarkSrc;await new Promise((resolve,reject)=>{script.onload=resolve;script.onerror=()=>reject(Error('Anwendungsdatei nicht geladen'));pending.replaceWith(script);});}
      else {script.textContent=pending.textContent;pending.replaceWith(script);}
    }
    window.__benchmarkApplicationStarted=true;
  }catch{window.__benchmarkEvents.push({kind:'bootstrap-failed',resource:null});}
})();
