// Jede Messreihe lädt genau eine eigene, sichtbare Anwendungsinstanz.

import { environment, safeError, stats, solverSample } from './core.mjs';

const workloads=[

  {id:'simple',label:'CircuitJS · einfache Schaltung',url:'circuitjs.html?startCircuit=benchmark-simple.txt&running=true&lang=de'},

  {id:'fup',label:'Mini-SPS / FUP',url:'circuitjs.html?startCircuit=egt-pumpe-real-beispiel.txt&running=true&lang=de&IECGates=true'},

  {id:'tor',label:'Tor-HMI',url:'egt-hmi-tor.html?example=1&test=benchmark'},

  {id:'pump',label:'Pumpen-HMI',url:'egt-hmi-pumpe.html?example=1&test=benchmark'},

  {id:'workshop',label:'Verdrahtungswerkstatt',url:'verdrahtungswerkstatt.html?test=benchmark'}

];

export const pause=(ms,signal)=>new Promise((resolve,reject)=>{

  signal?.throwIfAborted();

  const timer=setTimeout(done,ms);

  function done(){signal?.removeEventListener('abort',abort);resolve();}

  function abort(){clearTimeout(timer);signal.removeEventListener('abort',abort);reject(signal.reason);}

  signal?.addEventListener('abort',abort,{once:true});

});

async function until(fn,signal,timeout=60000){

  const start=performance.now();

  while(performance.now()-start<timeout){signal.throwIfAborted();const value=fn();if(value)return value;await pause(50,signal);}

  throw Error('Zeitlimit: Anwendung nicht bereit. Netzwerkfreigabe, Dateien oder Browser prüfen ('+(timeout/1000)+' s Sicherheitslimit).');

}

export function findApi(win){return win.CircuitJS1||win.document.querySelector('#circuitFrame')?.contentWindow?.CircuitJS1;}

export function applicationReady(work,win,api){
  if(typeof api?.getElements!=='function')return false;
  if(work.id!=='workshop')return api.getElements().length>0;
  if(typeof win.__workshop?.recovery!=='function'||!api.workbench?.readSnapshot)return false;
  const raw=api.workbench.readSnapshot(),state=typeof raw==='string'?JSON.parse(raw):raw;
  return state?.ok===true&&state.revision>0;
}

export function workshopProject(win){
  const saved=win.__workshop?.recovery();
  return saved?JSON.parse(saved).payload:null;
}

function assertIsolation(win){

  if(!win.__benchmarkIsolation?.ready)throw Error('Testspeicher nicht isoliert; Anwendung wird nicht gemessen.');

  const child=win.document.querySelector('#circuitFrame');

  if(child?.contentWindow?.CircuitJS1&&!child.contentWindow.__benchmarkIsolation?.ready)throw Error('Solverfenster nicht isoliert.');

}

function allEvents(win){

  return [...(win?.__benchmarkEvents||[]),...(win?.document.querySelector('#circuitFrame')?.contentWindow?.__benchmarkEvents||[])];

}

export function frames(win,ms,signal){

  return new Promise((resolve,reject)=>{

    const values=[],start=performance.now();let last,id;

    const timer=setTimeout(()=>finish(new Error('Keine Frame-Rückmeldung innerhalb des Sicherheitslimits.')),ms+10000);

    function finish(error){win.cancelAnimationFrame(id);clearTimeout(timer);signal.removeEventListener('abort',abort);error?reject(error):resolve(values);}

    function abort(){finish(signal.reason);}

    function tick(t){if(last!==undefined)values.push(t-last);last=t;if(performance.now()-start>=ms&&values.length)finish();else id=win.requestAnimationFrame(tick);}

    signal.addEventListener('abort',abort,{once:true});if(signal.aborted)return abort();id=win.requestAnimationFrame(tick);

  });

}

export class Benchmark {

  constructor(host,notify=()=>{}){this.host=host;this.notify=notify;this.frame=null;this.report=null;}

  dispose(){try{findApi(this.frame?.contentWindow)?.setSimRunning(false);}catch{}this.frame?.remove();this.frame=null;}
  cancel(reason='Vom Benutzer abgebrochen'){this.controller?.abort(new DOMException(reason,'AbortError'));this.dispose();}

  add(row){this.report.results.push(row);this.notify(this.report);}

  async load(work,signal){

    if(this.frame){try{findApi(this.frame.contentWindow)?.setSimRunning(false);this.frame.contentWindow.__benchmarkApplicationStarted=false;}catch{}}
    else this.frame=document.createElement('iframe');

    this.frame.title='Isolierte Testinstanz: '+work.label;

    // Trusted local code, no third-party content. Storage isolation is provided by the bootstrap.

    this.frame.setAttribute('sandbox','allow-scripts allow-same-origin');

    this.frame.src=new URL('../benchmark-runtime/'+work.url,import.meta.url).href;

    const start=performance.now();if(!this.frame.isConnected)this.host.replaceChildren(this.frame);

    let visibleMs=null;

    const api=await until(()=>{

      const win=this.frame?.contentWindow;

      if(!win)return false;

      if(allEvents(win).some(e=>['bootstrap-failed','resource-error','storage-isolation-failed'].includes(e.kind)))throw Error('Anwendungsdatei blockiert oder nicht geladen; Freigabe und Paket prüfen.');

      if(!win.__benchmarkApplicationStarted)return false;

      assertIsolation(win);

      if(visibleMs===null&&win.document.body?.getBoundingClientRect().height>0)visibleMs=performance.now()-start;

      const api=findApi(win);

      if(!applicationReady(work,win,api))return false;

      const events=allEvents(win);

      if(events.some(e=>['bootstrap-failed','storage-isolation-failed'].includes(e.kind)))throw Error('Anwendungsstart fehlgeschlagen.');

      return api;

    },signal);

    await pause(100,signal);

    return {api,win:this.frame.contentWindow,visibleMs,readyMs:performance.now()-start,renderViewport:{width:this.frame.contentWindow.innerWidth,height:this.frame.contentWindow.innerHeight}};

  }

  async measureSolver(api,signal,count=3,seconds=2){

    api.workbench.setRealtime(true);api.setSimRunning(true);await pause(300,signal);

    const samples=[];

    for(let i=0;i<count;i++){

      const w0=performance.now(),t0=api.getTime();await pause(seconds*1000,signal);

      const sample=solverSample(t0,api.getTime(),w0,performance.now());

      if(!api.isRunning()||sample.solverSeconds===0)throw Error('Solver steht während des Laufs.');

      samples.push({...sample,timeStep:api.getTimeStep(),running:api.isRunning()});

    }

    return {samples,summary:stats(samples.map(s=>s.ratio)),profile:'EGT-Echtzeitmodus aktiv angefordert; Zeitschritt aus API erfasst'};

  }

  async roundtrip(api,signal){
    const wasRunning=api.isRunning();
    api.workbench.setRealtime(true);api.setSimRunning(true);
    let t0=api.getTime();await until(()=>api.getTime()>=t0+.1,signal,15000);
    api.setSimRunning(false);
    const before=api.exportCircuit(),count=api.getElements().length;
    const isXml=before.trimStart().startsWith('<');
    const file=new File([before],isXml?'benchmark.xml':'benchmark.circuitjs',{type:isXml?'application/xml':'text/plain'});
    const text=await new Promise((resolve,reject)=>{const reader=new FileReader();reader.onload=()=>resolve(reader.result);reader.onerror=()=>reject(Error('Datei konnte nicht gelesen werden'));reader.readAsText(file);});
    signal.throwIfAborted();api.importCircuit(text,false);
    // Gate outputs are rebuilt on the first solver cycles after import.
    api.setSimRunning(true);t0=api.getTime();
    await until(()=>api.getTime()>=t0+.1,signal,15000);
    api.setSimRunning(false);
    const after=api.exportCircuit();
    if(api.getElements().length!==count||after!==before)throw Error('Schaltungs-Rundlauf verändert gespeicherte Daten nach den Anlaufzyklen.');
    api.setSimRunning(wasRunning);
    return {format:isXml?'CircuitJS-XML über File/FileReader und echten Import':'CircuitJS-Text über File/FileReader und echten Import',phase:'Kontrolliertes stationäres Beispiel: je 0,1 Solversekunden Anlauf, danach vollständiger pausierter Dateivergleich einschließlich Optionen',elements:count,bytes:file.size};
  }
  async workshopFile(win,signal){

    const d=win.document;

    d.querySelector('#crossoverExample').click();await pause(350,signal);

    let captured;

    const create=win.URL.createObjectURL,click=win.HTMLAnchorElement.prototype.click;

    try{

      win.URL.createObjectURL=blob=>{captured=blob;return create.call(win.URL,blob);};

      win.HTMLAnchorElement.prototype.click=function(){};

      d.querySelector('#saveProject').click();

    }finally{win.URL.createObjectURL=create;win.HTMLAnchorElement.prototype.click=click;}

    if(!captured)throw Error('Projektdatei nicht erzeugt.');

    const text=await captured.text(),project=JSON.parse(text),input=d.querySelector('#projectFile');

    const transfer=new win.DataTransfer();transfer.items.add(new win.File([text],'benchmark-project.json',{type:'application/json'}));

    // Explicitly load the same project through the real file input in setup mode.

    input.files=transfer.files;input.dispatchEvent(new win.Event('change',{bubbles:true}));

    const expected=project.payload||project;

    const loaded=await until(()=>{const value=workshopProject(win);return value&&value.modules.length===expected.modules.length&&value.wires.length===expected.wires.length?value:null;},signal,5000);

    const contains=(actual,wanted)=>wanted&&typeof wanted==='object'?Object.keys(wanted).every(key=>contains(actual?.[key],wanted[key])):actual===wanted;
    if(loaded.modules.length!==expected.modules.length||loaded.wires.length!==expected.wires.length||!contains(loaded.modules,expected.modules)||!contains(loaded.wires,expected.wires))throw Error('Werkstattprojekt nicht vollständig wiederhergestellt.');

    return {format:'Werkstatt-JSON über echte Speichern-/Dateiöffnen-Funktionen',devices:loaded.modules.length,wires:loaded.wires.length,bytes:captured.size};

  }

  async run({stabilitySeconds=20}={}){

    if(this.active)throw Error('Es läuft bereits ein Test.');

    if(document.hidden)throw Error('Benchmark im sichtbaren Tab starten.');

    this.active=true;this.controller=new AbortController();const signal=this.controller.signal;

    this.report={schemaVersion:1,startedAt:new Date().toISOString(),status:'running',environment:environment(navigator,screen,window),build:null,

      method:{loads:3,solverSamples:3,solverSampleSeconds:2,stabilitySeconds:Math.max(20,Math.min(180,stabilitySeconds)),cache:'Erster Aufruf dieses Laufs; Browser-/OS-Cache nicht geleert. Wiederholungen verwenden denselben URL.',formalAcceptance:false,schoolPcVerified:false},results:[],limits:['Keine Messung der gesamten CPU- oder RAM-Auslastung.','Frame-Abstände sind Browser-Callbacks, keine GPU-FPS.','Automatisierte Klicks ersetzen keine manuelle Bedienungsprüfung.','Keine formelle Abnahme AT01–AT09.']};

    const visibility=()=>{if(document.hidden)this.cancel('Tab im Hintergrund: Messung abgebrochen, Teilergebnisse bleiben erhalten.');};

    document.addEventListener('visibilitychange',visibility);

    const resized=()=>this.cancel('Fenstergröße während der Messung geändert; bitte neu starten.');

    window.addEventListener('resize',resized);

    let completed=0;const total=workloads.length*4+3;

    const step=label=>{this.report.current=label;this.report.progress=completed/total*100;this.notify(this.report);};

    const test=async(label,fn)=>{

      step(label);try{const data=await fn();this.add({test:label,status:'gemessen',...data});}

      catch(error){if(signal.aborted)throw error;this.add({test:label,status:'fehlgeschlagen',error:safeError(error)});}

      completed++;this.report.progress=completed/total*100;

    };

    try{

      const response=await fetch(new URL('../benchmark-build.json',import.meta.url),{signal,cache:'no-store'});

      if(!response.ok)throw Error('Buildkennung fehlt. Benchmark zuerst mit prepare_benchmark.py paketieren.');

      const manifest=await response.json();

      // The full file list identifies the artifact, but no local paths enter the report.

      this.report.build=Object.fromEntries(Object.entries(manifest).filter(([key])=>key!=='files'));

      for(const work of workloads){

        let instance;

        for(let i=0;i<3;i++)await test(work.label+' · '+(i===0?'Erstaufruf':'erneutes Laden '+i),async()=>{

          instance=null;instance=await this.load(work,signal);

          return {kind:'load',sample:i+1,visibleMs:instance.visibleMs,readyMs:instance.readyMs,renderViewport:instance.renderViewport,visibilityDefinition:'DOM nach Anwendungsbootstrap vorhanden; kein Pixel-/Paint-Nachweis',events:allEvents(instance.win)};

        });

        if(!instance){completed++;continue;}

        await test(work.label+' · Solver / Bedienung',async()=>{

          const {api,win}=instance;

          win.document.querySelector('[data-help-skip]')?.click();

          let roundtrip;

          if(work.id==='workshop'){

            roundtrip=await this.workshopFile(win,signal);win.document.querySelector('#modeRun').click();await pause(400,signal);

          }else{

            roundtrip=await this.roundtrip(api,signal);

            const engine=work.id==='tor'?api.tor:work.id==='pump'?api.pump:null;

            if(engine){engine.setDeviceAction('button:S4','press');await pause(250,signal);engine.setDeviceAction('button:S4','release');}

          }

          const solver=await this.measureSolver(api,signal),gaps=await frames(win,1200,signal),interaction=[];

          // Real run/pause API action; two scheduling opportunities, not physical input latency.

          for(let j=0;j<3;j++){

            const t=performance.now();

            const control=win.document.querySelector(work.id==='workshop'?'#fitScene':j%2?'#themeHell':'#themeKontrast');

            if(control)control.click();else api.setSimRunning(false);

            await frames(win,1,signal);interaction.push(performance.now()-t);api.setSimRunning(true);

          }

          return {kind:'simulation',solver,roundtrip,frameGapsMs:stats(gaps),rawFrameGapsMs:gaps,gapsOver50ms:gaps.filter(v=>v>50).length,

            interactionMs:stats(interaction),rawInteractionMs:interaction,interactionDefinition:'Echter HMI-Theme-/Werkstatt-Einpassen-Klick; CircuitJS: API-Pause. Bis zwei rAF-Callbacks, keine Hardware-Klicklatenz',events:allEvents(win),

            notice:solver.samples.every(s=>s.ratio<1)?'Alle Stichproben unter 1 Solversekunde/s; Schul-PC-Prüfung und Messunsicherheit beachten.':'Solverfortschritt gemessen; keine pauschale Leistungsfreigabe.'};

        });

      }

      const complex={id:'complex',label:'Schütz-/Motorsteuerung',url:'circuitjs.html?startCircuit=egt-stern-dreieck.txt&running=true&lang=de'};

      await test(complex.label,async()=>{const {api}=await this.load(complex,signal);return {kind:'simulation',solver:await this.measureSolver(api,signal)};});

      await test('Begrenzter Pumpen-Dauerlauf',async()=>{

        const {api,win}=await this.load(workloads[3],signal);

        api.pump.setDeviceAction('button:S4','press');await pause(250,signal);api.pump.setDeviceAction('button:S4','release');

        const seconds=this.report.method.stabilitySeconds;

        const result=await this.measureSolver(api,signal,Math.ceil(seconds/2),2);

        return {kind:'stability',requestedSeconds:seconds,actualSeconds:result.samples.reduce((s,r)=>s+r.wallSeconds,0),solver:result,events:allEvents(win)};

      });

      await test('Browsergrenzen',async()=>({kind:'limits',cpu:'nicht messbar',ram:'nicht messbar',schoolPc:'offen',manualVisualCheck:'offen',formalAcceptance:'nicht durchgeführt'}));

      this.report.status=this.report.results.some(r=>r.status==='fehlgeschlagen'||r.events?.length)?'completed-with-findings':'complete';

    }catch(error){this.report.status=signal.aborted?'aborted':'error';this.report.error=safeError(signal.aborted?signal.reason:error);}

    finally{

      document.removeEventListener('visibilitychange',visibility);window.removeEventListener('resize',resized);this.dispose();this.host.replaceChildren();this.active=false;

      this.report.finishedAt=new Date().toISOString();this.report.current=this.report.status==='aborted'?'Abgebrochen':'Lauf beendet';if(this.report.status.startsWith('complete'))this.report.progress=100;

      this.notify(this.report);

    }

    return this.report;

  }

}
