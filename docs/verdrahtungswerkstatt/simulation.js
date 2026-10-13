// Die CircuitJS-Bridge liefert elektrische Zustände; hier werden Betrieb und Fehler verwaltet.
export function createSimulation(ctx) {
  let bound = false;
  function applyProtectionLocal(m,action){
    if(action==='on'&&!m.protectionTripped)m.protectionOn=true;
    if(action==='off')m.protectionOn=false;
    if(action==='reset'){m.protectionOn=false;m.protectionTripped=false;m.protectionReason=0;}
    if(action==='test'){m.protectionTripped=true;m.protectionReason=1;}
  }
  function flushProtection(){
    if(!ctx.state.engineOk||!ctx.state.engine?.setProtection)return;
    ctx.state.modules.forEach(m=>{
      if(!ctx.services.editor.isProtection(m)||!m.pendingProtection||m.protectionSent)return;
      if(ctx.state.engine.setProtection(m.id,m.pendingProtection))m.protectionSent=true;
    });
  }
  function operateProtection(m,action){
    ctx.services.projekt.remember();
    ctx.services.simulation.applyProtectionLocal(m,action);
    m.pendingProtection=action;
    m.protectionSent=false;
    if(ctx.state.engineOk&&ctx.state.engine?.setProtection&&ctx.state.engine.setProtection(m.id,action))m.protectionSent=true;
    ctx.services.editor.paintProtection(m);
    ctx.services.projekt.schedulePersist();
    if(m.protectionSent)ctx.services.simulation.refreshLive();
    else ctx.services.simulation.scheduleSync();
  }
  function triggerMovement(m){
    if(ctx.state.simMode!=='betrieb'||!ctx.state.powered||ctx.state.gesture||ctx.state.pending||ctx.state.handMode||ctx.state.spaceHeld)return;
    if(ctx.state.engine?.triggerMotion?.(m.id))ctx.services.simulation.refreshLive();
  }
  function electricalModule(m){return !['note','junction'].includes(m.type)&&!ctx.services.editor.isMeter(m);}
  function requestElectricalEdit(action,onCancel){
    if(!ctx.services.simulation.liveMode()||ctx.state.booting)return true;
    if(ctx.state.electricalRequest)return false;
    clearTimeout(ctx.state.liveTimer);ctx.services.editor.cancel();
    ctx.state.electricalRequest={action,onCancel};ctx.dom.$('#electricalEditDialog').showModal();return false;
  }
  function cancelElectricalEdit(){const request=ctx.state.electricalRequest;ctx.state.electricalRequest=null;request?.onCancel?.();}
  function graph(current,includeProtection=false){
    const g=new Map(),link=(a,b)=>{if(!g.has(a))g.set(a,new Set());if(!g.has(b))g.set(b,new Set());g.get(a).add(b);g.get(b).add(a);};
    ctx.state.wires.forEach(w=>link(w.a,w.b));
    ctx.state.modules.forEach(m=>{
      if(m.type==='terminal'){const span=ctx.services.editor.terminalSpan(m),bridges=m.terminalVersion===2?ctx.services.editor.terminalLinks(m):null;for(let n=1;n<=ctx.services.editor.terminalPoles(m);n++){link(m.id+'.'+n+'o',m.id+'.'+n+'u');if(bridges?bridges.includes(n-1):span>1&&(n-1)%span)link(m.id+'.'+(n-1)+'o',m.id+'.'+n+'o');}}
      if(m.type==='connector')for(let n=2;n<=ctx.services.editor.connectorPorts(m);n++)link(m.id+'.1',m.id+'.'+n);
      if(m.type==='socket')for(const pin of ['L','N','PE'])link(m.id+'.'+pin,m.id+'.'+pin+'out');
      if(m.type==='changeover')link(m.id+'.L',m.id+'.'+(m.selection===1?'2':'1'));
      if(m.type==='crossover'){link(m.id+'.1',m.id+'.'+(m.selection===1?'4':'3'));link(m.id+'.2',m.id+'.'+(m.selection===1?'3':'4'));}
      if(m.type==='rail')for(const bar of ['+24','0V','PE'])for(let n=2;n<=ctx.services.editor.railScrews(m);n++)link(m.id+'.'+bar+'.1',m.id+'.'+bar+'.'+n);
      if(ctx.services.editor.isProtection(m)){
        const on=m.protectionOn&&!m.protectionTripped;
        if(includeProtection&&on)for(let i=0;i<m.poles;i++)link(m.id+'.'+(2*i+1),m.id+'.'+(2*i+2));
        if(m.type==='motorprotection')link(m.id+(on?'.13':'.21'),m.id+(on?'.14':'.22'));
      }
      if(m.type==='motion'&&ctx.state.lastSnap?.motion?.[m.id]?.active)link(m.id+'.L',m.id+'.OUT');
      if(m.type==='mains')link(m.id+'.N',m.id+'.PE');
      if(m.type==='selector'&&m.selection!==0)link(m.id+(m.selection===-1?'.13':'.23'),m.id+(m.selection===-1?'.14':'.24'));
      if(m.type==='start'||m.type==='stop'||m.type==='emergency'){
        if(m.pressed)link(m.id+'.13',m.id+'.14');
        else link(m.id+'.21',m.id+'.22');
      }
      if(m.type==='timer'){const on=!!current[m.id];link(m.id+'.15',m.id+(on?'.18':'.16'));}
      if(ctx.config.contacts[m.type]||m.type==='relay'){
        const on=!!current[m.type==='auxiliary'?m.parent:m.id];
        (m.type==='relay'?ctx.services.editor.relayPairs(m):ctx.config.contacts[m.type]).forEach(([a,b,kind])=>{if(kind==='NO'?on:!on)link(m.id+'.'+a,m.id+'.'+b);});
      }
    });return g;
  }
  function reachable(g,key){const seen=new Set([key]),q=[key];for(let i=0;i<q.length;i++)for(const x of g.get(q[i])||[])if(!seen.has(x)){seen.add(x);q.push(x);}return seen;}
  function evaluate(){
    if(ctx.state.engineOk){ctx.services.simulation.applySnapshot();return;}
    ctx.services.editor.paintMeters(null);
    ctx.services.editor.paintWireVoltage(null);
    ctx.services.editor.paintWireCurrent(null);
    ctx.state.modules.forEach(m=>{
      const label=m.el.querySelector('.module-state');
      if(ctx.config.contacts[m.type]){m.el.classList.remove('energized');if(label)label.textContent='Wartet auf Engine';}
      if(m.type==='motor'||m.type==='lineardrive'||m.type==='acmotor'){m.el.classList.remove('running','reverse');if(label)label.textContent='Wartet auf Engine';}
      if(ctx.services.editor.isLight(m)){m.el.classList.remove('lit');if(label)label.textContent='Wartet auf Engine';}
      if(m.type==='lineardrive')ctx.services.editor.paintDrive(m,null);
      if((m.type==='psu'||m.type==='mains')&&label)label.textContent='Wartet auf Engine';
      if((m.type==='start'||m.type==='stop'||m.type==='emergency')&&label)label.textContent=m.pressed?'NO zu · NC auf':'NO auf · NC zu';
    });
    ctx.services.editor.renderSimControls();
    ctx.dom.$('#simStatus').textContent=ctx.state.engine?'CircuitJS ohne Werkstatt-Bridge':'Keine Verbindung zu CircuitJS';
    ctx.dom.$('#statusDot').className='status-dot error';
  }
  function update(){ctx.services.editor.mark();ctx.services.editor.draw();ctx.services.simulation.evaluate();ctx.services.editor.renderComponents();ctx.services.simulation.scheduleSync();ctx.services.projekt.schedulePersist();}
  function liveMode(){return ctx.state.simMode==='betrieb'||ctx.state.simMode==='paused';}
  function defaultHint(){
    if(ctx.state.selectionMode)return 'Auswahl · Bauteile antippen oder ein Rechteck ziehen. Ausgewählte Bauteile am Kopf gemeinsam verschieben; Löschen oben in der Auswahlleiste.';
    if(ctx.state.simMode==='aufbau')return 'Aufbau · Spannung aus. Klemmen verbinden, dann Betrieb einschalten.';
    if(ctx.state.simMode==='paused')return 'Eingefroren · Spannung bleibt. Elektrische Änderungen erfordern den Wechsel in Aufbau.';
    return 'Betrieb · Spannung an. Zum elektrischen Ändern in Aufbau wechseln.';
  }
  function noteWiringReset(){if(ctx.services.simulation.liveMode())ctx.services.editor.say('Ader geändert. Angezoogene Schütze fallen ab, die Simulation wird neu aufgebaut.');}
  function paintHint(){
    const el=ctx.dom.$('#hint'),line=el&&el.parentElement;
    if(!el)return;
    if(ctx.state.pending||ctx.state.gesture)return;
    if(ctx.state.gesture?.kind==='lead'){el.textContent='Messleitung auf eine Klemme oder einen Messpunkt ziehen. Am Messgerät ablegen oder mit Esc abbrechen.';return;}
    el.textContent=ctx.services.simulation.defaultHint();
    if(line)line.classList.toggle('live',ctx.services.simulation.liveMode());
  }
  function wantEngineRun(){return ctx.state.engineOk&&!ctx.state.latchedFault&&ctx.state.simMode==='betrieb';}
  function plantLive(){return ctx.state.powered&&!ctx.state.latchedFault&&ctx.state.simMode!=='aufbau';}
  function faultWireKey(w){return w.a<w.b?w.a+'|'+w.b:w.b+'|'+w.a;}
  function traceShort(conductors,start,end){
    const parent=new Map([[start,null]]),queue=[start];
    for(let i=0;i<queue.length&&!parent.has(end);i++)for(const next of conductors.get(queue[i])||[])if(!parent.has(next)){parent.set(next,queue[i]);queue.push(next);}
    if(!parent.has(end))return null;
    const nodes=[];for(let node=end;node!==null;node=parent.get(node))nodes.push(node);nodes.reverse();
    const steps=[],wireKeys=[],ids=new Set();
    for(let i=1;i<nodes.length;i++){
      const a=nodes[i-1],b=nodes[i],key=ctx.services.simulation.faultWireKey({a,b}),wire=ctx.state.wires.find(w=>ctx.services.simulation.faultWireKey(w)===key);
      const id=a.split('.')[0];ids.add(id);ids.add(b.split('.')[0]);
      steps.push({a,b,kind:wire?'wire':'contact',id:wire?null:id});
      if(wire)wireKeys.push(key);
    }
    return {code:'SHORT_CIRCUIT',message:'Kurzschluss',componentIds:[...ids],trace:{nodes,steps,wireKeys,start,end}};
  }
  function detectWorkshopShort(snap,diagnose=false){
    if((!ctx.state.powered&&!diagnose)||!snap)return null;
    // A just-operated contact can still have the previous solver voltages.
    // A short requires a conductive path across a source, without a load.
    const current=Object.fromEntries(Object.entries(snap.coils||{}).map(([id,coil])=>[id,!!coil.energized]));
    const conductors=ctx.services.simulation.graph(current,diagnose);
    const link=(a,b)=>{if(!conductors.has(a))conductors.set(a,new Set());if(!conductors.has(b))conductors.set(b,new Set());conductors.get(a).add(b);conductors.get(b).add(a);};
    ctx.state.modules.filter(m=>m.type==='lineardrive').forEach(m=>ctx.services.editor.linkDrive(m,link));
    for(const supply of ctx.state.modules.filter(m=>m.type==='psu')){
      if(ctx.services.simulation.reachable(conductors,supply.id+'.+24').has(supply.id+'.0V')){
        return ctx.services.simulation.traceShort(conductors,supply.id+'.+24',supply.id+'.0V');
      }
    }
    const phases=['L1','L2','L3'], refs=['N','PE'];
    for(const supply of ctx.state.modules.filter(m=>m.type==='mains')){
      for(let i=0;i<phases.length;i++){
        const from=ctx.services.simulation.reachable(conductors,supply.id+'.'+phases[i]);
        for(let j=i+1;j<phases.length;j++){
          if(from.has(supply.id+'.'+phases[j]))
            return ctx.services.simulation.traceShort(conductors,supply.id+'.'+phases[i],supply.id+'.'+phases[j]);
        }
        for(const n of refs){
          if(from.has(supply.id+'.'+n))
            return ctx.services.simulation.traceShort(conductors,supply.id+'.'+phases[i],supply.id+'.'+n);
        }
      }
    }
    return null;
  }
  function faultKind(fault){
    const code=String(fault?.code||'');
    const message=String(fault?.message||'');
    if(code==='SHORT_CIRCUIT')return 'short';
    if(/singul|singular/i.test(message))return 'singular';
    if(/converg|konverg/i.test(message))return 'convergence';
    if(/wire loop|drahtschleife|leitungsschleife/i.test(message))return 'loop';
    return 'unknown';
  }
  function getFaultPresentation(fault){
    const message=String(fault?.message||'Keine Detailmeldung verfügbar.');
    const kind=ctx.services.simulation.faultKind(fault);
    const where=(fault?.componentIds||[]).filter(Boolean);
    const contactsInPath=(fault?.trace?.steps||[]).filter(step=>step.kind==='contact').map(step=>step.id+' ('+step.a.slice(step.id.length+1)+'–'+step.b.slice(step.id.length+1)+')');
    const via=contactsInPath.length?' über '+contactsInPath.slice(0,4).join(', ')+(contactsInPath.length>4?' und weitere interne Verbindungen':''):'';
    const copy={
      short:{
        title:'Kurzschluss',
        status:'Kurzschluss · Reset nötig',
        toast:'Kurzschluss erkannt. Spannung aus. Verbindung auftrennen, dann Reset.',
        block:'Nach einem Kurzschluss zuerst Reset.',
        explanation:fault?.trace?'Kurzschluss zwischen '+fault.trace.start+' und '+fault.trace.end+via+'. Spannung ist abgeschaltet.':(where.length?'Kurzschluss an '+where.join(', ')+'. ':'')+'Spannung ist abgeschaltet. Der genaue Leitungsweg konnte nicht ermittelt werden.',
        next:'Markierte Verbindungen und Kontakte prüfen, den Kurzschlussweg auftrennen. Danach Reset.'
      },
      singular:{
        title:'Berechnung nicht eindeutig',
        status:'Berechnung nicht eindeutig · Reset nötig',
        toast:'Die Schaltung kann nicht eindeutig berechnet werden. Spannung aus. Verbindungen prüfen, dann Reset.',
        block:'Zuerst Reset. Danach Verbindungen und Spannungsquellen prüfen.',
        explanation:'Die Schaltung kann nicht eindeutig berechnet werden.',
        next:'Die Schaltung kann nicht eindeutig berechnet werden. Verbindungen und Spannungsquellen prüfen. Danach Reset.'
      },
      convergence:{
        title:'Kein stabiler Zustand',
        status:'Kein stabiler Zustand · Reset nötig',
        toast:'Die Berechnung findet keinen stabilen Zustand. Spannung aus. Verdrahtung prüfen, dann Reset.',
        block:'Zuerst Reset. Danach Verdrahtung und Bauteilwerte prüfen.',
        explanation:'Die Berechnung findet keinen stabilen Zustand.',
        next:'Die Berechnung findet keinen stabilen Zustand. Verdrahtung und Bauteilwerte prüfen. Danach Reset.'
      },
      loop:{
        title:'Leitungsschleife',
        status:'Leitungsschleife · Reset nötig',
        toast:'Eine geschlossene Schleife aus idealen Leitungen verhindert die Berechnung. Spannung aus. Verbindungen prüfen, dann Reset.',
        block:'Zuerst Reset. Danach doppelte oder ringförmige Verbindungen prüfen.',
        explanation:'Eine geschlossene Schleife aus idealen Leitungen verhindert die Berechnung.',
        next:'Eine geschlossene Schleife aus idealen Leitungen verhindert die Berechnung. Doppelte oder ringförmige Verbindungen prüfen. Danach Reset.'
      },
      unknown:{
        title:'Simulation angehalten',
        status:'Berechnung angehalten · Reset nötig',
        toast:'CircuitJS hat die Berechnung angehalten. Spannung aus. Schaltung prüfen, dann Reset.',
        block:'Zuerst Reset. Danach die technische Meldung und die Schaltung prüfen.',
        explanation:'CircuitJS hat die Berechnung angehalten.',
        next:'CircuitJS hat die Berechnung angehalten. Technische Meldung öffnen und Schaltung prüfen. Danach Reset.'
      }
    };
    return Object.assign({kind,technical:message},copy[kind]);
  }
  function haltOnFault(fault){
    if(!fault)return;
    if(ctx.state.latchedFault)return;
    const first=!ctx.state.latchedFault;
    ctx.state.latchedFault=fault;
    ctx.state.recoveryTime=null;
    ctx.state.simMode='aufbau';
    ctx.state.powered=false;
    ctx.services.editor.paintFaultScene();ctx.services.editor.draw();
    if(!ctx.state.engine)return;
    try{
      if(first&&ctx.state.engine.loadProject){
        const raw=ctx.state.engine.loadProject(ctx.services.simulation.projectPayload());
        const result=typeof raw==='string'?JSON.parse(raw):raw;
        if(result&&result.ok)ctx.state.lastImportedSig=ctx.services.simulation.topologySig();
      }
      if(ctx.state.engine.setSupplyOn)ctx.state.engine.setSupplyOn(false);
      if(ctx.state.engine.setRunning)ctx.state.engine.setRunning(false);
    }catch(e){}
    if(first)ctx.services.editor.say(ctx.services.simulation.getFaultPresentation(fault).toast);
  }
  function topologySig(){
    const parts=ctx.state.modules.filter(ctx.services.simulation.electricalModule).map(m=>m.id+':'+m.type+':'+(m.type==='connector'?ctx.services.editor.connectorPorts(m):'')+':'+(m.type==='motion'?JSON.stringify([m.nomV,m.motionDelay]):'')+':'+(m.parent||'')+':'+JSON.stringify(m.relayContacts||null)+':'+(m.type==='timer'?JSON.stringify([m.timerMode,m.delayOn,m.delayOff]):'')+':'+(ctx.services.editor.isProtection(m)?JSON.stringify([m.ratedCurrent,m.poles,m.characteristic]):'')).slice().sort();
    const links=ctx.state.wires.map(w=>w.a<w.b?w.a+'~'+w.b:w.b+'~'+w.a).sort();
    return parts.join(',')+'|'+links.join(',');
  }
  function readEngineSnap(){
    if(!ctx.state.engine||!ctx.state.engine.readSnapshot)return null;
    try{const raw=ctx.state.engine.readSnapshot();const snap=typeof raw==='string'?JSON.parse(raw):raw;return snap&&snap.ok?snap:null;}catch(e){return null;}
  }
  function engineHasBoard(){
    const snap=ctx.services.simulation.readEngineSnap();
    if(!snap||!ctx.state.modules.length)return false;
    return ctx.state.modules.every(m=>{
      if(ctx.services.editor.isProtection(m))return !!snap.protections?.[m.id];
      if(m.type==='timer'||m.type==='contactor'||m.type==='relay')return !!(snap.coils&&snap.coils[m.id]);
      if(m.type==='psu'||m.type==='mains')return !!(snap.supplies&&snap.supplies[m.id]);
      if(m.type==='motor'||m.type==='lineardrive'||m.type==='acmotor')return !!(snap.motors&&snap.motors[m.id]);
      return true;
    });
  }
  function setEngineTag(text,err){ctx.dom.$('#engineTag').textContent=text;ctx.dom.$('#engineTag').style.color=err?'#be442f':'#56665b';}
  function projectPayload(){
    return {schema:1,revision:++ctx.state.importSeq,running:false,supplyOn:ctx.state.powered,components:ctx.state.modules.filter(ctx.services.simulation.electricalModule).map(m=>({ports:m.type==='connector'?ctx.services.editor.connectorPorts(m):undefined,resistance:m.resistance,potPosition:m.potPosition,motionDelay:m.motionDelay,timerMode:m.timerMode,delayOn:m.delayOn,delayOff:m.delayOff,ratedCurrent:m.ratedCurrent,characteristic:m.characteristic,protectionOn:m.protectionOn,protectionTripped:m.protectionTripped,protectionReason:m.protectionReason,relayContacts:m.relayContacts,id:m.id,type:m.type,parentId:m.parent||null,parent:m.parent||null,designation:m.type==='auxiliary'?m.parent:m.id,slide:m.slide,selection:m.selection,nomV:Number.isFinite(m.nomV)?m.nomV:24,nomP:Number.isFinite(m.nomP)?m.nomP:(m.type==='contactor'?4:50),voltage:Number.isFinite(m.voltage)?m.voltage:24,ratedKw:Number.isFinite(m.ratedKw)?m.ratedKw:1.5,eta:Number.isFinite(m.eta)?m.eta:0.8,poles:ctx.services.editor.isProtection(m)?m.poles:m.type==='terminal'?ctx.services.editor.terminalPoles(m):0,span:m.type==='terminal'?ctx.services.editor.terminalSpan(m):1,terminalVersion:m.type==='terminal'&&m.terminalVersion===2?2:undefined,bridges:m.type==='terminal'&&m.terminalVersion===2?ctx.services.editor.terminalLinks(m):undefined,screws:m.type==='rail'?ctx.services.editor.railScrews(m):0})),wires:ctx.state.wires.map((w,i)=>({id:'w'+i,a:w.a,b:w.b}))};
  }
  function scheduleSync(){
    if(!ctx.state.engine||!ctx.state.engineOk)return;
    clearTimeout(ctx.state.syncTimer);
    ctx.state.syncTimer=setTimeout(ctx.services.simulation.syncEngine,80);
  }
  function applyPresses(){
    ctx.state.modules.forEach(m=>{if(m.type==='potentiometer'&&ctx.state.engine.setPotentiometer)ctx.state.engine.setPotentiometer(m.id,m.resistance,m.potPosition);});

    ctx.state.modules.forEach(m=>{if((m.type==='selector'||ctx.services.editor.isInstallationSwitch(m))&&ctx.state.engine.setSelector)ctx.state.engine.setSelector(m.id,m.selection);});
    ctx.state.modules.forEach(m=>{if((m.type==='start'||m.type==='stop'||m.type==='emergency')&&ctx.state.engine.setPressed)ctx.state.engine.setPressed(m.id,!!m.pressed);});
    if(ctx.state.engine.setSupplyOn)ctx.state.engine.setSupplyOn(ctx.state.powered);
    if(ctx.state.engine.setRealtime)ctx.state.engine.setRealtime(true);
  }
  function syncEngine(force){
    if(ctx.state.gesture||ctx.state.pending){ctx.services.simulation.scheduleSync();return;}
    if(!ctx.state.engine||!ctx.state.engine.version){ctx.state.engineOk=false;ctx.services.simulation.setEngineTag('Bridge fehlt',true);ctx.services.simulation.evaluate();return;}
    const sig=ctx.services.simulation.topologySig();
    if(!force&&sig===ctx.state.lastImportedSig&&ctx.services.simulation.engineHasBoard()){
      ctx.services.simulation.applyPresses();
      if(ctx.state.engine.setRunning)ctx.state.engine.setRunning(ctx.services.simulation.wantEngineRun());
      ctx.services.simulation.applySnapshot();
      return;
    }
    try{
      const raw=ctx.state.engine.loadProject(ctx.services.simulation.projectPayload());
      const result=typeof raw==='string'?JSON.parse(raw):raw;
      if(!result||!result.ok){ctx.state.engineOk=false;ctx.state.engineImportError=result&&result.error?result.error:'Import fehlgeschlagen';ctx.services.simulation.setEngineTag(ctx.state.engineImportError,true);ctx.services.simulation.evaluate();return;}
      ctx.state.engineOk=true;
      ctx.state.engineImportError='';
      ctx.state.lastImportedSig=sig;
      ctx.services.simulation.applyPresses();
      if(ctx.state.engine.setRunning)ctx.state.engine.setRunning(ctx.services.simulation.wantEngineRun());
      ctx.services.simulation.applySnapshot();
      ctx.services.simulation.flushProtection();
    }catch(e){ctx.state.engineOk=false;ctx.state.lastImportedSig='';ctx.state.engineImportError=String(e);ctx.services.simulation.setEngineTag(ctx.state.engineImportError,true);ctx.services.simulation.evaluate();}
  }
  function applyLive(){
    if(!ctx.state.engineOk)return;
    if(ctx.state.liveRaf)return;
    ctx.state.liveRaf=requestAnimationFrame(function(){ctx.state.liveRaf=0;ctx.services.simulation.applySnapshot();});
  }
  function refreshLive(){
    if(!ctx.state.engineOk)return;
    ctx.services.simulation.applyLive();
    setTimeout(function(){if(ctx.state.engineOk)ctx.services.simulation.applySnapshot();},50);
  }
  function applySnapshot(){
    if(!ctx.state.engine||!ctx.state.engine.readSnapshot)return;
    let snap;try{const raw=ctx.state.engine.readSnapshot();snap=typeof raw==='string'?JSON.parse(raw):raw;}catch(e){return;}
    if(!snap||!snap.ok)return;
    const previousSnap=ctx.state.lastSnap;
    ctx.state.lastSnap=snap;
    const short=ctx.services.simulation.detectWorkshopShort(snap);
    if(snap.fault){
      const msg=String(snap.fault.message||'');
      const diagnosed=short||(ctx.services.simulation.faultKind(snap.fault)==='short'?ctx.services.simulation.detectWorkshopShort(previousSnap||snap,true):null);
      ctx.services.simulation.haltOnFault(diagnosed&&(/singul|kurzschluss|short/i.test(msg)||snap.fault.code==='SHORT_CIRCUIT')?{...diagnosed,message:msg||diagnosed.message}:snap.fault);
    }else if(short)ctx.services.simulation.haltOnFault(short);
    else if(ctx.state.recoveryTime!==null&&snap.running&&snap.time>ctx.state.recoveryTime){ctx.state.latchedFault=null;ctx.state.recoveryTime=null;}
    ctx.services.editor.renderSnapshot(snap);
  }
  function setWorkMode(mode){
    if(!ctx.state.engineOk){ctx.services.editor.say('CircuitJS ist nicht verbunden.');return;}
    if(mode==='betrieb'&&ctx.state.latchedFault){ctx.services.editor.say(ctx.services.simulation.getFaultPresentation(ctx.state.latchedFault).block);return;}
    clearTimeout(ctx.state.syncTimer);ctx.services.editor.cancel();
    try{
      if(mode==='aufbau'){
        ctx.state.simMode='aufbau';
        ctx.state.powered=false;
        if(ctx.state.engine.setSupplyOn)ctx.state.engine.setSupplyOn(false);
        if(ctx.state.engine.setRunning)ctx.state.engine.setRunning(false);
        ctx.services.simulation.applySnapshot();
        ctx.services.editor.say('Aufbau: Spannung aus. Verdrahten und umbenennen, danach Betrieb.');
      }else if(mode==='paused'){
        if(ctx.state.simMode==='aufbau'){ctx.services.simulation.setWorkMode('betrieb');return;}
        ctx.state.simMode='paused';
        ctx.state.powered=true;
        if(ctx.state.engine.setSupplyOn)ctx.state.engine.setSupplyOn(true);
        if(ctx.state.engine.setRunning)ctx.state.engine.setRunning(false);
        ctx.services.simulation.applySnapshot();
        ctx.services.editor.say('Eingefroren: Spannung bleibt, Zeit steht.');
      }else{
        ctx.state.powered=true;
        ctx.state.simMode='betrieb';
        if(ctx.state.engine.setSupplyOn)ctx.state.engine.setSupplyOn(true);
        ctx.services.simulation.syncEngine(!ctx.services.simulation.engineHasBoard());
        if(!ctx.state.engineOk)return;
        ctx.services.simulation.applyPresses();
        ctx.state.recoveryTime=ctx.state.lastSnap?.time??0;
        ctx.state.engine.setRunning(true);
        ctx.services.simulation.applySnapshot();
        ctx.services.editor.say('Betrieb: Spannung an. Taster und Schalter bedienen. Zum elektrischen Ändern in Aufbau wechseln.');
      }
    }catch(e){ctx.state.engineOk=false;ctx.services.editor.renderSimControls();ctx.services.editor.say('CircuitJS-Steuerung fehlgeschlagen: '+e.message);}
  }
  function resetSimulation(){
    if(!ctx.state.engineOk||typeof ctx.state.engine.resetSimulation!=='function')return false;
    clearTimeout(ctx.state.syncTimer);ctx.services.editor.cancel();ctx.services.editor.closeDevMenu();
    const previousFault=ctx.state.latchedFault;
    try{
      ctx.state.latchedFault=null;ctx.state.recoveryTime=0;
      ctx.state.modules.forEach(m=>{m.pressed=m.type==='emergency'?!!m.latched:false;if(m.type==='lineardrive'){m.slide=.5;m.linealDir='';ctx.state.driveFrames.delete(m);}ctx.services.editor.paintTaster(m);});
      const raw=ctx.state.engine.resetSimulation(ctx.services.simulation.projectPayload());
      const result=typeof raw==='string'?JSON.parse(raw):raw;
      if(!result?.ok){ctx.state.latchedFault=previousFault;ctx.services.editor.renderSimControls();ctx.services.editor.draw();ctx.services.editor.say(result?.error||'Reset fehlgeschlagen.');return false;}
      if(ctx.state.simMode==='paused')ctx.state.simMode='betrieb';
      ctx.state.lastImportedSig=ctx.services.simulation.topologySig();
      ctx.services.simulation.applyPresses();
      if(ctx.state.engine.setRunning)ctx.state.engine.setRunning(ctx.services.simulation.wantEngineRun());
      ctx.services.simulation.applySnapshot();
      if(previousFault&&ctx.services.simulation.faultKind(previousFault)==='short'&&!ctx.state.latchedFault){const unresolved=ctx.services.simulation.detectWorkshopShort(ctx.services.simulation.readEngineSnap(),true);if(unresolved)ctx.services.simulation.haltOnFault(unresolved);}
      ctx.services.editor.renderSimControls();ctx.services.editor.draw();
      ctx.services.editor.say(ctx.state.latchedFault?'Reset ausgeführt. Der Fehler liegt noch in der Verdrahtung.':ctx.state.simMode==='aufbau'?'Reset. Spannung bleibt aus — Betrieb einschalten.':'Reset. Spannung an, Simulation läuft.');
      return true;
    }catch(e){ctx.state.latchedFault=previousFault;ctx.state.engineOk=false;ctx.services.editor.renderSimControls();ctx.services.editor.draw();ctx.services.editor.say('Reset fehlgeschlagen: '+e.message);return false;}
  }

  function bindEvents() {
    if (bound) return;
    bound = true;
    ctx.dom.$('#cancelElectricalEdit').onclick=()=>{ctx.dom.$('#electricalEditDialog').close();ctx.services.simulation.cancelElectricalEdit();};
    ctx.dom.$('#electricalEditDialog').addEventListener('cancel',ctx.services.simulation.cancelElectricalEdit);
    ctx.dom.$('#confirmElectricalEdit').onclick=()=>{
    const request=ctx.state.electricalRequest;ctx.state.electricalRequest=null;ctx.dom.$('#electricalEditDialog').close();
    if(!request)return;ctx.services.simulation.setWorkMode('aufbau');
    if(ctx.state.simMode!=='aufbau'||!ctx.state.engineOk){request.onCancel?.();return;}
    if(typeof ctx.state.engine.resetSimulation==='function'){
      if(!ctx.services.simulation.resetSimulation()){request.onCancel?.();return;}
    }else{
      ctx.state.modules.forEach(m=>{m.pressed=m.type==='emergency'?!!m.latched:false;});ctx.services.simulation.syncEngine(true);
      if(!ctx.state.engineOk){request.onCancel?.();return;}
    }
    request.action();ctx.services.editor.renderComponents();
  };
    ctx.dom.$('#modeBuild').onclick=()=>ctx.services.simulation.setWorkMode('aufbau');
    ctx.dom.$('#modeRun').onclick=()=>ctx.services.simulation.setWorkMode('betrieb');
    ctx.dom.$('#simPause').onclick=()=>ctx.services.simulation.setWorkMode(ctx.state.simMode==='paused'?'betrieb':'paused');
    ctx.dom.$('#simReset').onclick=ctx.services.simulation.resetSimulation;
    ctx.dom.$('#faultReset').onclick=ctx.services.simulation.resetSimulation;
    ctx.dom.$('#faultLocate').onclick=ctx.services.editor.showFaultLocation;
  }

  function connectEngine() {
(function loadEngine(){
    const iframe=ctx.dom.$('#circuitFrame');
    ctx.state.iframeId=Date.now();
    function attached(){
      try{
        const api=iframe.contentWindow&&iframe.contentWindow.CircuitJS1;
        if(!api||!api.workbench)return false;
        ctx.state.engine=api.workbench;
        ctx.state.engineOk=true;
        if(ctx.state.engine.setRealtime)ctx.state.engine.setRealtime(true);
        else if(api.setMaxTimeStep)api.setMaxTimeStep(0.001);
        api.onupdate=function(){if(ctx.state.engineOk)ctx.services.simulation.applyLive();};
        ctx.services.simulation.setEngineTag('CircuitJS verbunden');
        ctx.state.lastImportedSig='';
        ctx.services.simulation.syncEngine(true);
        return true;
      }catch(e){return false;}
    }
    function wait(n){if(attached())return;if(n>0)setTimeout(()=>wait(n-1),400);else {ctx.state.engineOk=false;ctx.services.simulation.setEngineTag('Verbindung verloren',true);ctx.services.simulation.evaluate();}}
    iframe.onload=function(){
      try{iframe.contentWindow.oncircuitjsloaded=attached;}catch(e){}
      wait(60);
    };
    iframe.src='circuitjs.html?v=20261002connectors1&cct='+encodeURIComponent('$ 768 0.001 10.391247 50 24 50')+'&lang=de&IECGates=true&running=false&editable=false';
  })();
  }

  return {
    applyProtectionLocal,
    flushProtection,
    operateProtection,
    triggerMovement,
    electricalModule,
    requestElectricalEdit,
    cancelElectricalEdit,
    graph,
    reachable,
    evaluate,
    update,
    liveMode,
    defaultHint,
    noteWiringReset,
    paintHint,
    wantEngineRun,
    plantLive,
    faultWireKey,
    traceShort,
    detectWorkshopShort,
    faultKind,
    getFaultPresentation,
    haltOnFault,
    topologySig,
    readEngineSnap,
    engineHasBoard,
    setEngineTag,
    projectPayload,
    scheduleSync,
    applyPresses,
    syncEngine,
    applyLive,
    refreshLive,
    applySnapshot,
    setWorkMode,
    resetSimulation,
    bindEvents, connectEngine
  };
}
