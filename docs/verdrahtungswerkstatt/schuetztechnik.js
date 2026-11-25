import {savedSchuetzExamples} from './schuetz-vorlagen.js?v=20261125schuetzexport1';

const live = '#6b3f24';
const neutral = '#347caa';
const control = '#bb4234';
const switched = '#b98c24';
const phases = [live, '#384540', '#8d9296'];

function board(title, instruction) {
  const modules = [
    {id:'N1',type:'note',x:56,y:38,noteStyle:'label',noteSize:28,noteWidth:1300,noteText:title},
    {id:'N2',type:'note',x:56,y:89,noteStyle:'label',noteSize:17,noteWidth:1300,noteText:instruction}
  ];
  const wires = [];
  const part = (id,type,x,y,fields={}) => modules.push({id,type,x,y,...fields});
  const wire = (a,b,color=control) => wires.push({a,b,color});
  const path = (...points) => points.slice(1).forEach((point,index) => wire(points[index],point));
  return {part, wire, path, project:{modules,wires,powered:false}};
}

function mains(b) {
  b.part('U1','mains',56,192,{voltage:400});
}

function dc(b,y=192) {
  b.part('G1','psu',56,y,{voltage:24});
}

function breaker(b,id,x,y,poles=1,ratedCurrent=6) {
  b.part(id,'breaker',x,y,{poles,ratedCurrent,characteristic:poles===3?'D':'C',protectionOn:true});
}

function button(b,id,x,y,description,off=false) {
  b.part(id,off?'stop':'start',x,y,{description});
}

function contactor(b,id,x,y,description) {
  b.part(id,'contactor',x,y,{nomV:24,nomP:4,description});
}

function auxiliary(b,id,x,y,description) {
  b.part(id+'H','auxiliary',x,y,{parent:id,description});
}

function timer(b,x,y,mode,delayOn,delayOff) {
  b.part('K1','timer',x,y,{nomV:24,nomP:2,timerMode:mode,delayOn,delayOff});
  b.wire('K1.A2','G1.0V',neutral);
}

function lamp(b,id,x,y,ac=false) {
  b.part(id,ac?'lamp':'pilot',x,y,{nomV:ac?230:24,nomP:ac?60:2});
  b.wire(id+(ac?'.N':'.X2'),ac?'U1.N':'G1.0V',neutral);
  if(ac) b.wire(id+'.PE','U1.PE','pe');
}

function motor(b,x,y,star=true) {
  b.part('M1','acmotor',x,y,{ratedKw:1.5,eta:.8,nominalDelta:!star});
  b.wire('U1.PE','M1.PE','pe');
  if(star) {
    b.wire('M1.U2','M1.V2',phases[1]);
    b.wire('M1.V2','M1.W2',phases[1]);
  }
}

function threePhase(b,from,to,outputs,inputs) {
  outputs.forEach((port,i) => b.wire(from+'.'+port,to+'.'+inputs[i],phases[i]));
}

const phaseIn=['1','3','5'], phaseOut=['2','4','6'], windingIn=['U1','V1','W1'];

function feedMotor(b,contact) {
  threePhase(b,'F1',contact,phaseOut,phaseIn);
  threePhase(b,contact,'M1',phaseOut,windingIn);
}

function feedMains(b) {
  mains(b);
  breaker(b,'F1',374,192,3,32);
  threePhase(b,'U1','F1',['L1','L2','L3'],phaseIn);
}

function protectedControl(b,y) {
  dc(b,y);
  breaker(b,'F2',260,y);
  b.wire('G1.+24','F2.1');
  return 'F2.2';
}

function first() {
  const b=board('SCHÜTZ-01 · Steuer- und Hauptstromkreis','Betrieb einschalten. S1 halten: Das 24-V-Schütz schaltet die 230-V-Lampe E1.');
  mains(b);
  breaker(b,'F1',380,192);
  contactor(b,'Q1',630,192,'Steuer- und Lastkreis');
  lamp(b,'E1',940,204,true);
  dc(b,640);
  button(b,'S1',380,640,'Licht tasten');
  b.wire('U1.L1','F1.1',live);
  b.wire('F1.2','Q1.1',live);
  b.wire('Q1.2','E1.L',switched);
  b.path('G1.+24','S1.13');
  b.path('S1.14','Q1.A1');
  b.wire('Q1.A2','G1.0V',neutral);
  return b.project;
}

function starter(number,title,hold,overload=false) {
  const b=board(`SCHÜTZ-${number} · ${title}`,hold
    ? 'S1 startet, S2 stoppt. Versorgung aus- und einschalten: Der Motor bleibt bis zum nächsten Start aus.'
    : 'Betrieb einschalten. S1 gedrückt halten und loslassen. Der Motor läuft nur während des Tastens.');
  feedMains(b);
  contactor(b,'Q1',665,192,'Motorschütz');
  motor(b,overload?1250:975,204);
  if(overload){
    b.part('F3','overload',950,192,{ratedCurrent:3.4,delayOn:5});
    threePhase(b,'F1','Q1',phaseOut,phaseIn);
    threePhase(b,'Q1','F3',phaseOut,phaseIn);
    threePhase(b,'F3','M1',phaseOut,windingIn);
  } else feedMotor(b,'Q1');
  let supply=protectedControl(b,665);
  if(overload){
    b.wire(supply,'F3.95');
    supply='F3.96';
  }
  if(hold){
    button(b,'S2',485,676,'Motor aus',true);
    b.wire(supply,'S2.21');
    supply='S2.22';
    b.wire(supply,'Q1.13');
    b.wire('Q1.14','Q1.A1',switched);
  }
  button(b,'S1',hold?720:485,676,'Motor ein');
  b.wire(supply,'S1.13');
  b.wire('S1.14','Q1.A1',switched);
  b.wire('Q1.A2','G1.0V',neutral);
  return b.project;
}

function lights() {
  const b=board('SCHÜTZ-06 · Lampenverriegelung','S2 startet P1, S4 startet P2. S1 und S3 stoppen. Die gewählte Lampe sperrt die andere.');
  dc(b);
  breaker(b,'F3',270,192);
  const branches = [
    {i:1,other:2,stop:'S1',start:'S2',coil:[456,200],aux:[751,225],light:[733,686],off:[155,758],on:[151,1132]},
    {i:2,other:1,stop:'S3',start:'S4',coil:[1034,184],aux:[1461,221],light:[1399,670],off:[1041,698],on:[1045,1100]}
  ];
  for(const {i,other,stop,start,coil,aux,light,off,on} of branches){
    const q='Q'+i,p='P'+i;
    contactor(b,q,...coil,'Licht '+i);
    auxiliary(b,q,...aux,'Elektrische Verriegelung');
    lamp(b,p,...light);
    button(b,stop,...off,'Licht '+i+' aus',true);
    button(b,start,...on,'Licht '+i+' ein');
    b.wire('G1.+24',q+'.1');
    b.wire(q+'.2',p+'.X1',switched);
    b.wire('F3.2',stop+'.21');
    b.wire(stop+'.22',start+'.13');
    b.wire(stop+'.22',q+'.13');
    b.wire(start+'.14','Q'+other+'H.61');
    b.wire(q+'.14','Q'+other+'H.61');
    b.wire('Q'+other+'H.62',q+'.A1');
    b.wire(q+'.A2','G1.0V',neutral);
  }
  b.wire('G1.+24','F3.1');
  return b.project;
}

function starDelta() {
  const b=board('SCHÜTZ-10 · Stern-Dreieck','S1 startet im Stern. Nach 3 s fällt Q2 ab und Q3 schaltet auf Dreieck. S2 stoppt.');
  feedMains(b);
  contactor(b,'Q1',650,192,'Netzschütz');
  contactor(b,'Q2',940,192,'Sternschütz');
  contactor(b,'Q3',1230,192,'Dreieckschütz');
  motor(b,1540,206,false);
  b.part('F3','overload',1850,192,{ratedCurrent:3.4,delayOn:5});
  threePhase(b,'F1','Q1',phaseOut,phaseIn);
  threePhase(b,'Q1','F3',phaseOut,phaseIn);
  threePhase(b,'F3','M1',phaseOut,windingIn);
  threePhase(b,'M1','Q2',['U2','V2','W2'],phaseIn);
  b.wire('Q2.2','Q2.4',phases[1]);
  b.wire('Q2.4','Q2.6',phases[1]);
  threePhase(b,'F3','Q3',phaseOut,phaseIn);
  threePhase(b,'Q3','M1',phaseOut,['W2','U2','V2']);
  const supply=protectedControl(b,660);
  button(b,'S2',480,680,'Motor aus',true);
  button(b,'S1',710,680,'Start');
  timer(b,950,650,0,3,3);
  auxiliary(b,'Q2',1240,678,'Unterbricht Dreieck');
  auxiliary(b,'Q3',1530,678,'Unterbricht Stern');
  b.wire(supply,'F3.95');
  b.wire('F3.96','S2.21');
  b.wire('S2.22','S1.13');
  b.wire('S2.22','Q1.13');
  b.wire('S1.14','Q1.A1');
  b.wire('Q1.14','Q1.A1');
  b.wire('Q1.A1','K1.A1');
  b.wire('Q1.A1','K1.15');
  b.path('K1.16','Q3H.61');
  b.path('Q3H.62','Q2.A1');
  b.path('K1.18','Q2H.61');
  b.path('Q2H.62','Q3.A1');
  for(const q of ['Q1','Q2','Q3']) b.wire(q+'.A2','G1.0V',neutral);
  return b.project;
}

function staircase(threeWire) {
  const b=board('SCHÜTZ-11 · '+(threeWire?'Dreileiter':'Vierleiter'),threeWire
    ? 'Einen Taster kurz drücken. Während des Nachlaufs startet ein weiterer Tastendruck die Zeit nicht neu.'
    : 'Einen Taster kurz drücken. Während des Nachlaufs startet ein weiterer Tastendruck die Zeit neu.');
  mains(b);
  breaker(b,'F1',380,190);
  b.part('K1','staircase',655,190,{threeWire,delayOn:3});
  b.wire('U1.L1','F1.1',live);
  b.wire('F1.2','K1.L',live);
  b.wire('U1.N','K1.N',neutral);
  for(let i=1;i<=3;i++){
    const x=920+(i-1)*260,s='S'+i,e='E'+i;
    button(b,s,x,192,'Treppenlicht');
    lamp(b,e,x,610,true);
    b.wire('F1.2',s+'.13',live);
    b.wire(s+'.14','K1.'+(threeWire?'OUT':'T'),switched);
    b.wire('K1.OUT',e+'.L',switched);
  }
  return b.project;
}

export const schuetzExamples = [
  ['01','SCHÜTZ-01 · Steuer- und Hauptstromkreis',first],
  ['02','SCHÜTZ-02 · Motorstarter mit Motorschutz',()=>starter('02','Motorstarter mit Motorschutz',true,true)],
  ['04','SCHÜTZ-04 · Tippbetrieb',()=>starter('04','Tippbetrieb',false)],
  ['05','SCHÜTZ-05 · Selbsthaltung',()=>starter('05','Selbsthaltung',true)],
  ['06','SCHÜTZ-06 · Lampenverriegelung',lights],
  ['07','SCHÜTZ-07 · Wendeschütz',()=>structuredClone(savedSchuetzExamples['07'])],
  ['08-ein','SCHÜTZ-08 · Einschaltverzögert',()=>structuredClone(savedSchuetzExamples['08-ein'])],
  ['08-aus','SCHÜTZ-08 · Ausschaltverzögert',()=>structuredClone(savedSchuetzExamples['08-aus'])],
  ['08-kombiniert','SCHÜTZ-08 · Ansprech- und rückfallverzögert',()=>structuredClone(savedSchuetzExamples['08-kombiniert'])],
  ['08-blink','SCHÜTZ-08 · Blinkfunktion (2 Hz)',()=>structuredClone(savedSchuetzExamples['08-blink'])],
  ['09','SCHÜTZ-09 · Verzögertes Einschalten',()=>structuredClone(savedSchuetzExamples['09'])],
  ['10','SCHÜTZ-10 · Stern-Dreieck',starDelta],
  ['11-drei','SCHÜTZ-11 · Treppenlicht Dreileiter',()=>staircase(true)],
  ['11-vier','SCHÜTZ-11 · Treppenlicht Vierleiter',()=>staircase(false)]
];
