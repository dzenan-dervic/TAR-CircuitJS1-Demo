export const referenceCircuits = new Map([
  ['01', 'egt-schuetz-01.txt'],
  ['02', 'egt-schuetz-02.txt'],
  ['04', 'egt-schuetz-04.txt'],
  ['05', 'egt-schuetz-05.txt'],
  ['06', 'egt-schuetz-06.txt'],
  ['07', 'egt-schuetz-07.txt'],
  ['08-ein', 'egt-zeitrelais-ein.txt'],
  ['08-aus', 'egt-zeitrelais-aus.txt'],
  ['08-kombiniert', 'egt-zeitrelais-kombiniert.txt'],
  ['08-blink', 'egt-zeitrelais-blink.txt'],
  ['09', 'egt-schuetz-09.txt'],
  ['10', 'egt-stern-dreieck.txt'],
  ['11-drei', 'egt-schuetz-11-dreileiter.txt'],
  ['11-vier', 'egt-schuetz-11-vierleiter.txt'],
  ['inst-01', 'egt-inst-01.txt'],
  ['inst-02', 'egt-inst-02.txt'],
  ['inst-03', 'egt-inst-03.txt'],
  ['inst-05', 'egt-inst-05.txt'],
  ['inst-05-plan', 'egt-inst-05-uebersicht.txt'],
  ['inst-06', 'egt-inst-06.txt'],
  ['inst-06-plan', 'egt-inst-06-uebersicht.txt'],
  ['inst-07', 'egt-inst-07.txt'],
  ['inst-07-plan', 'egt-inst-07-uebersicht.txt'],
  ['inst-kreuz', 'egt-inst-kreuz.txt'],
  ['inst-kreuz-plan', 'egt-inst-kreuz-uebersicht.txt'],
  ['motor-01', 'egt-motor-01-drehfeld.txt'],
  ['motor-02-stern', 'egt-motor-02-stern-rechts.txt'],
  ['motor-02-dreieck', 'egt-motor-02-dreieck-links.txt'],
  ['motor-04', 'egt-motor-04-prinzip.txt'],
  ['anh-07-a', 'egt-anh-07-a.txt'],
  ['anh-07-b', 'egt-anh-07-b.txt'],
]);

export function wiringWorkshopUrl(id, classroom = false) {
  const query = new URLSearchParams({schaltung: id});
  if (classroom) query.set('unterricht', '1');
  return 'verdrahtungswerkstatt.html?' + query;
}
