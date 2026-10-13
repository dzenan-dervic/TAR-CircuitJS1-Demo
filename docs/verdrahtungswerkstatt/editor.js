// Bauteile platzieren, Leitungen verbinden und die Montagefläche zeichnen.
export function createEditor(ctx) {
  let bound = false;
  function applySplitLayout(){
    const appEl=ctx.dom.$('#app');
    if(!appEl)return;
    const touch=matchMedia('(max-width:1180px)').matches;
    document.documentElement.classList.toggle('touch-layout',touch);
    if(ctx.state.viewMode==='stick'||touch){
      appEl.style.gridTemplateColumns='1fr';
      appEl.style.gridTemplateRows='';
      return;
    }
    appEl.style.gridTemplateRows='';
    const v=Math.min(0.72,Math.max(0.22,ctx.state.splitFrac));
    appEl.style.gridTemplateColumns=`minmax(220px,${v}fr) 44px minmax(0,${1-v}fr)`;
  }
  const displayName = m=>m.type==='auxiliary'?m.parent+' Hilfskontakte':m.id;
  const shortName = m=>m.type==='auxiliary'?m.parent:m.id;
  function paletteIcon(m){
    let glyphType=m.type;
    if(m.type==='note')glyphType=m.noteStyle==='frame'?'frame':m.noteStyle==='label'?'label':'note';
    else if(m.type==='auxiliary')glyphType='relay';
    else if(m.type==='stop')glyphType='emergency';
    else if(m.type==='start'&&m.latching)glyphType='switch';
    return document.querySelector('.parts-rail [data-add="'+glyphType+'"] .glyph svg');
  }
  function paintDetailPreview(m){
    const box=ctx.dom.$('#detailPreview');if(!box)return;box.replaceChildren();box.setAttribute('aria-hidden','true');
    if(!m||!m.el||!m.el.querySelector('.face')){box.hidden=true;return;}
    const icon=ctx.services.editor.paletteIcon(m);
    if(icon){box.classList.add('glyph-preview');box.append(icon.cloneNode(true));box.hidden=false;return;}
    box.classList.remove('glyph-preview');
    const clone=m.el.cloneNode(true);
    clone.classList.remove('selected','dragging');
    clone.querySelectorAll('.handle,.state-label,.module-wires,.remove').forEach(n=>n.remove());
    clone.querySelectorAll('button,input,select,textarea,a').forEach(n=>{n.disabled=true;n.tabIndex=-1;});
    const stage=document.createElement('div');stage.className='detail-preview-stage';stage.append(clone);box.append(stage);box.hidden=false;
    const face=clone.querySelector('.face');
    const w=Math.max(1,face.offsetWidth),h=Math.max(1,face.offsetHeight);
    stage.style.width=w+'px';stage.style.height=h+'px';
    stage.style.transform='translate(-50%,-50%) scale('+Math.min(52/w,52/h)+')';
  }
  const fmtV = v=>{const n=Number(v);return (Number.isFinite(n)?n:24).toFixed(1);};
  const isLight = m=>m.type==='lamp'||m.type==='pilot';
  const isCombinedTaster = m=>m.type==='start'||m.type==='stop';
  const defaultPrefix = type=>type==='potentiometer'?'R':type==='motion'?'B':type==='note'?'N':type==='timer'?'K':type==='motorprotection'?'Q':type==='breaker'?'F':type==='relay'?'K':type==='voltmeter'?'V':type==='ammeter'?'A':type==='contactor'?'Q':type==='psu'?'G':type==='mains'?'T':(type==='motor'||type==='lineardrive'||type==='acmotor')?'M':type==='lamp'?'E':type==='pilot'?'P':['terminal','rail','junction','connector','socket'].includes(type)?'X':'S';
  function splitTag(id){const hit=String(id||'').match(/^([A-Za-z]+)(\d+)$/);return hit?{prefix:hit[1],n:Number(hit[2])}:{prefix:'',n:1};}
  function nextId(prefix){let n=1;while(ctx.state.modules.some(m=>m.id===prefix+n))n++;return prefix+n;}
  function rewriteTerm(key,from,to){if(key===from)return to;if(key.startsWith(from+'.'))return to+key.slice(from.length);return key;}
  function remount(m){
    const z=m.z,pressed=m.pressed,latched=m.latched;
    m.el?.remove();
    ctx.services.editor.renderModule(m);
    m.el.classList.toggle('selected',ctx.state.selectedIds.has(m.id));
    if(Number.isFinite(z)){m.z=z;m.el.style.zIndex=z;}
    m.pressed=pressed;m.latched=latched;
    if(m.type==='selector')ctx.services.editor.paintSelector(m);
    if(m.type==='lineardrive')ctx.services.editor.paintDrive(m,ctx.state.lastSnap);
    if(m.type==='start'||m.type==='stop'||m.type==='emergency')ctx.services.editor.paintTaster(m);
    ctx.services.editor.paintDeviceFace(m);
  }
  function renameModule(m,next){
    if(next===m.id)return true;
    if(ctx.state.modules.some(x=>x.id===next)){ctx.services.editor.say(next+' ist schon vergeben.');return false;}
    const from=m.id,aux=ctx.state.modules.find(x=>x.parent===from);
    const auxFrom=aux?.id,auxTo=next+'H';
    if(aux&&ctx.state.modules.some(x=>x!==aux&&x.id===auxTo)){ctx.services.editor.say(auxTo+' ist schon vergeben.');return false;}
    ctx.state.modules.filter(ctx.services.editor.isMeter).forEach(x=>{if(x.probeRed)x.probeRed=ctx.services.editor.rewriteTerm(x.probeRed,from,next);if(x.probeBlack)x.probeBlack=ctx.services.editor.rewriteTerm(x.probeBlack,from,next);if(x.measuredWire){x.measuredWire.a=ctx.services.editor.rewriteTerm(x.measuredWire.a,from,next);x.measuredWire.b=ctx.services.editor.rewriteTerm(x.measuredWire.b,from,next);}});
    ctx.state.wires.forEach(w=>{w.a=ctx.services.editor.rewriteTerm(w.a,from,next);w.b=ctx.services.editor.rewriteTerm(w.b,from,next);if(aux){w.a=ctx.services.editor.rewriteTerm(w.a,auxFrom,auxTo);w.b=ctx.services.editor.rewriteTerm(w.b,auxFrom,auxTo);}});
    if(ctx.state.selectedId===from)ctx.state.selectedId=next;
    if(ctx.state.selectedIds.delete(from))ctx.state.selectedIds.add(next);
    if(aux&&ctx.state.selectedIds.delete(auxFrom))ctx.state.selectedIds.add(auxTo);
    m.id=next;
    if(aux){aux.id=auxTo;aux.parent=next;}
    ctx.services.editor.remount(m);
    if(aux)ctx.services.editor.remount(aux);
    ctx.state.lastImportedSig='';
    return true;
  }
  function rebuildAfterRename(id){
    if(!ctx.state.engine||!ctx.state.engineOk){ctx.services.editor.say(id+' übernommen.');return;}
    clearTimeout(ctx.state.syncTimer);
    try{
      if(ctx.state.engine.setRunning)ctx.state.engine.setRunning(false);
      const api=typeof ctx.state.engine.resetSimulation==='function'?ctx.state.engine.resetSimulation.bind(ctx.state.engine):ctx.state.engine.loadProject.bind(ctx.state.engine);
      const raw=api(ctx.services.simulation.projectPayload());
      const result=typeof raw==='string'?JSON.parse(raw):raw;
      if(!result||!result.ok){
        ctx.state.engineOk=false;
        ctx.services.simulation.setEngineTag(result&&result.error?result.error:'Import fehlgeschlagen',true);
        ctx.services.simulation.evaluate();
        ctx.services.editor.say('Schild zeigt '+id+', CircuitJS hat den Kreis nicht übernommen. Aufbau prüfen.');
        return;
      }
      ctx.state.engineOk=true;
      ctx.state.lastImportedSig=ctx.services.simulation.topologySig();
      ctx.services.simulation.applyPresses();
      if(ctx.state.engine.setRunning)ctx.state.engine.setRunning(ctx.services.simulation.wantEngineRun());
      ctx.services.simulation.applySnapshot();
      ctx.services.editor.renderSimControls();
      ctx.services.editor.say(id+' übernommen. Simulation neu aufgebaut.');
    }catch(e){
      ctx.state.engineOk=false;
      ctx.state.lastImportedSig='';
      ctx.services.simulation.setEngineTag(String(e),true);
      ctx.services.simulation.evaluate();
      ctx.services.editor.say('Umbenennen fehlgeschlagen: '+(e.message||e));
    }
  }
  function tagFieldHtml(m){
    const base=ctx.config.tagPrefixes[m.type];
    if(!base)return '';
    const {prefix,n}=ctx.services.editor.splitTag(m.id);
    const letters=base.includes(prefix)||!prefix?base.slice():base.concat(prefix);
    const current=letters.includes(prefix)?prefix:letters[0];
    const select=letters.length===1?'<span class="tag-prefix">'+letters[0]+'</span><input type="hidden" id="devPrefix" value="'+letters[0]+'">':'<select id="devPrefix" aria-label="Kennbuchstabe">'+letters.map(p=>'<option value="'+p+'"'+(p===current?' selected':'')+'>'+p+'</option>').join('')+'</select>';
    return '<div class="tag-field"><span>Kennzeichnung</span><div class="tag-row">'+select+'<input id="devNumber" type="number" min="1" max="99" step="1" value="'+(n||1)+'" aria-label="Kennnummer"></div></div>';
  }
  function readTag(m){
    if(!ctx.config.tagPrefixes[m.type]||!ctx.dom.$('#devPrefix')||!ctx.dom.$('#devNumber'))return null;
    const prefix=ctx.dom.$('#devPrefix').value,n=Number(ctx.dom.$('#devNumber').value);
    const allowed=new Set(ctx.config.tagPrefixes[m.type]);
    const current=ctx.services.editor.splitTag(m.id).prefix;if(current)allowed.add(current);
    if(!allowed.has(prefix))return {ok:false,error:'Buchstabe nicht zulässig.'};
    if(!Number.isInteger(n)||n<1||n>99)return {ok:false,error:'Nummer 1 bis 99.'};
    const id=prefix+n;
    if(id!==m.id&&ctx.state.modules.some(x=>x.id===id))return {ok:false,error:id+' ist schon vergeben.'};
    return {ok:true,id};
  }
  function relayContactSet(list){
    const fallback=['NO','NC','NC','NO'];
    const src=Array.isArray(list)?list.filter(c=>c&&(c.kind==='NO'||c.kind==='NC')):[];
    const used=new Set(),out=[];
    for(let i=0;i<4;i++){
      const c=src[i];
      let number=c&&Number.isInteger(Number(c.number))?Number(c.number):0;
      if(number<1||number>9||used.has(number)){
        number=0;
        for(let n=1;n<=9&&!number;n++)if(!used.has(n))number=n;
      }
      used.add(number);
      out.push({kind:c?c.kind==='NC'?'NC':'NO':fallback[i],number});
    }
    return out;
  }
  function terminalPoles(m){const n=Number(m.poles);return Number.isInteger(n)&&n>=2&&n<=12?n:6;}
  function terminalSpan(m){const n=Number(m.span);return n===2||n===4?n:1;}
  function terminalLinks(m){
    const n=ctx.services.editor.terminalPoles(m);
    return [...new Set((Array.isArray(m.bridges)?m.bridges:[]).filter(i=>Number.isInteger(i)&&i>=1&&i<n))].sort((a,b)=>a-b);
  }
  function terminalGroupIndex(m,n){
    if(n<1||n>ctx.services.editor.terminalPoles(m))return -1;
    const links=ctx.services.editor.terminalLinks(m);while(n>1&&links.includes(n-1))n--;
    return n;
  }
  function toggleTerminalBridge(m,edge){
    if(m.type!=='terminal'||m.terminalVersion!==2||!Number.isInteger(edge)||edge<1||edge>=ctx.services.editor.terminalPoles(m))return;
    if(!ctx.services.simulation.requestElectricalEdit(()=>ctx.services.editor.toggleTerminalBridge(m,edge)))return;
    const restoreFocus=m.el?.querySelector('[data-terminal-bridge="'+edge+'"]')===document.activeElement;
    ctx.services.projekt.remember();const links=ctx.services.editor.terminalLinks(m);
    m.bridges=links.includes(edge)?links.filter(i=>i!==edge):links.concat(edge).sort((a,b)=>a-b);
    ctx.state.lastImportedSig='';m.el?.remove();ctx.services.editor.renderModule(m);ctx.services.editor.selectModule(m.id);ctx.services.simulation.update();
    if(restoreFocus)m.el.querySelector('[data-terminal-bridge="'+edge+'"]').focus({preventScroll:true});
    ctx.services.editor.say('Steckbrücke '+m.id+': '+edge+'–'+(edge+1)+(m.bridges.includes(edge)?' eingesetzt.':' entfernt.'));
  }
  function connectorPorts(m){return [2,3,5].includes(Number(m.ports))?Number(m.ports):3;}
  function railScrews(m){return Number(m.screws)===8?8:5;}
  function terminalGroups(m){
    const n=ctx.services.editor.terminalPoles(m),span=ctx.services.editor.terminalSpan(m),out=[],links=m.terminalVersion===2?ctx.services.editor.terminalLinks(m):null;
    for(let s=1;s<=n;){let e=links?s:Math.min(n,s+span-1);if(links)while(e<n&&links.includes(e))e++;out.push(s===e?String(s):s+'–'+e);s=e+1;}
    return out;
  }
  function terminalCaption(m){const n=ctx.services.editor.terminalPoles(m);return m.terminalVersion===2?n+' Durchgangsklemmen · '+ctx.services.editor.terminalLinks(m).length+' Steckbrücken':n+' Durchgangsklemmen · '+(ctx.services.editor.terminalSpan(m)===1?'jede Nummer für sich':ctx.services.editor.terminalGroups(m).join(' · '));}
  function railCaption(m){return '+24 · 0 V · PE, je '+ctx.services.editor.railScrews(m)+' Schrauben';}
  function dropLoose(m){
    const keys=new Set();
    if(m.type==='terminal'){for(let i=1;i<=ctx.services.editor.terminalPoles(m);i++){keys.add(m.id+'.'+i+'o');keys.add(m.id+'.'+i+'u');}}
    if(m.type==='connector')for(let i=1;i<=ctx.services.editor.connectorPorts(m);i++)keys.add(m.id+'.'+i);
    if(m.type==='rail'){for(const bar of ['+24','0V','PE'])for(let i=1;i<=ctx.services.editor.railScrews(m);i++)keys.add(m.id+'.'+bar+'.'+i);}
    const gone=k=>k&&k.startsWith(m.id+'.')&&!keys.has(k);
    ctx.state.wires=ctx.state.wires.filter(w=>!gone(w.a)&&!gone(w.b)&&!ctx.services.editor.internallyCommon(w.a,w.b));
    ctx.state.modules.filter(ctx.services.editor.isMeter).forEach(x=>{if(gone(x.probeRed))x.probeRed=null;if(gone(x.probeBlack))x.probeBlack=null;if(x.measuredWire&&(gone(x.measuredWire.a)||gone(x.measuredWire.b)))x.measuredWire=null;});
  }
  const isProtection = m=>m.type==='motorprotection'||m.type==='breaker';
  const componentTitle = m=>ctx.services.editor.isCombinedTaster(m)&&m.latching?'SCHALTER · RASTEND':m.type==='note'&&m.noteStyle==='label'?'BESCHRIFTUNG':m.type==='note'&&m.noteStyle==='frame'?'MARKIERUNGSRAHMEN':m.type==='terminal'&&m.terminalVersion===2?'REIHENKLEMMEN V2':ctx.config.types[m.type];
  function defaultNom(m){
    m.locked=!!m.locked;
    if(m.type==='junction'){m.junctionVersion=2;m.boxWidth=Number.isFinite(m.boxWidth)?Math.max(240,Math.min(1600,m.boxWidth)):420;m.boxHeight=Number.isFinite(m.boxHeight)?Math.max(180,Math.min(1600,m.boxHeight)):360;}
    if(m.type==='connector'){m.ports=ctx.services.editor.connectorPorts(m);m.showBanner=m.showBanner===true;}
    if(m.type==='potentiometer'){
      m.resistance=Number.isFinite(m.resistance)?Math.max(100,Math.min(1000000,m.resistance)):10000;
      m.potPosition=Number.isFinite(m.potPosition)?Math.max(0,Math.min(1,m.potPosition)):.5;
    }
    if(m.type==='motion'){
      m.nomV=m.nomV===24?24:230;
      m.motionDelay=Number.isFinite(m.motionDelay)?Math.max(1,Math.min(600,m.motionDelay)):10;
    }
    if(m.type==='note'){
      m.noteText=typeof m.noteText==='string'?m.noteText.slice(0,1000):'Doppelklick zum Bearbeiten';
      m.noteStyle=['label','frame'].includes(m.noteStyle)?m.noteStyle:'card';
      m.noteSize=Number.isFinite(m.noteSize)?Math.max(12,Math.min(32,m.noteSize)):16;
      m.noteWidth=Number.isFinite(m.noteWidth)?Math.max(120,Math.min(1600,m.noteWidth)):260;
      m.noteHeight=Number.isFinite(m.noteHeight)?(m.noteHeight<=0&&m.noteStyle==='label'?0:Math.max(48,Math.min(1600,m.noteHeight))):m.noteStyle==='frame'?240:0;
      if(!['transparent','white','cream'].includes(m.noteTitleTone))m.noteTitleTone='transparent';
      if(!ctx.config.noteTones[m.noteTone])m.noteTone='cream';
    }

    if(!Number.isInteger(m.titleLines)||m.titleLines<1||m.titleLines>3)m.titleLines=2;
    if(typeof m.subtitle!=='string')m.subtitle=null;
    if(m.type==='timer'){
      if(![0,1,2,3].includes(m.timerMode))m.timerMode=0;
      for(const key of ['delayOn','delayOff'])if(!Number.isFinite(m[key])||m[key]<.01||m[key]>3600)m[key]=3;
      if(!Number.isFinite(m.nomV))m.nomV=24;if(!Number.isFinite(m.nomP))m.nomP=2;
    }
    if(ctx.services.editor.isProtection(m)){
      if(!Number.isFinite(m.ratedCurrent)||m.ratedCurrent<=0)m.ratedCurrent=m.type==='motorprotection'?2.8:16;
      if(m.type==='breaker'&&!ctx.config.breakerRatings.includes(m.ratedCurrent))m.ratedCurrent=16;
      m.poles=m.type==='motorprotection'||m.poles===3?3:1;
      if(!['B','C','D'].includes(m.characteristic))m.characteristic='B';
      if(![6,10,15].includes(m.breakingCapacity))m.breakingCapacity=6;
      m.protectionOn=!!m.protectionOn;m.protectionTripped=!!m.protectionTripped;
    }
    if(m.type==='relay'){
      const next=ctx.services.editor.relayContactSet(m.relayContacts);
      const same=Array.isArray(m.relayContacts)&&m.relayContacts.length===4&&next.every((c,i)=>m.relayContacts[i].kind===c.kind&&Number(m.relayContacts[i].number)===c.number);
      if(!same)m.relayContacts=next;
    }

    if(m.type==='ammeter'&&m.measuredWire&&!m.probeRed&&!m.probeBlack){m.probeRed=m.measuredWire.a;m.probeBlack=m.measuredWire.b;}
    if(m.type==='psu'&&!Number.isFinite(m.voltage))m.voltage=24;if(m.type==='mains'&&!Number.isFinite(m.voltage))m.voltage=400;
    if(m.type==='contactor'||m.type==='relay'){if(!Number.isFinite(m.nomV))m.nomV=24;if(!Number.isFinite(m.nomP))m.nomP=4;}
    if(m.type==='motor'||m.type==='lineardrive'){if(!Number.isFinite(m.nomV))m.nomV=24;if(!Number.isFinite(m.nomP))m.nomP=50;}if(m.type==='acmotor'){if(!Number.isFinite(m.ratedKw))m.ratedKw=1.5;if(!Number.isFinite(m.eta)||m.eta<=0)m.eta=0.8;}
    if(ctx.services.editor.isLight(m)){if(!Number.isFinite(m.nomV))m.nomV=24;if(!Number.isFinite(m.nomP))m.nomP=m.type==='lamp'?10:1.5;}
    if(m.type==='lineardrive')m.slide=Math.max(0,Math.min(1,Number.isFinite(m.slide)?m.slide:.5));
    if(m.type==='pilot'&&!ctx.config.signalColors[m.signalColor])m.signalColor='green';
    if(ctx.services.editor.isCombinedTaster(m)){
      m.latching=!!m.latching;
      if(!ctx.config.buttonColors[m.buttonColor])m.buttonColor=m.type==='stop'?'red':'green';
      if(!ctx.config.signalColors[m.signalColor])m.signalColor='white';
    }
    if(m.type==='emergency'){m.latched=!!m.latched;m.pressed=m.latched;}
    if(ctx.services.editor.isInstallationSwitch(m)&&![0,1].includes(m.selection))m.selection=0;
    if(m.type==='selector'&&![-1,0,1].includes(m.selection))m.selection=0;
    if(m.type==='terminal'){m.poles=ctx.services.editor.terminalPoles(m);m.span=ctx.services.editor.terminalSpan(m);if(m.terminalVersion===2){m.span=1;m.bridges=ctx.services.editor.terminalLinks(m);m.terminalColors=Array.from({length:m.poles},(_,i)=>['blue','pe'].includes(m.terminalColors?.[i])?m.terminalColors[i]:'gray');}}
    if(m.type==='rail')m.screws=ctx.services.editor.railScrews(m);
    return m;
  }
  const isInstallationSwitch = m=>m.type==='changeover'||m.type==='crossover';
  function moduleCaption(m){
    if(m.type==='changeover')return 'L gemeinsam · 1 / 2 Korrespondierende';
    if(m.type==='crossover')return '1 / 2 hinein · 3 / 4 heraus';
    if(m.type==='junction')return 'Leeres Gehäuse · frei skalierbar';
    if(m.type==='connector')return ctx.services.editor.connectorPorts(m)+' Leiter · intern verbunden';
    if(m.type==='socket')return 'L / N / PE · getrennte Kontakte';
    if(m.type==='potentiometer')return (m.resistance/1000).toLocaleString('de-DE')+' kΩ · linear · 3 Anschlüsse';
    if(m.type==='motion')return m.nomV+' V '+(m.nomV===24?'DC':'AC')+' · Nachlauf '+m.motionDelay+' s';
    if(m.type==='note')return '';

    if(m.type==='timer')return '15 gemeinsam · 16 NC · 18 NO';
    if(ctx.services.editor.isProtection(m))return m.type==='motorprotection'?'3 Pole · 1 NO + 1 NC':'Leitungsschutzschalter';
    if(ctx.services.editor.isMeter(m))return 'Messleitungen · nicht im Stromkreis';
    if(m.type==='relay')return 'A1 / A2 · eigene Spule';
    if(m.type==='terminal')return ctx.services.editor.terminalCaption(m);
    if(m.type==='rail')return ctx.services.editor.railCaption(m);
    if(m.type==='contactor')return '3 Leistungskontakte + 1 NO';
    if(m.type==='auxiliary')return 'gekoppelt an '+(m.parent||'');
    if(m.type==='psu')return 'Versorgung 24 V DC';
    if(m.type==='mains')return 'L1 L2 L3 N PE · TN am Ursprung';
    if(m.type==='lineardrive')return 'B1–B3 Endschalter · Inkrementalgeber B6/B7';
    if(ctx.services.editor.isLight(m))return m.type==='lamp'?'Leuchte':'Signalleuchte';
    if(m.type==='selector')return '2 getrennte Schließer · rastend';
    if(m.type==='emergency')return '1 Öffner · 1 Schließer';
    if(m.type==='acmotor')return 'U1 V1 W1 · U2 V2 W2 · Stern oder Dreieck';
    if(m.type==='motor')return 'Verbraucher';
    return '13–14 · 21–22 · '+ctx.services.editor.lampTag(m)+(m.latching?' · rastend':'');
  }
  function moduleSubtitle(m){return m.subtitle===null?ctx.services.editor.moduleCaption(m):m.subtitle;}
  function badgeInk(hex){
    const n=parseInt(String(hex).slice(1),16);
    const r=(n>>16)&255,g=(n>>8)&255,b=n&255;
    return (0.2126*r+0.7152*g+0.0722*b)/255>0.62?'#142018':'#fff';
  }
  function badgeTone(m){
    const picked=String(m.labelColor||'').toLowerCase();
    if(/^#[0-9a-f]{6}$/.test(picked)&&picked!=='#394b41')return picked;
    if(m.type==='auxiliary'&&m.parent){const parent=ctx.state.modules.find(x=>x.id===m.parent);if(parent&&parent!==m)return ctx.services.editor.badgeTone(parent);}
    if(m.type==='psu'||m.type==='mains')return '#0e9f78';
    const n=parseInt(String(m.id).replace(/\D/g,''),10)||1;
    return ['#1d6fe8','#0e9f78','#be185d','#c2410c','#6d4ad4','#1e3a8a'][(n-1)%6];
  }
  function paintHandle(m){
    if(!m.el)return;
    const tone=ctx.services.editor.badgeTone(m);
    m.el.style.setProperty('--pick',tone);
    const label=m.el.querySelector('.handle-label');
    if(label){label.textContent=ctx.services.editor.shortName(m);label.style.background=tone;label.style.color=ctx.services.editor.badgeInk(tone);}
    const name=m.el.querySelector('.handle-name');
    if(name){name.textContent=m.description||ctx.services.editor.componentTitle(m);name.title=name.textContent;name.style.setProperty('--title-lines',String(m.titleLines||2));const length=[...name.textContent].length;name.style.setProperty('--title-font-size',length>32?'9px':length>22?'10px':'11px');}
    const sub=m.el.querySelector('.handle-sub');
    if(sub){sub.textContent=ctx.services.editor.moduleSubtitle(m);sub.title=sub.textContent;sub.hidden=!sub.textContent;}
  }
  function paintDeviceFace(m){
    ctx.services.editor.defaultNom(m);
    ctx.services.editor.paintHandle(m);
    m.el.classList.toggle('position-locked',m.locked);
    const handle=m.el.querySelector('.handle-label');
    if(handle)handle.setAttribute('aria-label',ctx.services.editor.displayName(m)+(m.locked?' Position gesperrt; Doppelklick für Eigenschaften':' verschieben, Pfeiltasten verwenden'));
    const lock=m.el.querySelector('.lock-indicator');if(lock){lock.hidden=!m.locked;lock.title='Position gesperrt – in Eigenschaften entsperren';}
    if(ctx.services.editor.isInstallationSwitch(m))ctx.services.editor.paintInstallationSwitch(m);
    if(m.type==='junction'){m.el.style.width=m.boxWidth+'px';m.el.querySelector('.face').style.height=m.boxHeight+'px';m.el.querySelector('.note-resize-controls').style.top=m.el.querySelector('.face').offsetTop+'px';m.el.querySelector('.note-resize-controls').setAttribute('aria-hidden','false');m.el.querySelector('.module-state').textContent='';}
    if(m.type==='connector'){
      m.el.style.width=(ctx.services.editor.connectorPorts(m)*52+24)+'px';m.el.querySelector('.module-state').textContent='Alle Anschlüsse intern verbunden';
      m.el.classList.toggle('without-banner',!m.showBanner);
      const banner=m.el.querySelector('.handle');banner.hidden=!m.showBanner;banner.querySelector('.handle-label').tabIndex=m.showBanner?0:-1;
    }
    if(m.type==='socket')m.el.querySelector('.module-state').textContent='Ohne Eigenverbrauch · L / N / PE getrennt';
    if(m.type==='terminal'&&m.terminalVersion===2){
      m.el.querySelectorAll('.terminal-v2-cell').forEach((cell,i)=>{cell.classList.toggle('tone-blue',m.terminalColors[i]==='blue');cell.classList.toggle('tone-pe',m.terminalColors[i]==='pe');});
    }
    if(m.type==='potentiometer')ctx.services.editor.paintPot(m);
    if(m.type==='motion')ctx.services.editor.paintMotion(m,ctx.state.lastSnap);
    if(m.type==='note'){
      const isLabel=m.noteStyle==='label',isFrame=m.noteStyle==='frame',isResizable=isLabel||isFrame;
      m.el.classList.toggle('text-only',isLabel);
      m.el.classList.toggle('framed-text',isFrame);
      m.el.style.width=m.noteWidth+'px';
      m.el.style.setProperty('--note-color',ctx.config.noteTones[m.noteTone][1]);
      const face=m.el.querySelector('.face'),text=m.el.querySelector('.note-content');
      text.textContent=m.noteText;text.style.fontSize=m.noteSize+'px';
      text.classList.toggle('has-background',isFrame&&m.noteTitleTone!=='transparent');
      text.classList.toggle('title-background-white',isFrame&&m.noteTitleTone==='white');
      text.classList.toggle('title-background-cream',isFrame&&m.noteTitleTone==='cream');
      face.style.height=isFrame?m.noteHeight+'px':isLabel&&m.noteHeight?m.noteHeight+'px':'';
      text.tabIndex=isResizable?0:-1;
      if(isResizable){text.setAttribute('role','button');text.setAttribute('aria-label',(isFrame?'Markierungsrahmen: ':'Beschriftung: ')+(m.noteText||'ohne Text')+(m.locked?'. Position gesperrt.':' mit Pfeiltasten verschieben, Umschalt plus Pfeiltasten ändert die Größe.')+' Doppelklick öffnet die Eigenschaften.');}
      else{text.removeAttribute('role');text.removeAttribute('aria-label');}
      const handle=m.el.querySelector('.handle');if(handle){handle.setAttribute('aria-hidden',String(isResizable));const handleButton=handle.querySelector('.handle-label');if(handleButton)handleButton.tabIndex=isResizable?-1:0;}
      m.el.querySelector('.note-resize-controls')?.setAttribute('aria-hidden',String(!isResizable));
    }

    if(ctx.services.editor.isLight(m)&&m.el)m.el.querySelector('.light-rating').textContent=m.nomV+' V · '+m.nomP+' W';
    if(m.type==='pilot'&&m.el){m.el.style.setProperty('--signal-color',ctx.config.signalColors[m.signalColor][1]);m.el.querySelector('.light-lens').setAttribute('aria-label','Meldeleuchte '+ctx.config.signalColors[m.signalColor][0]);}
    if(ctx.services.editor.isCombinedTaster(m)&&m.el){
      const [name,hex,ink,mark]=ctx.config.buttonColors[m.buttonColor];
      m.el.style.setProperty('--push-color',hex);
      m.el.style.setProperty('--push-ink',ink);
      m.el.style.setProperty('--signal-color',ctx.config.signalColors[m.signalColor][1]);
      const push=m.el.querySelector('.push');
      if(push){push.textContent=mark;push.setAttribute('aria-label',m.latching?'Schalter '+m.id+' '+(m.pressed?'ausschalten':'einschalten'):m.id+' Taster '+name+' gedrückt halten');}
    }
    if(m.type==='psu'||m.type==='mains'){
      const disp=m.el&&m.el.querySelector('.supply-display');
      if(disp)disp.innerHTML=(ctx.state.powered?ctx.services.editor.fmtV(m.voltage):'0.0')+' <small>V</small>';
      const ln=m.el&&m.el.querySelector('.supply-ln');
      if(ln)ln.textContent=m.type==='mains'?(ctx.state.powered?('U_LN '+ctx.services.editor.fmtV(m.voltage/Math.sqrt(3))+' V · 50 Hz'):'Aus'):'';
    }
    if(m.type==='contactor'){
      const mark=m.el&&m.el.querySelector('.coil-mark');
      if(mark)mark.textContent='DC '+(m.nomV||24)+' V';
    }
    if(m.type==='acmotor'&&m.el){
      const kw=m.el.querySelector('.ac-kw');
      if(kw)kw.textContent=(m.ratedKw||1.5).toLocaleString('de-DE',{minimumFractionDigits:2,maximumFractionDigits:2})+' kW · η '+Math.round((m.eta||0.8)*100)+' %';
    }
  }
  function say(message){ctx.dom.$('#toast').textContent=message;ctx.dom.$('#toast').hidden=false;clearTimeout(ctx.state.toastTimer);ctx.state.toastTimer=setTimeout(()=>ctx.dom.$('#toast').hidden=true,3200);}
  function terminal(m,name,label,direction,cls=''){return `<div class="terminal-cell"><span class="terminal-label">${label}</span><button class="term${cls?' '+cls:''}" data-key="${m.id}.${name}" data-direction="${direction}" aria-label="${ctx.services.editor.displayName(m)} Anschluss ${name}" title="${ctx.services.editor.displayName(m)} · ${name}"><span class="screw"></span></button></div>`;}
  function lampTag(m){const n=String(m.id).replace(/\D/g,'');return 'P'+(n||'1');}
  function dinNo(){return '<svg class="din-sym din-no" viewBox="0 0 100 40" preserveAspectRatio="none" aria-hidden="true"><path d="M0 15 H38 M70 15 H100"/><path class="rest" d="M38 15 L66 4"/><path class="act" d="M38 15 H70"/></svg>';}
  function dinNc(){return '<svg class="din-sym din-nc" viewBox="0 0 100 44" preserveAspectRatio="none" aria-hidden="true"><path d="M0 15 H40 M72 15 H100"/><path d="M72 15 V31"/><path class="rest" d="M40 15 L72 31"/><path class="act" d="M40 15 L58 42"/></svg>';}
  function dinLamp(tag){return '<span class="lamp-cell"><span class="lamp-glow" aria-hidden="true"></span><svg class="din-sym din-lamp" viewBox="0 0 48 40" aria-hidden="true"><circle class="lamp-glass" cx="24" cy="17" r="13"/><path class="lamp-x" d="M15 8 L33 26 M33 8 L15 26"/></svg><span class="lamp-mark">'+tag+'</span></span>';}
  function driveSocket(m,pin,label,tone,description){
    return `<div class="drive-pin"${/^B[67]$/.test(pin)?` data-pulse="${pin}"`:''}>${/^B[67]$/.test(pin)?`<span class="drive-pin-name">${pin}</span>`:''}<button class="term" style="--socket:${tone}" data-key="${m.id}.${pin}" data-direction="bottom" aria-label="${m.id} ${description||pin}" title="${m.id} · ${description||pin}"></button><span>${label}</span></div>`;
  }
  function driveFace(m){
    const id='drive-'+m.id;
    const socket=(pin,label,tone,description)=>ctx.services.editor.driveSocket(m,pin,label,tone,description);
    const changeover='<svg class="drive-symbol" viewBox="0 0 100 40" preserveAspectRatio="none" aria-hidden="true"><path d="M10 40 V2 H63 V10 M50 40 V19 H59 M90 40 V19 H78"/><path class="sensor-rest" d="M63 10 L59 19"/><path class="sensor-active" d="M63 10 L78 19"/></svg>';
    const sensors=[1,2,3].map((n)=>`<div class="drive-group drive-sensor" data-sensor="${n}"><h3><i class="drive-led"></i>B${n} <small>${['links','Mitte','rechts'][n-1]}</small></h3>${changeover}<div class="drive-pins">${['11','12','14'].map(pin=>socket('B'+n+'_'+pin,pin,'#e1c837','B'+n+' · '+pin+(pin==='11'?' gemeinsam':pin==='12'?' Öffner':' Schließer'))).join('')}</div></div>`).join('');
    const bolts=(xs,y)=>xs.map(x=>`<g transform="translate(${x} ${y})"><circle r="4.2" fill="#77818a"/><circle r="3" fill="#c9d2d4"/><path d="M-1.5 -1.5 L1.5 1.5" stroke="#43515e" stroke-width="1.4"/></g>`).join('');
    return `<svg class="drive-mechanics" viewBox="0 0 1200 192" aria-label="24-Volt-Motor mit Spindel, zwei Führungsstangen, Schlitten und Handrad" role="img">
      <defs>
        <linearGradient id="${id}-steel" x2="0" y2="1"><stop stop-color="#73838e"/><stop offset=".18" stop-color="#d5dfe2"/><stop offset=".36" stop-color="#fafcfa"/><stop offset=".55" stop-color="#aebbc3"/><stop offset=".8" stop-color="#637582"/><stop offset="1" stop-color="#273f50"/></linearGradient>
        <linearGradient id="${id}-alloy" x2=".25" y2="1"><stop stop-color="#eef2f1"/><stop offset=".44" stop-color="#ccd5d8"/><stop offset="1" stop-color="#8e9da7"/></linearGradient>
        <linearGradient id="${id}-blue" x2="0" y2="1"><stop stop-color="#21679b"/><stop offset=".22" stop-color="#155785"/><stop offset="1" stop-color="#082944"/></linearGradient>
        <pattern id="${id}-thread" width="7" height="20" patternUnits="userSpaceOnUse"><path d="M-4 20 L3 0 M3 20 L10 0" stroke="#d5dce1" stroke-width="1.7"/><path d="M-1 20 L6 0" stroke="#0e1d2a" stroke-width="2"/></pattern>
        <pattern id="${id}-knurl" width="5" height="6" patternUnits="userSpaceOnUse"><path d="M0 0 L5 6 M5 0 L0 6" stroke="#727f87" stroke-width=".6"/></pattern>
        <linearGradient id="${id}-plate" x2="0" y2="1"><stop stop-color="#b4beb8"/><stop offset=".3" stop-color="#f6f7f5"/><stop offset=".68" stop-color="#d7ddda"/><stop offset="1" stop-color="#8f9996"/></linearGradient>
      </defs>
      <path d="M31 161 H1167 V185 H31Z" fill="#091c2c"/><path d="M31 161 H1167 L1153 152 H41Z" fill="#284c6d"/>
      <rect x="29" y="57" width="148" height="80" rx="30" fill="url(#${id}-steel)" stroke="#4b616e" stroke-width="2"/>
      <ellipse cx="43" cy="97" rx="18" ry="38" fill="#7e949f" stroke="#ccd4d4" stroke-width="2"/>
      <path d="M68 86 C80 74 118 74 130 86 L130 108 C118 120 80 120 68 108 Z" fill="url(#${id}-plate)" stroke="#6d7c84" stroke-width=".8"/><path d="M74 88 C86 78 112 78 124 88" fill="none" stroke="#fff" stroke-width="1.1" opacity=".65"/><text x="99" y="97" text-anchor="middle" fill="#1e3340" font-size="10" font-family="Arial">DC MOTOR</text><text x="99" y="111" text-anchor="middle" fill="#1e3340" font-size="12" font-family="Arial">${Math.round(m.nomV||24)} V</text>
      <rect x="134" y="56" width="37" height="82" rx="4" fill="url(#${id}-blue)" stroke="#14324a"/>
      <rect x="164" y="82" width="60" height="29" rx="8" fill="url(#${id}-steel)" stroke="#75868e"/><path d="M186 83 V109 M200 83 V109" stroke="#405866" stroke-width="3"/>
      <rect x="205" y="39" width="36" height="129" rx="3" fill="url(#${id}-blue)" stroke="#09253b" stroke-width="2"/>
      <rect x="235" y="58" width="862" height="16" rx="8" fill="url(#${id}-steel)" stroke="#6c8794"/>
      <rect x="235" y="92" width="862" height="18" rx="6" fill="url(#${id}-steel)"/><rect x="235" y="92" width="862" height="18" rx="6" fill="url(#${id}-thread)"/>
      <rect x="235" y="136" width="862" height="18" rx="9" fill="url(#${id}-steel)" stroke="#6c8794"/>
      <path d="M250 176 H1082" stroke="#537184"/>
      ${Array.from({length:31},(_,i)=>`<path d="M${263+i*27} 176 v${i%5===0?7:4}" stroke="#aab9c1"/>`).join('')}
      ${[278,663,1048].map((x,i)=>`<g class="drive-sensor-marker" data-marker="${i+1}"><rect x="${x-15}" y="155" width="30" height="15" rx="2" fill="#233c4a" stroke="#768b94"/><circle class="sensor-light" cx="${x-6}" cy="162" r="3" fill="#5f727b"/><text x="${x+6}" y="166" fill="#c9d4d9" font-size="9" text-anchor="middle">B${i+1}</text></g>`).join('')}
      <g class="drive-carriage" transform="translate(${m.slide*704} 0)">
        <path d="M245 62 L259 42 H397 L405 62 V144 L391 158 H245Z" fill="#617984" stroke="#415d6b"/>
        <path d="M245 62 L259 42 H397 L383 62Z" fill="#e3e9e9"/>
        <rect x="245" y="62" width="138" height="96" rx="2" fill="url(#${id}-alloy)" stroke="#aebec7"/>
        <path d="M383 62 L397 42 L405 62 V144 L383 158Z" fill="#91a3ad"/>
        ${bolts([266,300,334,366],76)}${bolts([266,300,334,366],139)}
        <rect x="274" y="88" width="79" height="40" rx="2" fill="#c0cdd2" stroke="#90a3af"/><text x="314" y="113" text-anchor="middle" fill="#536c7a" font-size="13" font-family="Arial">${m.id}</text>
        <path d="M310 162 l5 8 5-8" fill="#d79d3e"/>
      </g>
      <rect x="1090" y="39" width="43" height="132" rx="3" fill="url(#${id}-blue)" stroke="#0b2b42" stroke-width="2"/>
      ${bolts([216,1111],51)}${bolts([216,1111],155)}
      <rect x="1133" y="79" width="21" height="40" fill="url(#${id}-steel)"/>
      <g class="drive-wheel" transform="translate(1161 99)"><circle r="34" fill="url(#${id}-steel)" stroke="#c5d1d5" stroke-width="2"/><circle r="29" fill="url(#${id}-knurl)" stroke="#647b8a"/><circle r="23" fill="url(#${id}-alloy)" stroke="#f1f3ed"/><g class="drive-wheel-index"><circle cx="0" cy="-15" r="3" fill="#344e61"/><path d="M0 -22 V-27" stroke="#263b4b" stroke-width="2"/></g></g>
    </svg><div class="drive-panel">
      <div class="drive-group drive-power"><h3>MOTOR 24 VDC</h3><div class="drive-mark">LINEAR TRAINER</div><div class="drive-pins">${socket('+','+','#c13c42','Motor +')}${socket('−','−','#238cc9','Motor −')}${socket('PE','⏚','#c5be36','PE · separate Gehäuseklemme')}</div></div>
      ${sensors}
      <div class="drive-group drive-supply"><h3>24 VDC</h3><div class="drive-pins">${socket('24V','1 +','#c13c42','Geberversorgung +')}${socket('0V','2 −','#238cc9','Geberversorgung −')}</div></div>
      <div class="drive-group drive-encoder" data-lead="b6" role="group" aria-label="Inkrementalgeber. Zwei getrennte Rechteckkanäle. Nach rechts führt B6, nach links B7."><h3>Inkrementalgeber <b class="drive-takt">0</b></h3><div class="encoder-channels"><div class="encoder-channel" data-pulse="B6"><svg class="encoder-mark" viewBox="0 0 56 16" aria-hidden="true"><path d="M1 13 H8 V2 H26 V13 H44 V2 H55"/></svg><button class="term" data-key="${m.id}.B6" data-direction="bottom" aria-label="${m.id} B6 Rechteck, führt nach rechts" title="${m.id} · B6"></button><span class="encoder-pin">3 <b class="encoder-name">B6</b></span></div><div class="encoder-channel" data-pulse="B7"><svg class="encoder-mark" viewBox="0 0 56 16" aria-hidden="true"><path d="M1 13 H18 V2 H36 V13 H52 V2 H55"/></svg><button class="term" data-key="${m.id}.B7" data-direction="bottom" aria-label="${m.id} B7 Rechteck, führt nach links" title="${m.id} · B7"></button><span class="encoder-pin">3 <b class="encoder-name">B7</b></span></div></div></div>
    </div><div class="drive-adjust"><label for="${id}-position">Schlitten von Hand</label><span class="drive-limits">B1 · links</span><input id="${id}-position" class="drive-position" type="range" min="0" max="1000" step="1" value="${Math.round(m.slide*1000)}" aria-label="${m.id} Schlittenposition von Hand"><span class="drive-limits">rechts · B3</span><output class="drive-position-value">${Math.round(m.slide*100)} %</output></div>`;
  }
  function drivePulses(slide){
    const step=Math.min(79,Math.floor(Math.max(0,Math.min(1,slide))*80));
    const quad=step&3;
    return {step,b6:quad===1||quad===2,b7:quad===2||quad===3};
  }
  function linkDrive(m,link){
    [m.slide<=.03,Math.abs(m.slide-.5)<=.035,m.slide>=.97].forEach((active,i)=>link(m.id+'.B'+(i+1)+'_11',m.id+'.B'+(i+1)+(active?'_14':'_12')));
    const pulses=ctx.services.editor.drivePulses(m.slide);
    link(m.id+'.B6',m.id+'.'+(pulses.b6?'24V':'0V'));
    link(m.id+'.B7',m.id+'.'+(pulses.b7?'24V':'0V'));
  }
  function paintDrive(m,snap){
    const st=snap?.motors?.[m.id]||{}, previous=ctx.state.driveFrames.get(m);
    const dt=snap&&previous&&previous.revision===snap.revision?Math.max(0,Math.min(.1,snap.time-previous.time)):0;
    const live=ctx.services.simulation.plantLive()&&ctx.state.simMode==='betrieb'&&!!snap?.running&&!!st.running;
    const voltage=(snap?.terminals?.[m.id+'.+']||0)-(snap?.terminals?.[m.id+'.−']||0);
    const before=m.slide;
    if(live){
      const next=Math.max(0,Math.min(1,m.slide+dt*.1*Math.min(2,Math.abs(voltage)/(m.nomV||24))*(st.reverse?-1:1)));
      if(next>before+1e-8)m.linealDir='rechts';
      else if(next<before-1e-8)m.linealDir='links';
      m.slide=next;
    }
    const angle=(previous?.angle||0)+(m.slide-before)*7200;
    ctx.state.driveFrames.set(m,{time:snap?.time||0,revision:snap?.revision,angle});
    m.el.querySelector('.drive-carriage').setAttribute('transform',`translate(${m.slide*704} 0)`);
    m.el.querySelector('.drive-wheel-index').setAttribute('transform',`rotate(${angle})`);
    m.el.querySelector('pattern[id$="-thread"]').setAttribute('patternTransform',`translate(${angle/30%7} 0)`);
    const sensors=[m.slide<=.03,Math.abs(m.slide-.5)<=.035,m.slide>=.97];
    sensors.forEach((active,i)=>{m.el.querySelector(`[data-sensor="${i+1}"]`).classList.toggle('active',active);m.el.querySelector(`[data-marker="${i+1}"] .sensor-light`).setAttribute('fill',active?'#a3e98d':'#5f727b');});
    const pulses=ctx.services.editor.drivePulses(m.slide);
    m.el.querySelector('[data-pulse="B6"]')?.classList.toggle('high',pulses.b6);
    m.el.querySelector('[data-pulse="B7"]')?.classList.toggle('high',pulses.b7);
    const pulseLead=m.linealDir==='links'?'b7':'b6';
    const box=m.el.querySelector('.drive-encoder');
    if(box&&box.dataset.lead!==pulseLead){
      box.dataset.lead=pulseLead;
      const early='M1 13 H8 V2 H26 V13 H44 V2 H55', late='M1 13 H18 V2 H36 V13 H52 V2 H55';
      m.el.querySelector('[data-pulse="B6"] .encoder-mark path').setAttribute('d',pulseLead==='b6'?early:late);
      m.el.querySelector('[data-pulse="B7"] .encoder-mark path').setAttribute('d',pulseLead==='b7'?early:late);
    }
    const takt=m.el.querySelector('.drive-takt');if(takt)takt.textContent=String(Math.floor(pulses.step/4));
    if(ctx.state.engineOk&&ctx.state.engine.setDrivePosition)ctx.state.engine.setDrivePosition(m.id,m.slide);
    const slider=m.el.querySelector('.drive-position');slider.value=Math.round(m.slide*1000);slider.disabled=live;
    m.el.querySelector('.drive-position-value').textContent=Math.round(m.slide*100)+' %';
    const end=m.slide===0?'links':m.slide===1?'rechts':null;
    const label=m.el.querySelector('.module-state');
    const bits=(pulses.b6?'B6 1':'B6 0')+' · '+(pulses.b7?'B7 1':'B7 0');
    const lead=m.linealDir==='rechts'?' · B6 führt':m.linealDir==='links'?' · B7 führt':'';
    const travel=!ctx.state.engineOk?'Wartet auf Engine':ctx.state.simMode==='aufbau'?'Spannung aus':ctx.state.simMode==='paused'||!snap?.running?'Eingefroren':live?(end?'Anschlag '+end+' · Motor bestromt':st.reverse?'Fährt nach links':'Fährt nach rechts'):'Stillstand';
    if(label)label.textContent=travel+lead+' · '+bits;
  }
  function paintSelector(m){
    m.el.style.setProperty('--selector-angle',(m.selection*42)+'deg');
    m.el.querySelectorAll('[data-selection]').forEach(b=>b.setAttribute('aria-pressed',String(Number(b.dataset.selection)===m.selection)));
    m.el.querySelector('.selector-contact.a').classList.toggle('active',m.selection===-1);
    m.el.querySelector('.selector-contact.b').classList.toggle('active',m.selection===1);
    const selected=m.selection===-1?'A':m.selection===1?'B':'0';
    m.el.querySelector('.selector-knob').setAttribute('aria-label',m.id+' Drehschalter Stellung '+selected+' · nächste Stellung');
    m.el.querySelector('.module-state').textContent=selected+' · '+(m.selection===-1?'13–14 geschlossen':m.selection===1?'23–24 geschlossen':'beide offen');
  }
  function meterSpan(m){
    const a=m.probeRed,b=m.probeBlack;
    if(!a||!b)return null;
    return ctx.state.wires.find(w=>w.a===a&&w.b===b||w.a===b&&w.b===a)||null;
  }
  function setLead(m,field,key){
    m[field]=key;
    if(m.type==='ammeter')m.measuredWire=m.probeRed&&m.probeBlack?{a:m.probeRed,b:m.probeBlack}:null;
  }
  function paintMeters(snap){
    const terms=snap?.terminals||{};
    const red=new Set(ctx.state.modules.filter(ctx.services.editor.isMeter).map(m=>m.probeRed)),black=new Set(ctx.state.modules.filter(ctx.services.editor.isMeter).map(m=>m.probeBlack));
    ctx.dom.board.querySelectorAll('.term').forEach(t=>{t.classList.toggle('probe-red',red.has(t.dataset.key));t.classList.toggle('probe-black',black.has(t.dataset.key));});
    let continuity=null;
    for(const m of ctx.state.modules.filter(ctx.services.editor.isMeter)){
      if(!m.el)continue;
      if(m.probeRed&&!ctx.services.editor.term(m.probeRed))ctx.services.editor.setLead(m,'probeRed',null);
      if(m.probeBlack&&!ctx.services.editor.term(m.probeBlack))ctx.services.editor.setLead(m,'probeBlack',null);
      let value='—',note='Messleitung auf eine Klemme ziehen',points='';
      if(m.type==='voltmeter'){
        const a=m.probeRed,b=m.probeBlack,valid=!!a&&!!b&&!!ctx.services.editor.term(a)&&!!ctx.services.editor.term(b);
        points='V: '+(a||'offen')+'\nCOM: '+(b||'offen');
        if(valid&&!ctx.state.powered){
          if(!continuity){continuity=ctx.services.simulation.graph({},true);ctx.state.modules.filter(x=>x.type==='lineardrive').forEach(x=>ctx.services.editor.linkDrive(x,(a,b)=>{if(!continuity.has(a))continuity.set(a,new Set());if(!continuity.has(b))continuity.set(b,new Set());continuity.get(a).add(b);continuity.get(b).add(a);}));}
          const closed=ctx.services.simulation.reachable(continuity,a).has(b);value=closed?'DURCHGANG':'OFFEN';note='Leitungen & Kontakte · ohne Lastprüfung';
        }else if(valid&&snap&&!ctx.state.latchedFault&&Number.isFinite(terms[a])&&Number.isFinite(terms[b])){
          value=(terms[a]-terms[b]).toLocaleString('de-DE',{minimumFractionDigits:2,maximumFractionDigits:2})+' V';note='Spannungsdifferenz · Momentanwert';
        }else if(a||b)note=valid?'Keine aktuellen Messwerte':'Zweite Messleitung setzen';
      }else{
        const w=ctx.services.editor.meterSpan(m);
        points='A: '+(m.probeRed||'offen')+'\nCOM: '+(m.probeBlack||'offen');
        if(!m.probeRed||!m.probeBlack)note='Beide Messleitungen an die Enden einer Ader';
        else if(!w)note='Keine direkte Leitung zwischen den Spitzen';
        else{
          const i=snap?.wireCurrents?.[w.a+'|'+w.b];
          if(!ctx.state.powered){value='0,00 A';note='Aufbau · Spannung aus';}
          else if(Number.isFinite(i)&&!ctx.state.latchedFault){value=(Math.abs(i)<1?i*1000:i).toLocaleString('de-DE',{minimumFractionDigits:2,maximumFractionDigits:2})+(Math.abs(i)<1?' mA':' A');note='Strom dieser Ader · Momentanwert';}
          else note='Keine aktuellen Messwerte';
        }
      }
      m.el.querySelector('.meter-mode').textContent=m.type==='ammeter'?'LEITUNGSSTROM':!ctx.state.powered?'DURCHGANGSPRÜFUNG':'SPANNUNG';
      m.el.querySelector('.meter-value').textContent=value;
      m.el.querySelector('.meter-value').classList.toggle('word',value==='DURCHGANG'||value==='OFFEN');
      m.el.querySelector('.meter-reading-note').textContent=note;
      m.el.querySelector('.meter-points').textContent=points;
      m.el.querySelector('.module-state').textContent=!ctx.state.powered?'Aufbau':'Messen';
    }
  }
  const isMeter = m=>m.type==='voltmeter'||m.type==='ammeter';
  const relayPairs = m=>(m.relayContacts||[]).map(c=>[String(c.number*10+(c.kind==='NC'?1:3)),String(c.number*10+(c.kind==='NC'?2:4)),c.kind]);
  function paintProtection(m,st){
    if(st&&m.pendingProtection){
      if(!!st.on===!!m.protectionOn&&!!st.tripped===!!m.protectionTripped){m.protectionReason=Number(st.reason)||0;m.pendingProtection='';m.protectionSent=false;}
    }else if(st){
      if(m.protectionOn!==st.on||m.protectionTripped!==st.tripped||m.protectionReason!==st.reason){m.protectionOn=st.on;m.protectionTripped=st.tripped;m.protectionReason=st.reason;ctx.services.projekt.schedulePersist();}
    }
    const on=m.protectionOn&&!m.protectionTripped;
    m.el.classList.toggle('protection-on',on);m.el.classList.toggle('protection-tripped',m.protectionTripped);
    const state=m.protectionTripped?'AUSGELÖST':on?'I · EIN':'O · AUS';
    m.el.querySelector('.protection-position').textContent=state;
    const switchButton=m.el.querySelector('.protection-switch');switchButton.disabled=m.protectionTripped;switchButton.setAttribute('aria-pressed',String(on));switchButton.setAttribute('aria-label',m.id+(on?' ausschalten':' einschalten'));
    m.el.querySelector('[data-protection="reset"]').disabled=!m.protectionTripped;
    const test=ctx.dom.$('#devMenu [data-protection="test"]');
    const resetMenu=ctx.dom.$('#devMenu [data-protection="reset"]');
    if(test&&ctx.state.selectedId===m.id)test.disabled=!!m.protectionTripped;
    if(resetMenu&&ctx.state.selectedId===m.id)resetMenu.hidden=!m.protectionTripped;
    m.el.querySelector('.protection-current').textContent=(st&&on?Number(st.current||0).toFixed(2):'0.00')+' A';
    m.el.querySelector('.module-state').textContent=m.protectionTripped?(m.protectionReason===2?'Magnetisch ausgelöst':'Thermisch / Test ausgelöst'):on?'Geschlossen':'Getrennt';
  }
  function paintTimer(m,snap){
    const state=snap?.timers?.[m.id],on=!!state?.on,waiting=!!state?.waiting;
    const phase=waiting?(on?'Bis Abfall':'Bis Anzug'):(on?'Angezogen':'Abgefallen');
    m.el.classList.toggle('energized',on);
    m.el.querySelector('.timer-phase').textContent=phase;
    m.el.querySelector('.timer-countdown').textContent=waiting?Math.max(0,state.remaining).toFixed(2)+' s':'—';
    m.el.querySelector('.timer-progress').value=waiting?Math.max(0,Math.min(1,1-state.remaining/Math.max(.01,state.duration))):on?1:0;
    m.el.querySelector('.timer-input').classList.toggle('active',!!state?.powered);
    m.el.querySelector('.timer-output').classList.toggle('active',on);
    m.el.querySelector('.module-state').textContent=(ctx.state.simMode==='paused'?'Eingefroren · ':'')+phase;
  }
  function paintPot(m){
    const value=Math.round(m.potPosition*100);
    m.el.querySelector('.pot-rotor').style.transform='rotate('+(-135+value*2.7)+'deg)';
    const slider=m.el.querySelector('.pot-slider');if(Number(slider.value)!==value)slider.value=value;
    slider.setAttribute('aria-valuetext',value+' Prozent');
    m.el.querySelector('.pot-value').textContent=value+' %';
    m.el.querySelector('.pot-rating').textContent=(m.resistance/1000).toLocaleString('de-DE')+' kΩ · linear';
    m.el.querySelector('.module-state').textContent='Schleifer 2 · '+value+' %';
  }
  function paintMotion(m,snap){
    const state=snap?.motion?.[m.id],active=ctx.state.powered&&!!state?.active;
    m.el.classList.toggle('motion-active',active);
    const test=m.el.querySelector('.motion-test');test.disabled=ctx.state.simMode!=='betrieb'||!ctx.state.powered||!state?.powered;
    m.el.querySelector('.motion-status').textContent=active?'Bewegung erkannt':state?.powered&&ctx.state.powered?'Bereit':'Ohne Versorgung';
    m.el.querySelector('.module-state').textContent=active?'Ausgang EIN · '+Math.ceil(state.remaining)+' s'+(ctx.state.simMode==='paused'?' · Pause':''):'Ausgang AUS';
  }
  function installationSwitchFace(m){
    const cross=m.type==='crossover';
    const symbol=cross?'<path class="switch-straight" d="M39 10V66 M153 10V66"/><g class="switch-cross"><path d="M39 10L88 36 M104 44L153 66 M153 10L39 66"/></g><path d="M39 -15V10 M153 -15V10 M39 66V109 M153 66V109"/>':'<path d="M39 -15V20 M153 -15V20 M96 48V109"/><path class="switch-throw" d="M96 48L39 20"/>';
    return `<div class="device-eyebrow">${cross?'KREUZSCHALTER':'WECHSELSCHALTER'}</div><div class="wall-frame"><button type="button" class="installation-rocker" aria-label="${m.id} umschalten"><span class="rocker-top" aria-hidden="true"></span><span class="rocker-bottom" aria-hidden="true"></span></button></div><div class="installation-map"><div class="installation-terminals">${ctx.services.editor.terminal(m,'1','1','left')}${ctx.services.editor.terminal(m,'2','2','right')}</div><svg class="installation-symbol" viewBox="0 0 192 76" role="img" aria-label="Kontaktverbindungen"><g>${symbol}</g></svg><div class="installation-terminals bottom">${cross?ctx.services.editor.terminal(m,'3','3','bottom')+ctx.services.editor.terminal(m,'4','4','bottom'):ctx.services.editor.terminal(m,'L','L · gemeinsam','bottom')}</div></div>`;
  }
  function paintInstallationSwitch(m){
    if(!m.el)return;
    const cross=m.type==='crossover',pos=m.selection===1;
    m.el.classList.toggle('switch-position-2',pos);
    const button=m.el.querySelector('.installation-rocker');
    button.setAttribute('aria-pressed',String(pos));
    button.setAttribute('aria-label',m.id+' '+(cross?'Kreuzschalter':'Wechselschalter')+', Stellung '+(pos?2:1)+'. Zum Umschalten drücken.');
    const paths=cross?(pos?'1–4 und 2–3':'1–3 und 2–4'):'L–'+(pos?'2':'1');
    m.el.querySelector('.installation-symbol').setAttribute('aria-label','Verbunden: '+paths);
    m.el.querySelector('.module-state').textContent='Verbunden: '+paths;
    if(!cross)m.el.querySelector('.switch-throw').setAttribute('d','M96 48L'+(pos?'153':'39')+' 20');
  }
  function junctionFace(m){
    return `<div class="junction-shell"><i class="junction-mount mount-left" aria-hidden="true"></i><i class="junction-mount mount-right" aria-hidden="true"></i><i class="junction-entry entry-left" aria-hidden="true"></i><i class="junction-entry entry-right" aria-hidden="true"></i><i class="junction-entry entry-bottom" aria-hidden="true"></i><i class="box-screw screw-nw" aria-hidden="true"></i><i class="box-screw screw-ne" aria-hidden="true"></i><i class="box-screw screw-sw" aria-hidden="true"></i><i class="box-screw screw-se" aria-hidden="true"></i><div class="device-eyebrow">ABZWEIGDOSE · OFFEN</div>${['n','e','s','w'].map(edge=>'<span class="junction-edge edge-'+edge+'" aria-hidden="true"></span>').join('')}</div>`;
  }
  function connectorFace(m){
    const count=ctx.services.editor.connectorPorts(m);
    return `<div class="lever-body" tabindex="0" role="group" aria-label="${m.id} Verbindungsklemme, ${count} Leiter. Mit Pfeiltasten verschieben; Enter öffnet die Eigenschaften." style="grid-template-columns:repeat(${count},minmax(0,1fr))"><div class="connector-bus" aria-hidden="true"></div><div class="connector-front" aria-hidden="true"></div>${Array.from({length:count},(_,i)=>'<div class="lever-connector"><i class="connector-lever" aria-hidden="true"></i>'+ctx.services.editor.terminal(m,String(i+1),String(i+1),'bottom')+'</div>').join('')}</div>`;
  }
  function socketFace(m){
    const contact=(key,label,cls)=>`<button type="button" class="term socket-contact ${cls}" data-key="${m.id}.${key}out" data-direction="${key==='L'?'left':key==='N'?'right':'top'}" aria-label="${m.id} ${label}, nur Messleitungen" title="${label} · nur Messleitungen · intern mit ${key} verbunden"></button>`;
    return `<div class="device-eyebrow">SCHUKO · STECKDOSE</div><div class="wall-frame socket-frame"><div class="socket-recess">${contact('L','Steckkontakt 1, intern an L','socket-left')}${contact('N','Steckkontakt 2, intern an N','socket-right')}${contact('PE','Schutzkontakt, intern an PE','socket-earth')}<i class="socket-earth-bottom" aria-hidden="true"></i><span class="socket-contact-name name-left">1</span><span class="socket-contact-name name-right">2</span></div></div><div class="socket-contact-caption"><b>Messpunkte · nur Messleitungen</b><br>1 ↔ L · 2 ↔ N · Schutzkontakt ↔ PE</div><div class="socket-back"><span class="socket-back-title">ANSCHLUSSKLEMMEN</span><div class="socket-terminal-bank">${ctx.services.editor.terminal(m,'L','L','bottom')}${ctx.services.editor.terminal(m,'N','N','bottom')}${ctx.services.editor.terminal(m,'PE','PE','bottom')}</div></div>`;
  }
  function face(m){
    if(ctx.services.editor.isInstallationSwitch(m))return ctx.services.editor.installationSwitchFace(m);
    if(m.type==='junction')return ctx.services.editor.junctionFace(m);
    if(m.type==='connector')return ctx.services.editor.connectorFace(m);
    if(m.type==='socket')return ctx.services.editor.socketFace(m);
    if(m.type==='note')return m.noteStyle==='frame'?'<div class="note-frame"><div class="note-content note-frame-title"></div><div class="note-frame-outline" aria-hidden="true"><i class="note-frame-edge edge-n"></i><i class="note-frame-edge edge-e"></i><i class="note-frame-edge edge-s"></i><i class="note-frame-edge edge-w"></i></div></div>':'<div class="note-content"></div>';
    if(m.type==='potentiometer')return `<div class="device-eyebrow">DREH-POTENTIOMETER</div><div class="pot-dial" aria-hidden="true"><span class="pot-zero">0</span><span class="pot-max">100</span><div class="pot-rotor"><i></i></div></div><div class="pot-controls"><label>Schleifer <output class="pot-value"></output><input class="pot-slider" type="range" min="0" max="100" step="1" value="${Math.round(m.potPosition*100)}" aria-label="${m.id} Schleiferposition"></label></div><div class="pot-rating"></div><svg class="pot-symbol" viewBox="0 0 194 56" aria-label="Potentiometer: Klemme 1 und 3 sind die Widerstandsenden, Klemme 2 ist der Schleifer"><path d="M16 51 V17 H55 M139 17 H178 V51 M97 51 V29 M91 37 L97 29 L103 37"/><rect x="55" y="9" width="84" height="16"/></svg><div class="sensor-terminals">${ctx.services.editor.terminal(m,'1','1','bottom')}${ctx.services.editor.terminal(m,'2','2','bottom')}${ctx.services.editor.terminal(m,'3','3','bottom')}</div>`;
    if(m.type==='motion')return `<div class="device-eyebrow">PIR · BEWEGUNGSMELDER</div><div class="motion-detection" title="Maus im Erfassungsbereich bewegen"><span class="motion-led" aria-hidden="true"></span><div class="pir-lens" aria-hidden="true"></div><span class="motion-status">Ohne Versorgung</span><small>Erfassungsbereich · Maus bewegen</small></div><button class="motion-test" type="button">Bewegung testen</button><div class="motion-rating">${m.nomV} V ${m.nomV===24?'DC':'AC'} · ${m.motionDelay} s Nachlauf</div><div class="sensor-terminals">${ctx.services.editor.terminal(m,'L',m.nomV===24?'+24':'L','bottom')}${ctx.services.editor.terminal(m,'N',m.nomV===24?'0 V':'N','bottom')}${ctx.services.editor.terminal(m,'OUT',m.nomV===24?'OUT':"L′",'bottom')}</div>`;

    if(m.type==='timer')return `<div class="timer-terminals top">${ctx.services.editor.terminal(m,'A1','A1','top')}${ctx.services.editor.terminal(m,'A2','A2','top')}</div><div class="timer-brand">MULTITIMER <small>${m.nomV} V · A1/A2</small></div><div class="timer-display"><span class="timer-phase">Abgefallen</span><output class="timer-countdown">—</output><progress class="timer-progress" max="1" value="0" aria-label="Zeitablauf"></progress></div><div class="timer-leds"><span class="timer-input">Spule</span><span class="timer-output">Kontakt</span></div><button class="timer-settings" type="button" title="Zeitrelais einstellen"><span>${ctx.config.timerModes[m.timerMode]}</span><strong>${m.timerMode===3?'EIN':m.timerMode===1?'AUS':'AN'} ${m.delayOn} s</strong>${m.timerMode>=2?'<strong>'+(m.timerMode===3?'AUS':'AB')+' '+m.delayOff+' s</strong>':''}<small>Funktion / Zeiten einstellen</small></button><svg class="timer-symbol" viewBox="0 0 160 72" role="img" aria-label="Wechsler: 15 gemeinsam, in Ruhe an 16, verzögert an 18"><g fill="none" stroke="#163848" stroke-width="2.6" stroke-linecap="round" stroke-linejoin="round"><path d="M80 18 V32"/><path class="timer-rest" d="M80 32 L46 56"/><path class="timer-work" d="M80 32 L114 56"/><path d="M34 56 H58"/><path d="M102 56 H126"/><path d="M46 56 V68"/><path d="M114 56 V68"/></g><text x="80" y="14" text-anchor="middle">15</text><text class="hint" x="28" y="52" text-anchor="end">16</text><text class="hint" x="132" y="52" text-anchor="start">18</text></svg><div class="timer-terminals bottom">${[['16','16 NC'],['15','15 COM'],['18','18 NO']].map(([key,label])=>ctx.services.editor.terminal(m,key,label,'bottom')).join('')}</div>`;

    if(ctx.services.editor.isProtection(m)){
      const mp=m.type==='motorprotection';
      const bank=bottom=>`<div class="protection-bank ${bottom?'bottom':'top'}">${Array.from({length:m.poles},(_,i)=>{const n=i*2+(bottom?2:1);return ctx.services.editor.terminal(m,String(n),mp?n+'/'+(bottom?'T':'L')+(i+1):String(n),bottom?'bottom':'top');}).join('')}</div>`;
      const brand=mp?`<div class="brand-strip"><span class="siemens">SIEMENS</span><span class="sirius">SIRIUS</span></div><div class="protection-aux">${[['21','NC'],['22','NC'],['13','NO'],['14','NO']].map(([n,k])=>ctx.services.editor.terminal(m,n,n+' '+k,'top')).join('')}</div><div class="protection-rating"><i class="current-dial" aria-hidden="true"></i><div><strong>${m.ratedCurrent} A</strong> · eingestellt<br>Magn. 13 × In · 3-polig</div></div>`:`<div class="breaker-brand">SIEMENS</div><div class="breaker-rating"><strong>${m.characteristic}${m.ratedCurrent}</strong>${m.poles}-polig · 230/400 V<br>MCB <span class="breaker-capacity">${m.breakingCapacity} kA</span></div>`;
      return bank(false)+brand+`<button type="button" class="protection-switch" aria-label="${m.id} einschalten"><span class="protection-grip" aria-hidden="true"></span></button><div class="protection-readout"><span class="protection-position">O · AUS</span><span class="protection-current">0.00 A</span></div><div class="protection-actions"><button type="button" data-protection="reset">Rückstellen</button></div>`+bank(true);
    }
    if(ctx.services.editor.isMeter(m)){
      const red=m.type==='voltmeter'?'V':'A',redName=m.type==='voltmeter'?'rote Messleitung V':'rote Messleitung A';
      return `<div class="meter-brand">WORKSHOP <span>${m.type==='voltmeter'?'VOLT':'AMPERE'}</span></div><div class="meter-screen"><span class="meter-mode">${m.type==='voltmeter'?'SPANNUNG':'LEITUNGSSTROM'}</span><output class="meter-value">—</output><span class="meter-reading-note">Messleitung legen</span></div><div class="meter-dial" aria-hidden="true"><i></i></div><div class="meter-controls"><div class="meter-jacks"><span class="jack-name">${red}</span><button type="button" class="lead-jack red" data-lead="probeRed" aria-label="${m.id} ${redName}"></button><button type="button" class="lead-jack black" data-lead="probeBlack" aria-label="${m.id} schwarze Messleitung COM"></button><span class="jack-name">COM</span></div><div class="meter-points"></div><button type="button" class="meter-clear">Messleitungen lösen</button></div>`;
    }
    if(m.type==='relay'){
      const pairs=ctx.services.editor.relayPairs(m),bank=bottom=>`<div class="terminal-bank ${bottom?'bottom':'top'}" style="grid-template-columns:repeat(${pairs.length+1},minmax(0,1fr))">${pairs.map(([a,b,k])=>ctx.services.editor.terminal(m,bottom?b:a,(bottom?b:a)+' '+k,bottom?'bottom':'top')).join('')}${ctx.services.editor.terminal(m,bottom?'A2':'A1',bottom?'A2−':'A1+',bottom?'bottom':'top')}</div>`;
      return `<div class="brand-strip"><span class="siemens">SIEMENS</span><span class="sirius">SIRIUS</span></div>${bank(false)}<div class="relay-center"><span>HILFSSCHÜTZ</span><div class="relay-armature"></div><strong>${m.nomV} V DC</strong><small>Eigene Spule · ${pairs.length} Hilfskontakte</small></div>${bank(true)}`;
    }

    if(m.type==='terminal'){
      const nPoles=ctx.services.editor.terminalPoles(m),span=ctx.services.editor.terminalSpan(m);
      if(m.terminalVersion===2){
        const links=ctx.services.editor.terminalLinks(m);
        const cells=Array.from({length:nPoles},(_,i)=>{const n=i+1;return `<div class="terminal-v2-cell">${ctx.services.editor.terminal(m,n+'o','','top')}<span class="terminal-v2-entry" aria-hidden="true"></span><span class="terminal-v2-marker">${n}</span><span class="terminal-v2-metal" aria-hidden="true"></span><span class="terminal-v2-entry" aria-hidden="true"></span>${ctx.services.editor.terminal(m,n+'u','','bottom')}</div>`;}).join('');
        const jumpers=Array.from({length:nPoles-1},(_,i)=>{const n=i+1,on=links.includes(n);return `<button type="button" class="terminal-v2-jumper${on?' inserted':''}" data-terminal-bridge="${n}" style="left:${n*36-18}px" aria-pressed="${on}" aria-label="Steckbrücke ${m.id}: ${n}–${n+1} ${on?'entfernen':'einsetzen'}" title="${n}–${n+1}: Steckbrücke ${on?'entfernen':'einsetzen'}"></button>`;}).join('');
        return `<div class="terminal-v2-din" aria-hidden="true"></div><div class="terminal-v2-strip" aria-label="${m.id} Reihenklemmen V2">${cells}<div class="terminal-v2-bridges">${jumpers}</div></div>`;
      }
      let html='',group=0;
      for(let start=1;start<=nPoles;start+=span){
        const count=Math.min(span,nPoles-start+1);
        const linked=span>1&&count>1;
        const clamps=Array.from({length:count},(_,i)=>{const n=start+i;const cell=`<div class="clamp">${ctx.services.editor.terminal(m,n+'o',String(n),'top')}<span class="clamp-bridge" aria-hidden="true"></span>${ctx.services.editor.terminal(m,n+'u','','bottom')}</div>`;return cell+(linked&&i<count-1?'<span class="clamp-link" aria-hidden="true"></span>':'');}).join('');
        const bridged=span>1?' bridged'+(group%2?' alt':''):'';
        const from=start,to=start+count-1;
        const label=span>1?'Klemmen '+from+'–'+to+', eine Verbindung':'Klemme '+from+', oben mit unten verbunden';
        const cols=linked?Array.from({length:count},(_,i)=>'minmax(34px,44px)'+(i<count-1?' 8px':'')).join(' '):`repeat(${count},minmax(34px,44px))`;
        html+=`<div class="clamp-group${bridged}" style="grid-template-columns:${cols}" title="${label}" aria-label="${label}">${clamps}</div>`;
        group++;
      }
      return `<div class="terminal-strip" aria-label="${m.id} Reihenklemme">${html}</div>`;
    }
    if(m.type==='rail'){
      const bars=[['+24','+24 V','plus'],['0V','0 V','zero'],['PE','PE','pe']],count=ctx.services.editor.railScrews(m);
      return `<div class="rail-face">${bars.map(([key,label,cls])=>`<div class="rail-bar ${cls}"><span class="rail-name">${label}</span><div class="rail-screws">${Array.from({length:count},(_,i)=>ctx.services.editor.terminal(m,key+'.'+(i+1),String(i+1),'bottom')).join('')}</div></div>`).join('')}</div>`;
    }
    if(m.type==='selector'){
      const pair=(side,top,bottom)=>`<div class="selector-contact ${side}">${ctx.services.editor.terminal(m,top,top,'top')}<svg viewBox="0 0 48 180" preserveAspectRatio="none" aria-hidden="true"><path d="M24 18 V83 M24 131 V162"/><path class="open" d="M24 131 L${side==='a'?38:10} 90"/><path class="closed" d="M24 131 V83"/></svg>${ctx.services.editor.terminal(m,bottom,bottom,'bottom')}</div>`;
      return `<span class="selector-caption">Schalterstellung</span><div class="selector-choices" role="group" aria-label="${m.id} Schalterstellung">${[[-1,'A'],[0,'0'],[1,'B']].map(([v,label])=>`<button type="button" data-selection="${v}" aria-label="${m.id} Stellung ${label}">${label}</button>`).join('')}</div><button class="selector-knob" type="button"><span class="selector-grip"></span></button><i class="selector-coupling" aria-hidden="true"></i>${pair('a','13','14')}${pair('b','23','24')}`;
    }
    if(m.type==='lineardrive')return ctx.services.editor.driveFace(m);
    if(ctx.services.editor.isLight(m)){
      const pins=m.type==='lamp'?[['L','L / +'],['PE','PE'],['N','N / −']]:[['X1','X1'],['X2','X2']];
      return `<div class="light-bezel"><div class="light-lens"><svg viewBox="0 0 48 48" aria-hidden="true"><circle cx="24" cy="24" r="18"/><path d="M11 11 L37 37 M37 11 L11 37"/></svg></div></div><span class="light-rating">${m.nomV} V · ${m.nomP} W</span><div class="aux-bank">${pins.map(([pin,label])=>ctx.services.editor.terminal(m,pin,label,'bottom')).join('')}</div>`;
    }
    if(m.type==='contactor'||m.type==='auxiliary'){
      const aux=m.type==='auxiliary';
      const bank=bottom=>{
        const pins=ctx.config.contacts[m.type].map(([a,b,kind],i)=>{
          const n=bottom?b:a;
          return [n,!aux&&i<3?n+'<small>/'+(bottom?'T':'L')+(i+1)+'</small>':n+'<small>'+kind+'</small>'];
        });
        if(!aux)pins.push([bottom?'A2':'A1',bottom?'A2−':'A1+']);
        return `<div class="terminal-bank ${bottom?'bottom':'top'}">${pins.map(([n,l])=>ctx.services.editor.terminal(m,n,l,bottom?'bottom':'top')).join('')}</div>`;
      };
      if(aux)return `${bank(false)}<div class="nameplate"><strong>${m.parent}</strong><span>SIEMENS</span></div><div class="indicator" aria-hidden="true"></div><span class="block-code">2 NO · 2 NC</span>${bank(true)}`;
      return `<div class="brand-strip"><span class="siemens">SIEMENS</span><span class="sirius">SIRIUS</span></div>${bank(false)}<div class="mechanism" aria-hidden="true"><div class="side-slot"></div><div class="plunger"></div><div class="side-slot"></div></div><div class="vents" aria-hidden="true"><i></i><i></i><i></i><i></i></div><div class="nameplate"><strong>${m.id}</strong><span>3P + 1 NO</span></div><div class="indicator" aria-hidden="true"></div>${bank(true)}<span class="series">3RT20</span><span class="coil-mark">DC ${Number.isFinite(m.nomV)?m.nomV:24} V</span>`;
    }
    if(m.type==='acmotor'){
      const line=[['U1','U1','phase-u'],['V1','V1','phase-v'],['W1','W1','phase-w']];
      const star=[['U2','U2','phase-u'],['V2','V2','phase-v'],['W2','W2','phase-w']];
      return `<svg class="ac-illustration" viewBox="0 0 296 162" aria-hidden="true"><ellipse cx="148" cy="147" rx="112" ry="6" fill="#12202c" opacity=".14"/><path d="M86 122H112V142H72V135ZM193 122H215L228 142H188Z" fill="#344e63" stroke="#24394c" stroke-width="2"/><rect x="225" y="79" width="52" height="22" rx="3" fill="#bdcbd4" stroke="#667c8c" stroke-width="2"/><path d="M243 82H273M250 87H272" stroke="#edf4f8" stroke-width="3"/><rect x="78" y="48" width="151" height="81" rx="15" fill="#527c9b" stroke="#28495f" stroke-width="2"/><path d="M100 56H210M100 66H215M100 76H217M100 86H217M100 96H216M100 106H214M100 116H209" stroke="#2b506c" stroke-width="5" stroke-linecap="round"/><path d="M101 53H210M101 63H215M101 73H217M101 83H217M101 93H216M101 103H214M101 113H209" stroke="#7ba0ba" stroke-width="1.5" stroke-linecap="round"/><rect x="216" y="49" width="17" height="80" rx="6" fill="#90a9ba" stroke="#47657b" stroke-width="2"/><rect x="121" y="26" width="69" height="29" rx="4" fill="#395e7a" stroke="#28465c" stroke-width="2"/><path d="M125 31H185" stroke="#8caabc" stroke-width="2"/><circle cx="130" cy="41" r="2" fill="#c3d1d9"/><circle cx="181" cy="41" r="2" fill="#c3d1d9"/><circle cx="69" cy="88" r="44" fill="#304e64" stroke="#243a4b" stroke-width="2"/><circle cx="69" cy="88" r="36" fill="#172e40" stroke="#91a9b9" stroke-width="3"/><circle cx="69" cy="88" r="29" fill="none" stroke="#54768e" stroke-width="2"/><circle cx="41" cy="59" r="2.5" fill="#cfdae0"/><circle cx="97" cy="117" r="2.5" fill="#cfdae0"/></svg><div class="ac-rotor" aria-hidden="true"><div class="fan"></div></div><div class="ac-rating"><span class="ac-mark">3~ · 400 V</span><span class="ac-kw">${(m.ratedKw||1.5).toLocaleString('de-DE',{minimumFractionDigits:2,maximumFractionDigits:2})} kW</span></div><div class="ac-banks"><div class="aux-bank">${line.map(([n,l,c])=>ctx.services.editor.terminal(m,n,l,'bottom',c)).join('')}</div><div class="aux-bank">${star.map(([n,l,c])=>ctx.services.editor.terminal(m,n,l,'bottom',c)).join('')}</div></div><div class="ac-earth">${ctx.services.editor.terminal(m,'PE','PE','bottom','phase-pe')}</div>`;
    }
    if(m.type==='mains'){
      const pins=[['L1','L1','phase-l1'],['L2','L2','phase-l2'],['L3','L3','phase-l3'],['N','N','phase-n'],['PE','PE','phase-pe']];
      return `<div class="psu-brand">400 V 3~</div><div class="supply-name">Drehstrom-Einspeisung</div><div class="supply-display">${ctx.services.editor.fmtV(m.voltage)} <small>V</small></div><div class="supply-ln">U_LN ${ctx.services.editor.fmtV(m.voltage/Math.sqrt(3))} V · 50 Hz</div><div class="supply-vents"></div><div class="aux-bank">${pins.map(([n,l,c])=>ctx.services.editor.terminal(m,n,l,'bottom',c)).join('')}</div>`;
    }
    const bank=`<div class="aux-bank">${(m.type==='psu'?[['+24','+24 V'],['0V','0 V']]:m.type==='motor'?[['+','+'],['−','−']]:[]).map(([n,l])=>ctx.services.editor.terminal(m,n,l,'bottom')).join('')}</div>`;
    if(m.type==='psu')return `<div class="psu-brand">DC POWER</div><div class="supply-name">Steuerspannungsversorgung</div><div class="supply-display">${ctx.services.editor.fmtV(m.voltage)} <small>V</small></div><div class="supply-vents"></div>${bank}`;
    if(m.type==='motor')return `<div class="motor-housing"><div class="fan"></div></div>${bank}`;
    if(m.type==='emergency')return `<span class="case-screw" aria-hidden="true"></span><span class="case-screw right" aria-hidden="true"></span><span class="emergency-title">NOT-AUS</span><button class="push" aria-label="${m.id} Not-Aus betätigen" aria-pressed="${!!m.latched}">↻</button><button class="emergency-unlock" type="button" ${m.latched?'':'disabled'}>↻ Entriegeln</button><div class="taster-deck"><i class="mech-dash" aria-hidden="true"></i><div class="contact-row">${ctx.services.editor.terminal(m,'21','21 NC','left')}${ctx.services.editor.dinNc()}${ctx.services.editor.terminal(m,'22','22 NC','right')}</div><div class="contact-row">${ctx.services.editor.terminal(m,'13','13 NO','left')}${ctx.services.editor.dinNo()}${ctx.services.editor.terminal(m,'14','14 NO','right')}</div></div>`;
    const p=ctx.services.editor.lampTag(m);
    const [name,,,mark]=ctx.config.buttonColors[m.buttonColor]||ctx.config.buttonColors.green;
    return `<span class="actuation-type">${m.latching?'SCHALTER · RASTEND':'TASTER · TASTEND'}</span><button class="push" aria-label="${m.id} Taster ${name} gedrückt halten">${mark}</button><div class="taster-deck">${m.latching?'<svg class="latch-link" viewBox="0 0 28 80" aria-hidden="true"><path class="latch-dash" d="M14 0 V32 M14 48 V80"/><path d="M14 32 L2 40 L14 48"/></svg>':'<i class="mech-dash" aria-hidden="true"></i>'}<div class="contact-row">${ctx.services.editor.terminal(m,'13','13','left')}${ctx.services.editor.dinNo()}${ctx.services.editor.terminal(m,'14','14','right')}</div><div class="contact-row">${ctx.services.editor.terminal(m,'21','21','left')}${ctx.services.editor.dinNc()}${ctx.services.editor.terminal(m,'22','22','right')}</div><div class="lamp-row">${ctx.services.editor.terminal(m,'X1','','left')}${ctx.services.editor.dinLamp(p)}${ctx.services.editor.terminal(m,'X2','','right')}<svg class="din-sym lamp-leads" viewBox="0 0 100 30" preserveAspectRatio="none" aria-hidden="true"><path d="M0 15 H100"/></svg></div></div>`;
  }
  function renderModule(m){
    ctx.services.editor.defaultNom(m);
    const el=document.createElement('div');m.el=el;el.className='module '+(m.type==='rail'?'bus':m.type);el.dataset.id=m.id;
    if(m.type==='terminal'){el.classList.toggle('terminal-v2',m.terminalVersion===2);el.style.width=(m.terminalVersion===2?24+ctx.services.editor.terminalPoles(m)*36:Math.max(220,36+ctx.services.editor.terminalPoles(m)*46+Math.ceil(ctx.services.editor.terminalPoles(m)/ctx.services.editor.terminalSpan(m))*8))+'px';}
    if(m.type==='breaker'&&m.poles===3){el.style.width='198px';el.classList.add('poles-3');}
    if(m.type==='rail')el.style.width=(ctx.services.editor.railScrews(m)>5?420:280)+'px';
    el.innerHTML=`<div class="handle"><button class="handle-label" aria-label="${ctx.services.editor.displayName(m)} verschieben, Pfeiltasten verwenden">${ctx.services.editor.shortName(m)}</button><span class="handle-copy"><span class="handle-name">${ctx.config.types[m.type]}</span><span class="handle-sub"></span></span><span class="lock-indicator" hidden aria-label="Position gesperrt"><svg viewBox="0 0 24 24" aria-hidden="true"><rect x="5" y="10" width="14" height="11" rx="2"/><path d="M8 10 V7 a4 4 0 0 1 8 0 v3"/></svg></span><button class="remove" aria-label="${ctx.services.editor.displayName(m)} entfernen" title="Bauteil entfernen">×</button></div><div class="face">${ctx.services.editor.face(m)}</div><div class="state-label"><span class="module-state"></span></div>`;
    if(m.type==='note'||m.type==='junction'){
      const controls=document.createElement('div');controls.className='note-resize-controls';controls.setAttribute('aria-hidden','true');
      controls.innerHTML=['n','ne','e','se','s','sw','w','nw'].map(edge=>'<button type="button" class="note-resize-grip" data-note-resize="'+edge+'" tabindex="-1" aria-label="Größe ändern"></button>').join('');el.append(controls);
      controls.querySelectorAll('[data-note-resize]').forEach(grip=>grip.addEventListener('pointerdown',e=>{
        if(e.button!==0||m.locked)return;e.preventDefault();e.stopPropagation();
        const box=m.el.querySelector('.face').getBoundingClientRect();ctx.services.editor.cancel();ctx.services.projekt.remember();ctx.services.editor.selectModule(m.id);
        ctx.state.gesture={kind:'resize-note',m,edge:grip.dataset.noteResize,x:e.clientX,y:e.clientY,startX:m.x,startY:m.y,startWidth:m.type==='junction'?m.boxWidth:m.noteWidth,startHeight:m.type==='junction'?m.boxHeight:m.noteHeight||Math.round(box.height),pointerId:e.pointerId};
        el.classList.add('resizing');grip.setPointerCapture(e.pointerId);
      }));
    }
    const overlay=document.createElementNS(ctx.config.NS,'svg');overlay.classList.add('module-wires');overlay.setAttribute('aria-hidden','true');el.appendChild(overlay);
    if(!Number.isFinite(m.z))m.z=ctx.state.nextZ++;ctx.state.nextZ=Math.max(ctx.state.nextZ,m.z+1);el.style.zIndex=m.z;
    ctx.dom.board.appendChild(el);ctx.services.editor.position(m);ctx.services.editor.paintDeviceFace(m);
    el.querySelector('.remove').onclick=()=>ctx.services.editor.removeModule(m);
    ctx.services.editor.bindMoveKeys(el.querySelector('.handle-label'),m);
    ctx.services.editor.bindMoveKeys(el.querySelector('.note-content'),m);
    ctx.services.editor.bindMoveKeys(el.querySelector('.lever-body'),m);
    el.querySelector('.lever-body')?.addEventListener('focus',()=>ctx.services.editor.selectModule(m.id));
    el.querySelector('.note-content')?.addEventListener('focus',()=>{if(['label','frame'].includes(m.noteStyle))ctx.services.editor.selectModule(m.id);});
    el.addEventListener('pointerdown',e=>{
      if(e.button!==0||!ctx.services.editor.moduleDragTarget(e))return;
      ctx.services.editor.startModuleDrag(m,e);
    });
    el.addEventListener('dblclick',e=>{
      if(e.target.closest('.term,.remove,.push,.wire-hit,.drive-adjust,.emergency-unlock,.selector-knob,.selector-choices,.installation-rocker,.meter-controls,.protection-switch,.protection-actions,.timer-settings,.pot-controls,.motion-test,[data-terminal-bridge],input,select,textarea,[contenteditable="true"]'))return;
      e.preventDefault();e.stopPropagation();ctx.services.editor.selectModule(m.id);ctx.services.editor.openSettings(m);
    });
    if(m.type==='potentiometer'){
      const slider=el.querySelector('.pot-slider');let recorded=false;
      slider.oninput=()=>{if(!recorded){ctx.services.projekt.remember();recorded=true;}m.potPosition=Number(slider.value)/100;if(ctx.state.editTarget===m&&ctx.dom.$('#devPotPosition'))ctx.dom.$('#devPotPosition').value=slider.value;ctx.services.editor.paintPot(m);if(ctx.state.engine?.setPotentiometer)ctx.state.engine.setPotentiometer(m.id,m.resistance,m.potPosition);ctx.services.projekt.schedulePersist();ctx.services.simulation.refreshLive();};
      slider.onchange=()=>{recorded=false;};slider.onblur=()=>{recorded=false;};
    }
    if(m.type==='motion'){
      let last=0;const area=el.querySelector('.motion-detection');
      area.onpointermove=e=>{if(e.pointerType==='touch'||e.buttons||performance.now()-last<120)return;last=performance.now();ctx.services.simulation.triggerMovement(m);};
      area.onpointerenter=e=>{if(e.pointerType==='mouse'&&!e.buttons)ctx.services.simulation.triggerMovement(m);};
      el.querySelector('.motion-test').onclick=()=>ctx.services.simulation.triggerMovement(m);
    }
    if(m.type==='timer'){el.querySelector('.timer-settings').onclick=()=>{ctx.services.editor.selectModule(m.id);ctx.services.editor.openSettings(m);};ctx.services.editor.paintTimer(m,ctx.state.lastSnap);}
    if(m.type==='terminal'&&m.terminalVersion===2)el.querySelectorAll('[data-terminal-bridge]').forEach(button=>{
      button.onclick=()=>ctx.services.editor.toggleTerminalBridge(m,Number(button.dataset.terminalBridge));
      button.addEventListener('keydown',e=>e.stopPropagation());
    });
    if(ctx.services.editor.isProtection(m)){
      el.querySelector('.protection-switch').onclick=()=>ctx.services.simulation.operateProtection(m,m.protectionOn?'off':'on');
      el.querySelectorAll('[data-protection]').forEach(b=>b.onclick=()=>ctx.services.simulation.operateProtection(m,b.dataset.protection));
      ctx.services.editor.paintProtection(m);
    }
    if(ctx.services.editor.isMeter(m)){
      el.querySelectorAll('[data-lead]').forEach(b=>b.addEventListener('pointerdown',e=>{if(e.button!==0)return;e.stopPropagation();e.preventDefault();ctx.services.editor.beginLead(m,b.dataset.lead,e);}));
      el.querySelector('.meter-clear').onclick=()=>{ctx.services.projekt.remember();ctx.services.editor.setLead(m,'probeRed',null);ctx.services.editor.setLead(m,'probeBlack',null);ctx.services.projekt.schedulePersist();ctx.services.editor.paintMeters(ctx.state.lastSnap);ctx.services.editor.draw();};
    }
    el.querySelectorAll('.term').forEach(t=>{
      t.addEventListener('pointerdown',e=>{if(e.button!==0||ctx.state.gesture)return;e.stopPropagation();if(ctx.services.editor.isMeasurementPoint(t.dataset.key)){e.preventDefault();ctx.services.editor.measurementPointHint();return;}ctx.state.gesture={kind:'wire',key:t.dataset.key,x:e.clientX,y:e.clientY,moved:false,pointerId:e.pointerId};t.setPointerCapture(e.pointerId);ctx.services.editor.updateWireTarget(e);});
      t.addEventListener('click',e=>{if(e.detail!==0)return;ctx.services.editor.choose(t.dataset.key);});
      t.addEventListener('lostpointercapture',e=>{if(ctx.state.gesture?.kind==='wire'&&ctx.state.gesture.pointerId===e.pointerId&&!ctx.state.gesture.moved)ctx.services.editor.cancel();});
      t.addEventListener('focus',()=>{ctx.state.wireTarget=ctx.services.editor.canConnect(ctx.services.editor.wireSource(),t.dataset.key)?t.dataset.key:null;ctx.services.editor.mark();});
      t.addEventListener('blur',()=>{if(ctx.state.wireTarget===t.dataset.key){ctx.state.wireTarget=null;ctx.services.editor.mark();}});
    });
    if(m.type==='lineardrive'){
      ctx.services.editor.paintDrive(m,ctx.state.lastSnap);
      const slider=el.querySelector('.drive-position');
      slider.addEventListener('pointerdown',()=>ctx.services.projekt.remember());
      slider.addEventListener('keydown',e=>{if(['ArrowLeft','ArrowRight','Home','End','PageUp','PageDown'].includes(e.key))ctx.services.projekt.remember();});
      slider.addEventListener('input',()=>{const next=Number(slider.value)/1000;if(next>m.slide+1e-9)m.linealDir='rechts';else if(next<m.slide-1e-9)m.linealDir='links';m.slide=next;ctx.state.driveFrames.delete(m);ctx.services.editor.paintDrive(m,ctx.state.lastSnap);ctx.services.projekt.schedulePersist();});
    }
    if(ctx.services.editor.isInstallationSwitch(m)){
      const rocker=el.querySelector('.installation-rocker');
      for(const event of ['keydown','keyup'])rocker.addEventListener(event,e=>{if(e.code==='Space'||e.key==='Enter')e.stopPropagation();});
      rocker.onclick=()=>{ctx.services.projekt.remember();m.selection=m.selection===1?0:1;if(ctx.state.engine?.setSelector)ctx.state.engine.setSelector(m.id,m.selection);ctx.services.editor.paintInstallationSwitch(m);ctx.services.projekt.schedulePersist();ctx.services.simulation.refreshLive();};
    }
    if(m.type==='selector'){
      const set=value=>{if(value===m.selection)return;ctx.services.projekt.remember();m.selection=value;if(ctx.state.engine?.setSelector)ctx.state.engine.setSelector(m.id,value);ctx.services.editor.paintSelector(m);ctx.services.simulation.refreshLive();};
      el.querySelectorAll('[data-selection]').forEach(b=>b.onclick=()=>set(Number(b.dataset.selection)));
      const knob=el.querySelector('.selector-knob');
      knob.onclick=()=>set(m.selection===0?1:m.selection===1?-1:0);
      knob.onkeydown=e=>{if(!['ArrowLeft','ArrowRight','Home'].includes(e.key))return;e.preventDefault();set(e.key==='Home'?0:Math.max(-1,Math.min(1,m.selection+(e.key==='ArrowLeft'?-1:1))));};
      ctx.services.editor.paintSelector(m);
    }
    const push=el.querySelector('.push');
    if(m.type==='emergency'){
      const latch=active=>{if(m.latched===active)return;ctx.services.projekt.remember();m.latched=active;m.pressed=active;if(ctx.state.engine)ctx.state.engine.setPressed(m.id,active);ctx.services.editor.paintTaster(m);ctx.services.simulation.refreshLive();};
      push.onclick=()=>latch(true);
      el.querySelector('.emergency-unlock').onclick=()=>latch(false);
      ctx.services.editor.paintTaster(m);
    }else if(push){
      const press=()=>{m.pressed=true;push.classList.add('down');m.el.classList.add('pressed');if(ctx.state.engine){ctx.state.engine.setPressed(m.id,true);if(ctx.state.engine.setRealtime)ctx.state.engine.setRealtime(true);}ctx.services.editor.paintTaster(m);ctx.services.simulation.refreshLive();};
      const release=()=>{if(!m.pressed)return;m.pressed=false;push.classList.remove('down');m.el.classList.remove('pressed');if(ctx.state.engine)ctx.state.engine.setPressed(m.id,false);ctx.services.editor.paintTaster(m);ctx.services.simulation.refreshLive();};
      const toggle=()=>{m.pressed=!m.pressed;if(ctx.state.engine){ctx.state.engine.setPressed(m.id,m.pressed);if(m.pressed&&ctx.state.engine.setRealtime)ctx.state.engine.setRealtime(true);}ctx.services.editor.paintTaster(m);ctx.services.simulation.refreshLive();};
      push.onclick=()=>{if(m.latching)toggle();};
      push.onpointerdown=e=>{if(e.button!==0||m.latching)return;e.preventDefault();push.setPointerCapture(e.pointerId);press();};
      push.onpointerup=()=>{if(!m.latching)release();};push.onpointercancel=()=>{if(!m.latching)release();};push.onlostpointercapture=()=>{if(!m.latching)release();};push.onblur=()=>{if(!m.latching)release();};
      push.onkeydown=e=>{if(e.key!==' '&&e.key!=='Enter'||m.latching)return;e.preventDefault();press();};
      push.onkeyup=e=>{if(e.key===' '||e.key==='Enter'){if(m.latching)return;e.preventDefault();release();}};
    }
    el.addEventListener('contextmenu',e=>{
      e.preventDefault();
      e.stopPropagation();
      ctx.services.editor.selectModule(m.id);
      ctx.services.editor.openSettings(m,e.clientX,e.clientY);
    });
  }
  function selection(){return ctx.state.modules.filter(m=>ctx.state.selectedIds.has(m.id));}
  function moduleDragTarget(e){return !e.target.closest('.term,.remove,.push,.wire-hit,.drive-adjust,.emergency-unlock,.selector-knob,.selector-choices,.installation-rocker,.meter-controls,.protection-switch,.protection-actions,.timer-settings,.pot-controls,.motion-test,[data-terminal-bridge],[data-note-resize],input,select,textarea,[contenteditable="true"]');}
  function startModuleDrag(m,e,toggleOnTap=false,preserveOnTap=false){
    ctx.services.editor.cancel();ctx.services.editor.selectModule(m.id);
    const entries=m.locked?[]:ctx.services.editor.selectedMovable().map(m=>({m,x:m.x,y:m.y}));
    ctx.state.gesture={kind:'module',m,entries,x:e.clientX,y:e.clientY,pointerId:e.pointerId,recorded:false,toggleOnTap,preserveOnTap};
    entries.forEach(({m})=>m.el.classList.add('dragging'));
    m.el.setPointerCapture(e.pointerId);e.preventDefault();
  }
  function moveModuleDrag(e){
    const g=ctx.state.gesture,dx=(e.clientX-g.x)/ctx.state.scale,dy=(e.clientY-g.y)/ctx.state.scale;
    if(!g.entries.length||(!g.recorded&&Math.hypot(dx,dy)<3))return;
    if(!g.recorded){ctx.services.projekt.remember();g.recorded=true;}
    ctx.services.editor.moveEntries(g.entries,dx,dy);ctx.services.editor.draw();
  }
  function finishModuleDrag(){
    const g=ctx.state.gesture;ctx.state.gesture=null;
    g.entries.forEach(({m})=>m.el.classList.remove('dragging'));
    if(g.recorded){
      const first=g.entries[0];
      ctx.services.editor.moveEntries(g.entries,Math.round((first.m.x-first.x)/4)*4,Math.round((first.m.y-first.y)/4)*4);
      ctx.services.projekt.schedulePersist();ctx.state.ignorePanClick=true;
    }else if(g.toggleOnTap){ctx.services.editor.toggleSelection(g.m.id);ctx.state.ignorePanClick=true;}
    else if(!g.preserveOnTap&&ctx.state.selectedIds.size>1)ctx.services.editor.setSelection([g.m.id],g.m.id);
    ctx.services.editor.draw();
  }
  function setSelection(ids,primary){
    ctx.state.selectedIds=new Set(ids.filter(id=>ctx.state.modules.some(m=>m.id===id)));
    ctx.state.selectedId=ctx.state.selectedIds.has(primary)?primary:[...ctx.state.selectedIds].at(-1)||null;
    ctx.state.selectedWire=null;ctx.state.wireScope='selection';ctx.services.editor.renderComponents();ctx.services.editor.draw();
  }
  function toggleSelection(id){const ids=new Set(ctx.state.selectedIds);if(ids.has(id))ids.delete(id);else ids.add(id);ctx.services.editor.setSelection([...ids],id);}
  function selectedMovable(){return ctx.services.editor.selection().filter(m=>!m.locked);}
  function moveEntries(entries,dx,dy){
    if(!entries.length)return false;
    const lowX=Math.max(...entries.map(({x})=>24-x)),highX=Math.min(...entries.map(({m,x})=>ctx.config.BOARD_W-24-m.el.offsetWidth-x));
    const lowY=Math.max(...entries.map(({y})=>61-y)),highY=Math.min(...entries.map(({m,y})=>ctx.config.BOARD_H-36-m.el.offsetHeight-y));
    dx=Math.max(lowX,Math.min(highX,dx));dy=Math.max(lowY,Math.min(highY,dy));
    entries.forEach(({m,x,y})=>ctx.services.editor.move(m,x+dx,y+dy));return !!(dx||dy);
  }
  function moveSelection(dx,dy,repeat=false){
    const entries=ctx.services.editor.selectedMovable().map(m=>({m,x:m.x,y:m.y}));
    if(!entries.length){ctx.services.editor.say('Ausgewählte Positionen sind gesperrt.');return;}
    if(!repeat||!ctx.state.selectionKeyboard){ctx.services.projekt.remember();ctx.state.selectionKeyboard=true;}
    ctx.services.editor.moveEntries(entries,dx,dy);ctx.services.editor.draw();ctx.services.projekt.schedulePersist();
  }
  function arrangeSelection(mode){
    const items=ctx.services.editor.selectedMovable(),distribution=mode.startsWith('distribute');
    if(items.length<(distribution?3:2))return;
    const left=Math.min(...items.map(m=>m.x)),right=Math.max(...items.map(m=>m.x+m.el.offsetWidth));
    const top=Math.min(...items.map(m=>m.y)),bottom=Math.max(...items.map(m=>m.y+m.el.offsetHeight));
    let distributionPlan;
    if(distribution){
      const horizontal=mode==='distribute-x',pos=horizontal?'x':'y',size=horizontal?'offsetWidth':'offsetHeight';
      const sorted=items.slice().sort((a,b)=>a[pos]-b[pos]),first=sorted[0],last=sorted.at(-1);
      const gap=(last[pos]+last.el[size]-first[pos]-sorted.reduce((sum,m)=>sum+m.el[size],0))/(sorted.length-1);
      if(gap<0){ctx.services.editor.say('Zu wenig Platz für gleiche Abstände. Die äußeren Bauteile weiter auseinanderziehen.');return;}
      distributionPlan={horizontal,pos,size,sorted,first,gap};
    }
    ctx.services.projekt.remember();
    if(distribution){
      const {horizontal,pos,size,sorted,first,gap}=distributionPlan;
      let cursor=first[pos];sorted.forEach(m=>{ctx.services.editor.move(m,horizontal?cursor:m.x,horizontal?m.y:cursor);cursor+=m.el[size]+gap;});
    }else items.forEach(m=>ctx.services.editor.move(m,mode==='left'?left:mode==='right'?right-m.el.offsetWidth:mode==='center-x'?(left+right-m.el.offsetWidth)/2:m.x,mode==='top'?top:mode==='bottom'?bottom-m.el.offsetHeight:mode==='center-y'?(top+bottom-m.el.offsetHeight)/2:m.y));
    ctx.services.editor.draw();ctx.services.projekt.schedulePersist();ctx.services.editor.say('Auswahl ausgerichtet. Gesperrte Positionen bleiben stehen.');
  }
  function renderGroupInspector(){
    const items=ctx.services.editor.selection(),movable=ctx.services.editor.selectedMovable();ctx.services.editor.closeDevMenu();ctx.dom.$('#addAux').hidden=true;ctx.dom.$('#couplingNote').hidden=true;
    ctx.dom.$('#detailTitle').textContent=items.length+' Bauteile ausgewählt';ctx.dom.$('#detailKind').textContent='Mehrfachauswahl';ctx.dom.$('#detailKind').hidden=false;
    const preview=ctx.dom.$('#detailPreview');preview.hidden=false;preview.classList.add('glyph-preview');preview.innerHTML='<svg viewBox="0 0 48 48" aria-hidden="true"><path d="M6 28V6h22M14 36V14h22" fill="none" stroke="#376457" stroke-width="2.5"/><rect x="22" y="22" width="20" height="20" rx="2" fill="none" stroke="#376457" stroke-width="2.5"/></svg>';
    const locked=items.length-movable.length;
    const details=ctx.dom.$('#componentDetails');details.innerHTML='<p class="empty">Gemeinsam ziehen oder mit Pfeiltasten verschieben. '+(locked?locked+' gesperrt; diese bleiben stehen.':'Alle Positionen verschiebbar.')+'</p><h3>Ausrichten</h3><div class="group-align"></div><h3>Gleichmäßig verteilen</h3><div class="group-distribute"></div><div class="property-actions group-actions"></div><p class="empty">Kopien enthalten interne Leitungen und zugehörige Schütze mit Hilfskontakten. Externe Leitungen werden ausgelassen.</p>';
    [['left','Links'],['center-x','Horizontal zentrieren'],['right','Rechts'],['top','Oben'],['center-y','Vertikal zentrieren'],['bottom','Unten'],['distribute-x','Horizontal'],['distribute-y','Vertikal']].forEach(([mode,label])=>{
      const b=document.createElement('button');b.className='ui-btn';b.textContent=label;b.disabled=movable.length<(mode.startsWith('distribute')?3:2);b.onclick=()=>ctx.services.editor.arrangeSelection(mode);details.querySelector(mode.startsWith('distribute')?'.group-distribute':'.group-align').append(b);
    });
    [['Kopieren',()=>{ctx.state.componentClipboard=ctx.services.projekt.copySelection();ctx.state.clipboardPasteCount=0;ctx.services.editor.say(items.length+' Bauteile kopiert.');}],['Duplizieren',()=>ctx.services.projekt.pastePackage(ctx.services.projekt.copySelection())],['Auswahl löschen',()=>ctx.services.editor.removeSelection()]].forEach(([label,action])=>{const b=document.createElement('button');b.className='ui-btn';b.textContent=label;b.onclick=action;details.querySelector('.group-actions').append(b);});
    ctx.services.editor.renderWires();
  }
  function selectModule(id,focus=false){
    const m=ctx.state.modules.find(m=>m.id===id);if(!m)return;
    ctx.state.selectedWire=null;ctx.state.wireScope='selection';
    if(focus||!ctx.state.selectedIds.has(id))ctx.state.selectedIds=new Set([id]);
    ctx.state.selectedId=id;m.z=ctx.state.nextZ++;m.el.style.zIndex=m.z;ctx.services.editor.raiseLeads();
    ctx.state.modules.forEach(x=>x.el.classList.toggle('selected',ctx.state.selectedIds.has(x.id)));
    ctx.services.editor.renderComponents();
    if(focus){ctx.services.editor.reveal(m);(m.type==='connector'&&!m.showBanner?m.el.querySelector('.lever-body'):m.type==='note'&&['label','frame'].includes(m.noteStyle)?m.el.querySelector('.note-content'):m.el.querySelector('.handle-label')).focus({preventScroll:true});}
  }
  function removeModule(m){ctx.services.editor.removeSelection([m]);}
  function removeSelection(items=ctx.services.editor.selection()){
    if(!items.length)return;
    const ids=new Set(items.map(m=>m.id));ctx.state.modules.filter(m=>ids.has(m.parent)).forEach(m=>ids.add(m.id));
    if(ctx.state.modules.some(m=>ids.has(m.id)&&ctx.services.simulation.electricalModule(m))&&!ctx.services.simulation.requestElectricalEdit(()=>ctx.services.editor.removeSelection(items)))return;
    ctx.services.projekt.remember();ctx.services.editor.cancel();ctx.state.wires=ctx.state.wires.filter(w=>!ids.has(w.a.split('.')[0])&&!ids.has(w.b.split('.')[0]));
    ctx.state.modules=ctx.state.modules.filter(m=>{if(ids.has(m.id)){m.el.remove();return false;}return true;});
    ctx.state.selectedIds=new Set([...ctx.state.selectedIds].filter(id=>!ids.has(id)));ctx.state.selectedId=[...ctx.state.selectedIds].at(-1)||null;ctx.services.simulation.update();
  }
  function createAux(parent){
    if(ctx.state.modules.some(m=>m.type==='auxiliary'&&m.parent===parent.id))return null;
    const aux={id:parent.id+'H',type:'auxiliary',parent:parent.id,x:Math.min(ctx.config.BOARD_W-204,parent.x+264),y:parent.y+70};
    ctx.state.modules.push(aux);ctx.services.editor.renderModule(aux);ctx.services.editor.move(aux,aux.x,aux.y);return aux;
  }
  function partKind(m){return ctx.services.editor.isCombinedTaster(m)&&m.latching?'Schalter · rastend':m.type==='note'?ctx.services.editor.componentTitle(m):m.type==='auxiliary'?'Hilfskontakte':m.type==='contactor'?'Hauptschütz':m.type==='terminal'&&m.terminalVersion===2?'Reihenklemmen V2':ctx.config.types[m.type];}
  function partBlob(m){return [m.id,ctx.services.editor.displayName(m),ctx.services.editor.shortName(m),ctx.services.editor.partKind(m),m.description||'',m.noteText||'',m.parent||''].join(' ').toLowerCase();}
  function inspectorRows(m){
    if(m.type==='terminal'&&m.terminalVersion===2)return [['Klemmen','1 bis '+ctx.services.editor.terminalPoles(m)],['Oben / unten','Dieselbe Nummer'],['Verbunden',ctx.services.editor.terminalGroups(m).join(' · ')],['Steckbrücken',String(ctx.services.editor.terminalLinks(m).length)],['Gehäusefarbe','Kennzeichnung, keine Verdrahtung']];
    if(m.type==='changeover')return [['Gemeinsamer Anschluss','L'],['Korrespondierende','1 / 2'],['Stellung 1','L–1'],['Stellung 2','L–2'],['Betätigung','Rastende Wippe · zwei Stellungen']];
    if(m.type==='crossover')return [['Eingangspaar','1 / 2'],['Ausgangspaar','3 / 4'],['Stellung 1','1–3 und 2–4'],['Stellung 2','1–4 und 2–3'],['Kreuzung','Kein elektrischer Knoten in der Mitte']];
    if(m.type==='junction')return [['Gehäuse','Keine elektrischen Anschlüsse'],['Größe',m.boxWidth+' × '+m.boxHeight],['Bestückung','Verbindungsklemmen separat einsetzen']];
    if(m.type==='connector')return [['Anschlüsse','1 bis '+ctx.services.editor.connectorPorts(m)],['Intern verbunden','Alle Anschlüsse dieser Klemme'],['Potenzial','Bestimmt deine Verdrahtung']];
    if(m.type==='socket')return [['Zuleitung','L / N / PE · untere Anschlussklemmen'],['Steckkontakt 1','Messpunkt · intern mit L verbunden'],['Steckkontakt 2','Messpunkt · intern mit N verbunden'],['Schutzkontakt','Messpunkt · intern mit PE verbunden'],['Vorderseite','Nur Messleitungen, keine Adern'],['L–N / L–PE / N–PE','Keine interne Verbindung'],['Eigenverbrauch','Keine Last']];
    if(m.type==='potentiometer')return [['Widerstand 1–3',m.resistance+' Ω'],['Schleifer','2 · '+Math.round(m.potPosition*100)+' %'],['Kennlinie','Linear']];
    if(m.type==='motion')return [['Versorgung',m.nomV+' V '+(m.nomV===24?'DC':'AC')],['Eingang','L / +24 · N / 0 V'],['Ausgang','OUT / L′ · geschaltete Versorgung'],['Nachlauf',m.motionDelay+' s · nachtriggerbar']];
    if(m.type==='note')return [['Darstellung',m.noteStyle==='label'?'Beschriftungsstreifen':m.noteStyle==='frame'?'Markierungsrahmen':'Notizzettel'],['Position',m.locked?'Gesperrt':'Verschiebbar'],...(m.noteStyle==='card'?[]:[['Größe',m.noteWidth+' × '+m.noteHeight+' px']])];

    return m.type==='timer'?[['Spule','A1 / A2 · '+(m.nomV||24)+' V'],['Gemeinsam','15'],['Ruhe','15–16'],['Arbeit','15–18'],['Funktion',ctx.config.timerModes[m.timerMode]||ctx.config.timerModes[0]]]:m.type==='contactor'?[['Leistung','1–2 · 3–4 · 5–6'],['Hilfskontakt','13–14 NO'],['Spule','A1+ / A2− · '+(m.nomV||24)+' V']]:m.type==='auxiliary'?[['Gekoppelt an',m.parent],['Schließer','53–54 · 83–84'],['Öffner','61–62 · 71–72'],['Eigene Spule','Keine']]:m.type==='psu'?[['Ausgang',ctx.services.editor.fmtV(m.voltage)+' V'],['Klemmen','+24 / 0 V']]:m.type==='mains'?[['U_LL',ctx.services.editor.fmtV(m.voltage)+' V'],['U_LN',ctx.services.editor.fmtV(m.voltage/Math.sqrt(3))+' V'],['Frequenz','50 Hz'],['Klemmen','L1 / L2 / L3 / N / PE'],['N–PE','intern verbunden']]:m.type==='lineardrive'?[['Motor','24 V DC · umpolbar'],['B1 / B2 / B3','Links / Mitte / rechts'],['Wechsler','11–12 Ruhe · 11–14 aktiv'],['B6 / B7','Inkrementalgeber · Rechteck'],['Richtung','Rechts: B6 vor B7 · Links: B7 vor B6'],['Versorgung','24 V nur für den Geber'],['Modell','10 s bei Nennspannung · 20 Takte'],['PE','Separate Gehäuseklemme']]:m.type==='acmotor'?[['Netz','400 V 3~'],['Nennleistung',(m.ratedKw||1.5)+' kW'],['Wirkungsgrad',Math.round((m.eta||0.8)*100)+' %'],['Zuleitung','U1 · V1 · W1'],['Wicklung','U2 · V2 · W2'],['PE','Gehäuse, nicht an der Wicklung']]:(m.type==='motor'||ctx.services.editor.isLight(m))?[['Nennspannung',(m.nomV||24)+' V'],['Nennleistung',(m.nomP||50)+' W']].concat(m.type==='pilot'?[['Leuchtfarbe',ctx.config.signalColors[m.signalColor][0]]]:[]):m.type==='selector'?[['Stellung A','13–14 geschlossen'],['Stellung 0','Beide offen'],['Stellung B','23–24 geschlossen'],['Betätigung','Rastend · A / 0 / B']]:m.type==='emergency'?[['Öffner','21–22 NC'],['Schließer','13–14 NO'],['Betätigung','Rastend'],['Rückstellen','Separat entriegeln']]:m.type==='terminal'?[['Klemmen','1 bis '+ctx.services.editor.terminalPoles(m)],['Oben / unten','dieselbe Nummer'],['Rand',ctx.services.editor.terminalSpan(m)===2?'Paare, eine Verbindung':ctx.services.editor.terminalSpan(m)===4?'je 4, eine Verbindung':'keine Brücke']]:m.type==='rail'?[['+24',ctx.services.editor.railScrews(m)+' Schrauben, eine Schiene'],['0 V',ctx.services.editor.railScrews(m)+' Schrauben, getrennt von +24'],['PE',ctx.services.editor.railScrews(m)+' Schrauben, nicht auf 0 V']]:ctx.services.editor.isCombinedTaster(m)?[['Schließer','13–14 NO'],['Öffner','21–22 NC'],['Meldeleuchte',ctx.services.editor.lampTag(m)+' · X1 / X2'],['Tasterfarbe',ctx.config.buttonColors[m.buttonColor][0]],['Leuchtfarbe',ctx.config.signalColors[m.signalColor][0]]]:[['Bauteil',ctx.config.types[m.type]]];
  }
  function refreshInspectorRows(m){
    const box=ctx.dom.$('#componentDetails .terminal-info');
    if(!box)return;
    const open=box.open;
    box.innerHTML='<summary>Anschlüsse und Daten</summary>'+ctx.services.editor.inspectorRows(m).map(([label,value])=>`<div class="detail-row"><span>${label}</span><b>${value}</b></div>`).join('');
    box.open=open;
  }
  function renderComponentResults(){
    if(ctx.dom.$('#partsPanel').hidden)return;
    const list=ctx.dom.$('#componentList'),scrollTop=list.scrollTop,q=(ctx.dom.$('#partSearch')?.value||'').trim().toLowerCase();
    list.replaceChildren();
    const used=new Set(),groups=[];
    ctx.state.modules.filter(m=>m.type==='contactor').forEach(parent=>{
      const items=[parent,...ctx.state.modules.filter(m=>m.type==='auxiliary'&&m.parent===parent.id)];
      items.forEach(m=>used.add(m.id));
      groups.push({title:parent.id,items});
    });
    ctx.state.modules.filter(m=>m.type==='auxiliary'&&!used.has(m.id)).forEach(m=>{used.add(m.id);groups.push({title:m.parent||m.id,items:[m]});});
    const rest=ctx.state.modules.filter(m=>!used.has(m.id));
    if(rest.length)groups.push({title:groups.length?'Weitere Bauteile':'',items:rest});
    let shown=0;
    groups.forEach(group=>{
      const items=group.items.filter(m=>!q||ctx.services.editor.partBlob(m).includes(q));
      if(!items.length)return;
      shown+=items.length;
      if(group.title){const h=document.createElement('div');h.className='part-group';h.textContent=group.title;list.appendChild(h);}
      items.forEach(m=>{
        const b=document.createElement('button');b.className='component-select';b.setAttribute('aria-pressed',String(ctx.state.selectedIds.has(m.id)));
        const title=m.description||(m.type==='note'?m.noteText:'')||ctx.services.editor.partKind(m);
        b.setAttribute('aria-label',ctx.services.editor.displayName(m)+' · '+title+' auf der Montageplatte anzeigen');
        const glyph=document.createElement('i');glyph.className='component-result-icon';glyph.setAttribute('aria-hidden','true');const icon=ctx.services.editor.paletteIcon(m);if(icon)glyph.append(icon.cloneNode(true));
        const id=document.createElement('strong');id.textContent=ctx.services.editor.shortName(m);
        const copy=document.createElement('span');copy.className='component-result-copy';const name=document.createElement('span');name.className='component-result-title';name.textContent=title;copy.append(name);
        if(title!==ctx.services.editor.partKind(m)){const kind=document.createElement('small');kind.textContent=ctx.services.editor.partKind(m);copy.append(kind);}
        b.append(glyph,id,copy);
        b.onclick=()=>{ctx.services.editor.setComponentSearch(false);ctx.services.editor.selectModule(m.id,true);};list.appendChild(b);
      });
    });
    if(!shown){const p=document.createElement('p');p.className='empty';p.textContent=q?'Kein Bauteil zu dieser Suche.':'Keine Bauteile auf der Platte.';list.appendChild(p);}
    list.scrollTop=scrollTop;
    ctx.dom.$('#partSearchStatus').textContent=shown+' '+(shown===1?'Bauteil':'Bauteile')+(q?' gefunden':' im Aufbau');
  }
  function setComponentSearch(open){
    ctx.dom.$('#partsPanel').hidden=!open;ctx.dom.$('#partsToggle').setAttribute('aria-expanded',String(open));ctx.dom.$('#partsToggle').setAttribute('aria-label',open?'Bauteilsuche schließen':'Bauteilsuche öffnen');
    if(open)ctx.services.editor.renderComponentResults();
  }
  function renderSelectionActions(){
    ctx.dom.$('#selectionActions').hidden=!ctx.state.selectedIds.size;
    ctx.dom.$('#selectionCount').textContent=ctx.state.selectedIds.size===1?'1 Bauteil ausgewählt':ctx.state.selectedIds.size+' Bauteile ausgewählt';
  }
  function renderComponents(){
    ctx.services.editor.renderSelectionActions();
    ctx.dom.$('#devMenu').querySelector('.property-actions')?.remove();
    ctx.services.editor.renderComponentResults();
    ctx.state.modules.forEach(m=>m.el.classList.toggle('selected',ctx.state.selectedIds.has(m.id)));
    if(ctx.state.selectedIds.size>1){ctx.services.editor.renderGroupInspector();return;}
    const m=ctx.state.modules.find(x=>x.id===ctx.state.selectedId);
    const parent=m?.type==='contactor'?m:m?.type==='auxiliary'?ctx.state.modules.find(x=>x.id===m.parent):null;
    const canAdd=!!parent&&!ctx.state.modules.some(x=>x.type==='auxiliary'&&x.parent===parent.id);
    const add=ctx.dom.$('#addAux');add.hidden=!canAdd;add.disabled=!canAdd;
    if(canAdd)add.textContent='Hilfskontakte für '+parent.id+' ergänzen';
    ctx.dom.$('#detailTitle').textContent=m?ctx.services.editor.displayName(m):'Kein Bauteil ausgewählt';const kind=ctx.dom.$('#detailKind');if(kind){kind.hidden=!m;kind.textContent=m?ctx.services.editor.partKind(m):'';}ctx.services.editor.paintDetailPreview(m);
    ctx.dom.$('#couplingNote').hidden=!(m&&(m.type==='contactor'||m.type==='auxiliary'));
    const details=ctx.dom.$('#componentDetails');
    if(ctx.state.selectedWire&&!ctx.state.wires.includes(ctx.state.selectedWire))ctx.state.selectedWire=null;
    if(ctx.state.selectedWire){ctx.services.editor.renderWireDetails();ctx.services.editor.renderWires();return;}
    if(!m){ctx.services.editor.closeDevMenu();details.innerHTML='<p class="empty">Bauteil oder Leitung auswählen, um Eigenschaften zu bearbeiten.</p>';ctx.services.editor.renderWires();return;}
    const rows=ctx.services.editor.inspectorRows(m);
details.innerHTML='<details class="terminal-info"><summary>Anschlüsse und Daten</summary>'+rows.map(([label,value])=>`<div class="detail-row"><span>${label}</span><b>${value}</b></div>`).join('')+'</details><button class="ui-btn" id="openSettings" type="button">Einstellungen</button>';
    ctx.dom.$('#openSettings').onclick=()=>{const r=m.el.getBoundingClientRect();ctx.services.editor.openSettings(m,r.right-8,r.top+32);};
    ctx.dom.$('#openSettings').hidden=true;
    ctx.services.editor.openSettings(m,0,0,false);
    const actions=document.createElement('div');actions.className='property-actions';
    if(m.type==='auxiliary'){const owner=document.createElement('button');owner.className='ui-btn';owner.textContent='Hauptschütz bearbeiten';owner.onclick=()=>ctx.services.editor.selectModule(m.parent,true);actions.append(owner);}
    if(ctx.services.editor.isProtection(m)){const test=document.createElement('button');test.className='ui-btn';test.dataset.protection='test';test.title='Mechanische Testauslösung';test.textContent='TEST';test.disabled=!!m.protectionTripped;test.onclick=()=>ctx.services.simulation.operateProtection(m,'test');const reset=document.createElement('button');reset.className='ui-btn';reset.dataset.protection='reset';reset.title='Nach der Auslösung wieder einschaltbereit';reset.textContent='Rückstellen';reset.hidden=!m.protectionTripped;reset.onclick=()=>ctx.services.simulation.operateProtection(m,'reset');actions.append(test,reset);}
    const copy=document.createElement('button');copy.className='ui-btn';copy.textContent='Duplizieren';copy.title='Duplizieren · Strg+D / Cmd+D';copy.disabled=m.type==='auxiliary';copy.onclick=()=>ctx.services.projekt.pastePackage(ctx.services.projekt.copyPackage(m));
    const remove=document.createElement('button');remove.className='ui-btn';remove.textContent='Bauteil löschen';remove.onclick=()=>ctx.services.editor.removeModule(m);actions.append(copy,remove);ctx.dom.$('#devMenu').append(actions);
    ctx.services.editor.renderWires();
  }
  function selectWire(w){ctx.state.selectedWire=w;ctx.state.wireScope='selection';ctx.services.editor.paintMeters(ctx.state.lastSnap);ctx.state.selectedId=null;ctx.state.selectedIds.clear();ctx.state.modules.forEach(m=>m.el.classList.remove('selected'));ctx.services.editor.renderComponents();ctx.services.editor.draw();ctx.dom.$('.workspace').classList.remove('hide-inspector');ctx.services.editor.inspectorLabel();const hint=ctx.dom.$('#hint');if(hint)hint.textContent='Leitung gewählt · Klick auf die freie Fläche oder Esc hebt die Auswahl auf.';}
  function clearPick(){if(!ctx.state.selectedId&&!ctx.state.selectedWire)return;ctx.state.selectedId=null;ctx.state.selectedIds.clear();ctx.state.selectedWire=null;ctx.state.modules.forEach(m=>m.el.classList.remove('selected'));ctx.services.editor.renderComponents();ctx.services.editor.draw();ctx.services.simulation.paintHint();}
  function clickWire(w){
    const now=performance.now();
    if(ctx.state.wireClickTarget===w&&now-ctx.state.wireClickAt<500){ctx.state.wireClickTarget=null;ctx.state.wireClickAt=0;ctx.services.editor.removeWire(ctx.state.wires.indexOf(w));return;}
    ctx.state.wireClickTarget=w;ctx.state.wireClickAt=now;ctx.services.editor.selectWire(w);
  }
  function sameCable(a,b){return String(a||'').toLowerCase()===String(b||'').toLowerCase();}
  function cablePaint(c){return c==='pe'?'repeating-linear-gradient(45deg,#e6c21a 0 3px,#2f7d38 3px 6px)':c;}
  function cableStroke(c){return c==='pe'?'url(#pe-stripe)':c;}
  function peStripe(){
    const defs=document.createElementNS(ctx.config.NS,'defs');
    const pat=document.createElementNS(ctx.config.NS,'pattern');
    pat.id='pe-stripe';pat.setAttribute('patternUnits','userSpaceOnUse');pat.setAttribute('width','8');pat.setAttribute('height','8');pat.setAttribute('patternTransform','rotate(45)');
    const yellow=document.createElementNS(ctx.config.NS,'rect');yellow.setAttribute('width','4');yellow.setAttribute('height','8');yellow.setAttribute('fill','#e6c21a');
    const green=document.createElementNS(ctx.config.NS,'rect');green.setAttribute('x','4');green.setAttribute('width','4');green.setAttribute('height','8');green.setAttribute('fill','#2f7d38');
    pat.append(yellow,green);defs.append(pat);return defs;
  }
  function syncCableButtons(root,current){
    root.querySelectorAll('[data-color]').forEach(b=>b.setAttribute('aria-pressed',String(ctx.services.editor.sameCable(b.dataset.color,current))));
    const own=root.querySelector('.own');if(!own)return;
    const known=ctx.config.wirePalette.some(e=>ctx.services.editor.sameCable(e[1],current));
    own.classList.toggle('picked',!known);own.style.background=known?'':current;
  }
  function mountCableButtons(root,current,onPick){
    root.replaceChildren();
    ctx.config.wirePalette.forEach(([name,value])=>{
      const b=document.createElement('button');b.type='button';b.className='color'+(value==='pe'?' pe':'');b.dataset.color=value;b.style.setProperty('--color',value==='pe'?'#2f7d38':value);b.setAttribute('aria-label',name);b.title=name;b.onclick=()=>onPick(value);root.append(b);
    });
    const own=document.createElement('label');own.className='color own';own.title='Eigene Farbe';
    const input=document.createElement('input');input.type='color';input.setAttribute('aria-label','Eigene Aderfarbe');
    input.value=/^#[0-9a-f]{6}$/i.test(current)&&!ctx.config.wirePalette.some(e=>ctx.services.editor.sameCable(e[1],current))?current:ctx.state.customWire;
    input.addEventListener('change',()=>{ctx.state.customWire=input.value;onPick(input.value);});
    own.append(input);root.append(own);ctx.services.editor.syncCableButtons(root,current);
  }
  function renderWireDetails(){
    ctx.services.editor.closeDevMenu();const w=ctx.state.selectedWire;ctx.dom.$('#detailTitle').textContent='Leitung';const kind=ctx.dom.$('#detailKind');if(kind){kind.hidden=false;kind.textContent='Ader';}ctx.services.editor.paintDetailPreview(null);ctx.dom.$('#addAux').hidden=true;ctx.dom.$('#couplingNote').hidden=true;
    const root=ctx.dom.$('#componentDetails');root.replaceChildren();
    const ends=document.createElement('p');ends.className='wire-endpoints';ends.textContent=w.a+' ↔ '+w.b;root.append(ends);
    const field=document.createElement('div');field.className='color-field';const cap=document.createElement('span');cap.textContent='Aderfarbe';const box=document.createElement('div');box.className='wire-colors';field.append(cap,box);root.append(field);
    ctx.services.editor.mountCableButtons(box,w.color,value=>{if(ctx.services.editor.sameCable(w.color,value))return;ctx.services.projekt.remember();w.color=value;ctx.services.editor.mark();ctx.services.editor.draw();ctx.services.editor.renderWires();ctx.services.editor.renderWireDetails();ctx.services.projekt.schedulePersist();});
    const remove=document.createElement('button');remove.className='ui-btn';remove.textContent='Leitung löschen';remove.id='deleteSelectedWire';remove.onclick=()=>ctx.services.editor.removeWire(ctx.state.wires.indexOf(w));root.append(remove);
  }
  function wireMeasure(w){
    const known=ctx.config.wirePalette.find(e=>ctx.services.editor.sameCable(e[1],w.color));
    const color=known?known[0].split(' · ')[0]:'Eigene';
    if(!ctx.state.powered||!ctx.state.lastSnap)return color;
    const terms=ctx.state.lastSnap.terminals||{},va=Number(terms[w.a]),vb=Number(terms[w.b]),bits=[color];
    if(Number.isFinite(va)&&Number.isFinite(vb))bits.push(Math.abs(va-vb).toLocaleString('de-DE',{minimumFractionDigits:1,maximumFractionDigits:1})+' V');
    const i=Number((ctx.state.lastSnap.wireCurrents||{})[w.a+'|'+w.b]);
    if(Number.isFinite(i)){const a=Math.abs(i);bits.push(a>=1?a.toLocaleString('de-DE',{minimumFractionDigits:2,maximumFractionDigits:2})+' A':Math.round(a*1000).toLocaleString('de-DE')+' mA');}
    return bits.join(' · ');
  }
  function renderWires(){
    ctx.dom.$('#clear').disabled=!ctx.state.wires.length;
    const sel=ctx.dom.$('#wiresSelection'),allBtn=ctx.dom.$('#wiresAll');
    if(sel)sel.setAttribute('aria-pressed',String(ctx.state.wireScope!=='all'));
    if(allBtn)allBtn.setAttribute('aria-pressed',String(ctx.state.wireScope==='all'));
    const list=ctx.dom.$('#wireList');if(!list)return;list.replaceChildren();
    const selected=ctx.state.modules.find(m=>m.id===ctx.state.selectedId),group=ctx.services.editor.selection().filter(m=>m.type!=='note');
    const electrical=group.length>0;
    ctx.dom.$('#connectionsSection').hidden=ctx.state.selectedIds.size>0&&!electrical;
    const rows=ctx.state.wires.map((w,i)=>({w,i})).filter(({w})=>ctx.state.wireScope==='all'||w===ctx.state.selectedWire||group.some(m=>w.a.startsWith(m.id+'.')||w.b.startsWith(m.id+'.')));
    const context=ctx.state.wireScope==='all'?'all':ctx.state.selectedWire?'wire:'+ctx.state.selectedWire.a+'|'+ctx.state.selectedWire.b:electrical?'module:'+group.map(m=>m.id).sort().join(','):'none';
    ctx.dom.$('#wireHeading').textContent=ctx.state.wireScope==='all'?'Alle Leitungen':ctx.state.selectedWire?'Ausgewählte Leitung':electrical?(group.length>1?'Leitungen an der Auswahl':'Leitungen an '+ctx.services.editor.shortName(selected)):'Verbindungen';
    ctx.dom.$('#count').textContent=context==='none'?ctx.state.wires.length:rows.length;
    sel.disabled=!electrical&&!ctx.state.selectedWire;allBtn.disabled=!ctx.state.wires.length;
    if(context!==ctx.state.wirePanelContext||(!ctx.state.wirePanelHadRows&&rows.length>0)){
      ctx.state.wirePanelContext=context;
      const open=rows.length>0&&context!=='none';
      ctx.dom.$('#wiresToggle').setAttribute('aria-expanded',String(open));ctx.dom.$('#wirePanel').hidden=!open;
    }
    ctx.state.wirePanelHadRows=rows.length>0;
    if(!ctx.state.wires.length){list.innerHTML='<p class="empty">Noch keine Leitungen.</p>';return;}
    if(!rows.length){list.innerHTML=electrical?'<p class="empty">Keine Leitung an diesem Bauteil.</p>':'<p class="empty">Bauteil auswählen oder „Alle Leitungen“ öffnen.</p>';return;}
    rows.forEach(({w,i})=>{
      const row=document.createElement('div');row.className='wire-item';row.style.setProperty('--cable',ctx.services.editor.cablePaint(w.color));
      row.innerHTML=`<i></i><span><b>${w.a} ↔ ${w.b}</b><small>${ctx.services.editor.wireMeasure(w)}</small></span><button aria-label="Leitung ${w.a} zu ${w.b} entfernen">×</button>`;
      row.querySelector('button').onclick=()=>ctx.services.editor.removeWire(i);list.appendChild(row);
      const pick=document.createElement('button');pick.className='wire-select';pick.textContent=w.a+' ↔ '+w.b;pick.setAttribute('aria-pressed',String(w===ctx.state.selectedWire));pick.onclick=()=>ctx.services.editor.selectWire(w);row.querySelector('b').replaceWith(pick);
    });
  }
  function badgeSwatchHtml(m){
    const picked=String(m.labelColor||'').toLowerCase();
    const known=ctx.config.badgePalette.some(([,hex])=>hex===picked);
    const rest=ctx.config.badgePalette.map(([name,hex])=>'<button type="button" class="swatch" data-badge="'+hex+'" aria-pressed="'+String(picked===hex)+'" aria-label="'+name+'" title="'+name+'" style="--swatch:'+hex+'"></button>').join('');
    const custom=known||!picked||picked==='#394b41'?ctx.services.editor.badgeTone(m):picked;
    return '<div class="color-field"><span>Beschriftungsfarbe</span><div class="color-swatches badge-swatches">'+rest+'<label class="swatch swatch-custom" title="Eigene Farbe" aria-pressed="'+String(!!picked&&!known&&picked!=='#394b41')+'"><input type="color" id="devBadgeCustom" value="'+custom+'" aria-label="Eigene Farbe"></label></div></div>';
  }
  function applyBadgeColor(m,root,next,keepHistory){
    if((m.labelColor||'')===next)return;
    if(!keepHistory)ctx.services.projekt.remember();
    if(next)m.labelColor=next;else delete m.labelColor;
    const picked=String(m.labelColor||'').toLowerCase();
    const known=ctx.config.badgePalette.some(([,hex])=>hex===picked);
    root.querySelectorAll('[data-badge]').forEach(x=>x.setAttribute('aria-pressed',String(x.dataset.badge===picked)));
    const custom=root.querySelector('.swatch-custom');
    if(custom)custom.setAttribute('aria-pressed',String(!!picked&&!known));
    ctx.services.editor.paintDeviceFace(m);
    const aux=ctx.state.modules.find(x=>x.parent===m.id);
    if(aux)ctx.services.editor.paintDeviceFace(aux);
    ctx.services.projekt.schedulePersist();
  }
  function bindBadgeSwatches(m,root){
    root.querySelectorAll('[data-badge]').forEach(b=>b.onclick=()=>ctx.services.editor.applyBadgeColor(m,root,b.dataset.badge));
    const input=root.querySelector('#devBadgeCustom');
    if(!input)return;
    input.addEventListener('pointerdown',()=>{if(!input.dataset.armed){ctx.services.projekt.remember();input.dataset.armed='1';}});
    input.addEventListener('input',()=>ctx.services.editor.applyBadgeColor(m,root,input.value.toLowerCase(),true));
    input.addEventListener('change',()=>{delete input.dataset.armed;ctx.services.projekt.schedulePersist();});
  }
  function colorField(field,label,palette,current){
    return '<div class="color-field"><span>'+label+'</span><div class="color-swatches" data-field="'+field+'">'+Object.entries(palette).map(([key,entry])=>'<button type="button" class="swatch" data-value="'+key+'" aria-pressed="'+String(key===current)+'" aria-label="'+entry[0]+'" title="'+entry[0]+'" style="--swatch:'+entry[1]+'"></button>').join('')+'</div></div>';
  }
  function bindColorSwatches(m,root){
    root.querySelectorAll('.color-swatches').forEach(group=>{
      group.querySelectorAll('.swatch').forEach(b=>{
        b.onclick=()=>{
          const field=group.dataset.field,key=b.dataset.value,palette=field==='buttonColor'?ctx.config.buttonColors:ctx.config.signalColors;
          if(!palette[key]||m[field]===key)return;
          ctx.services.projekt.remember();
          m[field]=key;
          group.querySelectorAll('.swatch').forEach(x=>x.setAttribute('aria-pressed',String(x===b)));
          ctx.services.editor.paintDeviceFace(m);
          ctx.services.editor.renderComponents();
          ctx.services.projekt.schedulePersist();
          ctx.services.editor.say(m.id+': '+(field==='buttonColor'?'Tasterfarbe ':'Leuchtfarbe ')+palette[key][0]);
        };
      });
    });
  }
  function closeDevMenu(){
    const menu=ctx.dom.$('#devMenu');
    if(menu)menu.hidden=true;
    ctx.state.editTarget=null;
  }
  function openSettings(m,x,y,focus=true){
    ctx.services.editor.defaultNom(m);
    ctx.state.editTarget=m;
    const menu=ctx.dom.$('#devMenu'), note=ctx.dom.$('#devNote'), fields=ctx.dom.$('#devFields'), apply=ctx.dom.$('#devApply');
    ctx.dom.$('#componentDetails').after(menu);
    menu.removeAttribute('role');
    ctx.dom.$('#devTitle').textContent='Bearbeiten';
    note.hidden=false;
    if(m.type==='potentiometer'){
      note.textContent='1 und 3 sind die Enden des Widerstands; 2 ist der Schleifer. 0 % liegt bei 1, 100 % bei 3. Der Drehregler verändert den echten Spannungsteiler, auch unter Last.';
      fields.innerHTML='<label>Gesamtwiderstand (Ω)<input id="devResistance" type="number" min="100" max="1000000" step="100" value="'+m.resistance+'"></label><label>Schleifer (%)<input id="devPotPosition" type="number" min="0" max="100" step="1" value="'+Math.round(m.potPosition*100)+'"></label>';
    }else if(m.type==='motion'){
      note.textContent='Maus im Erfassungsbereich bewegen oder „Bewegung testen“ drücken. Jede Bewegung startet den Nachlauf neu. OUT / L′ schaltet die Versorgung L durch und ist nicht potenzialfrei. Pause friert die Zeit ein. Das Lehrmodell bildet keine Helligkeitsschwelle ab.';
      fields.innerHTML='<label>Versorgung<select id="devMotionVoltage"><option value="230">230 V AC · Gebäude</option><option value="24">24 V DC · Steuerung</option></select></label><label>Nachlaufzeit (s)<input id="devMotionDelay" type="number" min="1" max="600" step="1" value="'+m.motionDelay+'"></label>';
      ctx.dom.$('#devMotionVoltage').value=String(m.nomV);
    }else if(m.type==='note'){
      note.textContent='Freier Text auf der Montageplatte. Doppelklick öffnet diese Einstellungen. Zeilenumbrüche bleiben erhalten; keine elektrischen Anschlüsse.';
      fields.innerHTML='<label>Text<textarea id="devNoteText" rows="5" maxlength="1000"></textarea></label><label>Darstellung<select id="devNoteStyle"><option value="card">Notizzettel</option><option value="label">Beschriftungsstreifen</option><option value="frame">Markierungsrahmen</option></select></label><label>Schriftgröße<input id="devNoteSize" type="number" min="12" max="32" value="'+m.noteSize+'"></label><label>Breite<input id="devNoteWidth" type="number" min="120" max="1600" step="10" value="'+m.noteWidth+'"></label><label id="devNoteHeightField">Höhe<input id="devNoteHeight" type="number" min="48" max="1600" step="10" value="'+(m.noteHeight||Math.round(m.el.querySelector('.face').offsetHeight))+'"></label><label id="devNoteTitleToneField">Titelhintergrund<select id="devNoteTitleTone"><option value="transparent">Transparent</option><option value="white">Weiß</option><option value="cream">Creme</option></select></label><label id="devNoteToneField">Papierfarbe<select id="devNoteTone">'+Object.entries(ctx.config.noteTones).map(([key,[name]])=>'<option value="'+key+'">'+name+'</option>').join('')+'</select></label>';
      ctx.dom.$('#devNoteText').value=m.noteText;ctx.dom.$('#devNoteStyle').value=m.noteStyle;ctx.dom.$('#devNoteTone').value=m.noteTone;ctx.dom.$('#devNoteTitleTone').value=m.noteTitleTone;
      const syncNoteStyleFields=()=>{const style=ctx.dom.$('#devNoteStyle').value;ctx.dom.$('#devNoteToneField').hidden=style!=='card';ctx.dom.$('#devNoteHeightField').hidden=style==='card';ctx.dom.$('#devNoteTitleToneField').hidden=style!=='frame';};syncNoteStyleFields();ctx.dom.$('#devNoteStyle').addEventListener('change',syncNoteStyleFields);
    }else if(ctx.services.editor.isProtection(m)){
      note.textContent=m.type==='motorprotection'?'Drei gekoppelte Pole. Einstellstrom nach Motornennstrom; magnetische Schwelle 13 × Iset (Effektivstrom). Thermisches CLASS-10-Lehrmodell: bei 7,2 × Iset aus kaltem Zustand nach etwa 5 Simulationssekunden. Hilfskontakte folgen EIN/AUS. Keine geprüfte Gerätekennlinie; Rückstellen lässt den Schalter AUS.':'Leitungsschutzschalter mit gekoppelten Polen. Magnetische Schwellen im Lehrmodell: B 4 × In, C 7,5 × In, D 15 × In (Effektivstrom). Die thermische Auslösung folgt einer angenäherten B/C/D-Zeit-Strom-Kennlinie. kA ist ein Nenndatum, keine Lichtbogen- oder Ausschaltvermögenssimulation.';
      fields.innerHTML=m.type==='motorprotection'?'<label>Einstellstrom (A)<input id="devCurrent" type="number" min="0.1" max="100" step="0.1" value="'+m.ratedCurrent+'"></label>':'<label>Nennstrom (A)<select id="devCurrent">'+ctx.config.breakerRatings.map(n=>'<option value="'+n+'">'+n+' A</option>').join('')+'</select></label><label>Charakteristik<select id="devCurve"><option>B</option><option>C</option><option>D</option></select></label><label>Polzahl<select id="devProtectionPoles"><option value="1">1-polig</option><option value="3">3-polig</option></select></label><label>Ausschaltvermögen (Nenndatum)<select id="devCapacity"><option value="6">6 kA</option><option value="10">10 kA</option><option value="15">15 kA</option></select></label>';
      ctx.dom.$('#devCurrent').value=m.ratedCurrent;
      if(m.type==='breaker'){ctx.dom.$('#devCurve').value=m.characteristic;ctx.dom.$('#devProtectionPoles').value=m.poles;ctx.dom.$('#devCapacity').value=m.breakingCapacity;}
      apply.hidden=false;
    }else if(m.type==='lineardrive'){
      note.textContent='Vereinfachtes 24-V-Lehrmodell: B1 links, B2 Mitte, B3 rechts. 11–12 ist in Ruhe geschlossen, 11–14 bei Betätigung. B6 und B7 sind Rechtecksignale des Inkrementalgebers. Nach rechts kommt die Flanke von B6 vor B7, nach links die Flanke von B7 vor B6. 20 Takte über den ganzen Weg. Die 24-V-Buchsen speisen nur den Geber. Die Fahrt folgt CircuitJS; am Anschlag stoppt der Schlitten, der Motor bleibt bestromt.';
      fields.innerHTML='';apply.hidden=true;
    }else if(m.type==='psu'){
      note.textContent='Klemmen bleiben +24 / 0 V. CircuitJS nimmt den eingestellten Wert. Spule und Motor müssen zur Spannung passen.';
      fields.innerHTML='<label>Spannung (V)<input id="devVolt" type="number" min="1" max="400" step="0.1" value="'+m.voltage+'"></label><div class="presets"><button type="button" class="ui-btn" data-v="12">12 V</button><button type="button" class="ui-btn" data-v="24">24 V</button><button type="button" class="ui-btn" data-v="48">48 V</button></div>';
      fields.querySelectorAll('[data-v]').forEach(b=>b.onclick=()=>{ctx.dom.$('#devVolt').value=b.dataset.v;});
      apply.hidden=false;
    }else if(m.type==='mains'){
      note.textContent='Außenleiterspannung U_LL. Bei 400 V 3~ ist U_LN = 230 V gegen N. N und PE sind an der Einspeisung intern verbunden (TN). Frequenz 50 Hz.';
      fields.innerHTML='<label>U_LL (V)<input id="devVolt" type="number" min="1" max="690" step="1" value="'+m.voltage+'"></label><div class="presets"><button type="button" class="ui-btn" data-v="400">400 V</button><button type="button" class="ui-btn" data-v="230">230 V</button></div>';
      fields.querySelectorAll('[data-v]').forEach(b=>b.onclick=()=>{ctx.dom.$('#devVolt').value=b.dataset.v;});
      apply.hidden=false;
    }else if(m.type==='timer'){
      note.textContent='A1/A2 ist die Spule. 15 ist der gemeinsame Anschluss des Wechslers, 16 der Öffner, 18 der Schließer. Beide Kontakte sind potenzialfrei und folgen der Zeitfunktion. Blinkbetrieb beginnt mit EIN. Zeiten in Sekunden, Pause friert den Ablauf ein. Eine geänderte Funktion oder Zeit startet den Ablauf neu.';
      fields.innerHTML='<label>Funktion<select id="devTimerMode">'+ctx.config.timerModes.map((text,i)=>'<option value="'+i+'">'+text+'</option>').join('')+'</select></label><label><span id="timerFirstLabel">'+(m.timerMode===3?'EIN-Zeit (s)':m.timerMode===1?'Ausschaltverzögerung (s)':'Einschaltverzögerung (s)')+'</span><input id="devDelayOn" type="number" min="0.01" max="3600" step="0.1" value="'+m.delayOn+'"></label><label id="timerSecondField" '+(m.timerMode<2?'hidden':'')+'><span id="timerSecondLabel">'+(m.timerMode===3?'AUS-Zeit (s)':'Rückfallverzögerung (s)')+'</span><input id="devDelayOff" type="number" min="0.01" max="3600" step="0.1" value="'+m.delayOff+'"></label><label>Spulenspannung (V)<input id="devNomV" type="number" min="1" value="'+m.nomV+'"></label><label>Spulenleistung (W)<input id="devNomP" type="number" min="0.1" step="0.1" value="'+m.nomP+'"></label>';
      ctx.dom.$('#devTimerMode').value=m.timerMode;
      ctx.dom.$('#devTimerMode').addEventListener('change',()=>{const mode=Number(ctx.dom.$('#devTimerMode').value);ctx.dom.$('#timerFirstLabel').textContent=mode===3?'EIN-Zeit (s)':mode===1?'Ausschaltverzögerung (s)':'Einschaltverzögerung (s)';ctx.dom.$('#timerSecondField').hidden=mode<2;ctx.dom.$('#timerSecondLabel').textContent=mode===3?'AUS-Zeit (s)':'Rückfallverzögerung (s)';});
    }else if(m.type==='relay'){
      note.textContent='Eigenständige Spule A1/A2. Immer vier Hilfskontakte. Jeder ist Schließer NO oder Öffner NC, Nummer 1–9 nur einmal (1 NO = 13–14, 2 NC = 21–22).';
      fields.innerHTML='<label>Spulenspannung (V)<input id="devNomV" type="number" min="1" value="'+m.nomV+'"></label><label>Spulenleistung (VA)<input id="devNomP" type="number" min="0.1" step="0.1" value="'+m.nomP+'"></label><div class="relay-settings">'+Array.from({length:4},(_,i)=>{const c=m.relayContacts[i];return '<div class="relay-setting"><label>Kontakt '+(i+1)+'<select data-relay-kind><option value="NO">Schließer NO</option><option value="NC">Öffner NC</option></select></label><label>Nummer<input data-relay-number type="number" min="1" max="9" value="'+c.number+'"></label></div>';}).join('')+'</div>';
      fields.querySelectorAll('[data-relay-kind]').forEach((el,i)=>el.value=m.relayContacts[i].kind);apply.hidden=false;
    }else if(m.type==='contactor'){
      note.textContent='Kennzeichnung Q oder K. Hilfskontakte folgen automatisch. Spulennennwerte; Leistungskontakte bleiben unverändert.';
      fields.innerHTML='<label>Spulenspannung (V)<input id="devNomV" type="number" min="1" max="400" step="0.1" value="'+m.nomV+'"></label><label>Spulenleistung (VA)<input id="devNomP" type="number" min="0.1" max="200" step="0.1" value="'+m.nomP+'"></label>';
      apply.hidden=false;
    }else if(m.type==='acmotor'){
      note.textContent='400 V 3~. Stern: U2, V2 und W2 brücken, U1 V1 W1 an L1 L2 L3. Dreieck: U1–W2, V1–U2, W1–V2. PE ist nur das Gehäuse. Nennleistung skaliert den Leiterstrom, die Spannung bleibt 400 V.';
      fields.innerHTML='<label>Nennleistung (kW)<input id="devKw" type="number" min="0.12" max="15" step="0.01" value="'+m.ratedKw+'"></label><label>Wirkungsgrad (%)<input id="devEta" type="number" min="50" max="98" step="1" value="'+Math.round(m.eta*100)+'"></label>';
      apply.hidden=false;
    }else if(m.type==='motor'||ctx.services.editor.isLight(m)){
      note.textContent=ctx.services.editor.isLight(m)?'Nenndaten der Leuchte. Sie leuchtet bei geschlossenem Stromkreis; die Versorgung berechnet CircuitJS.':'Nenndaten des Gleichstrommotors.';
      fields.innerHTML='<label>Nennspannung (V)<input id="devNomV" type="number" min="1" max="400" step="0.1" value="'+m.nomV+'"></label><label>Nennleistung (W)<input id="devNomP" type="number" min="1" max="2000" step="1" value="'+m.nomP+'"></label>';
      if(m.type==='pilot')fields.innerHTML+=ctx.services.editor.colorField('signalColor','Leuchtfarbe',ctx.config.signalColors,m.signalColor);
      apply.hidden=false;
    }else if(ctx.services.editor.isCombinedTaster(m)){
      note.textContent='Ein Druck betätigt Schließer 13–14 und Öffner 21–22 gleichzeitig. Rastend hält den Taster bis zum nächsten Druck betätigt.';
      fields.innerHTML='<label class="latching-field">Rastend wie ein Schalter<input id="devLatchMode" type="checkbox"'+(m.latching?' checked':'')+'></label>'+ctx.services.editor.colorField('buttonColor','Tasterfarbe',ctx.config.buttonColors,m.buttonColor)+ctx.services.editor.colorField('signalColor','Meldeleuchte',ctx.config.signalColors,m.signalColor);
      apply.hidden=true;
    }else if(m.type==='junction'){
      note.textContent='Leeres Gehäuse ohne interne Verbindungen. Verbindungsklemmen separat einsetzen und frei darin platzieren. Zum Verschieben den Rand oder den Bauteilkopf ziehen; die Fläche innen bleibt für Bauteile und Leitungen zugänglich. Die Griffe ändern nur die Gehäusegröße.';
      fields.innerHTML='<label>Breite<input id="devBoxWidth" type="number" min="240" max="1600" value="'+m.boxWidth+'"></label><label>Höhe<input id="devBoxHeight" type="number" min="180" max="1600" value="'+m.boxHeight+'"></label>';apply.hidden=true;
    }else if(m.type==='connector'){
      note.textContent='Eine Verbindungsklemme verbindet alle ihre Anschlüsse miteinander. Separate Klemmen bleiben voneinander getrennt. L, N und PE werden durch deine Verdrahtung zugeordnet. Beim Verkleinern entfallen Leitungen und Messleitungen an den entfernten Anschlüssen.';
      fields.innerHTML='<label>Leiteranschlüsse<select id="devPorts">'+[2,3,5].map(n=>'<option value="'+n+'">'+n+' Leiter</option>').join('')+'</select></label>';ctx.dom.$('#devPorts').value=String(ctx.services.editor.connectorPorts(m));
      fields.insertAdjacentHTML('beforeend','<label class="latching-field">Beschriftungsbanner anzeigen<input id="devShowBanner" type="checkbox"'+(m.showBanner?' checked':'')+'></label>');apply.hidden=true;
    }else if(m.type==='terminal'){
      note.textContent='Oben und unten sind immer dieselbe Klemme. Anzahl und Verbindung gelten sofort. Rand und Strich markieren die Gruppe.';
      fields.innerHTML='<label>Anzahl<input id="devPoles" type="number" min="2" max="12" step="1" value="'+ctx.services.editor.terminalPoles(m)+'"></label><div class="presets"><button type="button" class="ui-btn" data-poles="4">4</button><button type="button" class="ui-btn" data-poles="6">6</button><button type="button" class="ui-btn" data-poles="10">10</button></div><label>Gleiche Verbindung<select id="devSpan"><option value="1">Jede Nummer für sich</option><option value="2">Paar · je 2</option><option value="4">Vierer · je 4</option></select></label>';
      ctx.dom.$('#devSpan').value=String(ctx.services.editor.terminalSpan(m));
      if(m.terminalVersion===2){
        note.textContent='Oben und unten derselben Nummer sind verbunden. Rote Steckbrücken verbinden Nachbarn; die leeren Brückenplätze anklicken. Farben sind Kennzeichnungen, N und PE selbst verdrahten.';
        ctx.dom.$('#devSpan').closest('label').hidden=true;
        ctx.services.editor.renderTerminalColors(m);
      }
      ctx.services.editor.markPolePreset();
      apply.hidden=true;
    }else if(m.type==='rail'){
      note.textContent='Drei getrennte Schienen +24 V, 0 V und PE. Die Breite gilt sofort. PE liegt nicht auf 0 V.';
      fields.innerHTML='<label>Schrauben je Schiene<select id="devScrews"><option value="5">5 · normal</option><option value="8">8 · breiter</option></select></label>';
      ctx.dom.$('#devScrews').value=String(ctx.services.editor.railScrews(m));
      apply.hidden=true;
    }else{
      note.textContent=m.type==='changeover'?'L ist der gemeinsame Kontakt und wird auf 1 oder 2 geschaltet. Zwei Wechselschalter steuern eine Leuchte von zwei Stellen. Die Wippe ist rastend; ihre Stellung allein bedeutet nicht EIN oder AUS.':m.type==='crossover'?'Zwischen zwei Wechselschaltern einsetzen. Die Wippe verbindet entweder 1–3 und 2–4 oder 1–4 und 2–3. Die gekreuzten Wege sind elektrisch getrennt.':m.type==='socket'?'Normale Adern ausschließlich an den unteren Anschlussklemmen L, N und PE verdrahten. Die beiden Stecklöcher und der Schutzkontakt vorne sind nur Messpunkte für die Messleitungen der Messgeräte. Die leere Steckdose verbraucht keinen Strom. Kontakt 1 ist im Modell L zugeordnet, Kontakt 2 N; echte Schuko-Stecker sind umkehrbar. Ein angeschlossener PE ersetzt keine Schutzprüfung.':m.type==='voltmeter'?'Rote Messleitung V und schwarze COM auf zwei Klemmen oder Steckdosen-Messpunkte ziehen. Die Leitungen messen nur, sie verbinden nicht. Im Betrieb: Spannungsdifferenz als Momentanwert (kein AC-Effektivwert). Im Aufbau: Durchgang über Leitungen und Kontakte, ohne Widerstandsprüfung von Verbrauchern.':m.type==='ammeter'?'Beide Messleitungen an die zwei Enden einer Ader ziehen. Angezeigt wird der Strom dieser Ader, ohne das Messgerät in den Stromkreis zu legen. Momentanwert, kein AC-Effektivwert.':m.type==='selector'?'Rastender Drehschalter mit Mittelstellung. A schließt 13–14, B schließt 23–24. In 0 sind beide Kontakte offen. Stellung direkt über A / 0 / B oder am Griff mit Pfeiltasten wählen.':m.type==='emergency'?'Drücken rastet den Pilztaster ein: Öffner 21–22 öffnet und Schließer 13–14 schließt. Entriegeln stellt beide Kontakte zurück. Reset und Spannungswechsel entriegeln den Not-Aus nicht.':m.type==='auxiliary'?'Hilfskontakte folgen dem Schütz. Keine eigenen elektrischen Einstellungen.':'Keine einstellbaren Werte.';
      fields.innerHTML='';
      apply.hidden=true;
    }
    if(['potentiometer','motion','note'].includes(m.type))fields.querySelectorAll('label').forEach(label=>label.classList.add('extension-field'));
    if(ctx.config.tagPrefixes[m.type]){
      fields.insertAdjacentHTML('afterbegin',ctx.services.editor.tagFieldHtml(m));
      apply.hidden=false;
    }
    fields.insertAdjacentHTML('beforeend','<label class="latching-field">Position sperren<input id="devPositionLocked" type="checkbox"'+(m.locked?' checked':'')+'></label>');
    ctx.services.editor.bindColorSwatches(m,fields);
    const description=document.createElement('label');description.className='description-field';description.classList.toggle('note-card-only',m.type==='note');description.textContent='Titel';const input=document.createElement('input');input.id='devDescription';input.maxLength=40;input.value=m.description||'';input.placeholder=ctx.services.editor.componentTitle(m);description.append(input);fields.append(description);
    const subtitleField=document.createElement('label');subtitleField.className='description-field';subtitleField.classList.toggle('note-card-only',m.type==='note');subtitleField.textContent='Untertitel';const subtitleInput=document.createElement('textarea');subtitleInput.id='devSubtitle';subtitleInput.value=ctx.services.editor.moduleSubtitle(m);subtitleInput.placeholder=ctx.services.editor.moduleCaption(m);subtitleField.append(subtitleInput);fields.append(subtitleField);
    const defaultSubtitle=document.createElement('button');defaultSubtitle.type='button';defaultSubtitle.className='ui-btn subtitle-default'+(m.type==='note'?' note-card-only':'');defaultSubtitle.textContent='Standardtext verwenden';defaultSubtitle.title='Automatisch erzeugten Untertitel wiederherstellen';defaultSubtitle.onclick=()=>{subtitleInput.value=ctx.services.editor.moduleCaption(m);ctx.services.editor.queueLive(true);};fields.append(defaultSubtitle);
    const titleLines=document.createElement('label');titleLines.className='title-lines-field'+(m.type==='note'?' note-card-only':'');titleLines.textContent='Max. Titelzeilen';const titleLinesSelect=document.createElement('select');titleLinesSelect.id='devTitleLines';titleLinesSelect.setAttribute('aria-label','Maximale Titelzeilen');titleLinesSelect.innerHTML='<option value="1">1 Zeile</option><option value="2">2 Zeilen</option><option value="3">3 Zeilen</option>';titleLinesSelect.value=String(m.titleLines||2);titleLines.append(titleLinesSelect);fields.append(titleLines);
    fields.insertAdjacentHTML('beforeend',ctx.services.editor.badgeSwatchHtml(m));
    const badgeField=fields.querySelector('.badge-swatches')?.closest('.color-field');if(badgeField&&m.type==='note')badgeField.classList.add('note-card-only');
    ctx.services.editor.bindBadgeSwatches(m,fields);
    if(m.type==='note'){
      const syncNoteHeaderFields=()=>fields.querySelectorAll('.note-card-only').forEach(field=>{field.hidden=ctx.dom.$('#devNoteStyle').value!=='card';});
      ctx.dom.$('#devNoteStyle').addEventListener('change',syncNoteHeaderFields);syncNoteHeaderFields();
    }
    if(m.type==='connector'){
      const bannerFields=[...fields.querySelectorAll('.description-field,.subtitle-default,.title-lines-field,.badge-swatches')].map(field=>field.matches('.badge-swatches')?field.closest('.color-field'):field).filter(Boolean);
      const syncBannerFields=()=>bannerFields.forEach(field=>{field.hidden=!ctx.dom.$('#devShowBanner').checked;});
      ctx.dom.$('#devShowBanner').addEventListener('change',syncBannerFields);syncBannerFields();
    }
    apply.hidden=true;
    ctx.services.editor.bindLiveFields();
    menu.hidden=false;
    if(focus){ctx.dom.$('.workspace').classList.remove('hide-inspector');ctx.services.editor.inspectorLabel();}
    const first=[...menu.querySelectorAll('input,select,textarea')].find(el=>el.type!=='hidden');
    if(first&&focus)first.focus();
  }
  function renderTerminalColors(m){
    const fields=ctx.dom.$('#devFields');fields.querySelector('.terminal-v2-colors')?.remove();
    const colors=document.createElement('fieldset');colors.className='terminal-v2-colors';colors.innerHTML='<legend>Klemmenfarben</legend>'+Array.from({length:ctx.services.editor.terminalPoles(m)},(_,i)=>'<label>'+ (i+1) +'<select aria-label="Farbe Klemme '+(i+1)+'" data-terminal-color="'+i+'"><option value="gray">Grau</option><option value="blue">Blau · N</option><option value="pe">Grün-Gelb · PE</option></select></label>').join('');
    fields.append(colors);colors.querySelectorAll('select').forEach(select=>{select.value=m.terminalColors[Number(select.dataset.terminalColor)];select.onchange=()=>{ctx.services.projekt.remember();m.terminalColors[Number(select.dataset.terminalColor)]=select.value;ctx.services.editor.paintDeviceFace(m);ctx.services.projekt.schedulePersist();};});
  }
  function markPolePreset(){
    const n=ctx.dom.$('#devPoles')?.value;
    document.querySelectorAll('#devFields [data-poles]').forEach(b=>b.setAttribute('aria-pressed',String(b.dataset.poles===String(n))));
  }
  function bindLiveFields(){
    const fields=ctx.dom.$('#devFields');
    if(!fields)return;
    fields.querySelectorAll('input,select,textarea').forEach(el=>{
      if(el.hasAttribute('data-terminal-color'))return;
      el.addEventListener('input',()=>ctx.services.editor.queueLive(false));
      el.addEventListener('change',()=>ctx.services.editor.queueLive(true));
    });
    fields.querySelectorAll('[data-v]').forEach(b=>b.onclick=()=>{const input=ctx.dom.$('#devVolt');if(input)input.value=b.dataset.v;ctx.services.editor.queueLive(true);});
    fields.querySelectorAll('[data-poles]').forEach(b=>b.onclick=()=>{const input=ctx.dom.$('#devPoles');if(input)input.value=b.dataset.poles;ctx.services.editor.queueLive(true);});
  }
  function queueLive(now){
    clearTimeout(ctx.state.liveTimer);
    if(now){ctx.services.editor.commitLive(true);return;}
    ctx.state.liveTimer=setTimeout(()=>ctx.services.editor.commitLive(false),280);
  }
  function commitLive(announce){ctx.services.editor.applySettings({live:true,announce});}
  function applySettings(opts){
    const live=!!(opts&&opts.live);
    const announce=!opts||opts.announce!==false;
    const m=ctx.state.editTarget;if(!m)return;
    const fail=msg=>{if(announce)ctx.services.editor.say(msg);};
    const tag=ctx.services.editor.readTag(m);
    if(tag&&!tag.ok){fail(tag.error);return;}
    const extra={locked:!!ctx.dom.$('#devPositionLocked')?.checked};
    if(m.type==='potentiometer'){
      extra.resistance=Number(ctx.dom.$('#devResistance').value);extra.potPosition=Number(ctx.dom.$('#devPotPosition').value)/100;
      if(!Number.isFinite(extra.resistance)||extra.resistance<100||extra.resistance>1000000||!Number.isFinite(extra.potPosition)||extra.potPosition<0||extra.potPosition>1){fail('Widerstand 100–1.000.000 Ω und Schleifer 0–100 % eingeben.');return;}
    }
    if(m.type==='motion'){
      extra.nomV=Number(ctx.dom.$('#devMotionVoltage').value);extra.motionDelay=Number(ctx.dom.$('#devMotionDelay').value);
      if(![24,230].includes(extra.nomV)||!Number.isFinite(extra.motionDelay)||extra.motionDelay<1||extra.motionDelay>600){fail('Nachlaufzeit zwischen 1 und 600 Sekunden eingeben.');return;}
    }
    if(m.type==='note'){
      Object.assign(extra,{noteText:ctx.dom.$('#devNoteText').value,noteStyle:ctx.dom.$('#devNoteStyle').value,noteTone:ctx.dom.$('#devNoteTone').value,noteTitleTone:ctx.dom.$('#devNoteTitleTone').value,noteSize:Number(ctx.dom.$('#devNoteSize').value),noteWidth:Number(ctx.dom.$('#devNoteWidth').value),noteHeight:Number(ctx.dom.$('#devNoteHeight').value)});
      if(!Number.isFinite(extra.noteSize)||extra.noteSize<12||extra.noteSize>32||!Number.isFinite(extra.noteWidth)||extra.noteWidth<120||extra.noteWidth>1600||extra.noteStyle!=='card'&&(!Number.isFinite(extra.noteHeight)||extra.noteHeight<48||extra.noteHeight>1600)){fail('Schriftgröße 12–32, Breite 120–1600 und Höhe 48–1600 eingeben.');return;}
    }
    if(m.type==='junction'){
      extra.boxWidth=Number(ctx.dom.$('#devBoxWidth').value);extra.boxHeight=Number(ctx.dom.$('#devBoxHeight').value);
      if(!Number.isFinite(extra.boxWidth)||extra.boxWidth<240||extra.boxWidth>1600||!Number.isFinite(extra.boxHeight)||extra.boxHeight<180||extra.boxHeight>1600){fail('Breite 240–1600 und Höhe 180–1600 eingeben.');return;}
    }
    if(m.type==='connector'){extra.showBanner=!!ctx.dom.$('#devShowBanner')?.checked;extra.ports=Number(ctx.dom.$('#devPorts').value);if(![2,3,5].includes(extra.ports)){fail('2, 3 oder 5 Leiteranschlüsse wählen.');return;}}
    const extraSame=Object.keys(extra).every(key=>m[key]===extra[key]);
    let timerSettings=null;
    if(m.type==='timer'){
      timerSettings={timerMode:Number(ctx.dom.$('#devTimerMode').value),delayOn:Number(ctx.dom.$('#devDelayOn').value),delayOff:Number(ctx.dom.$('#devDelayOff').value)};
      if(![0,1,2,3].includes(timerSettings.timerMode)||[timerSettings.delayOn,timerSettings.delayOff].some(t=>!Number.isFinite(t)||t<.01||t>3600)){fail('Zeiten zwischen 0,01 und 3600 Sekunden eingeben.');return;}
    }
    const timerSame=!timerSettings||Object.keys(timerSettings).every(key=>m[key]===timerSettings[key]);
    let protectionSettings=null;
    if(ctx.services.editor.isProtection(m)){
      const ratedCurrent=Number(ctx.dom.$('#devCurrent').value);
      if(!Number.isFinite(ratedCurrent)||ratedCurrent<.1||ratedCurrent>100||(m.type==='breaker'&&!ctx.config.breakerRatings.includes(ratedCurrent))){fail('Gültigen Nennstrom wählen (0,1–100 A beim Motorschutz).');return;}
      protectionSettings={ratedCurrent};
      if(m.type==='breaker')Object.assign(protectionSettings,{poles:Number(ctx.dom.$('#devProtectionPoles').value),characteristic:ctx.dom.$('#devCurve').value,breakingCapacity:Number(ctx.dom.$('#devCapacity').value)});
    }
    let nextContacts=null;
    if(m.type==='relay'){
      nextContacts=[...ctx.dom.$('#devFields').querySelectorAll('.relay-setting')].map((row,slot)=>({kind:row.querySelector('select').value,number:Number(row.querySelector('input').value),slot}));
      if(nextContacts.length!==4||nextContacts.some(c=>(c.kind!=='NO'&&c.kind!=='NC')||!Number.isInteger(c.number)||c.number<1||c.number>9)||new Set(nextContacts.map(c=>c.number)).size!==4){fail('Vier Kontakte, jeder Schließer oder Öffner, eindeutige Nummern von 1 bis 9.');return;}
    }
    let poles=null,span=null,screws=null;
    if(m.type==='terminal'){
      poles=Number(ctx.dom.$('#devPoles').value);
      span=Number(ctx.dom.$('#devSpan').value);
      if(!Number.isInteger(poles)||poles<2||poles>12){fail('Anzahl 2 bis 12.');return;}
      if(span!==2&&span!==4)span=1;
      ctx.services.editor.markPolePreset();
    }
    if(m.type==='rail')screws=Number(ctx.dom.$('#devScrews').value)===8?8:5;
    let voltage=null;
    if((m.type==='psu'||m.type==='mains')&&ctx.dom.$('#devVolt')){
      voltage=Number(ctx.dom.$('#devVolt').value);
      if(!Number.isFinite(voltage)||voltage<1){fail('Spannung muss mindestens 1 V sein.');return;}
    }
    let kw=null,eta=null;
    if(m.type==='acmotor'&&ctx.dom.$('#devKw')&&ctx.dom.$('#devEta')){
      kw=Number(ctx.dom.$('#devKw').value);const pct=Number(ctx.dom.$('#devEta').value);
      if(!Number.isFinite(kw)||kw<0.12||kw>15){fail('Leistung 0,12 bis 15 kW.');return;}
      if(!Number.isFinite(pct)||pct<50||pct>98){fail('Wirkungsgrad 50 bis 98 %.');return;}
      eta=pct/100;
    }
    let nv=null,np=null;
    if(ctx.dom.$('#devNomV')&&ctx.dom.$('#devNomP')&&(m.type==='timer'||m.type==='relay'||m.type==='contactor'||m.type==='motor'||m.type==='lineardrive'||ctx.services.editor.isLight(m))){
      nv=Number(ctx.dom.$('#devNomV').value);np=Number(ctx.dom.$('#devNomP').value);
      if(!Number.isFinite(nv)||nv<1||!Number.isFinite(np)||np<=0){fail('Nennwerte prüfen.');return;}
    }
    const description=ctx.dom.$('#devDescription')?ctx.dom.$('#devDescription').value.trim():(m.description||'');
    const subtitle=ctx.dom.$('#devSubtitle')?ctx.dom.$('#devSubtitle').value.trim():ctx.services.editor.moduleSubtitle(m);
    const subtitleValue=subtitle===ctx.services.editor.moduleCaption(m)?null:subtitle;
    const titleLines=Number(ctx.dom.$('#devTitleLines')?.value||m.titleLines||2);
    const latching=ctx.dom.$('#devLatchMode')?ctx.dom.$('#devLatchMode').checked:!!m.latching;
    const contactsSame=!nextContacts||nextContacts.every((c,i)=>m.relayContacts[i]&&m.relayContacts[i].kind===c.kind&&m.relayContacts[i].number===c.number);
    const protectionSame=!protectionSettings||(m.ratedCurrent===protectionSettings.ratedCurrent&&(m.type!=='breaker'||(m.poles===protectionSettings.poles&&m.characteristic===protectionSettings.characteristic&&m.breakingCapacity===protectionSettings.breakingCapacity)));
    if(extraSame&&(!tag||tag.id===m.id)&&description===(m.description||'')&&subtitleValue===m.subtitle&&titleLines===m.titleLines&&(!ctx.services.editor.isCombinedTaster(m)||latching===!!m.latching)&&timerSame&&contactsSame&&protectionSame&&(m.type!=='terminal'||(ctx.services.editor.terminalPoles(m)===poles&&ctx.services.editor.terminalSpan(m)===span))&&(m.type!=='rail'||ctx.services.editor.railScrews(m)===screws)&&(voltage===null||m.voltage===voltage)&&(kw===null||(m.ratedKw===kw&&m.eta===eta))&&(nv===null||(m.nomV===nv&&m.nomP===np)))return;
    const electricalChange=ctx.services.simulation.electricalModule(m)&&((tag&&tag.id!==m.id)||!timerSame||!contactsSame||!protectionSame||ctx.services.editor.isCombinedTaster(m)&&latching!==!!m.latching||m.type==='terminal'&&(ctx.services.editor.terminalPoles(m)!==poles||ctx.services.editor.terminalSpan(m)!==span)||m.type==='rail'&&ctx.services.editor.railScrews(m)!==screws||voltage!==null&&m.voltage!==voltage||kw!==null&&(m.ratedKw!==kw||m.eta!==eta)||nv!==null&&(m.nomV!==nv||m.nomP!==np)||Object.keys(extra).some(key=>key!=='locked'&&key!=='showBanner'&&key!=='potPosition'&&extra[key]!==m[key]));
    const commit=()=>{
    ctx.services.projekt.remember();
    ctx.services.editor.defaultNom(m);
    const terminalCountChanged=m.type==='terminal'&&ctx.services.editor.terminalPoles(m)!==poles;
    if(m.type==='terminal'&&(terminalCountChanged||ctx.services.editor.terminalSpan(m)!==span)){m.poles=poles;m.span=span;ctx.services.editor.dropLoose(m);ctx.state.lastImportedSig='';}
    if(m.type==='rail'&&ctx.services.editor.railScrews(m)!==screws){m.screws=screws;ctx.services.editor.dropLoose(m);ctx.state.lastImportedSig='';}
    if(voltage!==null&&m.voltage!==voltage){m.voltage=voltage;if(ctx.state.engine&&ctx.state.engine.setPsuVoltage)ctx.state.engine.setPsuVoltage(m.id,voltage);else ctx.state.lastImportedSig='';}
    if(kw!==null&&(m.ratedKw!==kw||m.eta!==eta)){m.ratedKw=kw;m.eta=eta;if(ctx.state.engine&&ctx.state.engine.setAcMotorRatings)ctx.state.engine.setAcMotorRatings(m.id,kw,eta);else ctx.state.lastImportedSig='';}
    if(nv!==null&&(m.nomV!==nv||m.nomP!==np)){m.nomV=nv;m.nomP=np;if(ctx.state.engine&&ctx.state.engine.setRatings)ctx.state.engine.setRatings(m.id,nv,np);else ctx.state.lastImportedSig='';}
    if(timerSettings&&!timerSame){Object.assign(m,timerSettings);ctx.state.lastImportedSig='';}
    if(protectionSettings&&!protectionSame){
      Object.assign(m,protectionSettings);ctx.state.lastImportedSig='';
      if(m.type==='breaker'&&m.poles===1){const gone=k=>k.startsWith(m.id+'.')&&['3','4','5','6'].includes(k.slice(m.id.length+1));ctx.state.wires=ctx.state.wires.filter(w=>!gone(w.a)&&!gone(w.b));}
    }
    if(nextContacts&&!contactsSame){
      const before=ctx.services.editor.relayPairs(m);m.relayContacts=nextContacts.map(({kind,number})=>({kind,number}));const after=ctx.services.editor.relayPairs(m),map=new Map();
      before.forEach(([a,b],i)=>{const j=nextContacts.findIndex(c=>c.slot===i);map.set(m.id+'.'+a,after[j]?m.id+'.'+after[j][0]:null);map.set(m.id+'.'+b,after[j]?m.id+'.'+after[j][1]:null);});
      ctx.state.modules.filter(ctx.services.editor.isMeter).forEach(x=>{if(map.has(x.probeRed))x.probeRed=map.get(x.probeRed);if(map.has(x.probeBlack))x.probeBlack=map.get(x.probeBlack);if(x.measuredWire){if(map.has(x.measuredWire.a))x.measuredWire.a=map.get(x.measuredWire.a);if(map.has(x.measuredWire.b))x.measuredWire.b=map.get(x.measuredWire.b);}});
      ctx.state.wires=ctx.state.wires.map(w=>({...w,a:map.has(w.a)?map.get(w.a):w.a,b:map.has(w.b)?map.get(w.b):w.b})).filter(w=>w.a&&w.b);
    }
    const renamed=tag&&tag.id!==m.id;
    if(renamed&&!ctx.services.editor.renameModule(m,tag.id))return;
    const connectorChanged=m.type==='connector'&&m.ports!==extra.ports;
    Object.assign(m,extra);
    if(connectorChanged){ctx.services.editor.dropLoose(m);ctx.state.lastImportedSig='';}
    if(m.type==='potentiometer'&&!extraSame&&ctx.state.engine?.setPotentiometer)ctx.state.engine.setPotentiometer(m.id,m.resistance,m.potPosition);
    m.description=description;
    m.subtitle=subtitleValue;
    m.titleLines=titleLines>=1&&titleLines<=3?titleLines:2;
    if(ctx.services.editor.isCombinedTaster(m)&&m.latching!==latching){m.latching=latching;if(!latching&&m.pressed){m.pressed=false;if(ctx.state.engine)ctx.state.engine.setPressed(m.id,false);ctx.services.editor.paintTaster(m);ctx.services.simulation.refreshLive();}}
    ctx.services.editor.remount(m);
    if(live&&terminalCountChanged&&m.terminalVersion===2)ctx.services.editor.renderTerminalColors(m);
    ctx.services.editor.mark();ctx.services.editor.draw();
    ctx.services.editor.paintDeviceFace(m);
    if(live){
      ctx.dom.$('#devTitle').textContent='Bearbeiten';
      ctx.dom.$('#detailTitle').textContent=ctx.services.editor.displayName(m);const kind=ctx.dom.$('#detailKind');if(kind){kind.hidden=false;kind.textContent=ctx.services.editor.partKind(m);}ctx.services.editor.paintDetailPreview(m);
      ctx.services.editor.renderComponentResults();
      ctx.services.editor.refreshInspectorRows(m);
    }else ctx.services.editor.closeDevMenu();
    if(renamed){
      ctx.services.simulation.update();
      ctx.services.projekt.schedulePersist();
      ctx.services.editor.rebuildAfterRename(m.id);
      return;
    }
    if(!live)ctx.services.editor.renderComponents();
    ctx.services.projekt.schedulePersist();
    ctx.services.simulation.scheduleSync();
    if(ctx.state.engineOk)ctx.services.simulation.applySnapshot();
    if(!live)ctx.services.editor.say(m.id+' übernommen.');
    };
    if(electricalChange&&!ctx.services.simulation.requestElectricalEdit(commit,()=>ctx.services.editor.renderComponents()))return;
    commit();
  }
  function position(m){m.el.style.left=m.x+'px';m.el.style.top=m.y+'px';}
  function move(m,x,y){if(m.locked)return;m.x=Math.max(24,Math.min(ctx.config.BOARD_W-24-m.el.offsetWidth,x));m.y=Math.max(61,Math.min(ctx.config.BOARD_H-36-m.el.offsetHeight,y));ctx.services.editor.position(m);}
  function term(key){return [...ctx.dom.board.querySelectorAll('.term')].find(t=>t.dataset.key===key);}
  function isMeasurementPoint(key){
    if(typeof key!=='string')return false;
    const dot=key.indexOf('.');
    return dot>0&&/^(L|N|PE)out$/.test(key.slice(dot+1))&&ctx.state.modules.some(m=>m.id===key.slice(0,dot)&&m.type==='socket');
  }
  function measurementPointHint(){ctx.services.editor.say('Vorne nur Messleitungen anschließen. Normale Adern an den unteren Klemmen L, N und PE verdrahten.');}
  function normalizeSocketWires(links){
    if(!links.some(w=>ctx.services.editor.isMeasurementPoint(w.a)||ctx.services.editor.isMeasurementPoint(w.b)))return links.map(w=>({...w}));
    const key=k=>ctx.services.editor.isMeasurementPoint(k)?k.slice(0,-3):k;
    const seen=new Set();
    return links.map(w=>({...w,a:key(w.a),b:key(w.b)})).filter(w=>{
      if(w.a===w.b||ctx.services.editor.internallyCommon(w.a,w.b))return false;
      const pair=w.a<w.b?w.a+'|'+w.b:w.b+'|'+w.a;
      if(seen.has(pair))return false;
      seen.add(pair);return true;
    });
  }
  function point(key){
    const t=ctx.services.editor.term(key);if(!t)return null;
    const r=t.getBoundingClientRect(),b=ctx.dom.board.getBoundingClientRect(),module=t.closest('.module');
    let y=(r.top+r.height/2-b.top)/ctx.state.scale;
    if(module.classList.contains('terminal-v2'))y=t.dataset.direction==='top'?(r.top-b.top)/ctx.state.scale+1:(r.bottom-b.top)/ctx.state.scale-1;
    return{x:(r.left+r.width/2-b.left)/ctx.state.scale,y,d:t.dataset.direction,id:module.dataset.id};
  }
  function localPoint(e){const b=ctx.dom.board.getBoundingClientRect();return{x:(e.clientX-b.left)/ctx.state.scale,y:(e.clientY-b.top)/ctx.state.scale,d:'free'};}
  function wireSource(){return ctx.state.gesture?.kind==='wire'?(ctx.state.gesture.moved?ctx.state.gesture.key:ctx.state.pending||ctx.state.gesture.key):ctx.state.pending;}
  function directlyConnected(a,b){return ctx.state.wires.some(w=>w.a===a&&w.b===b||w.a===b&&w.b===a);}
  function canConnect(a,b){return !!a&&!!b&&a!==b&&!ctx.services.editor.isMeasurementPoint(a)&&!ctx.services.editor.isMeasurementPoint(b)&&!!ctx.services.editor.term(a)&&!!ctx.services.editor.term(b)&&!ctx.services.editor.directlyConnected(a,b)&&!ctx.services.editor.internallyCommon(a,b);}
  function nearestTerm(cx,cy,source){
    if(!source)return null;
    let best=null,bestD=56;
    ctx.dom.board.querySelectorAll('.term').forEach(t=>{
      const key=t.dataset.key;
      if(key===source||!ctx.services.editor.canConnect(source,key))return;
      const r=t.getBoundingClientRect();
      const d=Math.hypot(cx-(r.left+r.width/2),cy-(r.top+r.height/2));
      if(d<=bestD){bestD=d;best=t;}
    });
    return best;
  }
  function updateWireTarget(e){
    const target=ctx.services.editor.nearestTerm(e.clientX,e.clientY,ctx.services.editor.wireSource());
    ctx.state.wireTarget=target?target.dataset.key:null;
    ctx.services.editor.mark();
  }
  function choose(key){if(ctx.services.editor.isMeasurementPoint(key)){ctx.services.editor.measurementPointHint();return;}ctx.state.wireTarget=null;if(!ctx.state.pending){ctx.state.pending=key;ctx.dom.$('#hint').textContent=key+' gewählt. Zweite Klemme anklicken.';}else if(ctx.state.pending===key){ctx.services.editor.cancel();return;}else{const created=ctx.services.editor.connect(ctx.state.pending,key);ctx.state.pending=null;ctx.dom.$('#hint').textContent=created?(ctx.services.simulation.liveMode()?'Verbindung erstellt. Angezoogene Schütze fallen ab.':'Verbindung erstellt · Weitere Klemmen verbinden.'):ctx.services.simulation.defaultHint();}ctx.services.editor.updateWirePrompt();ctx.services.editor.mark();ctx.services.editor.draw();}
  function leadDock(cx,cy,meter){
    const el=meter&&meter.el;if(!el)return false;
    const r=el.getBoundingClientRect(),pad=28;
    return cx>=r.left-pad&&cx<=r.right+pad&&cy>=r.top-pad&&cy<=r.bottom+pad;
  }
  function markLeadDock(on,meter){
    ctx.dom.board.querySelectorAll('.lead-return').forEach(el=>el.classList.remove('lead-return'));
    if(on&&meter&&meter.el)meter.el.classList.add('lead-return');
  }
  function nearestLeadTerm(cx,cy,meter,field){
    const other=field==='probeRed'?meter.probeBlack:meter.probeRed;
    let best=null,bestD=56;
    ctx.dom.board.querySelectorAll('.term').forEach(t=>{
      const key=t.dataset.key;
      if(key===other)return;
      const r=t.getBoundingClientRect();
      const d=Math.hypot(cx-(r.left+r.width/2),cy-(r.top+r.height/2));
      if(d<=bestD){bestD=d;best=t;}
    });
    return best;
  }
  function beginLead(m,field,e){
    ctx.services.editor.cancel();
    ctx.state.gesture={kind:'lead',m,field,x:e.clientX,y:e.clientY,moved:false,pointerId:e.pointerId,preview:ctx.services.editor.localPoint(e)};
    try{ctx.dom.board.setPointerCapture(e.pointerId);}catch(err){}
    ctx.dom.$('#hint').textContent='Messleitung auf eine Klemme oder einen Messpunkt ziehen. Am Messgerät ablegen oder mit Esc abbrechen.';
    ctx.services.editor.draw();
  }
  function finishLead(g,e){
    ctx.services.editor.markLeadDock(false);
    if(!g.moved){ctx.services.editor.say('Messleitung ziehen und auf eine Klemme legen oder am Messgerät ablegen.');ctx.services.editor.draw();return;}
    if(ctx.services.editor.leadDock(e.clientX,e.clientY,g.m)){
      const had=!!g.m[g.field];
      if(had){ctx.services.projekt.remember();ctx.services.editor.setLead(g.m,g.field,null);ctx.services.projekt.schedulePersist();ctx.services.editor.paintMeters(ctx.state.lastSnap);}
      ctx.services.editor.draw();
      ctx.services.editor.say(had?'Messleitung abgelegt.':'Messleitung liegt schon am Messgerät.');
      return;
    }
    const snap=ctx.services.editor.nearestLeadTerm(e.clientX,e.clientY,g.m,g.field);
    if(!snap){ctx.services.editor.say('Messleitung auf einer Klemme oder einem Messpunkt loslassen.');ctx.services.editor.draw();return;}
    ctx.services.projekt.remember();
    ctx.services.editor.setLead(g.m,g.field,snap.dataset.key);
    ctx.services.projekt.schedulePersist();ctx.services.editor.paintMeters(ctx.state.lastSnap);ctx.services.editor.draw();
    ctx.services.editor.say('Messleitung an '+snap.dataset.key);
  }
  function clampGroup(key){
    const dot=String(key).indexOf('.');
    if(dot<0)return '';
    const id=key.slice(0,dot),rest=key.slice(dot+1),m=ctx.state.modules.find(x=>x.id===id);
    if(!m)return '';
    if(m.type==='terminal'){const hit=rest.match(/^(\d+)[ou]$/);if(!hit)return '';const n=Number(hit[1]);if(n<1||n>ctx.services.editor.terminalPoles(m))return '';const span=ctx.services.editor.terminalSpan(m);return id+'#'+(m.terminalVersion===2?ctx.services.editor.terminalGroupIndex(m,n):Math.floor((n-1)/span));}
    if(m.type==='connector')return /^[1-5]$/.test(rest)&&Number(rest)<=ctx.services.editor.connectorPorts(m)?id+'#common':'';
    if(m.type==='socket'){const hit=rest.match(/^(L|N|PE)(out)?$/);return hit?id+'#'+hit[1]:'';}
    if(m.type==='rail'){const hit=rest.match(/^(\+24|0V|PE)\.\d+$/);return hit?id+'#'+hit[1]:'';}
    if(m.type==='mains'&&(rest==='N'||rest==='PE'))return id+'#NPE';
    return '';
  }
  function internallyCommon(a,b){const ga=ctx.services.editor.clampGroup(a),gb=ctx.services.editor.clampGroup(b);return !!ga&&ga===gb;}
  function connect(a,b){
    if(a===b||!ctx.services.editor.term(a)||!ctx.services.editor.term(b))return false;
    if(ctx.services.editor.isMeasurementPoint(a)||ctx.services.editor.isMeasurementPoint(b)){ctx.services.editor.measurementPointHint();return false;}
    if(ctx.services.editor.internallyCommon(a,b)){const id=a.slice(0,a.indexOf('.'));const m=ctx.state.modules.find(x=>x.id===id);ctx.services.editor.say(m&&m.type==='mains'?'N und PE sind an der Einspeisung intern verbunden.':m&&m.type==='rail'?'Diese Schrauben liegen auf derselben Schiene.':m?.type==='connector'?'Alle Anschlüsse dieser Verbindungsklemme sind intern verbunden.':m?.type==='socket'?'Dieser Steckkontakt ist bereits mit seiner Anschlussklemme verbunden.':'Oben und unten sind an dieser Klemme schon verbunden.');return false;}
    if(ctx.services.editor.directlyConnected(a,b)){ctx.services.editor.say('Diese Klemmen sind bereits verbunden.');return false;}
    if(!ctx.services.simulation.requestElectricalEdit(()=>ctx.services.editor.connect(a,b)))return false;
    ctx.services.projekt.remember();ctx.state.wires.push({a,b,color: ctx.state.color});ctx.services.simulation.update();ctx.services.simulation.noteWiringReset();return true;
  }
  function cancel(){ctx.dom.selectionRectangle.hidden=true;ctx.state.pending=null;ctx.state.gesture=null;ctx.state.wireTarget=null;ctx.state.viewDrag=null;ctx.services.editor.markLeadDock(false);ctx.dom.viewport.classList.remove('panning');ctx.dom.board.querySelectorAll('.dragging,.resizing').forEach(e=>e.classList.remove('dragging','resizing'));ctx.services.editor.updateWirePrompt();ctx.services.simulation.paintHint();ctx.services.editor.mark();ctx.services.editor.draw();}
  function updateWirePrompt(){const line=ctx.dom.$('.hint-line'),button=ctx.dom.$('#cancelWire');if(line)line.classList.toggle('wire-pending',!!ctx.state.pending);if(button)button.hidden=!ctx.state.pending;}
  function handleDocumentPointerMove(e) {
    if(!ctx.state.gesture){if(ctx.state.pending){ctx.services.editor.updateWireTarget(e);ctx.services.editor.draw(ctx.state.wireTarget?ctx.services.editor.point(ctx.state.wireTarget):ctx.services.editor.localPoint(e));}return;}if(e.pointerId!==ctx.state.gesture.pointerId)return;
    if(ctx.state.gesture.kind==='module'){ctx.services.editor.moveModuleDrag(e);}
    else if(ctx.state.gesture.kind==='selection'){ctx.services.editor.paintSelectionRectangle(e);}
    else if(ctx.state.gesture.kind==='resize-note'){
      const g=ctx.state.gesture,dx=(e.clientX-g.x)/ctx.state.scale,dy=(e.clientY-g.y)/ctx.state.scale,west=g.edge.includes('w'),east=g.edge.includes('e'),north=g.edge.includes('n'),south=g.edge.includes('s');
      const isBox=g.m.type==='junction',minWidth=isBox?240:120,minHeight=isBox?180:48,width=Math.max(minWidth,Math.min(1600,g.startWidth+(east?dx:west?-dx:0))),height=Math.max(minHeight,Math.min(1600,g.startHeight+(south?dy:north?-dy:0)));
      if(isBox){g.m.boxWidth=Math.round(width);g.m.boxHeight=Math.round(height);}else{g.m.noteWidth=Math.round(width);g.m.noteHeight=Math.round(height);}g.m.x=Math.max(24,Math.min(ctx.config.BOARD_W-24-width,west?g.startX+g.startWidth-width:g.startX));g.m.y=Math.max(61,Math.min(ctx.config.BOARD_H-36-height,north?g.startY+g.startHeight-height:g.startY));
      ctx.services.editor.position(g.m);ctx.services.editor.paintDeviceFace(g.m);if(ctx.dom.$('#devBoxWidth'))ctx.dom.$('#devBoxWidth').value=g.m.boxWidth;if(ctx.dom.$('#devBoxHeight'))ctx.dom.$('#devBoxHeight').value=g.m.boxHeight;if(ctx.dom.$('#devNoteWidth'))ctx.dom.$('#devNoteWidth').value=g.m.noteWidth;if(ctx.dom.$('#devNoteHeight'))ctx.dom.$('#devNoteHeight').value=g.m.noteHeight;ctx.services.editor.draw();
    }
    else if(ctx.state.gesture.kind==='lead'){
      if(Math.hypot(e.clientX-ctx.state.gesture.x,e.clientY-ctx.state.gesture.y)>4)ctx.state.gesture.moved=true;
      const home=ctx.services.editor.leadDock(e.clientX,e.clientY,ctx.state.gesture.m);
      ctx.services.editor.markLeadDock(home,ctx.state.gesture.m);
      if(home){ctx.state.gesture.preview=ctx.services.editor.localPoint(e);ctx.dom.$('#hint').textContent='Loslassen legt die Messleitung am Messgerät ab.';}
      else{const snap=ctx.services.editor.nearestLeadTerm(e.clientX,e.clientY,ctx.state.gesture.m,ctx.state.gesture.field);ctx.state.gesture.preview=snap?ctx.services.editor.point(snap.dataset.key):ctx.services.editor.localPoint(e);ctx.dom.$('#hint').textContent='Messleitung auf eine Klemme oder einen Messpunkt ziehen. Am Messgerät ablegen oder mit Esc abbrechen.';}
      ctx.services.editor.draw();
    }
    else{if(Math.hypot(e.clientX-ctx.state.gesture.x,e.clientY-ctx.state.gesture.y)>4)ctx.state.gesture.moved=true;if(ctx.state.gesture.moved){ctx.state.pending=null;ctx.services.editor.updateWireTarget(e);ctx.state.gesture.target=ctx.state.wireTarget;ctx.services.editor.draw(ctx.state.wireTarget?ctx.services.editor.point(ctx.state.wireTarget):ctx.services.editor.localPoint(e),ctx.state.gesture.key);}}

  }
  function mark(){
    const source=ctx.services.editor.wireSource(),neighbors=new Set();
    if(source)ctx.state.wires.forEach(w=>{if(w.a===source)neighbors.add(w.b);if(w.b===source)neighbors.add(w.a);});
    ctx.dom.board.querySelectorAll('.term').forEach(t=>{const key=t.dataset.key,attached=ctx.state.wires.find(w=>w.a===key||w.b===key);t.classList.toggle('connected',!!attached);t.classList.toggle('pending',source===key);t.classList.toggle('direct-neighbor',neighbors.has(key));t.classList.toggle('wire-target',ctx.state.wireTarget===key&&ctx.services.editor.canConnect(source,key));t.style.setProperty('--cable',ctx.services.editor.cablePaint(attached?.color||ctx.state.color));});
  }
  function routeBetween(a,b,boxes,lane){
    const xs=[...new Set([a.x,b.x,20,ctx.config.BOARD_W-20,...boxes.flatMap(r=>[r.l-lane,r.r+lane])])].filter(x=>x>=8&&x<=ctx.config.BOARD_W-8).sort((a,b)=>a-b);
    const ys=[...new Set([a.y,b.y,20,ctx.config.BOARD_H-16,...boxes.flatMap(r=>[r.t-lane,r.b+lane])])].filter(y=>y>=8&&y<=ctx.config.BOARD_H-8).sort((a,b)=>a-b);
    const inside=(x,y)=>boxes.some(r=>x>r.l&&x<r.r&&y>r.t&&y<r.b);
    const blocked=(x,y,u,v)=>boxes.some(r=>x===u?x>r.l&&x<r.r&&Math.max(y,v)>r.t&&Math.min(y,v)<r.b:y>r.t&&y<r.b&&Math.max(x,u)>r.l&&Math.min(x,u)<r.r);
    const start={i:xs.indexOf(a.x),j:ys.indexOf(a.y),dir:'',cost:0,prev:null};
    const queue=[start],best=new Map();let end=null;
    while(queue.length){
      let winner=0;for(let i=1;i<queue.length;i++){const score=n=>n.cost+Math.abs(xs[n.i]-b.x)+Math.abs(ys[n.j]-b.y);if(score(queue[i])<score(queue[winner]))winner=i;}
      const n=queue.splice(winner,1)[0],x=xs[n.i],y=ys[n.j];
      if(x===b.x&&y===b.y){end=n;break;}
      for(const [di,dj,dir] of [[-1,0,'h'],[1,0,'h'],[0,-1,'v'],[0,1,'v']]){
        const i=n.i+di,j=n.j+dj;if(i<0||j<0||i>=xs.length||j>=ys.length)continue;
        const u=xs[i],v=ys[j];if(inside(u,v)||blocked(x,y,u,v))continue;
        const cost=n.cost+Math.abs(x-u)+Math.abs(y-v)+(n.dir&&n.dir!==dir?18:0),key=i+','+j+','+dir;
        if(best.has(key)&&best.get(key)<=cost)continue;best.set(key,cost);queue.push({i,j,dir,cost,prev:n});
      }
    }
    if(!end)return [[a.x,a.y],[a.x,b.y],[b.x,b.y]];
    const result=[];for(let n=end;n;n=n.prev)result.unshift([xs[n.i],ys[n.j]]);return result;
  }
  function moduleBoxes(){
    return ctx.state.modules.filter(m=>m.el&&m.el.querySelector('.face')).map(m=>({id:m.id,l:m.x-9,r:m.x+m.el.offsetWidth+9,t:m.y-5,b:m.y+31+m.el.querySelector('.face').offsetHeight+23}));
  }
  function jogOf(w,key){
    if(!w||!key)return 0;
    const mates=ctx.state.wires.filter(x=>x.a===key||x.b===key);
    if(mates.length<2)return 0;
    return (mates.indexOf(w)-(mates.length-1)/2)*14;
  }
  function exitPoint(p,jog,boxes){
    const r=boxes.find(box=>box.id===p.id),lane=18;
    if(!r)return {x:p.x,y:p.y};
    if(p.d==='top')return {x:p.x+jog,y:r.t-lane};
    if(p.d==='left')return {x:r.l-lane,y:p.y+jog};
    if(p.d==='right')return {x:r.r+lane,y:p.y+jog};
    return {x:p.x+jog,y:r.b+lane};
  }
  function tidyPoints(points){
    points=points.filter((p,i)=>!i||Math.abs(p[0]-points[i-1][0])>.5||Math.abs(p[1]-points[i-1][1])>.5);
    for(let i=points.length-2;i>0;i--)if((Math.abs(points[i-1][0]-points[i][0])<.5&&Math.abs(points[i][0]-points[i+1][0])<.5)||(Math.abs(points[i-1][1]-points[i][1])<.5&&Math.abs(points[i][1]-points[i+1][1])<.5))points.splice(i,1);
    return points;
  }
  function routePoints(a,b,w,boxes){
    const first=ctx.services.editor.exitPoint(a,w?ctx.services.editor.jogOf(w,w.a):0,boxes),last=ctx.services.editor.exitPoint(b,w?ctx.services.editor.jogOf(w,w.b):0,boxes);
    return ctx.services.editor.tidyPoints([[a.x,a.y],...ctx.services.editor.routeBetween(first,last,boxes,18),[b.x,b.y]]);
  }
  function orthoPath(points){
    let d=`M${points[0][0]} ${points[0][1]}`;
    for(let i=1;i<points.length-1;i++){
      const p=points[i],prev=points[i-1],next=points[i+1],aLen=Math.hypot(p[0]-prev[0],p[1]-prev[1]),bLen=Math.hypot(next[0]-p[0],next[1]-p[1]),r=Math.min(10,aLen/2,bLen/2);
      if(aLen<1||bLen<1){d+=` L${p[0]} ${p[1]}`;continue;}
      d+=` L${p[0]-(p[0]-prev[0])/aLen*r} ${p[1]-(p[1]-prev[1])/aLen*r} Q${p[0]} ${p[1]} ${p[0]+(next[0]-p[0])/bLen*r} ${p[1]+(next[1]-p[1])/bLen*r}`;
    }
    return d+` L${points[points.length-1][0]} ${points[points.length-1][1]}`;
  }
  function segmentHits(horizontal,pos,spanA,spanB,boxes,margin){
    const pad=margin==null?8:margin;
    return boxes.some(r=>horizontal?pos>r.t-pad&&pos<r.b+pad&&spanB>r.l+1&&spanA<r.r-1:pos>r.l-pad&&pos<r.r+pad&&spanB>r.t+1&&spanA<r.b-1);
  }
  function spreadBundles(routes,boxes){
    const occupied=(pts,horizontal,pos,spanA,spanB)=>{
      for(let n=1;n<pts.length-2;n++){
        const c=pts[n],d=pts[n+1];
        if(horizontal){
          if(Math.abs(c[1]-d[1])>.5||Math.abs(c[1]-pos)>8)continue;
          if(Math.min(spanB,Math.max(c[0],d[0]))-Math.max(spanA,Math.min(c[0],d[0]))>16)return true;
        }else if(Math.abs(c[0]-d[0])<.5&&Math.abs(c[0]-pos)<=8&&Math.min(spanB,Math.max(c[1],d[1]))-Math.max(spanA,Math.min(c[1],d[1]))>16)return true;
      }
      return false;
    };
    const run=horizontal=>{
      const segs=[];
      routes.forEach((pts,wi)=>{
        for(let i=1;i<pts.length-2;i++){
          const a=pts[i],b=pts[i+1];
          const straight=horizontal?Math.abs(a[1]-b[1])<.5&&Math.abs(a[0]-b[0])>28:Math.abs(a[0]-b[0])<.5&&Math.abs(a[1]-b[1])>28;
          if(!straight)continue;
          const prev=pts[i-1],next=pts[i+2];
          const prevOk=i===1||(horizontal?Math.abs(prev[0]-a[0])<.5:Math.abs(prev[1]-a[1])<.5);
          const nextOk=i+1===pts.length-2||(horizontal?Math.abs(next[0]-b[0])<.5:Math.abs(next[1]-b[1])<.5);
          if(!prevOk||!nextOk)continue;
          const span=horizontal?[a[0],b[0]]:[a[1],b[1]];
          segs.push({wi,i,p:horizontal?a[1]:a[0],a:Math.min(span[0],span[1]),b:Math.max(span[0],span[1])});
        }
      });
      const parent=segs.map((_,i)=>i);
      const find=i=>parent[i]===i?i:(parent[i]=find(parent[i]));
      for(let i=0;i<segs.length;i++)for(let j=i+1;j<segs.length;j++){
        if(segs[i].wi===segs[j].wi||Math.abs(segs[i].p-segs[j].p)>22)continue;
        if(Math.min(segs[i].b,segs[j].b)-Math.max(segs[i].a,segs[j].a)<=16)continue;
        const x=find(i),y=find(j);if(x!==y)parent[x]=y;
      }
      const groups=new Map();
      segs.forEach((s,i)=>{const r=find(i);if(!groups.has(r))groups.set(r,[]);groups.get(r).push(s);});
      groups.forEach(group=>{
        const by=new Map();
        group.forEach(s=>{if(!by.has(s.wi))by.set(s.wi,[]);by.get(s.wi).push(s);});
        const ids=[...by.keys()].sort((a,b)=>a-b);
        if(ids.length<2)return;
        const origin=ids.reduce((sum,wi)=>sum+by.get(wi)[0].p,0)/ids.length;
        const clear=(pos,margin)=>ids.every((wi,k)=>by.get(wi).every(s=>{
          if(ctx.services.editor.segmentHits(horizontal,pos[k],s.a,s.b,boxes,margin))return false;
          return !routes.some((pts,other)=>other!==wi&&!ids.includes(other)&&occupied(pts,horizontal,pos[k],s.a,s.b));
        }));
        let chosen=null;
        for(const margin of [8,2]){
          for(const pitch of [12,8]){
            const base=ids.map((_,k)=>(k-(ids.length-1)/2)*pitch);
            for(let step=0;step<=ids.length+6&&!chosen;step++){
              for(const sign of step?[1,-1]:[0]){
                const pos=base.map(o=>origin+o+sign*step*pitch);
                if(clear(pos,margin)){chosen=pos;break;}
              }
            }
            if(chosen)break;
          }
          if(chosen)break;
        }
        if(!chosen){
          const used=[],axis=horizontal?1:0,pitch=10;
          ids.forEach(wi=>{
            const segs=by.get(wi);
            const fits=pos=>segs.every(s=>!ctx.services.editor.segmentHits(horizontal,pos,s.a,s.b,boxes,2)&&!used.some(u=>Math.abs(u.p-pos)<8&&Math.min(s.b,u.b)-Math.max(s.a,u.a)>16)&&!routes.some((pts,other)=>other!==wi&&!ids.includes(other)&&occupied(pts,horizontal,pos,s.a,s.b)));
            let slot=null;
            for(let step=0;step<=ids.length&&slot==null;step++){
              for(const sign of step?[1,-1]:[0]){
                const pos=origin+sign*step*pitch;
                if(fits(pos)){slot=pos;break;}
              }
            }
            if(slot==null)return;
            segs.forEach(s=>{routes[wi][s.i][axis]=slot;routes[wi][s.i+1][axis]=slot;used.push({p:slot,a:s.a,b:s.b});});
          });
          return;
        }
        const axis=horizontal?1:0;
        ids.forEach((wi,k)=>by.get(wi).forEach(s=>{routes[wi][s.i][axis]=chosen[k];routes[wi][s.i+1][axis]=chosen[k];}));
      });
    };
    run(true);run(false);
  }
  function pathFor(a,b,index,w){
    if(a.id===b.id&&ctx.state.modules.some(m=>m.id===a.id&&m.type==='acmotor')){
      const motor=ctx.state.modules.find(m=>m.id===a.id),dx=b.x-a.x,dy=b.y-a.y,len2=dx*dx+dy*dy;
      const crossesTerminal=len2>0&&[...motor.el.querySelectorAll('.term')].some(t=>{
        const p=ctx.services.editor.point(t.dataset.key),u=((p.x-a.x)*dx+(p.y-a.y)*dy)/len2;
        return u>.05&&u<.95&&Math.hypot(p.x-a.x-u*dx,p.y-a.y-u*dy)<20;
      });
      if(crossesTerminal){
        const len=Math.sqrt(len2),bend=52;
        return `M${a.x} ${a.y} Q${(a.x+b.x)/2-dy/len*bend} ${(a.y+b.y)/2+dx/len*bend} ${b.x} ${b.y}`;
      }
      return `M${a.x} ${a.y} L${b.x} ${b.y}`;
    }
    const boxes=ctx.services.editor.moduleBoxes();
    const first=ctx.services.editor.exitPoint(a,w?ctx.services.editor.jogOf(w,w.a):0,boxes);
    if(b.d==='free'){
      if(a.d==='left'||a.d==='right')return `M${a.x} ${a.y} C${first.x} ${a.y} ${b.x} ${b.y} ${b.x} ${b.y}`;
      return `M${a.x} ${a.y} C${a.x} ${first.y} ${b.x} ${b.y} ${b.x} ${b.y}`;
    }
    return ctx.services.editor.orthoPath(ctx.services.editor.routePoints(a,b,w,boxes));
  }
  function path(d,cls,c){const p=document.createElementNS(ctx.config.NS,'path');p.setAttribute('d',d);p.setAttribute('class',cls);if(c)p.setAttribute('stroke',c);return p;}
  function removeWire(i){if(i<0||!ctx.state.wires[i])return;const target=ctx.state.wires[i];if(!ctx.services.simulation.requestElectricalEdit(()=>ctx.services.editor.removeWire(ctx.state.wires.indexOf(target))))return;ctx.services.projekt.remember();if(ctx.state.wires[i]===ctx.state.selectedWire)ctx.state.selectedWire=null;ctx.state.wires.splice(i,1);ctx.services.simulation.update();ctx.services.simulation.noteWiringReset();}
  function draw(preview,start=ctx.state.pending){
    ctx.dom.svg.replaceChildren();ctx.dom.svg.append(ctx.services.editor.peStripe());
    const overlays=new Map();
    ctx.state.modules.forEach(m=>{const layer=m.el.querySelector('.module-wires');layer.replaceChildren();layer.setAttribute('viewBox',`${m.x} ${m.y} ${m.el.offsetWidth} ${m.el.offsetHeight}`);layer.style.clipPath=`inset(${m.el.querySelector('.handle').offsetHeight}px 0 0 0)`;overlays.set(m.id,layer);});
    const tag=(p,i)=>{p.dataset.wire=String(i);p.classList.toggle('wire-selected',ctx.state.wires[i]===ctx.state.selectedWire);return p;};
    const paint=(target,d,c,i)=>{target.append(tag(ctx.services.editor.path(d,'wire-shadow'),i),tag(ctx.services.editor.path(d,'wire-volt'),i));if(ctx.state.latchedFault?.trace?.wireKeys.includes(ctx.services.simulation.faultWireKey(ctx.state.wires[i])))target.append(tag(ctx.services.editor.path(d,'fault-wire'),i));target.append(tag(ctx.services.editor.path(d,'wire-line',c),i),tag(ctx.services.editor.path(d,'wire-highlight'),i));};
    const setHot=(i,on)=>document.querySelectorAll('[data-wire="'+i+'"]').forEach(el=>el.classList.toggle('hot',on));
    const boxes=ctx.services.editor.moduleBoxes();
    const routed=[];
    ctx.state.wires.forEach((w,i)=>{
      const a=ctx.services.editor.point(w.a),b=ctx.services.editor.point(w.b);if(!a||!b)return;
      if(a.id===b.id&&ctx.state.modules.some(m=>m.id===a.id&&m.type==='acmotor'))routed.push({i,w,a,b,d:ctx.services.editor.pathFor(a,b,i,w)});
      else routed.push({i,w,a,b,pts:ctx.services.editor.routePoints(a,b,w,boxes)});
    });
    ctx.services.editor.spreadBundles(routed.filter(r=>r.pts).map(r=>r.pts),boxes);
    routed.forEach(r=>{
      const {w,i,a,b}=r,d=r.d||ctx.services.editor.orthoPath(ctx.services.editor.tidyPoints(r.pts));paint(ctx.dom.svg,d,ctx.services.editor.cableStroke(w.color),i);
      const hitOn=target=>{
        const hit=tag(ctx.services.editor.path(d,'wire-hit'),i);
        hit.onpointerdown=e=>e.stopPropagation();
        hit.onclick=e=>{e.stopPropagation();ctx.services.editor.clickWire(w);};
        hit.ondblclick=e=>{e.stopPropagation();e.preventDefault();ctx.services.editor.removeWire(ctx.state.wires.indexOf(w));};
        hit.setAttribute('title','Doppelklick löscht die Leitung');
        hit.onpointerenter=()=>setHot(i,true);
        hit.onpointerleave=e=>{const n=e.relatedTarget&&e.relatedTarget.closest&&e.relatedTarget.closest('[data-wire]');if(n&&n.dataset.wire===String(i))return;setHot(i,false);};
        target.append(hit);
      };
      hitOn(ctx.dom.svg);
      for(const id of new Set([a.id,b.id])){paint(overlays.get(id),d,ctx.services.editor.cableStroke(w.color),i);hitOn(overlays.get(id));}
    });
    if(start&&preview){const a=ctx.services.editor.point(start);if(a){const d=ctx.services.editor.pathFor(a,preview,0);ctx.dom.svg.append(ctx.services.editor.path(d,'wire-preview'));overlays.get(a.id).append(ctx.services.editor.path(d,'wire-preview'));}}
    const coupling=ctx.dom.$('#couplings');coupling.replaceChildren();
    ctx.state.modules.filter(m=>m.type==='auxiliary').forEach(m=>{const parent=ctx.state.modules.find(p=>p.id===m.parent);if(!parent)return;const left=m.x>parent.x;
      const a={x:parent.x+(left?parent.el.offsetWidth:0),y:parent.y+190},b={x:m.x+(left?0:m.el.offsetWidth),y:m.y+118};
      coupling.append(ctx.services.editor.path(`M${a.x} ${a.y} L${b.x} ${b.y}`,'mechanical-link'));
    });
    ctx.services.editor.paintWireVoltage(ctx.state.lastSnap);
    ctx.services.editor.paintWireCurrent(ctx.state.lastSnap);
    ctx.services.editor.drawLeads();
    ctx.services.editor.paintFaultScene();
  }
  function leadOrigin(m,field){
    const jack=m.el&&m.el.querySelector('[data-lead="'+field+'"]');
    if(!jack)return null;
    const r=jack.getBoundingClientRect(),b=ctx.dom.board.getBoundingClientRect();
    return {x:(r.left+r.width/2-b.left)/ctx.state.scale,y:(r.bottom-b.top)/ctx.state.scale};
  }
  function leadHang(origin,field){return {x:origin.x+(field==='probeRed'?-120:120),y:origin.y+430,d:'free'};}
  function leadCable(a,b,field){
    const side=field==='probeRed'?-1:1;
    const sag=90+Math.min(70,Math.abs(b.y-a.y)*.08);
    const low=Math.max(a.y,b.y)+sag;
    return `M${a.x} ${a.y} C${a.x+side*70} ${a.y+8} ${(a.x+b.x)/2+side*90} ${low} ${b.x} ${b.y}`;
  }
  function leadBanana(parent,o,tone){
    const g=document.createElementNS(ctx.config.NS,'g');
    g.setAttribute('class','lead-banana');
    g.setAttribute('transform',`translate(${o.x} ${o.y-6})`);
    const side=tone==='probeRed'?-1:1;
    const boot=tone==='probeRed'?'#e10600':'#1a1a1a';
    const box=(x,y,w,h,rx,fill)=>{const r=document.createElementNS(ctx.config.NS,'rect');r.setAttribute('x',x);r.setAttribute('y',y);r.setAttribute('width',w);r.setAttribute('height',h);r.setAttribute('rx',rx);r.setAttribute('fill',fill);g.append(r);};
    box(-8,-36,16,30,3,'#3a3a3a');
    box(-16,-10,32,22,4,'#1a1a1a');
    box(-10,4,20,10,3,boot);
    parent.append(g);
    return {x:o.x+side*20,y:o.y+4};
  }
  function leadProbe(parent,tip,from,tone){
    let dx=tip.x-from.x,dy=tip.y-from.y,len=Math.hypot(dx,dy);
    if(len<1){dx=0;dy=1;len=1;}
    dx/=len;dy/=len;
    const g=document.createElementNS(ctx.config.NS,'g');
    g.setAttribute('class','lead-probe '+tone);
    g.setAttribute('transform',`translate(${tip.x} ${tip.y}) rotate(${Math.atan2(dy,dx)*180/Math.PI-90})`);
    const color=tone==='probeRed'?'#e10600':'#1c1c1c';
    const box=(x,y,w,h,rx,fill)=>{const r=document.createElementNS(ctx.config.NS,'rect');r.setAttribute('x',x);r.setAttribute('y',y);r.setAttribute('width',w);r.setAttribute('height',h);r.setAttribute('rx',rx);r.setAttribute('fill',fill);g.append(r);};
    const needle=document.createElementNS(ctx.config.NS,'polygon');
    needle.setAttribute('points','0,2 -7,-42 7,-42');
    needle.setAttribute('fill','#d5dee4');
    g.append(needle);
    box(-14,-86,28,46,5,'#c5ced2');
    box(-14,-188,28,104,6,color);
    box(-16,-108,32,8,2,'#5c666b');
    const grip=document.createElementNS(ctx.config.NS,'rect');
    grip.setAttribute('class','lead-probe-hit');
    grip.setAttribute('x','-24');grip.setAttribute('y','-192');grip.setAttribute('width','48');grip.setAttribute('height','198');
    g.append(grip);
    parent.append(g);
    return {x:tip.x-dx*188,y:tip.y-dy*188,hit:grip};
  }
  function raiseLeads(){ctx.dom.leads.style.zIndex=String(ctx.state.modules.reduce((n,m)=>Math.max(n,Number(m.z)||0),0)+1);}
  function drawLeads(){
    ctx.services.editor.raiseLeads();
    ctx.dom.leads.replaceChildren();
    ctx.state.modules.filter(ctx.services.editor.isMeter).forEach(m=>{
      ['probeRed','probeBlack'].forEach(field=>{
        const origin=ctx.services.editor.leadOrigin(m,field);if(!origin)return;
        const dragging=ctx.state.gesture?.kind==='lead'&&ctx.state.gesture.m===m&&ctx.state.gesture.field===field&&ctx.state.gesture.preview;
        const parked=m[field]&&ctx.services.editor.term(m[field])?ctx.services.editor.point(m[field]):ctx.services.editor.leadHang(origin,field);
        const target=dragging?ctx.state.gesture.preview:parked;
        if(!target)return;
        const tone=field==='probeRed'?'probeRed':'probeBlack';
        const exit=ctx.services.editor.leadBanana(ctx.dom.leads,origin,tone);
        const back=ctx.services.editor.leadProbe(ctx.dom.leads,target,origin,tone);
        const cable=ctx.services.editor.leadCable(exit,back,field);
        ctx.dom.leads.append(ctx.services.editor.path(cable,'lead-shadow'),ctx.services.editor.path(cable,'lead-line '+tone));
        const bind=el=>{
          el.dataset.lead=field;el.dataset.meter=m.id;
          el.setAttribute('title','Ziehen und auf eine Klemme legen oder am Messgerät ablegen. Doppelklick löst diese Messleitung.');
          el.onpointerdown=e=>{if(e.button!==0)return;e.stopPropagation();e.preventDefault();ctx.services.editor.beginLead(m,field,e);};
          el.ondblclick=e=>{e.stopPropagation();e.preventDefault();ctx.services.projekt.remember();ctx.services.editor.setLead(m,field,null);ctx.services.projekt.schedulePersist();ctx.services.editor.paintMeters(ctx.state.lastSnap);ctx.services.editor.draw();};
        };
        const hit=ctx.services.editor.path(cable,'lead-hit');
        bind(hit);bind(back.hit);
        ctx.dom.leads.append(hit);
      });
    });
  }
  let acFrame=null,acNetwork=null,acSignature='';
  const acLampMarks=new Map(),acWireMarks=new Map();
  function acDisplayNetwork(snap){
    if(!ctx.state.powered||ctx.state.latchedFault||!snap||!ctx.state.modules.some(m=>m.type==='mains')){
      acFrame=null;acNetwork=null;acSignature='';acLampMarks.clear();acWireMarks.clear();return null;
    }
    const signature=String(snap.revision)+'|'+ctx.state.modules.map(m=>[m.id,m.pressed,m.selection,m.protectionOn,m.protectionTripped,m.voltage,m.nomV,m.resistance,m.potPosition].join(':')).join('|')+'|'+JSON.stringify(snap.contacts||{});
    if(signature!==acSignature){acLampMarks.clear();acWireMarks.clear();acSignature=signature;acFrame=null;}
    if(acFrame===snap)return acNetwork;
    const current=Object.fromEntries(Object.entries(snap.coils||{}).map(([id,coil])=>[id,!!coil.energized]));
    const g=ctx.services.simulation.graph(current,true),phases=new Set(),terminals=new Set();
    ctx.state.modules.filter(m=>m.type==='mains').forEach(m=>{
      ['L1','L2','L3','N','PE'].forEach(pin=>ctx.services.simulation.reachable(g,m.id+'.'+pin).forEach(key=>{terminals.add(key);if(pin.startsWith('L'))phases.add(key);}));
    });
    const signals=new Map([...g].map(([key,values])=>[key,new Set(values)]));
    const link=(a,b)=>{if(!signals.has(a))signals.set(a,new Set());if(!signals.has(b))signals.set(b,new Set());signals.get(a).add(b);signals.get(b).add(a);};
    ctx.state.modules.forEach(m=>{if(ctx.services.editor.isLight(m)||ctx.services.editor.isCombinedTaster(m)||m.type==='emergency'){const [a,b]=lampPins(m);link(m.id+'.'+a,m.id+'.'+b);}});
    const displayTerminals=new Set(terminals);
    phases.forEach(key=>ctx.services.simulation.reachable(signals,key).forEach(term=>displayTerminals.add(term)));
    acFrame=snap;acNetwork={g,phases,terminals,displayTerminals};return acNetwork;
  }
  function lampPins(m){return m.type==='pilot'||ctx.services.editor.isCombinedTaster(m)||m.type==='emergency'?['X1','X2']:['L','N'];}
  function displayLampOn(m,snap){
    const raw=!!snap?.lamps?.[m.id]?.on;
    if(!ctx.services.simulation.plantLive()){acLampMarks.delete(m.id);return false;}
    const [a,b]=lampPins(m),ac=acDisplayNetwork(snap),l=m.id+'.'+a,n=m.id+'.'+b;
    if(!ac||!ac.displayTerminals.has(l)&&!ac.displayTerminals.has(n))return raw;
    if(ctx.services.simulation.reachable(ac.g,l).has(n)){acLampMarks.delete(m.id);return false;}
    const now=performance.now();
    if(raw)acLampMarks.set(m.id,now);
    // Ein Nulldurchgang löscht die Anzeige nicht; Schaltänderungen leeren den Verlauf sofort.
    const closed=(ac.phases.has(l)||ac.phases.has(n))&&ac.terminals.has(l)&&ac.terminals.has(n);
    if(acLampMarks.has(m.id)&&(closed||!snap.running||now-acLampMarks.get(m.id)<=250))return true;
    acLampMarks.delete(m.id);return false;
  }
  function displayWireCurrent(w,snap){
    const current=ctx.services.editor.wireCurrentOf(w,snap),network=acDisplayNetwork(snap);
    const ac=!!network&&(network.displayTerminals.has(w.a)||network.displayTerminals.has(w.b));
    const key=w.a+'|'+w.b,now=performance.now();
    if(ac&&Math.abs(current)>=ctx.config.I_DOT)acWireMarks.set(key,now);
    const active=Math.abs(current)>=ctx.config.I_DOT||ac&&acWireMarks.has(key)&&(!snap.running||now-acWireMarks.get(key)<=250);
    if(!active)acWireMarks.delete(key);
    return {current,ac,active};
  }
  function acPhaseLive(snap){return acDisplayNetwork(snap)?.phases||new Set();}
  function wireHasVoltage(w,snap){
    if(!ctx.state.showVoltage||!ctx.state.powered||!snap)return false;
    const terms=snap.terminals||{};
    const ac=ctx.services.editor.acPhaseLive(snap);
    const live=key=>{if(ac.has(key))return true;const v=Number(terms[key]);return Number.isFinite(v)&&Math.abs(v)>=ctx.config.LIVE_V;};
    return live(w.a)||live(w.b);
  }
  function paintWireVoltage(snap){
    ctx.state.wires.forEach((w,i)=>{
      const on=ctx.services.editor.wireHasVoltage(w,snap);
      document.querySelectorAll('[data-wire="'+i+'"]').forEach(el=>{
        if(!el.classList.contains('wire-volt')&&!el.classList.contains('wire-highlight'))return;
        el.classList.toggle('volt-live',on);
      });
    });
  }
  function setShowVoltage(on){
    ctx.state.showVoltage=!!on;
    const btn=ctx.dom.$('#showVoltage');
    if(btn)btn.setAttribute('aria-pressed',String(ctx.state.showVoltage));
    ctx.services.editor.paintWireVoltage(ctx.state.lastSnap);
    if(!ctx.config.skipStore)try{localStorage.setItem(ctx.config.VOLT_STORE,ctx.state.showVoltage?'1':'0');}catch(e){}
  }
  function wireCurrentOf(w,snap){
    if(!ctx.state.showCurrent||!ctx.state.powered||!snap)return 0;
    const i=Number((snap.wireCurrents||{})[w.a+'|'+w.b]);
    return Number.isFinite(i)?i:0;
  }
  function currentDotAdvance(current,elapsedMs){
    const advance=Math.min(ctx.config.DOT_ADD_MAX,Math.abs(current)*ctx.config.DOT_BASE_MULT*ctx.config.DOT_SPEED);
    return advance*(ctx.state.currentSpeed/100)*(elapsedMs/(1000/60));
  }
  function setCurrentSpeed(value){
    const n=Number(value);
    ctx.state.currentSpeed=Number.isFinite(n)?Math.max(5,Math.min(100,Math.round(n/5)*5)):25;
    const input=ctx.dom.$('#currentSpeed'),output=ctx.dom.$('#currentSpeedValue');
    if(input){if(input.value!==String(ctx.state.currentSpeed))input.value=String(ctx.state.currentSpeed);input.setAttribute('aria-valuetext',ctx.state.currentSpeed+' Prozent');}
    if(output)output.textContent=ctx.state.currentSpeed+' %';
    if(!ctx.config.skipStore)try{localStorage.setItem(ctx.config.CURRENT_SPEED_STORE,String(ctx.state.currentSpeed));}catch(e){}
  }
  function paintWireCurrent(snap){
    if(!ctx.dom.dots)return;
    ctx.dom.dots.replaceChildren();
    ctx.state.modules.forEach(m=>m.el?.querySelector('.module-wires')?.querySelectorAll('.wire-dot').forEach(dot=>dot.remove()));
    if(!ctx.state.showCurrent||!ctx.state.powered||!snap){ctx.state.dotPos=[];ctx.state.lastDotTime=null;return;}
    const running=!!snap.running;
    const now=performance.now();
    const elapsed=ctx.state.lastDotTime===null?1000/60:Math.min(100,Math.max(0,now-ctx.state.lastDotTime));
    ctx.state.lastDotTime=now;
    const electronFlow=Number(snap.currentMult)<0;
    ctx.state.wires.forEach((w,i)=>{
      const display=ctx.services.editor.displayWireCurrent(w,snap),cur=display.current;
      if(!display.active){ctx.state.dotPos[i]=0;return;}
      if(running){
        ctx.state.dotPos[i]=(ctx.state.dotPos[i]||0)+ctx.services.editor.currentDotAdvance(cur,elapsed)*(display.ac&&cur<0?-1:1);
      }
      const path=ctx.dom.svg.querySelector('.wire-line[data-wire="'+i+'"]');
      if(!path||typeof path.getTotalLength!=='function')return;
      const len=path.getTotalLength();
      if(len<8)return;
      let pos=(ctx.state.dotPos[i]||0)%ctx.config.DOT_SP;
      if(pos<0)pos+=ctx.config.DOT_SP;
      const reverse=(display.ac?false:cur<0)!==electronFlow;
      const half=ctx.config.DOT_SIZE/2;
      for(let d=pos;d<len;d+=ctx.config.DOT_SP){
        const pt=path.getPointAtLength(reverse?len-d:d);
        const r=document.createElementNS(ctx.config.NS,'rect');
        r.setAttribute('class','wire-dot');
        r.setAttribute('x',pt.x-half);
        r.setAttribute('y',pt.y-half);
        r.setAttribute('width',String(ctx.config.DOT_SIZE));
        r.setAttribute('height',String(ctx.config.DOT_SIZE));
        ctx.dom.dots.append(r);
        ctx.state.modules.forEach(m=>{
          if((Number(m.z)||0)<6||pt.x<m.x||pt.x>m.x+m.el.offsetWidth||pt.y<m.y||pt.y>m.y+m.el.offsetHeight)return;
          const layer=m.el.querySelector('.module-wires');
          if(layer)layer.append(r.cloneNode(true));
        });
      }
    });
  }
  function setShowCurrent(on){
    ctx.state.showCurrent=!!on;
    const btn=ctx.dom.$('#showCurrent');
    if(btn){btn.setAttribute('aria-pressed',String(ctx.state.showCurrent));btn.setAttribute('aria-expanded',String(ctx.state.showCurrent));btn.title=ctx.state.showCurrent?'Stromanzeige ausschalten':'Stromanzeige einschalten und Tempo einstellen';}
    const control=ctx.dom.$('#currentSpeedControl');
    if(!ctx.state.showCurrent&&control.contains(document.activeElement))btn.focus({preventScroll:true});
    ctx.dom.$('#currentDisplay').classList.toggle('expanded',ctx.state.showCurrent);
    control.setAttribute('aria-hidden',String(!ctx.state.showCurrent));control.inert=!ctx.state.showCurrent;
    ctx.dom.$('#currentSpeed').disabled=!ctx.state.showCurrent;
    ctx.state.lastDotTime=null;
    ctx.services.editor.paintWireCurrent(ctx.state.lastSnap);
    if(!ctx.config.skipStore)try{localStorage.setItem(ctx.config.CURRENT_STORE,ctx.state.showCurrent?'1':'0');}catch(e){}
  }
  function paintTaster(m){
    if(!m.el)return;
    m.el.classList.toggle('pressed',!!m.pressed);
    m.el.querySelector('.push')?.classList.toggle('down',!!m.pressed);
    const label=m.el.querySelector('.module-state');
    if(m.type==='emergency'){m.el.querySelector('.push').setAttribute('aria-pressed',String(m.latched));m.el.querySelector('.emergency-unlock').disabled=!m.latched;if(label)label.textContent=m.latched?'Eingerastet · NC offen · NO zu':'Entriegelt · NC zu · NO offen';return;}
    const push=m.el.querySelector('.push');if(push){push.setAttribute('aria-pressed',String(!!m.pressed));push.setAttribute('aria-label',m.latching?'Schalter '+m.id+' '+(m.pressed?'ausschalten':'einschalten'):'Taster '+m.id+' gedrückt halten');}
    if(label)label.textContent=(m.pressed?'NO zu · NC auf':'NO auf · NC zu')+(m.latching?' · rastend':'');
  }
  function setView(mode){
    if(mode==='compare'&&ctx.state.viewMode!=='compare'){
      ctx.state.panelsBeforeCompare={parts:!ctx.dom.workspace.classList.contains('hide-parts'),inspector:!ctx.dom.workspace.classList.contains('hide-inspector')};
      ctx.dom.workspace.classList.add('hide-parts','hide-inspector');
    }else if(mode==='stick'&&ctx.state.viewMode!=='stick'&&ctx.state.panelsBeforeCompare){
      ctx.dom.workspace.classList.toggle('hide-parts',!ctx.state.panelsBeforeCompare.parts);
      ctx.dom.workspace.classList.toggle('hide-inspector',!ctx.state.panelsBeforeCompare.inspector);
      ctx.state.panelsBeforeCompare=null;
    }
    ctx.state.viewMode=mode;
    document.documentElement.classList.toggle('view-stick',mode==='stick');
    ctx.dom.$('#viewCompare').setAttribute('aria-pressed',String(mode==='compare'));
    ctx.dom.$('#viewStick').setAttribute('aria-pressed',String(mode==='stick'));
    ctx.services.editor.partsLabel();ctx.services.editor.inspectorLabel();
    ctx.services.editor.applySplitLayout();
  }
  function renderSimControls(){
    ctx.services.editor.renderFaultBanner();
    const ready=ctx.state.engineOk&&!!ctx.state.engine?.setRunning;
    document.documentElement.classList.toggle('mode-aufbau',ctx.state.simMode==='aufbau');
    document.documentElement.classList.toggle('mode-betrieb',ctx.services.simulation.liveMode());
    document.documentElement.classList.toggle('sim-paused',ctx.state.simMode==='paused');
    const pauseEl=ctx.dom.$('#simPause'),buildEl=ctx.dom.$('#modeBuild'),runEl=ctx.dom.$('#modeRun');
    if(buildEl)buildEl.setAttribute('aria-pressed',String(ctx.state.simMode==='aufbau'));
    if(runEl){
      runEl.setAttribute('aria-pressed',String(ctx.services.simulation.liveMode()));
      runEl.disabled=!ready||!!ctx.state.latchedFault;
    }
    if(pauseEl){
      pauseEl.disabled=!ready||ctx.state.simMode==='aufbau'||!!ctx.state.latchedFault;
      pauseEl.textContent=ctx.state.simMode==='paused'?'Fortsetzen':'Einfrieren';
      pauseEl.setAttribute('aria-pressed',String(ctx.state.simMode==='paused'));
    }
    ctx.dom.$('#simReset').disabled=!ready||typeof ctx.state.engine.resetSimulation!=='function';
    ctx.dom.$('#simReset').title=ready&&typeof ctx.state.engine.resetSimulation!=='function'?'CircuitJS-Build mit Reset-Schnittstelle erforderlich':'';
    const status=ctx.state.latchedFault?'Fehler':!ready?'Nicht verbunden':ctx.state.simMode==='aufbau'?'Aufbau':ctx.state.simMode==='paused'?'Eingefroren':'Betrieb';
    ctx.dom.$('#simState').textContent=status;ctx.dom.$('#simState').title=status;
    ctx.services.simulation.setEngineTag(ctx.state.latchedFault?'CircuitJS-Fehler':ready?'CircuitJS verbunden':ctx.state.engineImportError||'Keine Verbindung',!!ctx.state.latchedFault||!ready);
    ctx.services.simulation.paintHint();
  }
  function renderFaultBanner(){
    const banner=ctx.dom.$('#faultBanner');
    banner.hidden=!ctx.state.latchedFault;
    document.documentElement.classList.toggle('fault-latched',!!ctx.state.latchedFault);
    ctx.services.editor.paintFaultScene();ctx.services.editor.renderFaultPath();
    if(!ctx.state.latchedFault)return;
    const message=String(ctx.state.latchedFault.message||'Keine Detailmeldung verfügbar.');
    const text=ctx.services.simulation.getFaultPresentation(ctx.state.latchedFault);
    if(ctx.dom.$('#faultTitle').textContent!==text.title)ctx.dom.$('#faultTitle').textContent=text.title;
    if(ctx.dom.$('#faultExplanation').textContent!==text.explanation)ctx.dom.$('#faultExplanation').textContent=text.explanation;
    if(ctx.dom.$('#faultNextStep').textContent!==text.next)ctx.dom.$('#faultNextStep').textContent=text.next;
    if(ctx.dom.$('#faultTechnical').textContent!==message)ctx.dom.$('#faultTechnical').textContent=message;
    if(ctx.dom.$('#faultReset'))ctx.dom.$('#faultReset').disabled=!ctx.state.engineOk||typeof ctx.state.engine?.resetSimulation!=='function';
  }
  function paintFaultScene(){
    const fault=ctx.state.latchedFault&&ctx.services.simulation.faultKind(ctx.state.latchedFault)==='short'?ctx.state.latchedFault:null,trace=fault?.trace;
    if(!fault&&!ctx.dom.board.querySelector('.fault-module,.fault-terminal,.fault-contact,.fault-contact-link')&&!document.querySelector('.fault-wire'))return;
    const ids=new Set(fault?.componentIds||[]),terminals=new Set(trace?.nodes||[]);
    ctx.state.modules.forEach(m=>{
      const involved=ids.has(m.id);m.el.classList.toggle('fault-module',involved);
      const handle=m.el.querySelector('.handle');
      if(involved){
        handle.dataset.faultLabel='Am Kurzschluss beteiligt';handle.title='Kurzschluss: '+m.id+' ist am erkannten Fehlerweg beteiligt.';
        if(!handle.querySelector('.fault-part-warning')){const icon=document.createElementNS(ctx.config.NS,'svg');icon.setAttribute('class','fault-part-warning');icon.setAttribute('viewBox','0 0 24 24');icon.setAttribute('aria-hidden','true');icon.append(ctx.services.editor.path('M12 3 L22 21 H2 Z M12 9 V14 M12 17 V18',''));handle.append(icon);}
      }
      else{delete handle.dataset.faultLabel;handle.removeAttribute('title');handle.querySelector('.fault-part-warning')?.remove();}
      m.el.querySelectorAll('.term').forEach(t=>t.classList.toggle('fault-terminal',terminals.has(t.dataset.key)));
      m.el.querySelectorAll('.fault-contact').forEach(row=>row.classList.remove('fault-contact'));
      m.el.querySelectorAll('.fault-contact-link').forEach(p=>p.remove());
    });
    for(const step of trace?.steps||[]){
      if(step.kind!=='contact')continue;
      const m=ctx.state.modules.find(x=>x.id===step.id),a=ctx.services.editor.term(step.a),b=ctx.services.editor.term(step.b);
      if(!m||!a||!b)continue;
      const row=a.closest('.contact-row,.selector-contact');
      if(row&&row.contains(b)){row.classList.add('fault-contact');continue;}
      const pa=ctx.services.editor.point(step.a),pb=ctx.services.editor.point(step.b);if(!pa||!pb)continue;
      m.el.querySelector('.module-wires').append(ctx.services.editor.path('M'+pa.x+' '+pa.y+' L'+pb.x+' '+pb.y,'fault-contact-link'));
    }
    document.querySelectorAll('.fault-wire').forEach(p=>{const wire=ctx.state.wires[Number(p.dataset.wire)];p.style.display=wire&&trace?.wireKeys.includes(ctx.services.simulation.faultWireKey(wire))?'':'none';});
  }
  function renderFaultPath(){
    const short=ctx.state.latchedFault&&ctx.services.simulation.faultKind(ctx.state.latchedFault)==='short',trace=short?ctx.state.latchedFault.trace:null;
    ctx.dom.$('#faultDiagnosis').hidden=!short;
    ctx.dom.$('#faultLocate').hidden=!short||!(ctx.state.latchedFault.componentIds||[]).some(id=>ctx.state.modules.some(m=>m.id===id));
    if(ctx.state.faultPathRendered===ctx.state.latchedFault)return;
    ctx.state.faultPathRendered=ctx.state.latchedFault;const list=ctx.dom.$('#faultPathList');list.replaceChildren();
    if(!short)return;
    if(!trace){const li=document.createElement('li');li.textContent='Der genaue Leitungsweg konnte nicht ermittelt werden. Betroffene Bauteile: '+(ctx.state.latchedFault.componentIds||[]).join(', ');list.append(li);return;}
    trace.steps.forEach(step=>{
      const li=document.createElement('li'),button=document.createElement('button');button.type='button';
      button.textContent=step.a+' → '+step.b;
      const kind=document.createElement('small');kind.textContent=step.kind==='wire'?'Leitung auswählen':'Interne Verbindung · beim Fehler leitend';
      button.append(kind);button.onclick=()=>{
        if(step.kind==='wire'){const wire=ctx.state.wires.find(w=>ctx.services.simulation.faultWireKey(w)===ctx.services.simulation.faultWireKey(step));if(wire){ctx.services.editor.selectWire(wire);ctx.services.editor.showFaultLocation();}else ctx.services.editor.say('Diese Leitung wurde bereits entfernt oder geändert.');}
        else{const m=ctx.state.modules.find(x=>x.id===step.id);if(m)ctx.services.editor.selectModule(m.id,true);else ctx.services.editor.say('Dieses Bauteil wurde bereits entfernt.');}
      };li.append(button);list.append(li);
    });
  }
  function showFaultLocation(){
    if(!ctx.state.latchedFault)return;
    if(ctx.state.viewMode!=='stick')ctx.services.editor.setView('stick');
    ctx.dom.workspace.classList.remove('hide-inspector');ctx.services.editor.inspectorLabel();
    const affected=ctx.state.modules.filter(m=>(ctx.state.latchedFault.componentIds||[]).includes(m.id));
    if(!affected.length)return;
    const left=Math.min(...affected.map(m=>m.x)),top=Math.min(...affected.map(m=>m.y));
    const right=Math.max(...affected.map(m=>m.x+m.el.offsetWidth)),bottom=Math.max(...affected.map(m=>m.y+m.el.offsetHeight));
    ctx.state.scale=Math.max(.15,Math.min(1.4,(ctx.dom.viewport.clientWidth-80)/(right-left),(ctx.dom.viewport.clientHeight-80)/(bottom-top)));
    ctx.state.camera={x:ctx.dom.viewport.clientWidth/2-(left+right)/2*ctx.state.scale,y:ctx.dom.viewport.clientHeight/2-(top+bottom)/2*ctx.state.scale};ctx.services.editor.applyView();ctx.state.viewReady=true;ctx.services.projekt.schedulePersist();
    ctx.dom.$('#faultDiagnosis').scrollIntoView({block:'nearest'});
  }
  function applyView(){
    const pad=48,w=ctx.dom.viewport.clientWidth,h=ctx.dom.viewport.clientHeight;
    ctx.state.camera.x=Math.min(pad,Math.max(w-ctx.config.BOARD_W*ctx.state.scale-pad,ctx.state.camera.x));
    ctx.state.camera.y=Math.min(pad,Math.max(h-ctx.config.BOARD_H*ctx.state.scale-pad,ctx.state.camera.y));
    ctx.dom.board.style.setProperty('--scale',ctx.state.scale);ctx.dom.board.style.setProperty('--pan-x',ctx.state.camera.x+'px');ctx.dom.board.style.setProperty('--pan-y',ctx.state.camera.y+'px');
    ctx.dom.$('#zoom').options[0].textContent=Math.round(ctx.state.scale*100)+' %';ctx.dom.$('#zoom').value='custom';
    ctx.dom.$('#zoomOut').disabled=ctx.state.scale<=.15;ctx.dom.$('#zoomIn').disabled=ctx.state.scale>=2.5;
  }
  function zoomAt(value,x=ctx.dom.viewport.clientWidth/2,y=ctx.dom.viewport.clientHeight/2){
    const world={x:(x-ctx.state.camera.x)/ctx.state.scale,y:(y-ctx.state.camera.y)/ctx.state.scale};
    ctx.state.scale=Math.max(.15,Math.min(2.5,value));ctx.state.camera={x:x-world.x*ctx.state.scale,y:y-world.y*ctx.state.scale};ctx.services.editor.applyView();
  }
  function fit(){
    const left=ctx.state.modules.length?Math.min(...ctx.state.modules.map(m=>m.x)):24,top=ctx.state.modules.length?Math.min(...ctx.state.modules.map(m=>m.y)):61;
    const right=ctx.state.modules.length?Math.max(...ctx.state.modules.map(m=>m.x+m.el.offsetWidth)):1700,bottom=ctx.state.modules.length?Math.max(...ctx.state.modules.map(m=>m.y+m.el.offsetHeight)):780;
    ctx.state.scale=Math.max(.15,Math.min(1.2,(ctx.dom.viewport.clientWidth-72)/(right-left),(ctx.dom.viewport.clientHeight-72)/(bottom-top)));
    ctx.state.camera={x:(ctx.dom.viewport.clientWidth-(right-left)*ctx.state.scale)/2-left*ctx.state.scale,y:(ctx.dom.viewport.clientHeight-(bottom-top)*ctx.state.scale)/2-top*ctx.state.scale};
    ctx.services.editor.applyView();ctx.state.viewReady=true;
  }
  function comfortView(){
    const left=ctx.state.modules.length?Math.min(...ctx.state.modules.map(m=>m.x)):24,top=ctx.state.modules.length?Math.min(...ctx.state.modules.map(m=>m.y)):61;
    const right=ctx.state.modules.length?Math.max(...ctx.state.modules.map(m=>m.x+(m.el?m.el.offsetWidth:240))):1700;
    const bottom=ctx.state.modules.length?Math.max(...ctx.state.modules.map(m=>m.y+(m.el?m.el.offsetHeight:200))):780;
    const fitScale=Math.min((ctx.dom.viewport.clientWidth-28)/Math.max(1,right-left),(ctx.dom.viewport.clientHeight-28)/Math.max(1,bottom-top));
    ctx.state.scale=Math.max(.9,Math.min(1.35,fitScale));
    ctx.state.camera={x:16-left*ctx.state.scale,y:12-top*ctx.state.scale};
    ctx.services.editor.applyView();ctx.state.viewReady=true;
  }
  function layoutView(){if(matchMedia('(max-width:1480px)').matches)ctx.services.editor.comfortView();else ctx.services.editor.fit();}
  function reveal(m){
    const w=m.el.offsetWidth*ctx.state.scale,h=m.el.offsetHeight*ctx.state.scale;
    if(w>ctx.dom.viewport.clientWidth-40||h>ctx.dom.viewport.clientHeight-40)ctx.state.scale=Math.min(ctx.state.scale,(ctx.dom.viewport.clientWidth-40)/m.el.offsetWidth,(ctx.dom.viewport.clientHeight-40)/m.el.offsetHeight);
    ctx.state.camera={x:ctx.dom.viewport.clientWidth/2-(m.x+m.el.offsetWidth/2)*ctx.state.scale,y:ctx.dom.viewport.clientHeight/2-(m.y+m.el.offsetHeight/2)*ctx.state.scale};ctx.services.editor.applyView();
  }
  function startSelectionRectangle(e){
    ctx.services.editor.cancel();const point=ctx.services.editor.localPoint(e);ctx.state.gesture={kind:'selection',pointerId:e.pointerId,start:point,end:point,base:e.shiftKey?[...ctx.state.selectedIds]:[],moved:false};
    ctx.dom.viewport.setPointerCapture(e.pointerId);ctx.services.editor.paintSelectionRectangle(e);
  }
  function paintSelectionRectangle(e){
    const g=ctx.state.gesture;if(g?.kind!=='selection')return;g.end=ctx.services.editor.localPoint(e);g.moved ||= Math.hypot(g.end.x-g.start.x,g.end.y-g.start.y)>3;
    ctx.dom.selectionRectangle.hidden=!g.moved;Object.assign(ctx.dom.selectionRectangle.style,{left:Math.min(g.start.x,g.end.x)+'px',top:Math.min(g.start.y,g.end.y)+'px',width:Math.abs(g.start.x-g.end.x)+'px',height:Math.abs(g.start.y-g.end.y)+'px'});
  }
  function finishSelectionRectangle(e){
    ctx.services.editor.paintSelectionRectangle(e);const g=ctx.state.gesture;ctx.state.gesture=null;ctx.dom.selectionRectangle.hidden=true;ctx.state.ignorePanClick=true;
    const l=Math.min(g.start.x,g.end.x),r=Math.max(g.start.x,g.end.x),t=Math.min(g.start.y,g.end.y),b=Math.max(g.start.y,g.end.y);
    const picked=g.moved?ctx.state.modules.filter(m=>(m.type==='junction'||m.type==='note'&&m.noteStyle==='frame')?m.x>=l&&m.y>=t&&m.x+m.el.offsetWidth<=r&&m.y+m.el.offsetHeight<=b:m.x<r&&m.x+m.el.offsetWidth>l&&m.y<b&&m.y+m.el.offsetHeight>t).map(m=>m.id):[];
    ctx.services.editor.setSelection([...new Set([...g.base,...picked])]);
    if(picked.length)ctx.services.editor.say(ctx.state.selectedIds.size+' Bauteile ausgewählt.');
  }
  function boardLocal(clientX,clientY){const r=ctx.dom.viewport.getBoundingClientRect();return {x:clientX-r.left,y:clientY-r.top};}
  function pinchGeometry(){
    const pts=[...ctx.state.boardPointers.values()];
    if(pts.length<2)return null;
    return {dist:Math.hypot(pts[0].x-pts[1].x,pts[0].y-pts[1].y)||1,mid:ctx.services.editor.boardLocal((pts[0].x+pts[1].x)/2,(pts[0].y+pts[1].y)/2)};
  }
  function startPinch(){
    const geo=ctx.services.editor.pinchGeometry();
    if(!geo)return;
    ctx.services.editor.cancel();
    ctx.state.pinch={dist:geo.dist,scale: ctx.state.scale,worldX:(geo.mid.x-ctx.state.camera.x)/ctx.state.scale,worldY:(geo.mid.y-ctx.state.camera.y)/ctx.state.scale};
    ctx.state.ignorePanClick=true;ctx.dom.viewport.classList.add('panning');
  }
  function movePinch(){
    const geo=ctx.services.editor.pinchGeometry();
    if(!ctx.state.pinch||!geo)return;
    ctx.state.scale=Math.max(.15,Math.min(2.5,ctx.state.pinch.scale*geo.dist/ctx.state.pinch.dist));
    ctx.state.camera={x:geo.mid.x-ctx.state.pinch.worldX*ctx.state.scale,y:geo.mid.y-ctx.state.pinch.worldY*ctx.state.scale};
    ctx.services.editor.applyView();
  }
  function handleBoardPointerDown(e){
    if(e.pointerType==='mouse'&&e.button!==0&&e.button!==1)return;
    ctx.state.ignorePanClick=false;
    ctx.state.boardPointers.set(e.pointerId,{x:e.clientX,y:e.clientY});
    if(ctx.state.boardPointers.size>=2){ctx.services.editor.startPinch();try{ctx.dom.viewport.setPointerCapture(e.pointerId);}catch(err){}e.preventDefault();e.stopPropagation();return;}
    const part=e.target.closest('.module'),blank=!e.target.closest('.module,.wire-hit,.lead-hit,.lead-probe-hit');
    if(e.button===0&&!ctx.state.spaceHeld&&part&&e.target.closest('.remove,[data-note-resize]'))return;
    if(e.button===0&&!ctx.state.spaceHeld&&!ctx.state.handMode&&(e.shiftKey||ctx.state.selectionMode)){
      if(part){
        if(ctx.state.selectionMode&&!e.shiftKey&&ctx.state.selectedIds.has(part.dataset.id)&&ctx.services.editor.moduleDragTarget(e))ctx.services.editor.startModuleDrag(ctx.state.modules.find(m=>m.id===part.dataset.id),e,true,true);
        else{ctx.services.editor.cancel();ctx.services.editor.toggleSelection(part.dataset.id);ctx.state.ignorePanClick=true;e.preventDefault();}
        e.stopPropagation();return;
      }
      if(blank){ctx.services.editor.startSelectionRectangle(e);e.preventDefault();e.stopPropagation();return;}
    }
    if(e.button===0&&ctx.state.handMode&&!ctx.state.spaceHeld&&part){
      if(!ctx.services.editor.moduleDragTarget(e))return;
      const m=ctx.state.modules.find(m=>m.id===part.dataset.id);
      if(e.target.closest('.handle,.junction-edge')||m?.type==='connector'&&!m.showBanner&&e.target.closest('.lever-body')||ctx.state.selectedIds.has(part.dataset.id)&&e.target.closest('.note-content')){
        ctx.services.editor.startModuleDrag(m,e,false,true);e.stopPropagation();return;
      }
    }
    if(!blank&&!ctx.state.handMode&&!ctx.state.spaceHeld&&e.button!==1)return;
    ctx.services.editor.cancel();ctx.state.ignorePanClick=false;ctx.state.viewDrag={id:e.pointerId,x:e.clientX,y:e.clientY,cx:ctx.state.camera.x,cy:ctx.state.camera.y,moved:false,blank:blank&&e.button===0&&!ctx.state.handMode&&!ctx.state.spaceHeld};
    ctx.dom.viewport.setPointerCapture(e.pointerId);ctx.dom.viewport.classList.add('panning');e.preventDefault();e.stopPropagation();
  }
  function endBoardPointer(e){
    ctx.state.boardPointers.delete(e.pointerId);
    if(ctx.state.boardPointers.size<2&&ctx.state.pinch){ctx.state.pinch=null;ctx.services.projekt.schedulePersist();}
    if(!ctx.state.viewDrag||e.pointerId!==ctx.state.viewDrag.id){if(!ctx.state.viewDrag&&!ctx.state.pinch)ctx.dom.viewport.classList.remove('panning');return;}
    const drop=ctx.state.viewDrag.blank&&!ctx.state.viewDrag.moved;ctx.state.ignorePanClick=ctx.state.viewDrag.moved||ctx.state.handMode||ctx.state.spaceHeld;if(ctx.state.viewDrag.moved)ctx.services.projekt.schedulePersist();ctx.state.viewDrag=null;if(drop)ctx.services.editor.clearPick();
    ctx.dom.viewport.classList.remove('panning');e.preventDefault();e.stopPropagation();
  }
  function suppressPanClick(e) {if(ctx.state.ignorePanClick){e.stopPropagation();e.preventDefault();ctx.state.ignorePanClick=false;}
  }
  const editable = e=>e.target.closest('input,textarea,select,button,[contenteditable=true]');
  function duplicateSelection() {ctx.services.projekt.pastePackage(ctx.services.projekt.copySelection());
  }
  function deleteSelection() {ctx.services.editor.removeSelection();
  }
  function clearSelectionAction() {ctx.services.editor.cancel();ctx.services.editor.clearPick();
  }
  function inspectorLabel(){const shown=!ctx.dom.workspace.classList.contains('hide-inspector');const btn=ctx.dom.$('#toggleInspector');const text=shown?'Eigenschaften ausblenden':'Eigenschaften einblenden';btn.title=text;btn.setAttribute('aria-label',text);btn.setAttribute('aria-expanded',String(shown));btn.querySelector('.inspector-tab-chevron').style.transform=shown?'':'scaleX(-1)';}
  function bindFold(btnId,panelId){const btn=ctx.dom.$('#'+btnId),panel=ctx.dom.$('#'+panelId);btn.onclick=()=>{const open=btn.getAttribute('aria-expanded')!=='true';btn.setAttribute('aria-expanded',String(open));panel.hidden=!open;};}
  const openWorkshopHelp = ()=>ctx.dom.$('#helpDialog').showModal();
  function closePalettes(){}
  function partsLabel(){
    const shown=!ctx.dom.workspace.classList.contains('hide-parts');
    const text=shown?'Bauteile ausblenden':'Bauteile einblenden';
    [ctx.dom.$('#toggleParts'),ctx.dom.$('#partsTab')].forEach(btn=>{btn.title=text;btn.setAttribute('aria-label',text);btn.setAttribute('aria-expanded',String(shown));});
    ctx.dom.$('#partsTab').querySelector('.parts-tab-chevron').style.transform=shown?'scaleX(-1)':'';
  }
  function toggleParts(){ctx.dom.workspace.classList.toggle('hide-parts');ctx.services.editor.partsLabel();}
  function partButton(type,title,hint){return '<button type="button" class="add" data-add="'+type+'" title="'+hint+'"><i class="glyph" aria-hidden="true"><svg viewBox="0 0 28 28">'+ctx.config.newGlyph[type]+'</svg></i>'+title+'</button>';}
  function partGroup(title,entries){
    const group=document.createElement('div');group.className='add-set';group.setAttribute('role','group');group.setAttribute('aria-label',title);
    group.innerHTML='<span class="tool-label">'+title+'</span><div class="add-row">'+entries.map(entry=>ctx.services.editor.partButton(...entry)).join('')+'</div>';return group;
  }
  const textEditing = e=>e.target.closest('input,textarea,select,[contenteditable]:not([contenteditable=false])');
  function handleKeyDown(e) {
    if(e.isComposing||e.defaultPrevented)return;
    if(document.querySelector('dialog[open]'))return;
    if(e.key==='Escape'){ctx.services.editor.closePalettes();ctx.services.editor.closeDevMenu();ctx.services.editor.cancel();if(!ctx.services.editor.textEditing(e))ctx.services.editor.clearPick();return;}
    if(ctx.services.editor.textEditing(e)||document.querySelector('dialog[open]'))return;
    const modifier=e.ctrlKey||e.metaKey,key=e.key.toLowerCase(),m=ctx.state.modules.find(x=>x.id===ctx.state.selectedId);
    if(modifier&&!e.altKey){
      if(key==='z'||key==='y'){e.preventDefault();if(!e.repeat)(key==='y'||e.shiftKey?ctx.dom.$('#redo'):ctx.dom.$('#undo')).click();}
      else if(key==='s'){e.preventDefault();if(!e.repeat)ctx.services.projekt.saveProjectFile();}
      else if(key==='c'&&m){e.preventDefault();if(!e.repeat){ctx.state.componentClipboard=ctx.services.projekt.copySelection();ctx.state.clipboardPasteCount=0;ctx.services.editor.say(ctx.state.componentClipboard.modules.length+' Bauteile mit internen Leitungen kopiert.');}}
      else if(key==='v'&&ctx.state.componentClipboard){e.preventDefault();if(!e.repeat)ctx.services.projekt.pastePackage(ctx.state.componentClipboard,40*(++ctx.state.clipboardPasteCount));}
      else if(key==='d'&&m){e.preventDefault();if(!e.repeat)ctx.services.projekt.pastePackage(ctx.services.projekt.copySelection());}
      else if(key==='a'){e.preventDefault();if(!e.repeat)ctx.services.editor.setSelection(ctx.state.modules.map(m=>m.id));}
      return;
    }
    const delta={ArrowLeft:[-10,0],ArrowRight:[10,0],ArrowUp:[0,-10],ArrowDown:[0,10]}[e.key];
    if(delta&&ctx.state.selectedIds.size&&!e.altKey){e.preventDefault();ctx.services.editor.moveSelection(...delta,e.repeat);return;}
    if(e.key==='Delete'&&!e.altKey){
      if(ctx.state.selectedWire){e.preventDefault();if(!e.repeat)ctx.services.editor.removeWire(ctx.state.wires.indexOf(ctx.state.selectedWire));}
      else if(m){e.preventDefault();if(!e.repeat)ctx.services.editor.removeSelection();}
    }

  }
  function renderSnapshot(snap) {
    ctx.services.editor.renderSimControls();
    ctx.services.editor.paintMeters(snap);
    const coils=snap.coils||{}, motors=snap.motors||{}, contactsSnap=snap.contacts||{};
    ctx.state.modules.forEach(m=>{
      const label=m.el.querySelector('.module-state');
      if(m.type==='motion')ctx.services.editor.paintMotion(m,snap);
      if(ctx.services.editor.isProtection(m))ctx.services.editor.paintProtection(m,snap.protections?.[m.id]);
      if(m.type==='timer')ctx.services.editor.paintTimer(m,snap);
      if(m.type==='contactor'||m.type==='auxiliary'||m.type==='relay'){
        const id=m.type==='auxiliary'?m.parent:m.id;
        const coil=coils[id]||{};
        const on=ctx.services.simulation.plantLive()&&!!coil.energized;
        m.el.classList.toggle('energized',on);
        if(label)label.textContent=on?'Angezogen':'Abgefallen';
      }
      if(ctx.services.editor.isLight(m)){const on=ctx.services.editor.displayLampOn(m,snap);m.el.classList.toggle('lit',on);if(label)label.textContent=on?'Leuchtet':'Aus';}
      if(ctx.services.editor.isInstallationSwitch(m))ctx.services.editor.paintInstallationSwitch(m);
      if(m.type==='selector')ctx.services.editor.paintSelector(m);
      if(m.type==='lineardrive')ctx.services.editor.paintDrive(m,snap);
      if(m.type==='motor'){
        const st=motors[m.id]||{};
        m.el.classList.toggle('running',ctx.services.simulation.plantLive()&&!!st.running);
        m.el.classList.toggle('reverse',ctx.services.simulation.plantLive()&&!!st.reverse);
        if(label)label.textContent=!ctx.services.simulation.plantLive()?'Stillstand':st.reverse?'Linkslauf':st.running?'Rechtslauf':'Stillstand';
      }
      if(m.type==='acmotor'){
        const st=motors[m.id]||{};
        const on=ctx.services.simulation.plantLive()&&!!st.running;
        m.el.classList.toggle('running',on);
        m.el.classList.toggle('reverse',ctx.services.simulation.plantLive()&&!!st.reverse);
        const wire=st.wiring?(' · '+st.wiring):'';
        if(label)label.textContent=!ctx.services.simulation.plantLive()?'Stillstand':(st.status||'Stillstand')+wire;
      }
      if(m.type==='psu'){
        const snapPsu=(snap.supplies||{})[m.id]||{};
        const shown=ctx.state.powered?(Number.isFinite(snapPsu.voltage)?snapPsu.voltage:m.voltage):0;
        const disp=m.el.querySelector('.supply-display');
        if(disp)disp.innerHTML=ctx.services.editor.fmtV(shown)+' <small>V</small>';
        if(label)label.textContent=ctx.state.powered?(snap.running?'Eingeschaltet':'Bereit'):'Aus';
      }
      if(m.type==='mains'){
        const shown=ctx.state.powered?m.voltage:0;
        const disp=m.el.querySelector('.supply-display');
        if(disp)disp.innerHTML=ctx.services.editor.fmtV(shown)+' <small>V</small>';
        const ln=m.el.querySelector('.supply-ln');
        if(ln)ln.textContent=ctx.state.powered?('U_LN '+ctx.services.editor.fmtV(m.voltage/Math.sqrt(3))+' V · 50 Hz'):'Aus';
        if(label)label.textContent=ctx.state.powered?(snap.running?'3~ 50 Hz':'Bereit'):'Aus';
      }
      if(m.type==='start'||m.type==='stop'||m.type==='emergency'){
        m.el.classList.toggle('pressed',!!m.pressed);
        const lamp=(snap.lamps||{})[m.id]||{};
        m.el.classList.toggle('lit',ctx.services.editor.displayLampOn(m,snap));
        if(m.type==='emergency')ctx.services.editor.paintTaster(m);else if(label)label.textContent=m.pressed?'NO zu · NC auf':'NO auf · NC zu';
      }
    });
    const pulled=ctx.state.modules.filter(x=>x.type==='contactor'&&coils[x.id]?.energized).map(x=>x.id);
    ctx.services.simulation.flushProtection();
    ctx.dom.$('#simStatus').textContent=ctx.state.latchedFault?ctx.services.simulation.getFaultPresentation(ctx.state.latchedFault).status:ctx.state.simMode==='aufbau'?'Aufbau · Spannung aus':ctx.state.simMode==='paused'?(pulled.length?pulled.join(', ')+' angezogen · eingefroren':'Eingefroren · Spannung bleibt'):!ctx.state.powered?'Betrieb · Spannung aus':pulled.length?'Betrieb · '+pulled.join(', ')+' angezogen':'Betrieb · Spannung an';
    ctx.dom.$('#statusDot').className='status-dot'+(ctx.state.latchedFault?' error':!ctx.state.powered||ctx.state.simMode==='aufbau'||!snap.running?' off':'');
    const flag=ctx.dom.$('#modeFlag');
    if(flag)flag.textContent=ctx.state.simMode==='aufbau'?'Spannung AUS':ctx.state.simMode==='paused'?'Eingefroren · AN':'Spannung AN';
    ctx.services.editor.paintWireVoltage(snap);
    ctx.services.editor.paintWireCurrent(snap);
    void contactsSnap;
  }
  function bindMoveKeys(target,m) {if(!target)return;target.onkeydown=e=>{
      if(e.ctrlKey||e.metaKey||e.altKey||e.target.closest('.term'))return;
      if(m.type==='connector'&&(e.key==='Enter'||e.key===' ')){e.preventDefault();e.stopPropagation();ctx.services.editor.selectModule(m.id);ctx.services.editor.openSettings(m);return;}
      if(m.type==='note'&&['label','frame'].includes(m.noteStyle)&&(e.key==='Enter'||e.key===' ')){e.preventDefault();ctx.services.editor.selectModule(m.id);ctx.services.editor.openSettings(m);return;}
      const delta={ArrowLeft:[-10,0],ArrowRight:[10,0],ArrowUp:[0,-10],ArrowDown:[0,10]}[e.key];if(!delta)return;
      e.preventDefault();ctx.services.editor.selectModule(m.id);
      if(m.type==='note'&&ctx.state.selectedIds.size===1&&['label','frame'].includes(m.noteStyle)&&e.shiftKey){
        if(m.locked){ctx.services.editor.say('Position gesperrt. In den Eigenschaften entsperren.');return;}
        if(!e.repeat||!ctx.state.selectionKeyboard){ctx.services.projekt.remember();ctx.state.selectionKeyboard=true;}
        m.noteWidth=Math.max(120,Math.min(1600,m.noteWidth+delta[0]));m.noteHeight=Math.max(48,Math.min(1600,(m.noteHeight||Math.round(m.el.querySelector('.face').offsetHeight))+delta[1]));ctx.services.editor.paintDeviceFace(m);ctx.services.editor.move(m,m.x,m.y);
        if(ctx.dom.$('#devNoteWidth'))ctx.dom.$('#devNoteWidth').value=m.noteWidth;if(ctx.dom.$('#devNoteHeight'))ctx.dom.$('#devNoteHeight').value=m.noteHeight;ctx.services.editor.draw();ctx.services.projekt.schedulePersist();
      }else ctx.services.editor.moveSelection(...delta,e.repeat);
    };
  }

  function bindEvents() {
    if (bound) return;
    bound = true;
    document.addEventListener('keyup',e=>{if(e.key.startsWith('Arrow'))ctx.state.selectionKeyboard=false;});
    window.addEventListener('blur',()=>{ctx.state.selectionKeyboard=false;});
    document.addEventListener('pointermove',ctx.services.editor.handleDocumentPointerMove);
    document.addEventListener('pointerup',e=>{
    if(!ctx.state.gesture||e.pointerId!==ctx.state.gesture.pointerId)return;
    const g=ctx.state.gesture;
    if(g.kind==='module'){ctx.services.editor.finishModuleDrag();return;}
    if(g.kind==='selection'){ctx.services.editor.finishSelectionRectangle(e);return;}
    if(g.kind==='resize-note'){ctx.state.ignorePanClick=true;ctx.state.gesture=null;g.m.el.classList.remove('resizing');ctx.services.projekt.schedulePersist();return;}
    if(g.kind==='lead'){ctx.state.gesture=null;ctx.services.editor.finishLead(g,e);return;}
    if(g.moved){
      const target=document.elementFromPoint(e.clientX,e.clientY)?.closest('.term');
      if(target&&ctx.services.editor.isMeasurementPoint(target.dataset.key)){ctx.services.editor.cancel();ctx.services.editor.measurementPointHint();return;}
      const snap=ctx.services.editor.nearestTerm(e.clientX,e.clientY,g.key);
      const dest=(snap&&snap.dataset.key)||g.target;
      if(dest&&ctx.services.editor.canConnect(g.key,dest))ctx.services.editor.connect(g.key,dest);
      else ctx.services.editor.say('Leitung auf einer Schraubklemme loslassen.');
      ctx.services.editor.cancel();
      return;
    }
    ctx.state.gesture=null;
    ctx.services.editor.choose(g.key);
  });
    document.addEventListener('pointercancel',ctx.services.editor.cancel);
    ctx.dom.viewport.addEventListener('pointerleave',()=>{if(ctx.state.gesture?.kind==='wire')return;ctx.state.wireTarget=null;ctx.services.editor.mark();});
    document.addEventListener('pointerout',e=>{if(ctx.state.gesture?.kind==='wire'||e.relatedTarget)return;ctx.state.wireTarget=null;ctx.services.editor.mark();});
    window.addEventListener('blur',()=>{ctx.services.editor.cancel();ctx.state.modules.forEach(m=>{if(!m.pressed||m.type==='emergency'||m.latching)return;m.pressed=false;if(ctx.state.engine)ctx.state.engine.setPressed(m.id,false);ctx.services.editor.paintTaster(m);});if(ctx.state.engineOk)ctx.services.simulation.applySnapshot();});
    ctx.dom.viewport.addEventListener('wheel',e=>{
    e.preventDefault();if(ctx.state.gesture||ctx.state.viewDrag)return;
    const r=ctx.dom.viewport.getBoundingClientRect(),units=e.deltaMode===1?16:e.deltaMode===2?ctx.dom.viewport.clientHeight:1;
    ctx.services.editor.zoomAt(ctx.state.scale*Math.exp(-Math.max(-300,Math.min(300,e.deltaY*units))*.002),e.clientX-r.left,e.clientY-r.top);
  },{passive:false});
    ctx.dom.selectionRectangle.className='selection-rectangle';
    ctx.dom.selectionRectangle.hidden=true;
    ctx.dom.selectionRectangle.setAttribute('aria-hidden','true');
    ctx.dom.board.append(ctx.dom.selectionRectangle);
    ctx.dom.viewport.addEventListener('pointerdown',ctx.services.editor.handleBoardPointerDown,true);
    ctx.dom.viewport.addEventListener('pointermove',e=>{
    if(ctx.state.boardPointers.has(e.pointerId))ctx.state.boardPointers.set(e.pointerId,{x:e.clientX,y:e.clientY});
    if(ctx.state.pinch&&ctx.state.boardPointers.size>=2){ctx.services.editor.movePinch();e.preventDefault();e.stopPropagation();return;}
    if(!ctx.state.viewDrag||e.pointerId!==ctx.state.viewDrag.id)return;e.stopPropagation();
    const dx=e.clientX-ctx.state.viewDrag.x,dy=e.clientY-ctx.state.viewDrag.y;ctx.state.viewDrag.moved ||= Math.hypot(dx,dy)>3;
    ctx.state.camera={x:ctx.state.viewDrag.cx+dx,y:ctx.state.viewDrag.cy+dy};ctx.services.editor.applyView();
  },true);
    ctx.dom.viewport.addEventListener('pointerup',ctx.services.editor.endBoardPointer,true);
    ctx.dom.viewport.addEventListener('pointercancel',ctx.services.editor.endBoardPointer,true);
    ctx.dom.viewport.addEventListener('lostpointercapture',e=>{if(e.pointerId!=null)ctx.state.boardPointers.delete(e.pointerId);if(ctx.state.boardPointers.size<2)ctx.state.pinch=null;if(ctx.state.viewDrag&&e.pointerId===ctx.state.viewDrag.id){ctx.state.viewDrag=null;ctx.dom.viewport.classList.remove('panning');}});
    ctx.dom.viewport.addEventListener('click',ctx.services.editor.suppressPanClick,true);
    ctx.dom.viewport.addEventListener('auxclick',e=>{if(e.button===1)e.preventDefault();});
    document.addEventListener('keydown',e=>{if(e.code==='Space'&&!ctx.services.editor.editable(e)){e.preventDefault();ctx.state.spaceHeld=true;ctx.dom.viewport.classList.add('pan-mode');}});
    document.addEventListener('keyup',e=>{if(e.code==='Space'){ctx.state.spaceHeld=false;ctx.dom.viewport.classList.toggle('pan-mode',ctx.state.handMode);}});
    window.addEventListener('blur',()=>{ctx.state.spaceHeld=false;ctx.dom.viewport.classList.toggle('pan-mode',ctx.state.handMode);});
    ctx.dom.$('#panTool').onclick=()=>{ctx.services.editor.cancel();ctx.state.selectionMode=false;ctx.dom.$('#selectionTool').setAttribute('aria-pressed','false');ctx.dom.viewport.classList.remove('selection-mode');ctx.state.handMode=!ctx.state.handMode;ctx.dom.$('#panTool').setAttribute('aria-pressed',String(ctx.state.handMode));ctx.dom.viewport.classList.toggle('pan-mode',ctx.state.handMode||ctx.state.spaceHeld);};
    ctx.dom.$('#selectionTool').onclick=()=>{
    ctx.services.editor.cancel();ctx.state.selectionMode=!ctx.state.selectionMode;ctx.state.handMode=false;ctx.dom.$('#panTool').setAttribute('aria-pressed','false');ctx.dom.viewport.classList.remove('pan-mode');ctx.dom.viewport.classList.toggle('selection-mode',ctx.state.selectionMode);ctx.dom.$('#selectionTool').setAttribute('aria-pressed',String(ctx.state.selectionMode));
    ctx.services.editor.say(ctx.state.selectionMode?'Auswahl: Bauteile antippen oder ein Rechteck auf freier Fläche ziehen.':'Auswahlwerkzeug beendet.');
  };
    ctx.dom.$('#duplicateSelection').onclick=ctx.services.editor.duplicateSelection;
    ctx.dom.$('#deleteSelection').onclick=ctx.services.editor.deleteSelection;
    ctx.dom.$('#clearSelection').onclick=ctx.services.editor.clearSelectionAction;
    ctx.dom.$('#showVoltage').onclick=()=>ctx.services.editor.setShowVoltage(!ctx.state.showVoltage);
    ctx.dom.$('#showCurrent').onclick=()=>ctx.services.editor.setShowCurrent(!ctx.state.showCurrent);
    ctx.dom.$('#currentSpeed').oninput=e=>ctx.services.editor.setCurrentSpeed(e.target.value);
    ctx.dom.$('#zoomIn').onclick=()=>{ctx.services.editor.cancel();ctx.services.editor.zoomAt(ctx.state.scale*1.25);};
    ctx.dom.$('#zoomOut').onclick=()=>{ctx.services.editor.cancel();ctx.services.editor.zoomAt(ctx.state.scale/1.25);};
    ctx.dom.$('#fitScene').onclick=()=>{ctx.services.editor.cancel();ctx.services.editor.fit();};
    if(innerWidth<=1050)ctx.dom.workspace.classList.add('hide-inspector','hide-parts');
    ctx.dom.$('#toggleInspector').setAttribute('aria-controls','propertiesInspector');
    ctx.dom.$('#toggleInspector').onclick=()=>{ctx.dom.workspace.classList.toggle('hide-inspector');ctx.services.editor.inspectorLabel();};
    ctx.dom.$('#closeInspector').onclick=()=>ctx.dom.$('#toggleInspector').click();
    ctx.services.editor.inspectorLabel();
    ctx.services.editor.bindFold('wiresToggle','wirePanel');
    ctx.dom.$('#partSearch').addEventListener('focus',()=>ctx.services.editor.setComponentSearch(true));
    ctx.dom.$('#partSearch').addEventListener('click',()=>ctx.services.editor.setComponentSearch(true));
    ctx.dom.$('#partSearch').addEventListener('input',()=>ctx.services.editor.setComponentSearch(true));
    ctx.dom.$('#partsToggle').onclick=()=>{const open=ctx.dom.$('#partsPanel').hidden;if(open)ctx.dom.$('#partSearch').focus();ctx.services.editor.setComponentSearch(open);};
    document.addEventListener('pointerdown',e=>{if(!e.target.closest('#componentSearch'))ctx.services.editor.setComponentSearch(false);});
    ctx.dom.$('#componentSearch').addEventListener('keydown',e=>{
    if(e.key==='Escape'){e.preventDefault();e.stopPropagation();ctx.dom.$('#partSearch').focus();ctx.services.editor.setComponentSearch(false);return;}
    if(e.key!=='ArrowDown'&&e.key!=='ArrowUp')return;
    if(ctx.dom.$('#partsPanel').hidden)ctx.services.editor.setComponentSearch(true);
    const results=Array.from(ctx.dom.$('#componentList').querySelectorAll('.component-select'));
    if(!results.length)return;
    e.preventDefault();
    const index=results.indexOf(document.activeElement);
    results[index<0?(e.key==='ArrowDown'?0:results.length-1):Math.max(0,Math.min(results.length-1,index+(e.key==='ArrowDown'?1:-1)))].focus();
  });
    ctx.dom.$('#wiresSelection').onclick=()=>{ctx.state.wireScope='selection';ctx.services.editor.renderWires();};
    ctx.dom.$('#wiresAll').onclick=()=>{ctx.state.wireScope='all';ctx.services.editor.renderWires();};
    ctx.dom.$('#openHelp').onclick=ctx.services.editor.openWorkshopHelp;
    ctx.dom.$('#openBoardHelp').onclick=ctx.services.editor.openWorkshopHelp;
    ctx.dom.$('#closeHelp').onclick=()=>ctx.dom.$('#helpDialog').close();
    new ResizeObserver(()=>ctx.dom.workspace.style.setProperty('--viewbar-height',ctx.dom.$('.board-bar').offsetHeight+'px')).observe(ctx.dom.$('.board-bar'));
    matchMedia('(max-width:1050px)').addEventListener('change',e=>{ctx.dom.workspace.classList.toggle('hide-inspector',e.matches);ctx.dom.workspace.classList.toggle('hide-parts',e.matches);ctx.services.editor.inspectorLabel();ctx.services.editor.partsLabel();requestAnimationFrame(ctx.services.editor.layoutView);});
    document.querySelectorAll('.app-menu,.board-more').forEach(menu=>{
    menu.addEventListener('toggle',()=>{if(menu.open)document.querySelectorAll('.app-menu,.board-more').forEach(other=>{if(other!==menu)other.open=false;});});
    menu.querySelectorAll('button').forEach(button=>button.addEventListener('click',()=>{menu.open=false;}));
  });
    document.addEventListener('pointerdown',e=>{if(!e.target.closest('.app-menu,.board-more'))document.querySelectorAll('.app-menu,.board-more').forEach(menu=>{menu.open=false;});});
    document.addEventListener('keydown',e=>{if(e.key==='Escape')document.querySelectorAll('.app-menu,.board-more').forEach(menu=>{menu.open=false;});});
    ctx.dom.$('#toggleParts').onclick=ctx.services.editor.toggleParts;
    ctx.dom.$('#partsTab').onclick=ctx.services.editor.toggleParts;
    ctx.dom.$('#closeParts').onclick=ctx.services.editor.toggleParts;
    ctx.services.editor.partsLabel();
    ctx.dom.$('#partsFilter').addEventListener('input',()=>{
    const q=ctx.dom.$('#partsFilter').value.trim().toLowerCase();
    document.querySelectorAll('.parts-rail .add-set').forEach(set=>{
      let any=false;
      set.querySelectorAll('.add').forEach(b=>{
        const hit=!q||b.textContent.toLowerCase().includes(q);
        b.hidden=!hit;if(hit)any=true;
      });
      set.hidden=!any;
    });
  });
    document.querySelector('[data-add="start"]').insertAdjacentHTML('afterend',ctx.services.editor.partButton('switch','Schalter','Rastend · NO + NC'));
    document.querySelector('[data-add="lamp"]').closest('.add-set').before(ctx.services.editor.partGroup('Gebäudeinstallation',[
    ['changeover','Wechselschalter','Rastend · L auf 1 oder 2'],['crossover','Kreuzschalter','Zwei Korrespondierende kreuzen'],['junction','Abzweigdose','Leeres, frei skalierbares Gehäuse'],['socket','Steckdose · L / N / PE','Schuko · Messpunkte vorne, Anschlussklemmen unten']
  ]));
    document.querySelector('[data-add="lamp"]').closest('.add-set').before(ctx.services.editor.partGroup('Regler & Sensoren',[
    ['potentiometer','Potentiometer','Linear · 3 Anschlüsse'],['motion','Bewegungsmelder','PIR · 24 / 230 V']
  ]));
    document.querySelector('.parts-rail .add-set').parentElement.append(ctx.services.editor.partGroup('Beschriften',[
    ['note','Notiz','Text auf der Montageplatte'],['label','Beschriftung','Weißer Textstreifen mit freier Größe'],['frame','Rahmen','Gestrichelter Rahmen mit Text, frei größenveränderbar']
  ]));
    document.querySelector('[data-add="terminal"]').outerHTML=ctx.services.editor.partButton('terminal','Reihenklemmen V2','Einzelne Durchgangsklemmen mit steckbaren Brücken').replace('data-add="terminal"','data-add="terminal" data-terminal-version="2"');
    document.querySelectorAll('[data-add]').forEach(b=>b.onclick=()=>{
    ctx.services.editor.closePalettes();
    const requested=b.dataset.add;
    if(!['note','label','frame','junction','voltmeter','ammeter'].includes(requested)&&!ctx.services.simulation.requestElectricalEdit(()=>b.onclick()))return;
    ctx.services.projekt.remember();const type=['switch','label','frame'].includes(requested)?(requested==='switch'?'start':'note'):requested,prefix=ctx.services.editor.defaultPrefix(type);
    const m={id:ctx.services.editor.nextId(prefix),type,x:Math.max(24,Math.min(ctx.config.BOARD_W-510,(ctx.dom.viewport.clientWidth/2-ctx.state.camera.x)/ctx.state.scale-122)),y:Math.max(61,Math.min(ctx.config.BOARD_H-420,(ctx.dom.viewport.clientHeight/2-ctx.state.camera.y)/ctx.state.scale-170))};
    if(type==='connector')m.ports=Number(b.dataset.ports);
    if(type==='terminal'&&b.dataset.terminalVersion==='2'){m.terminalVersion=2;m.bridges=[];m.poles=6;}
    if(type==='junction')m.junctionVersion=2;
    if(requested==='switch'){m.latching=true;m.buttonColor='black';}
    if(requested==='label'){m.noteStyle='label';m.noteText='Beschriftung';}
    if(requested==='frame'){m.noteStyle='frame';m.noteText='Text';m.noteWidth=420;m.noteHeight=240;}
    if(type==='psu')m.voltage=24;if(type==='mains')m.voltage=400;
    if(type==='contactor'){m.nomV=24;m.nomP=4;}
    if(type==='motor'||type==='lineardrive'){m.nomV=24;m.nomP=50;}if(type==='acmotor'){m.ratedKw=1.5;m.eta=0.8;}
    if(type==='lineardrive'){m.x=Math.max(24,Math.min(ctx.config.BOARD_W-1224,m.x));m.slide=.5;}
    ctx.state.modules.push(m);ctx.services.editor.renderModule(m);if(type==='contactor')ctx.services.editor.createAux(m);ctx.services.editor.selectModule(m.id);ctx.services.simulation.update();
    if(matchMedia('(max-width:1050px)').matches){ctx.dom.workspace.classList.add('hide-parts');ctx.services.editor.partsLabel();}
    if(type==='note')ctx.services.editor.openSettings(m);
    ctx.services.editor.say(m.id+(type==='contactor'?' mit gekoppelten Hilfskontakten':'')+' hinzugefügt.');
  });
    ctx.dom.$('#addAux').onclick=()=>{const m=ctx.state.modules.find(m=>m.id===ctx.state.selectedId);const parent=m?.type==='contactor'?m:ctx.state.modules.find(x=>x.id===m?.parent);if(!parent||ctx.state.modules.some(x=>x.parent===parent.id))return;if(!ctx.services.simulation.requestElectricalEdit(()=>ctx.dom.$('#addAux').onclick()))return;ctx.services.projekt.remember();const aux=ctx.services.editor.createAux(parent);ctx.services.editor.selectModule(aux.id);ctx.services.simulation.update();};
    ctx.services.editor.mountCableButtons(ctx.dom.$('#wireColors'),ctx.state.color,value=>{ctx.state.color=value;ctx.services.editor.syncCableButtons(ctx.dom.$('#wireColors'),value);});
    ctx.dom.$('#zoom').onchange=()=>{if(ctx.dom.$('#zoom').value!=='custom'){const value=Number(ctx.dom.$('#zoom').value);ctx.services.editor.cancel();ctx.services.editor.zoomAt(value);}};
    new ResizeObserver(()=>{if(!ctx.state.viewReady)ctx.services.editor.layoutView();else ctx.services.editor.applyView();}).observe(ctx.dom.viewport);
    document.addEventListener('keydown',ctx.services.editor.handleKeyDown);
    ctx.dom.board.addEventListener('click',e=>{if(e.target===ctx.dom.board){ctx.services.editor.cancel();ctx.services.editor.clearPick();}});
    ctx.dom.$('#devCancel').hidden=true;
    ctx.dom.$('#devApply').hidden=true;
    ctx.dom.$('#devMenu').addEventListener('keydown',e=>{if(e.key==='Enter'&&ctx.state.editTarget&&e.target.closest('#devFields')){e.preventDefault();ctx.services.editor.commitLive(true);}});
    document.addEventListener('pointerdown',e=>{
    const menu=ctx.dom.$('#devMenu');
    if(!menu||menu.hidden)return;
    if(e.target.closest('#devMenu'))return;
  },true);
    ctx.dom.board.addEventListener('contextmenu',e=>{if(!e.target.closest('.module'))e.preventDefault();});
    ctx.dom.$('#viewCompare').onclick=()=>ctx.services.editor.setView('compare');
    ctx.dom.$('#viewStick').onclick=()=>ctx.services.editor.setView('stick');
    ctx.dom.$('#cancelWire').onclick=ctx.services.editor.cancel;
    (function splitDrag(){
    const appEl=ctx.dom.$('#app'), handle=ctx.dom.$('#split'), simPane=appEl.querySelector('.sim-pane');
    let dragging=false,startX=0,startFrac=0;
    const min=0.22,max=0.72;
    function frac(){const w=appEl.getBoundingClientRect().width;const sim=simPane.getBoundingClientRect().width;return Math.min(max,Math.max(min,sim/w));}
    handle.addEventListener('pointerdown',e=>{if(ctx.state.viewMode==='stick')return;dragging=true;startX=e.clientX;startFrac=frac();handle.setPointerCapture(e.pointerId);});
    handle.addEventListener('pointermove',e=>{if(!dragging)return;const w=appEl.getBoundingClientRect().width;ctx.state.splitFrac=Math.min(max,Math.max(min,startFrac+(e.clientX-startX)/w));ctx.services.editor.applySplitLayout();});
    handle.addEventListener('pointerup',()=>{dragging=false;});
    handle.addEventListener('keydown',e=>{if(ctx.state.viewMode==='stick')return;if(e.key==='ArrowLeft')ctx.state.splitFrac=frac()-0.02;if(e.key==='ArrowRight')ctx.state.splitFrac=frac()+0.02;if(e.key==='ArrowLeft'||e.key==='ArrowRight')ctx.services.editor.applySplitLayout();});
  })();
  }

  return {
    applySplitLayout,
    displayName,
    shortName,
    paletteIcon,
    paintDetailPreview,
    fmtV,
    isLight,
    isCombinedTaster,
    defaultPrefix,
    splitTag,
    nextId,
    rewriteTerm,
    remount,
    renameModule,
    rebuildAfterRename,
    tagFieldHtml,
    readTag,
    relayContactSet,
    terminalPoles,
    terminalSpan,
    terminalLinks,
    terminalGroupIndex,
    toggleTerminalBridge,
    connectorPorts,
    railScrews,
    terminalGroups,
    terminalCaption,
    railCaption,
    dropLoose,
    isProtection,
    componentTitle,
    defaultNom,
    isInstallationSwitch,
    moduleCaption,
    moduleSubtitle,
    badgeInk,
    badgeTone,
    paintHandle,
    paintDeviceFace,
    say,
    terminal,
    lampTag,
    dinNo,
    dinNc,
    dinLamp,
    driveSocket,
    driveFace,
    drivePulses,
    linkDrive,
    paintDrive,
    paintSelector,
    meterSpan,
    setLead,
    paintMeters,
    isMeter,
    relayPairs,
    paintProtection,
    paintTimer,
    paintPot,
    paintMotion,
    installationSwitchFace,
    paintInstallationSwitch,
    junctionFace,
    connectorFace,
    socketFace,
    face,
    renderModule,
    selection,
    moduleDragTarget,
    startModuleDrag,
    moveModuleDrag,
    finishModuleDrag,
    setSelection,
    toggleSelection,
    selectedMovable,
    moveEntries,
    moveSelection,
    arrangeSelection,
    renderGroupInspector,
    selectModule,
    removeModule,
    removeSelection,
    createAux,
    partKind,
    partBlob,
    inspectorRows,
    refreshInspectorRows,
    renderComponentResults,
    setComponentSearch,
    renderSelectionActions,
    renderComponents,
    selectWire,
    clearPick,
    clickWire,
    sameCable,
    cablePaint,
    cableStroke,
    peStripe,
    syncCableButtons,
    mountCableButtons,
    renderWireDetails,
    wireMeasure,
    renderWires,
    badgeSwatchHtml,
    applyBadgeColor,
    bindBadgeSwatches,
    colorField,
    bindColorSwatches,
    closeDevMenu,
    openSettings,
    renderTerminalColors,
    markPolePreset,
    bindLiveFields,
    queueLive,
    commitLive,
    applySettings,
    position,
    move,
    term,
    isMeasurementPoint,
    measurementPointHint,
    normalizeSocketWires,
    point,
    localPoint,
    wireSource,
    directlyConnected,
    canConnect,
    nearestTerm,
    updateWireTarget,
    choose,
    leadDock,
    markLeadDock,
    nearestLeadTerm,
    beginLead,
    finishLead,
    clampGroup,
    internallyCommon,
    connect,
    cancel,
    updateWirePrompt,
    mark,
    routeBetween,
    moduleBoxes,
    jogOf,
    exitPoint,
    tidyPoints,
    routePoints,
    orthoPath,
    segmentHits,
    spreadBundles,
    pathFor,
    path,
    removeWire,
    draw,
    leadOrigin,
    leadHang,
    leadCable,
    leadBanana,
    leadProbe,
    raiseLeads,
    drawLeads,
    acDisplayNetwork,
    lampPins,
    displayLampOn,
    displayWireCurrent,
    acPhaseLive,
    wireHasVoltage,
    paintWireVoltage,
    setShowVoltage,
    wireCurrentOf,
    currentDotAdvance,
    setCurrentSpeed,
    paintWireCurrent,
    setShowCurrent,
    paintTaster,
    setView,
    renderSimControls,
    renderFaultBanner,
    paintFaultScene,
    renderFaultPath,
    showFaultLocation,
    applyView,
    zoomAt,
    fit,
    comfortView,
    layoutView,
    reveal,
    startSelectionRectangle,
    paintSelectionRectangle,
    finishSelectionRectangle,
    boardLocal,
    pinchGeometry,
    startPinch,
    movePinch,
    handleBoardPointerDown,
    endBoardPointer,
    editable,
    inspectorLabel,
    bindFold,
    openWorkshopHelp,
    closePalettes,
    partsLabel,
    toggleParts,
    partButton,
    partGroup,
    textEditing,
    handleDocumentPointerMove,
    suppressPanClick,
    duplicateSelection,
    deleteSelection,
    clearSelectionAction,
    handleKeyDown,
    renderSnapshot,
    bindMoveKeys,
    bindEvents
  };
}
