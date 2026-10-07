(function(root){
  'use strict';
  class TorController {
    constructor(){this.api=null;this.snapshot=null;this.position=0;this.lastTime=null;this.obstacle=false;this.edge=false;this.stall=0;this.wiring={};}
    attach(sim){this.release();this.sim=sim;this.api=sim&&sim.tor;this.snapshot=null;this.lastTime=null;this.stall=0;this.position=0;this.realtimeReady=false;this.wiring={};}
    device(id){return this.snapshot&&this.snapshot.devices[id]||{};}
    action(id,action){return !!(this.api&&this.api.setDeviceAction(id,action));}
    release(){['S4','S5','S6'].forEach(id=>this.action('button:'+id,'release'));this.edge=false;}
    suspend(){this.release();this.lastTime=null;this.stall=0;}
    process(){return {position:this.position,obstacle:this.obstacle,edge:this.edge};}
    read(){
      if(!this.api)return null;
      this.api.setProcessState(this.process());this.snapshot=this.api.readSnapshot();
      if(!this.realtimeReady&&this.device('sps:A1').present&&this.sim.workbench){this.sim.workbench.setRealtime(true);this.realtimeReady=true;}
      return this.snapshot;
    }
    step(speed,obstacle,edge){
      this.obstacle=obstacle;this.edge=edge;
      if(!this.read())return this.position;
      const time=this.snapshot.time,dt=this.lastTime===null?0:Math.max(0,time-this.lastTime);
      this.lastTime=time;
      const m=this.device('motor:M1');
      if(this.snapshot.running&&m.present&&m.supplied&&m.running&&dt>0){
        const next=Math.max(0,Math.min(100,this.position+m.direction*speed*dt));
        const stalled=next===this.position;
        this.stall=stalled?this.stall+dt:0;
        this.position=next;
        if(this.stall>=1.5){this.action('protection:F1','trip');this.stall=0;}
      }else this.stall=0;
      this.api.setProcessState(this.process());
      return this.position;
    }
    reset(){this.release();if(this.api)this.api.reset();this.position=0;this.lastTime=null;this.obstacle=false;this.edge=false;this.stall=0;this.read();}
    render(doc){
      const get=id=>doc.getElementById(id),set=(id,text)=>{const e=get(id);if(e&&e.textContent!==text)e.textContent=text;};
      const p=this.device('sps:A1'),m=this.device('motor:M1'),f=this.device('protection:F1');
      const motor=!!(this.snapshot&&this.snapshot.running&&m.present&&m.supplied&&m.running),dir=motor?m.direction:0;
      const inputs=p.inputs||[],outputs=p.outputs||[];
      if(!p.present)this.wiring={};
      for(const key of ['inputDevices','outputDevices']){
        const rows=p[key];
        if(p.present&&Array.isArray(rows)&&rows.length===8&&rows.every(Array.isArray))this.wiring[key]=rows.map(row=>row.slice());
      }
      for(let i=1;i<=8;i++) {
        const names=this.wiring.inputDevices&&this.wiring.inputDevices[i-1];
        const label=!p.present?'SPS A1 fehlt':!names?'Zuordnung wird ermittelt':names.length?names.join('\n'):'Nicht zugeordnet';
        const inputId=i===3?'stopLabel':'inputFn'+i;set(inputId,label);get(inputId).title=label.replace(/\n/g, ', ');
        const loads=this.wiring.outputDevices&&this.wiring.outputDevices[i-1];
        const outputLabel=!p.present?'SPS A1 fehlt':i>p.outputCount?'Nicht vorhanden (8/4)':!loads?'Zuordnung wird ermittelt':loads.length?loads.join('\n'):'Nicht zugeordnet';
        set('outputFn'+i,outputLabel);get('outputFn'+i).title=outputLabel.replace(/\n/g, ', ');
      }
      for(const [id,name] of [['S4','Taster AUF'],['S5','Taster ZU'],['S6','Taster STOP'],['S0','NOT-HALT · Öffner'],['S1','S1 oben'],['S2','S2 unten'],['B1','B1 Lichtschranke'],['B2','B2 Sicherheitsleiste']]) {
        const channels=[];
        (this.wiring.inputDevices||[]).forEach((names,index)=>{if(names.some(label=>label.startsWith(id+' ')))channels.push('I'+(index+1));});
        set('deviceFn'+id,name+(channels.length?' · '+channels.join(', '):''));
      }
      for(let i=1;i<=8;i++)for(const [kind,states]of[['I',inputs],['Q',outputs]]){
        const e=get('led'+kind+i);e.className='dot'+(states[i-1]?' on-green':'');e.title=kind+i+': '+(states[i-1]?'EIN':'AUS');
      }
      ['Q1','Q2'].forEach(id=>{
        const coil=this.device('coil:'+id),e=get('tor'+id),on=!!coil.energized;
        e.classList.toggle('energized',on);e.setAttribute('aria-label',id+' Schütz '+(on?'angezogen':'abgefallen'));
        set('tor'+id+'State',on?'Angezogen':'Abgefallen');
        e.title=id+': Spule '+(on?'angezogen':'abgefallen')+' · Leistungskontakte '+(this.device('contacts:'+id).closed?'geschlossen':'offen');
      });
      get('door').style.height=(100-this.position*.86)+'%';
      get('door').classList.remove('past');
      set('kpiPos',Math.round(this.position)+' % '+(this.position>=99.5?'(OFFEN)':this.position<=.5?'(ZU)':''));
      const drive=get('drive');drive.classList.toggle('run',motor);drive.classList.toggle('auf',dir>0);drive.classList.toggle('zu',dir<0);drive.classList.remove('overheat');drive.classList.toggle('protection-trip',!!f.tripped);
      set('driveDir',motor?(dir>0?'Rechtslauf ↻':'Linkslauf ↺'):'Stillstand');
      get('torMotor').setAttribute('aria-label','Motor M1: '+get('driveDir').textContent);
      const moving=motor&&((dir>0&&this.position<100)||(dir<0&&this.position>0));
      get('torMotion').hidden=!moving;
      set('torMotionArrow',dir>0?'↑':'↓');set('torMotionText',dir>0?'Öffnet':'Schließt');
      get('driveDir').className=motor?(dir>0?'auf':'zu'):'';
      set('anlageState',motor?(dir>0?'Fährt AUF':'Fährt ZU'):this.position>=99.5?'Tor offen':this.position<=.5?'Tor geschlossen':'Stillstand');
      get('anlageState').className='v'+(motor?(dir>0?' state-auf':' state-zu'):this.position>=99.5?' state-offen':'');
      ['S1','S2'].forEach(id=>get('ls'+id).classList.toggle('on',!!this.device('sensor:'+id).actuated));
      get('pe').classList.toggle('cut',this.obstacle);
      const photo=this.device('sensor:B1');get('pe').classList.toggle('unpowered',!photo.supplied);
      for(const [id,button]of[['S4','btnAuf'],['S5','btnZu'],['S6','btnStop']]){
        const pressed=!!this.device('button:'+id).pressed;get(button).classList.toggle('active',pressed);get(button).setAttribute('aria-pressed',String(pressed));
      }
      const stopped=!!this.device('estop:S0').pressed;
      get('btnNotAus').classList.toggle('active',stopped);get('btnNotAus').setAttribute('aria-pressed',String(stopped));get('btnNotAusRelease').disabled=!stopped;
      ['P1','P2'].forEach(id=>get('tb'+id).classList.toggle('on',!!this.device('lamp:'+id).on));
      const traffic=get('ampel');traffic.className='ampel';
      ['P5','P3','P6'].forEach((id,i)=>traffic.children[i].classList.toggle('lit',!!this.device('lamp:'+id).on));
      const alarm=get('alarm');alarm.className='alarm';alarm.querySelector('.p4').classList.toggle('lit',!!this.device('lamp:P4').on);alarm.querySelector('.hupe').classList.toggle('lit',!!this.device('horn:H1').on);
      let msg=!this.api?'Warte auf Tor-Schnittstelle':!p.present?'SPS A1 fehlt':!p.supplied?'SPS spannungslos':!p.run?'SPS STOP':f.tripped?'Motorschutz F1 ausgelöst':stopped?'Not-Aus':outputs[0]&&!this.device('coil:Q1').energized?'A1.Q1 EIN · Q1 abgefallen':outputs[1]&&!this.device('coil:Q2').energized?'A1.Q2 EIN · Q2 abgefallen':(this.device('coil:Q1').energized||this.device('coil:Q2').energized)&&!motor?'Schütz angezogen · Motor steht':'Elektrische Simulation';
      set('sysMsg',msg);
      get('sysMsg').className='v'+(f.tripped||stopped?' state-fault':'');
      const errors=this.snapshot?this.snapshot.errors:[];get('torErrors').hidden=!errors.length;set('torErrors',errors.join(' · '));
    }
  }
  root.TorController=TorController;
  if(typeof module==='object'&&module.exports)module.exports=TorController;
})(typeof window==='object'?window:globalThis);
