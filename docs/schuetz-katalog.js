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
]);

export function wiringWorkshopUrl(id, classroom = false) {
  const query = new URLSearchParams({schaltung: id});
  if (classroom) query.set('unterricht', '1');
  return 'verdrahtungswerkstatt.html?' + query;
}
