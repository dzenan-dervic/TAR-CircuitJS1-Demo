// Nur die Testfenster bekommen flüchtigen Speicher. Echte Projekte bleiben unangetastet.
(() => {
  const events=[];
  const record=(kind,resource)=>events.push({kind,resource:resource?String(resource).split(/[\\/]/).pop().split(/[?#]/)[0].slice(0,100):null});
  window.__benchmarkEvents=events;
  addEventListener('error',e=>record(e.target!==window?'resource-error':'javascript-error',e.target?.src||e.target?.href||e.filename),true);
  addEventListener('unhandledrejection',()=>record('unhandled-rejection'));
  addEventListener('securitypolicyviolation',e=>record('policy-block',e.blockedURI));
  try {
    const memory=()=>{const values=new Map();return {get length(){return values.size;},key:i=>[...values.keys()][i]??null,getItem:k=>values.get(String(k))??null,
      setItem:(k,v)=>values.set(String(k),String(v)),removeItem:k=>values.delete(String(k)),clear:()=>values.clear()};};
    for(const name of ['localStorage','sessionStorage']){
      const store=memory();Object.defineProperty(window,name,{configurable:false,get:()=>store});
      store.setItem('__probe','ok');if(window[name].getItem('__probe')!=='ok')throw Error();store.clear();
    }
    window.__benchmarkIsolation={ready:true,version:1};
  } catch {window.__benchmarkIsolation={ready:false,version:1};record('storage-isolation-failed');}
})();
