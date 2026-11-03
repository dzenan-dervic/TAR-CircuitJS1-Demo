// Auch ein blockiertes Startmodul soll eine lesbare Meldung hinterlassen.
(() => {
  let failure = null;
  function show() {
    if (!failure || !document.getElementById('failure')) return;
    const note = document.getElementById('failure');
    note.hidden = false; note.textContent = failure;
    document.getElementById('current').textContent = 'Start nicht möglich';
    document.getElementById('start').disabled = true;
  }
  addEventListener('error', event => {
    if (window.__benchmarkReady) return;
    const resource = event.target?.src;
    if (resource || event.target === window) {
      failure = 'Eine Benchmark-Startdatei konnte nicht geladen werden. Seite neu laden; bei erneutem Fehler Netzwerkfreigabe und Website-Paket prüfen.';
      show();
    }
  }, true);
  addEventListener('DOMContentLoaded', show);
  setTimeout(() => {
    if (!window.__benchmarkReady) {
      failure = 'Die Benchmark-Oberfläche ist nach 30 Sekunden noch nicht bereit. Netzwerkfreigabe, blockierte Dateien oder das Website-Paket prüfen und die Seite erneut laden.';
      show();
    }
  }, 30000);
})();
