// Projekte, Wiederherstellung und Undo verwenden dasselbe gespeicherte Datenformat.
import {schuetzExamples} from './schuetztechnik.js?v=20261125schuetzexport1';
import {wiringExamples} from './unterricht-aufbauten.js';

import {referenceCircuits} from '../schuetz-katalog.js';

const classroomExamples = [...schuetzExamples, ...wiringExamples];

export function createProject(ctx) {
  let bound = false;
  let referenceExample = null;
  let referenceApi = null;
  let referenceMode = null;
  let referenceBreakerState = null;
  const referencePresses = new Map();

  function syncReference(reset = false) {
    if (!referenceApi) return;
    const mode = ctx.state.latchedFault ? 'aufbau' : ctx.state.simMode;
    if (reset || (mode === 'aufbau' && referenceMode !== 'aufbau')) {
      referenceApi.reference.reset();
      referencePresses.clear();
      referenceMode = null;
      referenceBreakerState = null;
    }
    if (referenceExample === '01' && referenceApi.reference.setBreaker) {
      const breaker = ctx.state.modules.find(module => module.id === 'F1' && module.type === 'breaker');
      const state = breaker?.protectionTripped ? 'tripped' : breaker?.protectionOn ? 'on' : 'off';
      if (state !== referenceBreakerState) {
        referenceApi.reference.setBreaker('F1', state);
        referenceBreakerState = state;
      }
    }
    if (mode !== 'aufbau') {
      for (const module of ctx.state.modules) {
        if (!['start', 'stop', 'emergency'].includes(module.type)) continue;
        const id = referenceExample === '02' && module.id === 'S2' ? 'S0'
          : referenceExample === '10' && module.id === 'S1' ? 'S2'
          : referenceExample === '10' && module.id === 'S2' ? 'S1'
          : module.id;
        const pressed = Boolean(module.pressed);
        if (pressed === (referencePresses.get(id) ?? false)) continue;
        referenceApi.reference.setPressed(id, pressed);
        referencePresses.set(id, pressed);
      }
    }
    if (referenceMode !== mode) {
      referenceApi.setSimRunning(mode === 'betrieb');
      referenceMode = mode;
    }
  }

  function renderReference() {
    const circuit = referenceCircuits.get(referenceExample);
    const show = ctx.state.viewMode === 'compare' && Boolean(circuit);
    ctx.dom.$('#referencePane').hidden = !show;
    const engineFrame = ctx.dom.$('#circuitFrame');
    engineFrame.inert = show;
    engineFrame.setAttribute('aria-hidden', String(show));
    const frame = ctx.dom.$('#referenceFrame');
    if (!circuit) {
      frame.onload = null;
      if (frame.hasAttribute('src')) frame.removeAttribute('src');
      referenceApi = null;
      return;
    }
    const params = new URLSearchParams({
      startCircuit: circuit, lang: 'de', IECGates: 'true', running: 'false',
      editable: 'false', hideMenu: 'true', hideSidebar: 'true', hideInfoBox: 'true',
      autoSave: 'false'
    });
    const url = 'circuitjs.html?' + params;
    if (frame.getAttribute('src') === url) return;
    referenceApi = null;
    referenceMode = null;
    referenceBreakerState = null;
    referencePresses.clear();
    frame.onload = () => {
      const document = frame.contentDocument;
      function connect(attemptsLeft) {
        if (frame.getAttribute('src') !== url || frame.contentDocument !== document) return;
        const api = frame.contentWindow.CircuitJS1;
        if (api?.reference?.prepare()) {
          referenceApi = api;
          syncReference();
        } else if (attemptsLeft > 0) {
          setTimeout(() => connect(attemptsLeft - 1), 100);
        }
      }
      connect(100);
    };
    frame.src = url;
  }

  function data(){return {modules:ctx.state.modules.map(({el,pressed,...m})=>m),wires:ctx.state.wires.map(w=>({...w})),powered: ctx.state.powered,view:{x:ctx.state.camera.x,y:ctx.state.camera.y,scale: ctx.state.scale},referenceExample};}
  function persist(){if(ctx.state.startupBlank&&!ctx.state.modules.length&&!ctx.state.wires.length)return;ctx.state.startupBlank=false;const packed=JSON.stringify({v:1,savedAt:Date.now(),payload:ctx.services.projekt.data()});ctx.state.recoveryMemory=packed;if(ctx.config.skipStore)return;try{localStorage.setItem(ctx.config.STORE_KEY,packed);}catch(e){}}
  function schedulePersist(){if(ctx.state.booting)return;clearTimeout(ctx.state.persistTimer);ctx.state.persistTimer=setTimeout(ctx.services.projekt.persist,400);}
  function loadStored(){if(ctx.config.skipStore)return null;try{const raw=JSON.parse(localStorage.getItem(ctx.config.STORE_KEY)||'null');if(raw){const age=Date.now()-raw.savedAt;if(!Number.isFinite(raw.savedAt)||age<0||age>=24*60*60*1000){localStorage.removeItem(ctx.config.STORE_KEY);return null;}}if(raw&&raw.payload&&Array.isArray(raw.payload.modules)&&Array.isArray(raw.payload.wires))return raw.payload;}catch(e){}return null;}
  function paintHistory(){ctx.dom.$('#undo').disabled=!ctx.state.history.length;ctx.dom.$('#redo').disabled=!ctx.state.future.length;}
  function remember(){ctx.state.history.push(JSON.stringify(ctx.services.projekt.data()));if(ctx.state.history.length>60)ctx.state.history.shift();ctx.state.future=[];ctx.services.projekt.paintHistory();}
  function stepHistory(from,to){
    if(!from.length)return;
    const next=JSON.parse(from.at(-1));
    if(ctx.services.projekt.electricalKey(next)!==ctx.services.projekt.electricalKey(ctx.services.projekt.data())&&!ctx.services.simulation.requestElectricalEdit(()=>ctx.services.projekt.stepHistory(from,to)))return;
    const snap=JSON.parse(from.pop());
    to.push(JSON.stringify(ctx.services.projekt.data()));
    if(to.length>60)to.shift();
    if(ctx.services.projekt.electricalKey(snap)===ctx.services.projekt.electricalKey(ctx.services.projekt.data()))ctx.services.projekt.applyLayout(snap);
    else ctx.services.projekt.restore(snap,true);
    ctx.services.projekt.paintHistory();
  }
  function migrateJunctions(data){
    if(!(data.modules||[]).some(m=>m.type==='junction'&&m.junctionVersion!==2))return data;
    const reserved=new Set(data.modules.map(m=>m.id)),keys=new Map(),result=[];
    for(const item of data.modules){
      if(item.type!=='junction'||item.junctionVersion===2){result.push({...item});continue;}
      result.push({...item,junctionVersion:2,boxWidth:390,boxHeight:780,z:0});
      for(const [row,group] of ['L','N','PE','A','B'].entries()){
        let n=1;while(reserved.has('X'+n))n++;const id='X'+n;reserved.add(id);
        result.push({id,type:'connector',ports:5,x:item.x+42,y:item.y+90+row*140,locked:!!item.locked,description:group});
        for(let pin=1;pin<=5;pin++)keys.set(item.id+'.'+group+'.'+pin,id+'.'+pin);
      }
    }
    const rewrite=key=>keys.get(key)||key;
    return {...data,modules:result.map(m=>({...m,probeRed:rewrite(m.probeRed),probeBlack:rewrite(m.probeBlack),...(m.measuredWire?{measuredWire:{...m.measuredWire,a:rewrite(m.measuredWire.a),b:rewrite(m.measuredWire.b)}}:{})})),wires:(data.wires||[]).map(w=>({...w,a:rewrite(w.a),b:rewrite(w.b)}))};
  }
  function copyPackage(m){return ctx.services.projekt.copySelection([m]);}
  function copySelection(items=ctx.services.editor.selection()){
    const ids=new Set(items.map(m=>m.id));
    items.filter(m=>m.type==='auxiliary').forEach(m=>ids.add(m.parent));
    ctx.state.modules.filter(m=>ids.has(m.parent)).forEach(m=>ids.add(m.id));
    const selected=ctx.state.modules.filter(m=>ids.has(m.id));
    return JSON.parse(JSON.stringify({modules:selected.map(({el,pressed,...m})=>m),wires:ctx.state.wires.filter(w=>ids.has(w.a.split('.')[0])&&ids.has(w.b.split('.')[0]))}));
  }
  function pastePackage(pack,offset=40){
    if(pack)pack=ctx.services.projekt.migrateJunctions(pack);
    const items=pack?.modules;if(!items?.length)return;
    if(items.some(ctx.services.simulation.electricalModule)&&!ctx.services.simulation.requestElectricalEdit(()=>ctx.services.projekt.pastePackage(pack,offset)))return;
    ctx.services.projekt.remember();ctx.services.editor.cancel();
    const ids=new Map(),reserved=new Set(ctx.state.modules.map(m=>m.id));
    items.filter(m=>m.type!=='auxiliary').forEach(m=>{
      const prefix=m.id.match(/^[A-Za-z]+/)?.[0]||ctx.services.editor.defaultPrefix(m.type);let n=1;while(reserved.has(prefix+n)||reserved.has(prefix+n+'H'))n++;
      ids.set(m.id,prefix+n);reserved.add(prefix+n);reserved.add(prefix+n+'H');
    });
    items.filter(m=>m.type==='auxiliary').forEach(m=>ids.set(m.id,ids.get(m.parent)+'H'));
    const clones=items.map(item=>{
      const clone=ctx.services.editor.defaultNom(JSON.parse(JSON.stringify(item)));clone.id=ids.get(item.id);if(clone.parent)clone.parent=ids.get(item.parent);
      Object.assign(clone,{pressed:false,latched:false,locked:false,protectionTripped:false,protectionReason:0,probeRed:null,probeBlack:null,measuredWire:null});delete clone.z;
      ctx.state.modules.push(clone);ctx.services.editor.renderModule(clone);return {m:clone,x:item.x,y:item.y};
    });
    ctx.services.editor.moveEntries(clones,offset,offset);
    const rewrite=key=>{const dot=key.indexOf('.');return ids.get(key.slice(0,dot))+key.slice(dot);};
    const dx=clones[0].m.x-clones[0].x,dy=clones[0].m.y-clones[0].y;
    const pastedWires=ctx.services.editor.normalizeSocketWires((pack.wires||[]).map(w=>({...w,a:rewrite(w.a),b:rewrite(w.b),...(Array.isArray(w.route)?{route:w.route.map(p=>[p[0]+dx,p[1]+dy])}:{})})));
    ctx.state.wires.push(...pastedWires);
    ctx.services.editor.setSelection(clones.map(({m})=>m.id),clones[0].m.id);ctx.services.simulation.update();ctx.services.editor.reveal(clones[0].m);ctx.services.editor.say(clones.length+' Bauteile mit '+pastedWires.length+' internen Leitungen eingefügt.');
  }
  function restore(d,keepMode){
    clearTimeout(ctx.state.liveTimer);
    d=ctx.services.projekt.migrateJunctions(d);
    referenceExample = referenceCircuits.has(d.referenceExample) ? d.referenceExample : null;
    renderReference();
    ctx.services.editor.cancel();ctx.services.editor.closeDevMenu();
    ctx.dom.board.querySelectorAll('.module').forEach(e=>e.remove());
    ctx.state.modules=d.modules.map(m=>ctx.services.editor.defaultNom({...m,pressed:false}));
    ctx.state.wires=ctx.services.editor.normalizeSocketWires(d.wires);
    if(keepMode)ctx.state.powered=ctx.services.simulation.liveMode();
    else{ctx.state.powered=false;ctx.state.simMode='aufbau';}
    syncReference(true);
    ctx.state.lastImportedSig='';ctx.state.lastSnap=null;
    ctx.state.states={};ctx.state.nextZ=2;ctx.state.modules.forEach(ctx.services.editor.renderModule);
    ctx.state.selectedIds=new Set([...ctx.state.selectedIds].filter(id=>ctx.state.modules.some(m=>m.id===id)));
    if(!ctx.state.modules.some(m=>m.id===ctx.state.selectedId))ctx.state.selectedId=[...ctx.state.selectedIds].at(-1)||ctx.state.modules.find(m=>m.type==='contactor')?.id||null;
    if(!ctx.state.selectedIds.size&&ctx.state.selectedId)ctx.state.selectedIds.add(ctx.state.selectedId);
    const hadEngine=ctx.state.engineOk;ctx.state.engineOk=false;
    ctx.services.simulation.update();
    ctx.state.engineOk=hadEngine;
    if(ctx.state.engineOk){clearTimeout(ctx.state.syncTimer);ctx.services.simulation.syncEngine(true);}
    if(d.view&&Number.isFinite(d.view.scale)&&Number.isFinite(d.view.x)&&Number.isFinite(d.view.y)){ctx.state.scale=Math.max(.15,Math.min(2.5,d.view.scale));ctx.state.camera={x:d.view.x,y:d.view.y};ctx.services.editor.applyView();ctx.state.viewReady=true;}
    else ctx.services.editor.layoutView();
  }
  function electricalKey(d){
    const mods=(d.modules||[]).filter(ctx.services.simulation.electricalModule).map(m=>{
      const rest=Object.assign({},m);
      delete rest.x;delete rest.y;delete rest.z;delete rest.el;delete rest.pressed;delete rest.description;delete rest.subtitle;delete rest.labelColor;delete rest.buttonColor;delete rest.signalColor;delete rest.titleLines;delete rest.locked;delete rest.showBanner;delete rest.noteText;delete rest.noteStyle;delete rest.noteTone;delete rest.noteTitleTone;delete rest.noteSize;delete rest.noteWidth;delete rest.noteHeight;
      if(m.type==='rail'&&m.railPotential)rest.railPotential=true;
      return JSON.stringify(rest,Object.keys(rest).sort());
    }).sort();
    const links=(d.wires||[]).map(w=>w.a<w.b?w.a+'~'+w.b:w.b+'~'+w.a).sort();
    return JSON.stringify({mods,links});
  }
  function applyLayout(d){
    referenceExample = referenceCircuits.has(d.referenceExample) ? d.referenceExample : null;
    renderReference();
    const byId=new Map(d.modules.map(m=>[m.id,m]));
    ctx.state.modules=ctx.state.modules.filter(m=>{if(!byId.has(m.id)&&!ctx.services.simulation.electricalModule(m)){m.el.remove();return false;}return true;});
    d.modules.filter(m=>!ctx.services.simulation.electricalModule(m)&&!ctx.state.modules.some(x=>x.id===m.id)).forEach(m=>{const restored=ctx.services.editor.defaultNom({...m,pressed:false});ctx.state.modules.push(restored);ctx.services.editor.renderModule(restored);});
    ctx.state.selectedIds=new Set([...ctx.state.selectedIds].filter(id=>byId.has(id)));ctx.state.selectedId=[...ctx.state.selectedIds].at(-1)||null;
    ctx.state.modules.forEach(m=>{
      const prev=byId.get(m.id);
      if(!prev||!m.el)return;
      m.x=prev.x;m.y=prev.y;
      m.description=prev.description||'';m.subtitle=typeof prev.subtitle==='string'?prev.subtitle:null;m.labelColor=prev.labelColor;m.buttonColor=prev.buttonColor;m.signalColor=prev.signalColor;m.titleLines=prev.titleLines||2;m.latching=!!prev.latching;m.locked=!!prev.locked;for(const key of ['noteText','noteStyle','noteTone','noteTitleTone','noteSize','noteWidth','noteHeight','boxWidth','boxHeight','junctionVersion','showBanner','railPotential'])m[key]=prev[key];
      if(ctx.services.editor.isMeter(m)){m.probeRed=prev.probeRed||null;m.probeBlack=prev.probeBlack||null;m.measuredWire=prev.measuredWire||null;}
      ctx.services.editor.remount(m);
      if(Number.isFinite(prev.z)){m.z=prev.z;m.el.style.zIndex=prev.z;}
      ctx.services.editor.position(m);
    });
    ctx.state.nextZ=Math.max(ctx.state.nextZ,...ctx.state.modules.map(m=>(m.z||0)+1));
    (d.wires||[]).forEach((w,i)=>{if(ctx.state.wires[i]){ctx.state.wires[i].color=w.color;if(Array.isArray(w.route)&&w.route.every(p=>Array.isArray(p)&&p.length===2&&p.every(Number.isFinite)))ctx.state.wires[i].route=w.route.map(p=>[...p]);else delete ctx.state.wires[i].route;}});
    if(d.view&&Number.isFinite(d.view.scale)&&Number.isFinite(d.view.x)&&Number.isFinite(d.view.y)){
      ctx.state.scale=Math.max(.15,Math.min(2.5,d.view.scale));
      ctx.state.camera={x:d.view.x,y:d.view.y};
      ctx.services.editor.applyView();
      ctx.state.viewReady=true;
    }
    ctx.services.editor.renderComponents();ctx.services.editor.draw();
    ctx.services.projekt.schedulePersist();
  }
  function initial(){
    const result=[{id:'G1',type:'psu',x:60,y:120,voltage:24}];
    [268,740,1212].forEach((x,i)=>{const id='Q'+(i+1);result.push({id,type:'contactor',x,y:109,nomV:24,nomP:4},{id:id+'H',type:'auxiliary',parent:id,x:x+264,y:179});});
    result.push({id:'S1',type:'start',x:334,y:526},{id:'S2',type:'stop',x:660,y:526},{id:'M1',type:'motor',x:1280,y:526,nomV:24,nomP:50});
    result.push({id:'M2',type:'lineardrive',x:268,y:975,nomV:24,nomP:50,slide:.5});
    return {modules:result,wires:[],powered:true};
  }
  function selfHoldingBoard() {
    const demo = initial();
    const pairs = [
      ['G1.+24','S2.21','#bb4234'],
      ['S2.22','S1.13'],
      ['S1.14','Q1.A1'],
      ['Q1.A2','G1.0V','#347caa'],
      ['S1.13','Q1H.53'],
      ['S1.14','Q1H.54'],
      ['G1.+24','Q1.1','#bb4234'],
      ['Q1.2','M1.+'],
      ['M1.−','G1.0V','#347caa']
    ];
    const wiredModules = new Set(pairs.flatMap(([a,b]) => [a.split('.')[0], b.split('.')[0]]));
    demo.modules = demo.modules.filter(module => wiredModules.has(module.id));
    demo.wires = pairs.map(([a,b,color = '#d2ae27']) => ({a,b,color}));
    return demo;
  }
  function installationBoard(cross=false){
    const result=[
      {id:'T1',type:'mains',x:48,y:88,voltage:400},
      {id:'F1',type:'breaker',x:48,y:600,ratedCurrent:16,protectionOn:true,poles:1},
      {id:'X1',type:'junction',x:300,y:88},
      {id:'S1',type:'changeover',x:720,y:88,selection:0},
      {id:'S2',type:cross?'crossover':'changeover',x:1010,y:88,selection:0},
      {id:'X2',type:'socket',x:710,y:660},
      {id:'E1',type:'lamp',x:cross?1360:1040,y:680,nomV:230,nomP:60},
      {id:'E2',type:'lamp',x:1010,y:680,nomV:230,nomP:25,description:'Verbraucher im Steckdosenstromkreis'}
    ];
    if(cross)result.push({id:'S3',type:'changeover',x:1300,y:88,selection:0});
    else result.find(m=>m.id==='E2').x=1290;
    const end=cross?'S3':'S2',brown='#6b3f24',blue='#347caa';
    const pairs=[['T1.L1','F1.1',brown],['F1.2','X1.L.1',brown],['X1.L.2','S1.L',brown],['X1.L.3','X2.L',brown],
      ['T1.N','X1.N.1',blue],['X1.N.2','E1.N',blue],['X1.N.3','X2.N',blue],
      ['T1.PE','X1.PE.1','pe'],['X1.PE.2','E1.PE','pe'],['X1.PE.3','X2.PE','pe'],
      ['S1.1','X1.A.1','#384540'],['S1.2','X1.B.1','#8d9296'],
      ['X1.A.2','S2.1','#384540'],['X1.B.2','S2.2','#8d9296'],
      [end+'.L','E1.L',brown],['X2.L','E2.L',brown],['X2.N','E2.N',blue],['X2.PE','E2.PE','pe']];
    if(cross)pairs.push(['S2.3','S3.1','#384540'],['S2.4','S3.2','#8d9296']);
    return ctx.services.projekt.migrateJunctions({modules:result,wires:pairs.map(([a,b,color])=>({a,b,color})),powered:false});
  }
  function reversingBoard(){
    const modules=[
      {id:'G1',type:'psu',x:48,y:80,voltage:24,description:'24 V für Steuerung und Anker'},
      {id:'S0',type:'stop',x:280,y:110,description:'Halt'},
      {id:'S1',type:'start',x:500,y:110,description:'Rechtslauf'},
      {id:'S2',type:'start',x:720,y:110,description:'Linkslauf'},
      {id:'Q1',type:'contactor',x:48,y:420,nomV:24,nomP:4,description:'Rechtslauf'},
      {id:'Q1H',type:'auxiliary',parent:'Q1',x:312,y:500,description:'Selbsthaltung 53-54, Öffner 61-62 sperrt links'},
      {id:'Q2',type:'contactor',x:560,y:420,nomV:24,nomP:4,description:'Linkslauf'},
      {id:'Q2H',type:'auxiliary',parent:'Q2',x:824,y:500,description:'Selbsthaltung 53-54, Öffner 61-62 sperrt rechts'},
      {id:'M1',type:'motor',x:1100,y:470,nomV:24,nomP:50,description:'Polwender über Q1 und Q2'}
    ];
    const pairs=[
      ['G1.+24','S0.21','#bb4234'],
      ['S0.22','S1.13','#bb4234'],['S0.22','Q1H.53','#bb4234'],
      ['S1.14','Q2H.61','#bb4234'],['Q1H.54','Q2H.61','#bb4234'],
      ['Q2H.62','Q1.A1','#bb4234'],['Q1.A2','G1.0V','#347caa'],
      ['S0.22','S2.13','#bb4234'],['S0.22','Q2H.53','#bb4234'],
      ['S2.14','Q1H.61','#bb4234'],['Q2H.54','Q1H.61','#bb4234'],
      ['Q1H.62','Q2.A1','#bb4234'],['Q2.A2','G1.0V','#347caa'],
      ['G1.+24','Q1.1','#bb4234'],['Q1.2','M1.+','#bb4234'],
      ['G1.0V','Q1.3','#347caa'],['Q1.4','M1.−','#347caa'],
      ['G1.+24','Q2.1','#384540'],['Q2.2','M1.−','#384540'],
      ['G1.0V','Q2.3','#347caa'],['Q2.4','M1.+','#347caa']
    ];
    return {modules,wires:pairs.map(([a,b,c])=>({a,b,color:c})),powered:true};
  }
  function projectDocument(){const d=ctx.services.projekt.data();return {kind:'schuetzwerkstatt',schema:1,savedAt:new Date().toISOString(),modules:d.modules,wires:d.wires,view:d.view,referenceExample:d.referenceExample};}
  function saveProjectFile(){
    const blob=new Blob([JSON.stringify(ctx.services.projekt.projectDocument(),null,2)],{type:'application/json'});
    const a=document.createElement('a');a.href=URL.createObjectURL(blob);a.download='schuetzwerkstatt.json';a.click();
    setTimeout(()=>URL.revokeObjectURL(a.href),1000);
    ctx.services.editor.say('Projektdatei gespeichert. Auf einem anderen Rechner mit Projekt öffnen laden.');
  }
  function readProjectFile(file){
    const reader=new FileReader();
    reader.onload=()=>{
      try{
        const raw=JSON.parse(String(reader.result||''));
        const payload=raw&&raw.payload&&Array.isArray(raw.payload.modules)?raw.payload:raw;
        if(!payload||!Array.isArray(payload.modules)||!Array.isArray(payload.wires)){ctx.services.editor.say('Keine Verdrahtungswerkstatt-Datei.');return;}
        if(payload.kind&&payload.kind!=='schuetzwerkstatt'){ctx.services.editor.say('Keine Verdrahtungswerkstatt-Datei.');return;}
        if(payload.schema!=null&&Number(payload.schema)!==1){ctx.services.editor.say('Dateiformat wird nicht gelesen.');return;}
        const known=new Set(Object.keys(ctx.config.types));
        if(payload.modules.some(m=>!m||typeof m.id!=='string'||!known.has(m.type)||!Number.isFinite(Number(m.x))||!Number.isFinite(Number(m.y)))){ctx.services.editor.say('Datei enthält ein unbekanntes oder unvollständiges Bauteil.');return;}
        const ids=new Set(payload.modules.map(m=>m.id));
        if(ids.size!==payload.modules.length){ctx.services.editor.say('Datei enthält doppelte Kennzeichen.');return;}
        if(payload.modules.some(m=>m.type==='auxiliary'&&!ids.has(m.parent))){ctx.services.editor.say('Hilfskontakt ohne Leistungsschütz.');return;}
        const nextWires=payload.wires.map(w=>({a:String(w&&w.a||''),b:String(w&&w.b||''),color:w&&w.color||'#d2ae27',...(Array.isArray(w?.route)&&w.route.length<=200&&w.route.every(p=>Array.isArray(p)&&p.length===2&&p.every(Number.isFinite))?{route:w.route.map(p=>[...p])}:{})}));
        if(nextWires.some(w=>!w.a||!w.b)){ctx.services.editor.say('Datei enthält eine Leitung ohne Klemme.');return;}
        const owner=key=>{const d=key.indexOf('.');return d<0?key:key.slice(0,d);};
        if(nextWires.some(w=>!ids.has(owner(w.a))||!ids.has(owner(w.b)))){ctx.services.editor.say('Datei enthält eine Leitung zu einem fehlenden Bauteil.');return;}
        const load=()=>{ctx.services.projekt.remember();
        ctx.services.projekt.restore({modules:payload.modules.map(m=>({...m,x:Number(m.x),y:Number(m.y)})),wires:nextWires,view:payload.view,referenceExample:payload.referenceExample});
        ctx.services.editor.say('Projekt geladen. Spannung aus — danach Betrieb.');};
        if(ctx.services.simulation.requestElectricalEdit(load))load();
      }catch(e){ctx.services.editor.say('Datei lässt sich nicht lesen.');}
    };
    reader.readAsText(file);
  }

  function bindEvents() {
    if (bound) return;
    bound = true;
    const viewport = ctx.dom.$('#referenceViewport');
    const zoomAtCenter = factor => referenceApi?.reference.zoom(factor,viewport.clientWidth/2,viewport.clientHeight/2);
    ctx.dom.$('#referenceZoomIn').onclick = () => zoomAtCenter(1.25);
    ctx.dom.$('#referenceZoomOut').onclick = () => zoomAtCenter(0.8);
    ctx.dom.$('#referenceFit').onclick = () => referenceApi?.reference.fit();
    viewport.addEventListener('wheel',event => {
      event.preventDefault();
      const bounds = viewport.getBoundingClientRect();
      const delta = event.deltaY * (event.deltaMode === 1 ? 16 : event.deltaMode === 2 ? viewport.clientHeight : 1);
      referenceApi?.reference.zoom(Math.exp(-delta/400),event.clientX-bounds.left,event.clientY-bounds.top);
    },{passive:false});
    let pointer = null;
    viewport.addEventListener('pointerdown',event => {
      if(event.button !== 0 || pointer || !referenceApi) return;
      viewport.focus({preventScroll:true});
      viewport.setPointerCapture(event.pointerId);
      pointer = {id:event.pointerId,x:event.clientX,y:event.clientY};
    });
    viewport.addEventListener('pointermove',event => {
      if(pointer?.id !== event.pointerId) return;
      referenceApi?.reference.pan(event.clientX-pointer.x,event.clientY-pointer.y);
      pointer.x = event.clientX;
      pointer.y = event.clientY;
    });
    viewport.addEventListener('lostpointercapture',event => {
      if(pointer?.id === event.pointerId) pointer = null;
    });
    const arrows = new Map([['ArrowLeft',[36,0]],['ArrowRight',[-36,0]],['ArrowUp',[0,36]],['ArrowDown',[0,-36]]]);
    viewport.addEventListener('keydown',event => {
      event.stopPropagation();
      if(!referenceApi || event.ctrlKey || event.metaKey || event.altKey) return;
      const offset = arrows.get(event.key);
      if(offset) referenceApi.reference.pan(...offset);
      else if(event.key === '+' || event.key === '=') zoomAtCenter(1.25);
      else if(event.key === '-') zoomAtCenter(0.8);
      else return;
      event.preventDefault();
    });
    const examplesMenu = ctx.dom.$('#examplesMenu');
    const schuetzSubmenu = ctx.dom.$('#schuetzSubmenu');
    const examplesList = ctx.dom.$('#schuetzExamples');
    const wiringList = ctx.dom.$('#wiringExamples');
    const wiringSubmenu = ctx.dom.$('#wiringSubmenu');
    examplesMenu.addEventListener('toggle', () => {
      if (!examplesMenu.open) {
        schuetzSubmenu.open = false;
        if (wiringSubmenu) wiringSubmenu.open = false;
      }
    });
    const dialog = ctx.dom.$('#exampleDialog');
    const saveBeforeLoad = ctx.dom.$('#saveBeforeExample');
    let pendingExample = null;
    function requestExample(title,load) {
      const hasWork = ctx.state.modules.length > 0 || ctx.state.wires.length > 0;
      pendingExample = load;
      ctx.dom.$('#exampleDialogName').textContent = title;
      ctx.dom.$('#exampleDialogDescription').textContent = hasWork
        ? 'Die Schaltung ersetzt deinen aktuellen Aufbau. Du kannst ihn vorher als Projektdatei speichern.'
        : 'Die Schaltung wird auf der Montageplatte geöffnet.';
      if(ctx.services.simulation.liveMode()){
        ctx.dom.$('#exampleDialogDescription').textContent += ' Beim Laden wird die Spannung ausgeschaltet.';
      }
      saveBeforeLoad.hidden = !hasWork;
      examplesMenu.open = false;
      dialog.returnValue = '';
      dialog.showModal();
    }
    saveBeforeLoad.onclick = ctx.services.projekt.saveProjectFile;
    dialog.addEventListener('close', () => {
      const load = pendingExample;
      pendingExample = null;
      examplesMenu.querySelector('summary').focus();
      if(dialog.returnValue !== 'load' || !load) return;
      if(ctx.services.simulation.liveMode()) {
        ctx.services.simulation.setWorkMode('aufbau');
        if(ctx.services.simulation.liveMode()) return;
      }
      load();
    });
    for (const [id, title, create] of classroomExamples) {
      const button = document.createElement('button');
      button.type = 'button';
      button.className = 'example-option';
      button.dataset.exampleId = id;
      const [code,name] = title.split(' · ');
      const badge = document.createElement('span');
      badge.className = 'example-code';
      badge.textContent = code;
      const label = document.createElement('span');
      label.textContent = name;
      button.append(badge,label);
      button.onclick = () => requestExample(title,() => {
        ctx.services.projekt.remember();
        ctx.services.projekt.restore({...create(), referenceExample: id});
        ctx.services.editor.clearPick();
        ctx.services.editor.fit();
        ctx.services.editor.say(title + ' geladen. Betrieb einschalten.');
      });
      (wiringExamples.some(([key]) => key === id) && wiringList ? wiringList : examplesList).append(button);
    }
    ctx.dom.$('#reversing').onclick=()=>{ctx.services.projekt.remember();ctx.services.projekt.restore(ctx.services.projekt.reversingBoard());ctx.services.editor.say('Drehrichtungsumkehr geladen. Betrieb einschalten. S1 Rechtslauf, S2 Linkslauf. Der Öffner 61–62 des anderen Schützes sperrt die Gegenrichtung. S0 hält an.');};
    for(const [id,cross] of [['changeoverExample',false],['crossoverExample',true]])ctx.dom.$('#'+id).onclick=()=>{ctx.services.projekt.remember();ctx.services.projekt.restore(ctx.services.projekt.installationBoard(cross));ctx.services.editor.fit();ctx.services.editor.say((cross?'Kreuzschaltung':'Wechselschaltung')+' geladen. Betrieb einschalten. Jede Wippe schaltet E1 um; E2 hängt an der Steckdose.');};
    ctx.dom.$('#demo').onclick = () => {
      ctx.services.projekt.remember();
      ctx.services.projekt.restore(selfHoldingBoard());
      ctx.services.editor.fit();
      ctx.services.editor.say('Selbsthaltung geladen. Betrieb einschalten, dann S1 starten, S2 unterbrechen.');
    };
    ctx.dom.$('#reset').onclick=()=>{ctx.services.projekt.remember();ctx.services.projekt.restore(ctx.services.projekt.initial());ctx.services.editor.say('Grundaufbau geladen. Spannung aus — danach Betrieb.');};
    for (const id of ['reversing','demo','changeoverExample','crossoverExample','reset']) {
      const button = ctx.dom.$('#'+id);
      const load = button.onclick;
      button.onclick = () => requestExample(button.textContent,load);
    }
    ctx.dom.$('#clearBoard').onclick=()=>{if(!ctx.state.modules.length&&!ctx.state.wires.length){ctx.services.editor.say('Montagefläche ist schon leer.');return;}if(!confirm('Montagefläche leeren?\nAlle Geräte und Leitungen werden entfernt.'))return;ctx.services.projekt.remember();ctx.services.projekt.restore({modules:[],wires:[]});ctx.services.projekt.persist();ctx.services.editor.say('Montagefläche geleert.');};
    ctx.dom.$('#clear').onclick=()=>{if(!ctx.state.wires.length||!confirm('Alle '+ctx.state.wires.length+' Leitungen der Schaltung löschen? Die Bauteile bleiben erhalten.'))return;ctx.services.projekt.remember();ctx.services.editor.cancel();ctx.state.wires=[];ctx.services.simulation.update();ctx.services.simulation.noteWiringReset();ctx.dom.$('#projectMenu').open=false;};
    ctx.dom.$('#saveProject').onclick=ctx.services.projekt.saveProjectFile;
    ctx.dom.$('#openProject').onclick=()=>{ctx.dom.$('#projectFile').value='';ctx.dom.$('#projectFile').click();};
    ctx.dom.$('#restoreProject').onclick=()=>{const saved=ctx.services.projekt.loadStored();if(!saved)return;const load=()=>{ctx.services.projekt.remember();ctx.state.startupBlank=false;ctx.services.projekt.restore(saved);ctx.services.editor.say('Letzten Stand wiederhergestellt. Spannung aus — danach Betrieb.');};if(ctx.services.simulation.requestElectricalEdit(load))load();ctx.dom.$('#projectMenu').open=false;};
    ctx.dom.$('#projectFile').onchange=()=>{const file=ctx.dom.$('#projectFile').files&&ctx.dom.$('#projectFile').files[0];if(file)ctx.services.projekt.readProjectFile(file);};
    ctx.dom.$('#undo').onclick=()=>ctx.services.projekt.stepHistory(ctx.state.history,ctx.state.future);
    ctx.dom.$('#redo').onclick=()=>ctx.services.projekt.stepHistory(ctx.state.future,ctx.state.history);
    ['clearBoard','clear'].forEach(id=>{const button=ctx.dom.$('#'+id),action=button.onclick;button.onclick=e=>{if(ctx.services.simulation.requestElectricalEdit(()=>action(e)))action(e);};});
    window.addEventListener('pagehide',ctx.services.projekt.persist);
  }

  function loadInitial() {
const query = new URLSearchParams(location.search);
const requested = query.get('schaltung');
const example = classroomExamples.find(([id]) => id === requested);
const selfHolding = requested === 'selbsthaltung-dc';
ctx.dom.$('#otherExamples').hidden = query.get('unterricht') === '1';
if (example || selfHolding) ctx.config.STORE_KEY += ':' + requested;
try{ctx.state.saved=JSON.parse(ctx.dom.$('#saved-state').textContent);}catch{}
const embedded=ctx.state.saved&&Array.isArray(ctx.state.saved.modules)?ctx.state.saved:null;
ctx.state.startupBlank=!embedded;
if (example) {
  ctx.services.editor.setView('compare');
  ctx.services.projekt.restore({...example[2](), referenceExample: example[0]});
  ctx.services.editor.clearPick();
  ctx.services.editor.fit();
} else if (selfHolding) {
  ctx.services.projekt.restore(selfHoldingBoard());
  ctx.services.editor.clearPick();
  ctx.services.editor.fit();
} else {
  ctx.services.projekt.restore(embedded||{modules:[],wires:[],powered:false});
}
const recovered=ctx.services.projekt.loadStored(),restoreButton=ctx.dom.$('#restoreProject');
if(restoreButton)restoreButton.disabled=!(recovered&&(recovered.modules.length||recovered.wires.length));
if(!ctx.config.skipStore){try{const speed=localStorage.getItem(ctx.config.CURRENT_SPEED_STORE);if(speed!==null)ctx.services.editor.setCurrentSpeed(speed);if(localStorage.getItem(ctx.config.VOLT_STORE)==='1')ctx.services.editor.setShowVoltage(true);if(localStorage.getItem(ctx.config.CURRENT_STORE)==='1')ctx.services.editor.setShowCurrent(true);}catch(e){}}
ctx.state.booting=false;
ctx.services.projekt.persist();
if(ctx.config.skipStore)window.__workshop={present:ctx.services.simulation.getFaultPresentation,recovery:()=>ctx.state.recoveryMemory};
  }

  return {
    renderReference,
    syncReference,
    data,
    persist,
    schedulePersist,
    loadStored,
    paintHistory,
    remember,
    stepHistory,
    migrateJunctions,
    copyPackage,
    copySelection,
    pastePackage,
    restore,
    electricalKey,
    applyLayout,
    initial,
    installationBoard,
    reversingBoard,
    projectDocument,
    saveProjectFile,
    readProjectFile,
    bindEvents, loadInitial
  };
}
