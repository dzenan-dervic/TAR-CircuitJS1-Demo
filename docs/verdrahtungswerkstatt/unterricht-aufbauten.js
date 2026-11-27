const line = '#6b3f24';
const neutral = '#347caa';
const switched = '#b98c24';
const travellers = ['#384540', '#8d9296'];
const phases = [line, ...travellers];

function board(title, instruction, poles = 1, ratedCurrent = poles === 1 ? 16 : 10) {
  const modules = [
    {id:'N1', type:'note', x:56, y:38, noteStyle:'label', noteSize:28, noteWidth:1400, noteText:title},
    {id:'N2', type:'note', x:56, y:89, noteStyle:'label', noteSize:17, noteWidth:1400, noteText:instruction},
  ];
  const wires = [];
  const part = (id, type, x, y, options = {}) => modules.push({id, type, x, y, ...options});
  const wire = (a, b, color = line) => wires.push({a, b, color});
  part('U1', 'mains', 56, 190, {voltage:400});
  part('F1', 'breaker', 390, 190, {poles, ratedCurrent, characteristic:poles === 3 ? 'D' : 'B', protectionOn:true});
  for (let i = 0; i < poles; i++) wire('U1.L' + (i + 1), 'F1.' + (i * 2 + 1), phases[i]);
  return {part, wire, project:{modules, wires, powered:false}};
}

function load(b, id, type, x, y, feed) {
  b.part(id, type, x, y, type === 'lamp' ? {nomV:230, nomP:60} : {});
  b.wire(feed, id + '.L', type === 'lamp' ? switched : line);
  b.wire('U1.N', id + '.N', neutral);
  b.wire('U1.PE', id + '.PE', 'pe');
}

function rocker(b, id, x, y, description) {
  b.part(id, 'start', x, y, {latching:true, description});
}

function single(title, socket) {
  const b = board(title, 'Betrieb einschalten. Q1 schaltet E1.' + (socket ? ' Die Steckdose X1 bleibt unabhängig davon versorgt.' : ''));
  rocker(b,'Q1',650,190,'Ausschalter');
  b.wire('F1.2','Q1.13');
  load(b,'E1','lamp',930,200,'Q1.14');
  if (socket) load(b,'X1','socket',650,640,'F1.2');
  return b.project;
}

function changeover(title, arrangement) {
  const cross = arrangement === 'cross';
  const economy = arrangement === 'economy';
  const b = board(title,'Betrieb einschalten. Jede der Wippen schaltet E1 um.');
  b.part('S1','changeover',610,190,{selection:0});
  b.part('S2',cross?'crossover':'changeover',890,190,{selection:0});
  const last = cross ? 'S3' : 'S2';
  if (cross) b.part('S3','changeover',1170,190,{selection:0});
  if (economy) {
    b.wire('F1.2','S1.1');
    b.wire('S1.1','S2.1');
    b.wire('S1.L','S2.L',travellers[0]);
    b.wire('S1.2','S2.2',travellers[1]);
    load(b,'E1','lamp',1170,200,'S2.2');
  } else {
    b.wire('F1.2','S1.L');
    b.wire('S1.1','S2.1',travellers[0]);
    b.wire('S1.2','S2.2',travellers[1]);
    if (cross) {
      b.wire('S2.3','S3.1',travellers[0]);
      b.wire('S2.4','S3.2',travellers[1]);
    }
    load(b,'E1','lamp',cross?1450:1170,200,last+'.L');
  }
  if (!cross) load(b,'ST1','socket',610,630,'F1.2');
  if (economy) load(b,'ST2','socket',890,630,'F1.2');
  return b.project;
}

function series(title) {
  const b = board(title,'S1a und S1b bilden die beiden Wippen des Serienschalters ab. Jede schaltet eine eigene Leuchte.');
  for (const [id, lamp, y] of [['S1a','E1',190],['S1b','E2',630]]) {
    rocker(b,id,650,y,'Wippe für ' + lamp);
    b.wire('F1.2',id+'.13');
    load(b,lamp,'lamp',930,y+10,id+'.14');
  }
  return b.project;
}

function directMotor(title, delta = false) {
  const b = board(title,delta
    ? 'Betrieb einschalten. Der Motor läuft im Dreieck mit vertauschter Phasenfolge.'
    : 'Betrieb einschalten. Die drei Außenleiter versorgen den Motor in Sternschaltung.',3);
  b.part('M1','acmotor',705,185,{ratedKw:1.5,eta:.8,nominalDelta:delta});
  b.wire('U1.PE','M1.PE','pe');
  (delta?['V1','U1','W1']:['U1','V1','W1']).forEach((port,i) => {
    b.wire('F1.'+(i*2+2),'M1.'+port,phases[i]);
  });
  if (delta) {
    for (const [a,bPort] of [['U1','W2'],['V1','U2'],['W1','V2']]) b.wire('M1.'+a,'M1.'+bPort,switched);
  } else {
    b.wire('M1.U2','M1.V2',switched);
    b.wire('M1.V2','M1.W2',switched);
  }
  return b.project;
}

function starDeltaLesson(title, overloadPosition) {
  const automatic = overloadPosition !== undefined;
  const b = board(title,automatic
    ? 'S2 startet im Stern. Nach 5 s folgt Dreieck. S1 stoppt. Bei Überlast leuchtet P1.'
    : 'Schaltfolge wie im Schaltplan. S1 Stern EIN, S2 Netz EIN, S1 Stern AUS, S3 Dreieck EIN.',3,automatic?32:10);
  b.part('M1','acmotor',1440,190,{ratedKw:automatic?15:1.5,eta:.8,nominalDelta:true});
  b.wire('U1.PE','M1.PE','pe');
  for (const [id,x,label] of [['Q1',930,'Stern'],['Q2',630,'Netz'],['Q3',1180,'Dreieck']]) {
    b.part(id,'contactor',x,190,{nomV:24,nomP:4,description:label});
  }
  b.part('Q1H','auxiliary',930,625,{parent:'Q1',description:'Sperrt Dreieck'});
  b.part('Q3H','auxiliary',1180,625,{parent:'Q3',description:'Sperrt Stern'});
  if (automatic) {
    b.part('F3','overload',1740,190,{ratedCurrent:overloadPosition==='A'?32:18.5,delayOn:5});
  }
  for (const [i,[inPort,outPort,deltaPort]] of [['U1','U2','W2'],['V1','V2','U2'],['W1','W2','V2']].entries()) {
    const input = i*2+1, output = input+1;
    const upstream = overloadPosition === 'A' ? 'F3.'+output : 'F1.'+output;
    if (overloadPosition === 'A') b.wire('F1.'+output,'F3.'+input,phases[i]);
    b.wire(upstream,'Q2.'+input,phases[i]);
    b.wire('Q2.'+output,'M1.'+inPort,phases[i]);
    b.wire('Q2.'+output,'Q3.'+input,phases[i]);
    const windingEnd = 'M1.'+outPort;
    const returnPoint = overloadPosition === 'B' ? 'F3.'+output : windingEnd;
    if (overloadPosition === 'B') b.wire(windingEnd,'F3.'+input,phases[i]);
    b.wire(returnPoint,'Q1.'+input,phases[i]);
    const deltaIndex = ['U2','V2','W2'].indexOf(deltaPort);
    b.wire('Q3.'+output,overloadPosition === 'B' ? 'F3.'+(deltaIndex*2+2) : 'M1.'+deltaPort,phases[i]);
  }
  b.wire('Q1.2','Q1.4');
  b.wire('Q1.4','Q1.6');
  b.part('G1','psu',56,620,{voltage:24});
  for (const id of ['Q1','Q2','Q3']) b.wire(id+'.A2','G1.0V',neutral);
  b.wire('Q3H.62','Q1.A1',switched);
  b.wire('Q1H.62','Q3.A1',switched);
  if (automatic) {
    b.part('S1','stop',390,625,{description:'Stopp'});
    b.part('S2','start',650,625,{description:'Start'});
    b.part('K1','timer',1480,625,{nomV:24,nomP:2,timerMode:0,delayOn:5,delayOff:.25});
    b.part('P1','pilot',1740,630,{nomV:24,nomP:2,signalColor:'red',description:'Überlast'});
    b.wire('G1.+24','F3.95');
    b.wire('F3.96','S1.21');
    b.wire('S1.22','S2.13');
    b.wire('S1.22','Q2.13');
    b.wire('S2.14','Q2.A1',switched);
    b.wire('Q2.14','Q2.A1',switched);
    b.wire('Q2.A1','K1.A1',switched);
    b.wire('Q2.A1','K1.15',switched);
    b.wire('K1.A2','G1.0V',neutral);
    b.wire('K1.16','Q3H.61',switched);
    b.wire('K1.18','Q1H.61',switched);
    b.wire('G1.+24','F3.97');
    b.wire('F3.98','P1.X1',switched);
    b.wire('P1.X2','G1.0V',neutral);
  } else {
    for (const [id,x,label,output] of [['S1',390,'Stern','Q3H.61'],['S2',590,'Netz','Q2.A1'],['S3',790,'Dreieck','Q1H.61']]) {
      rocker(b,id,x,875,label);
      b.wire('G1.+24',id+'.13');
      b.wire(id+'.14',output,switched);
    }
  }
  return b.project;
}

export const wiringExamples = [
  ['inst-01','INST-01 · Installationsverdrahtung im Kinderzimmer',title => single(title,true)],
  ['inst-02','INST-02 · Ausschaltung (aufgelöst)',title => single(title,false)],
  ['inst-03','INST-03 · Ausschaltung (zusammenhängend)',title => single(title,true)],
  ['inst-05','INST-05 · Wechselschaltung',title => changeover(title,'regular')],
  ['inst-05-plan','INST-05 · Wechselschaltung (Installationsschaltplan)',title => changeover(title,'regular')],
  ['inst-06','INST-06 · Sparwechselschaltung',title => changeover(title,'economy')],
  ['inst-06-plan','INST-06 · Sparwechselschaltung (Installationsschaltplan)',title => changeover(title,'economy')],
  ['inst-07','INST-07 · Serienschaltung',series],
  ['inst-07-plan','INST-07 · Serienschaltung (Installationsschaltplan)',series],
  ['inst-kreuz','Installation · Kreuzschaltung',title => changeover(title,'cross')],
  ['inst-kreuz-plan','Installation · Kreuzschaltung (Installationsschaltplan)',title => changeover(title,'cross')],
  ['motor-01','MOTOR-01 · Drehfeld und Phasenfolge',title => directMotor(title)],
  ['motor-02-stern','MOTOR-02 · Stern mit Rechtslauf',title => directMotor(title)],
  ['motor-02-dreieck','MOTOR-02 · Dreieck mit Linkslauf',title => directMotor(title,true)],
  ['motor-04','MOTOR-04 · Stern-Dreieck-Prinzip',starDeltaLesson],
  ['anh-07-a','ANH-07 · Überlastrelais im Außenleiter',title => starDeltaLesson(title,'A')],
  ['anh-07-b','ANH-07 · Überlastrelais im Wicklungsstrang',title => starDeltaLesson(title,'B')],
].map(([id,title,create]) => [id,title,() => create(title)]);
