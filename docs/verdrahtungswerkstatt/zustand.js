// Gemeinsame Daten bleiben hier, damit alle Bereiche denselben aktuellen Stand sehen.
export function createContext() {
  const ctx = {state: {}, dom: {}, config: {}, services: {}};
  ctx.dom.$ = s => document.querySelector(s);
  ctx.dom.board = ctx.dom.$('#board');
  ctx.dom.svg = ctx.dom.$('#wires');
  ctx.dom.dots = ctx.dom.$('#wire-dots');
  ctx.dom.leads = ctx.dom.$('#leads');
  ctx.dom.viewport = ctx.dom.$('#viewport');
  ctx.config.NS = 'http://www.w3.org/2000/svg';
  ctx.config.BOARD_W = 6000;
  ctx.config.BOARD_H = 3600;
  ctx.config.contacts = {contactor:[['1','2','NO'],['3','4','NO'],['5','6','NO'],['13','14','NO']],auxiliary:[['53','54','NO'],['61','62','NC'],['71','72','NC'],['83','84','NO']]};
  ctx.config.types = {changeover:'WECHSELSCHALTER',crossover:'KREUZSCHALTER',junction:'ABZWEIGDOSE',connector:'VERBINDUNGSKLEMME',socket:'SCHUKO-STECKDOSE',potentiometer:'POTENTIOMETER',motion:'BEWEGUNGSMELDER',note:'NOTIZ',timer:'ZEITRELAIS',motorprotection:'MOTORSCHUTZSCHALTER',breaker:'SICHERUNG · LS',relay:'HILFSSCHÜTZ',voltmeter:'SPANNUNGSMESSGERÄT',ammeter:'STROMMESSGERÄT',contactor:'LEISTUNGSSCHÜTZ',auxiliary:'HILFSKONTAKTE',psu:'NETZTEIL',mains:'DREHSTROM 400 V',start:'TASTER',stop:'TASTER',motor:'MOTOR · DC',acmotor:'MOTOR · 3~ 400 V',lineardrive:'LINEARANTRIEB · 24 V DC',lamp:'LAMPE',pilot:'MELDELEUCHTE',emergency:'NOT-AUS · RASTEND',selector:'DREHSCHALTER · A–0–B',terminal:'REIHENKLEMME',rail:'POTENZIALSCHIENE'};
  ctx.state.modules = [];
  ctx.state.wires = [];
  ctx.state.history = [];
  ctx.state.future = [];
  ctx.state.pending = null;
  ctx.state.gesture = null;
  ctx.state.scale = 1;
  ctx.state.color = '#6b3f24';
  ctx.state.powered = false;
  ctx.state.toastTimer = undefined;
  ctx.state.persistTimer = undefined;
  ctx.state.booting = true;
  ctx.state.recoveryMemory = null;
  ctx.config.STORE_KEY = 'workshopRecovery:v1:egt-hmi-stecken';
  ctx.config.skipStore = new URLSearchParams(location.search).has('test')||new URLSearchParams(location.search).has('fresh');
  ctx.state.selectedIds = new Set();
  ctx.state.selectionMode = false;
  ctx.state.selectionKeyboard = false;
  ctx.state.electricalRequest = null;
  ctx.state.states = {};
  ctx.state.selectedId = null;
  ctx.state.nextZ = 2;
  ctx.state.wireTarget = null;
  ctx.state.selectedWire = null;
  ctx.state.wireClickAt = 0;
  ctx.state.wireClickTarget = null;
  ctx.state.camera = {x:0,y:0};
  ctx.state.viewDrag = null;
  ctx.state.handMode = false;
  ctx.state.spaceHeld = false;
  ctx.state.ignorePanClick = false;
  ctx.state.viewReady = false;
  ctx.state.engine = null;
  ctx.state.engineOk = false;
  ctx.state.iframeId = 0;
  ctx.state.simMode = 'aufbau';
  ctx.state.viewMode = 'stick';
  ctx.state.syncTimer = null;
  ctx.state.lastSnap = null;
  ctx.state.importSeq = 0;
  ctx.state.lastImportedSig = '';
  ctx.state.liveRaf = 0;
  ctx.state.editTarget = null;
  ctx.state.latchedFault = null;
  ctx.state.recoveryTime = null;
  ctx.state.showVoltage = false;
  ctx.state.showCurrent = false;
  ctx.state.splitFrac = 0.425;
  ctx.state.wireScope = 'selection';
  ctx.state.panelsBeforeCompare = null;
  ctx.state.engineImportError = '';
  ctx.config.VOLT_STORE = ctx.config.STORE_KEY+':showVoltage';
  ctx.config.CURRENT_STORE = ctx.config.STORE_KEY+':showCurrent';
  ctx.config.CURRENT_SPEED_STORE = ctx.config.STORE_KEY+':currentSpeed';
  ctx.config.LIVE_V = 2;
  ctx.config.I_DOT = 1e-4;
  ctx.config.DOT_SP = 48;
  ctx.config.DOT_SIZE = 8;
  ctx.config.DOT_SPEED = 0.25;
  ctx.config.DOT_BASE_MULT = 25;
  ctx.config.DOT_ADD_MAX = 12;
  ctx.state.dotPos = [];
  ctx.state.lastDotTime = null;
  ctx.state.currentSpeed = 25;
  ctx.config.signalColors = {green:['Grün','#37ce70'],red:['Rot','#f05252'],amber:['Gelb','#f4ca43'],blue:['Blau','#459af5'],white:['Weiß','#eff6fb'],orange:['Orange','#f79536']};
  ctx.config.buttonColors = {green:['Grün','#2f9a56','#fff','I'],red:['Rot','#c43328','#fff','O'],black:['Schwarz','#1d262c','#fff',''],white:['Weiß','#eef3f6','#1d262c','I'],yellow:['Gelb','#e6c21a','#1d262c','I']};
  ctx.config.tagPrefixes = {changeover:['S'],crossover:['S'],junction:['X'],connector:['X'],socket:['X'],potentiometer:['R'],motion:['B'],note:['N'],timer:['K'],motorprotection:['Q'],breaker:['F'],relay:['K'],voltmeter:['V'],ammeter:['A'],contactor:['Q','K'],psu:['G'],mains:['T','U'],motor:['M'],acmotor:['M'],lineardrive:['M'],lamp:['E'],pilot:['P'],start:['S'],stop:['S'],selector:['S'],emergency:['S'],terminal:['X'],rail:['X']};
  ctx.config.breakerRatings = [6,10,13,16,20,25,32,40,50,63];
  ctx.config.timerModes = ['Einschaltverzögert','Ausschaltverzögert','Ansprech- und rückfallverzögert','Blinkend'];
  ctx.config.noteTones = {cream:['Creme','#fff7d6'],white:['Weiß','#ffffff'],blue:['Blau','#e4f1fa'],green:['Grün','#e7f2e8']};
  ctx.config.badgePalette = [['Blau','#1d6fe8'],['Grün','#0e9f78'],['Magenta','#be185d'],['Orange','#c2410c'],['Violett','#6d4ad4'],['Rot','#b42318'],['Braun','#8a4b12']];
  ctx.state.driveFrames = new WeakMap();
  ctx.state.componentClipboard = null;
  ctx.state.clipboardPasteCount = 0;
  ctx.config.wirePalette = [['Braun · L1','#6b3f24'],['Schwarz · L2','#384540'],['Grau · L3','#8d9398'],['Blau · N','#347caa'],['Grün-Gelb · PE','pe'],['Rot','#bb4234']];
  ctx.state.customWire = '#7a4ea3';
  ctx.state.wirePanelContext = '';
  ctx.state.wirePanelHadRows = false;
  ctx.state.liveTimer = 0;
  ctx.state.faultPathRendered = null;
  ctx.dom.selectionRectangle = document.createElement('div');
  ctx.state.boardPointers = new Map();
  ctx.state.pinch = null;
  ctx.dom.workspace = ctx.dom.$('.workspace');
  ctx.config.newGlyph = {
    terminal:'<path d="M1 12h26v5H1" fill="#aebac1" stroke="#74848c"/><g fill="#cbd1d4" stroke="#809097">'+[3,10,17].map(x=>'<rect x="'+x+'" y="2" width="7" height="24" rx="1"/><circle cx="'+(x+3.5)+'" cy="6" r="2" fill="#78858c"/><rect x="'+(x+1)+'" y="10" width="5" height="5" fill="#fff"/><circle cx="'+(x+3.5)+'" cy="22" r="2" fill="#78858c"/>').join('')+'</g><path d="M13 17h8" stroke="#d84735" stroke-width="3"/>',
    changeover:`<rect x="1.5" y="1.5" width="25" height="25" rx="4" fill="#e5e8e6" stroke="#929d97"/><rect x="4" y="4" width="20" height="20" rx="2" fill="#fafcf9" stroke="#c3ccc5"/>
      <g fill="none" stroke="#30483d" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M7 7v5m14-5v5M14 21v-4l-7-5"/><path d="M6 12h2m12 0h2"/>
      <circle cx="7" cy="7" r="1.7" fill="#fafcf9"/><circle cx="21" cy="7" r="1.7" fill="#fafcf9"/><circle cx="14" cy="21" r="1.7" fill="#fafcf9"/></g>`,
    crossover:`<rect x="1.5" y="1.5" width="25" height="25" rx="4" fill="#e5e8e6" stroke="#929d97"/><rect x="4" y="4" width="20" height="20" rx="2" fill="#fafcf9" stroke="#c3ccc5"/>
      <g fill="none" stroke="#30483d" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M7 7v2l5 3.6m4 2.8L21 19v2M21 7v2L7 19v2"/>
      <circle cx="7" cy="7" r="1.7" fill="#fafcf9"/><circle cx="21" cy="7" r="1.7" fill="#fafcf9"/><circle cx="7" cy="21" r="1.7" fill="#fafcf9"/><circle cx="21" cy="21" r="1.7" fill="#fafcf9"/></g>`,
    junction:`<path d="M1 11h4v6H1m22-6h4v6h-4" fill="#b4bcbc" stroke="#859290" stroke-width=".8"/><rect x="3" y="2" width="22" height="24" rx="4" fill="#dfe3e1" stroke="#84908c"/><rect x="5" y="4" width="18" height="20" rx="2" fill="#b8c2bd" stroke="#7e8e85"/><path d="M7 7h14M7 21h14" stroke="#e8eeea"/><circle cx="7" cy="6" r="1" fill="#718479"/><circle cx="21" cy="22" r="1" fill="#718479"/>`,
    connector:`<rect x="1" y="1" width="26" height="26" rx="3" fill="#dce8e2" stroke="#92a79d"/><path d="M3 21h22" stroke="#bb945c" stroke-width="1.5"/>${[3,11,19].map(x=>'<rect x="'+x+'" y="3" width="6" height="16" rx="2" fill="#f18729" stroke="#b65c15"/><path d="M'+(x+1)+' 5h4" stroke="#dce5df" stroke-width="2"/><rect x="'+(x+1)+'" y="21" width="4" height="4" rx="1" fill="#304137" stroke="#aabbb1"/>').join('')}`,
    socket:'<rect x="2" y="2" width="24" height="24" rx="4" fill="#fafbf6" stroke="#a4aaa4"/><circle cx="14" cy="14" r="9" fill="#e5e7df" stroke="#a4aaa4"/><circle cx="10" cy="14" r="1.8" fill="#334239"/><circle cx="18" cy="14" r="1.8" fill="#334239"/><path d="M12 5h4m-4 18h4" stroke="#797d68" stroke-width="2.5"/>',
    potentiometer:'<rect width="28" height="28" rx="3" fill="#e8eadc"/><circle cx="14" cy="13" r="9" fill="#6b786e"/><circle cx="14" cy="13" r="6" fill="#263b31"/><path d="M14 7v5" stroke="#fff" stroke-width="2"/><path d="M5 25h18" stroke="#536b5b"/>',
    switch:'<rect width="28" height="28" rx="3" fill="#e5e9e0"/><rect x="4" y="2" width="20" height="23" rx="3" fill="#829087"/><rect x="6" y="4" width="16" height="19" rx="2" fill="#21372c"/><path d="M7 5h14v9H7z" fill="#40564b"/><path d="M14 7v4" stroke="#f4f7f4" stroke-width="1.6" stroke-linecap="round"/><circle cx="14" cy="18" r="2.2" fill="none" stroke="#d1ddd4" stroke-width="1.3"/><path d="M9 25v2m10-2v2" stroke="#43584a" stroke-width="2"/>',
    motion:'<rect width="28" height="28" rx="3" fill="#e0e8e1"/><rect x="4" y="3" width="20" height="22" rx="4" fill="#fff" stroke="#94a599"/><path d="M7 16a7 8 0 0 1 14 0v3H7z" fill="#dce8dc" stroke="#a7b9aa"/><circle cx="20" cy="7" r="1.5" fill="#008375"/>',
    note:'<rect x="3" y="2" width="22" height="24" rx="2" fill="#fff1bb" stroke="#a79966"/><path d="M7 8h14M7 13h14M7 18h9" stroke="#716943" stroke-width="1.5"/>',
    label:'<rect x="2" y="6" width="24" height="17" rx="2" fill="#e3eeea"/><path d="M8 10h12M14 10v10M11 20h6" stroke="#285b4e" stroke-width="2"/>',
    frame:'<path d="M3 5h5M12 5h4M20 5h5M3 12v4M25 12v4M3 21h5M12 21h4M20 21h5" stroke="#285b4e" stroke-width="2" stroke-linecap="round"/><path d="M8 10h12" stroke="#285b4e" stroke-width="1.5"/>'
  };
  ctx.state.saved = null;
  return ctx;
}
